package com.novamotion.ui.editor

import android.content.Context
import android.view.Choreographer
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novamotion.core.model.Layer
import com.novamotion.core.model.Project
import com.novamotion.core.project.ProjectManager
import com.novamotion.core.project.ProjectPersistenceManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Production-grade ViewModel for the Studio editor.
 *
 * Responsibilities:
 *  - Survive configuration changes (ViewModel lifecycle)
 *  - Authoritative playback clock via Choreographer.FrameCallback (no delay(16))
 *  - Project state mutations funneled through ProjectManager (undo/redo history)
 *  - Layer selection, playhead, playing/paused state
 */
class EditorViewModel(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    /** Application context — set by EditorViewModelFactory after creation. */
    var appContext: Context? = null

    /** Debounce job for auto-save (cancelled and restarted on every project mutation). */
    private var autoSaveJob: Job? = null

    /** Schedule an auto-save 1.5s after the last mutation. Cancels any pending save. */
    private fun scheduleSave(project: Project) {
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            delay(1500L)
            appContext?.let { ctx ->
                ProjectPersistenceManager.saveProject(ctx, project)
            }
        }
    }

    // ─── Project state ─────────────────────────────────────────────────────

    private val _project = MutableStateFlow<Project?>(null)
    val project: StateFlow<Project?> = _project.asStateFlow()

    // ─── Playback state ─────────────────────────────────────────────────────

    /** Authoritative playhead in milliseconds, driven by Choreographer. */
    private val _playheadMs = MutableStateFlow(0L)
    val playheadMs: StateFlow<Long> = _playheadMs.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    // ─── Selection ──────────────────────────────────────────────────────────

    private val _selectedLayerId = MutableStateFlow<String?>(null)
    val selectedLayerId: StateFlow<String?> = _selectedLayerId.asStateFlow()

    // ─── UI overlay flags ───────────────────────────────────────────────────

    private val _showCurveGraph = MutableStateFlow(false)
    val showCurveGraph: StateFlow<Boolean> = _showCurveGraph.asStateFlow()

    private val _showExportDialog = MutableStateFlow(false)
    val showExportDialog: StateFlow<Boolean> = _showExportDialog.asStateFlow()

    private val _showAddLayerSheet = MutableStateFlow(false)
    val showAddLayerSheet: StateFlow<Boolean> = _showAddLayerSheet.asStateFlow()

    private val _showEffectsSheet = MutableStateFlow(false)
    val showEffectsSheet: StateFlow<Boolean> = _showEffectsSheet.asStateFlow()

    private val _showTemplatesSheet = MutableStateFlow(false)
    val showTemplatesSheet: StateFlow<Boolean> = _showTemplatesSheet.asStateFlow()

    // ─── Export state ────────────────────────────────────────────────────────

    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()

    private val _exportProgress = MutableStateFlow(0f)
    val exportProgress: StateFlow<Float> = _exportProgress.asStateFlow()

    private val _exportResultPath = MutableStateFlow<String?>(null)
    val exportResultPath: StateFlow<String?> = _exportResultPath.asStateFlow()

    // ─── Timeline customization state ────────────────────────────────────────

    private val _pxPerMs = MutableStateFlow(0.1f) // 100px/sec default, range 0.02-0.3
    val pxPerMs: StateFlow<Float> = _pxPerMs.asStateFlow()

    private val _snapEnabled = MutableStateFlow(true)
    val snapEnabled: StateFlow<Boolean> = _snapEnabled.asStateFlow()

    private val _showWaveforms = MutableStateFlow(true)
    val showWaveforms: StateFlow<Boolean> = _showWaveforms.asStateFlow()

    private val _showThumbnails = MutableStateFlow(false)
    val showThumbnails: StateFlow<Boolean> = _showThumbnails.asStateFlow()

    private val _timelineTool = MutableStateFlow(TimelineTool.SELECT)
    val timelineTool: StateFlow<TimelineTool> = _timelineTool.asStateFlow()

    fun setPxPerMs(v: Float) { _pxPerMs.value = v.coerceIn(0.02f, 0.3f) }
    fun zoomBy(factor: Float, anchorMs: Long? = null) {
        val newZoom = (_pxPerMs.value * factor).coerceIn(0.02f, 0.3f)
        _pxPerMs.value = newZoom
    }
    fun toggleSnap() { _snapEnabled.update { !it } }
    fun setShowWaveforms(v: Boolean) { _showWaveforms.value = v }
    fun setShowThumbnails(v: Boolean) { _showThumbnails.value = v }
    fun setTimelineTool(tool: TimelineTool) { _timelineTool.value = tool }

    enum class TimelineTool { SELECT, RIPPLE, ROLL, SLIP, SLIDE }

    // ─── Choreographer clock ─────────────────────────────────────────────────

    /** Last nanoTime when a frame callback fired during playback. */
    private var lastFrameNanos = 0L
    /** Sub-ms accumulator to avoid truncation jitter at 120Hz */
    private var elapsedAccumMs = 0f

    private val choreographer: Choreographer by lazy { Choreographer.getInstance() }

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!_isPlaying.value) return
            val proj = _project.value ?: return
            if (lastFrameNanos != 0L) {
                val nowNanos = System.nanoTime()
                val lagMs = (nowNanos - frameTimeNanos) / 1_000_000L
                // Drop late frame if >90% of refresh period (prevents queue stuffing)
                val refreshMs = 16L // approx 60Hz; adaptive via Display refresh if needed
                if (lagMs > (refreshMs * 0.9f).toLong()) {
                    lastFrameNanos = frameTimeNanos
                    choreographer.postFrameCallback(this)
                    return
                }
                val elapsedFloat = (frameTimeNanos - lastFrameNanos) / 1_000_000f
                elapsedAccumMs += elapsedFloat
                val elapsedWhole = elapsedAccumMs.toLong()
                if (elapsedWhole > 0) {
                    elapsedAccumMs -= elapsedWhole
                    val newPlayheadMs = (_playheadMs.value + elapsedWhole) % proj.durationMs.coerceAtLeast(1L)
                    _playheadMs.value = newPlayheadMs
                }
            }
            lastFrameNanos = frameTimeNanos
            choreographer.postFrameCallback(this)
        }
    }

    // ─── Project operations ──────────────────────────────────────────────────

    fun loadProject(project: Project) {
        _project.value = project
        ProjectManager.setActiveProject(project)
        _playheadMs.value = 0L
        _isPlaying.value = false
        _selectedLayerId.value = project.layers.firstOrNull()?.id
    }

    fun updateProject(updated: Project, recordHistory: Boolean = true) {
        ProjectManager.updateActiveProject(updated, recordHistory)
        _project.value = updated
        // Schedule debounced auto-save (1.5s after last mutation)
        scheduleSave(updated)
    }

    fun addLayer(layer: Layer) {
        val current = _project.value ?: return
        val updated = current.copy(layers = current.layers + layer)
        updateProject(updated)
        _selectedLayerId.value = layer.id
    }

    fun removeLayer(layerId: String) {
        val current = _project.value ?: return
        val updated = current.copy(layers = current.layers.filter { it.id != layerId })
        updateProject(updated)
        if (_selectedLayerId.value == layerId) {
            _selectedLayerId.value = updated.layers.firstOrNull()?.id
        }
    }

    fun updateLayer(layer: Layer) {
        val current = _project.value ?: return
        val updated = current.copy(layers = current.layers.map { if (it.id == layer.id) layer else it })
        updateProject(updated)
    }

    /**
     * Splits target or selected layer at the current playhead position.
     * Accurately splits keyframe curves and media in/out offsets.
     */
    fun splitLayerAtPlayhead(targetLayerId: String? = null) {
        val current = _project.value ?: return
        val playhead = _playheadMs.value
        val layerId = targetLayerId ?: _selectedLayerId.value
        val layerToSplit = if (layerId != null) {
            current.layers.find { it.id == layerId }
        } else {
            // Fallback: find topmost active layer at current playhead
            current.layers.lastOrNull { it.isActiveAt(playhead) }
        } ?: return

        val splitResult = com.novamotion.core.timeline.TimelineOperations.splitLayer(layerToSplit, playhead) ?: return
        val (part1, part2) = splitResult

        val updatedLayers = current.layers.flatMap { layer ->
            if (layer.id == layerToSplit.id) listOf(part1, part2) else listOf(layer)
        }
        updateProject(current.copy(layers = updatedLayers))
        _selectedLayerId.value = part2.id
    }

    /**
     * Trims in-point (head) of a layer.
     */
    fun trimLayerHead(layerId: String, newStartTimeMs: Long) {
        val current = _project.value ?: return
        val layer = current.layers.find { it.id == layerId } ?: return
        val trimmed = com.novamotion.core.timeline.TimelineOperations.trimLayerHead(layer, newStartTimeMs)
        updateLayer(trimmed)
    }

    /**
     * Trims out-point (tail) of a layer.
     */
    fun trimLayerTail(layerId: String, newEndTimeMs: Long) {
        val current = _project.value ?: return
        val layer = current.layers.find { it.id == layerId } ?: return
        val trimmed = com.novamotion.core.timeline.TimelineOperations.trimLayerTail(layer, newEndTimeMs)
        updateLayer(trimmed)
    }

    /**
     * Duplicates a layer with a new ID.
     */
    fun duplicateLayer(layerId: String) {
        val current = _project.value ?: return
        val layer = current.layers.find { it.id == layerId } ?: return
        val duplicated = com.novamotion.core.timeline.TimelineOperations.duplicateLayer(layer)
        addLayer(duplicated)
    }

    fun undo() {
        if (ProjectManager.undo()) {
            _project.value = ProjectManager.activeProject.value
        }
    }

    fun redo() {
        if (ProjectManager.redo()) {
            _project.value = ProjectManager.activeProject.value
        }
    }

    // ─── Playback control ────────────────────────────────────────────────────

    fun play() {
        if (_isPlaying.value) return
        lastFrameNanos = 0L
        elapsedAccumMs = 0f
        _isPlaying.value = true
        choreographer.postFrameCallback(frameCallback)
    }

    fun pause() {
        _isPlaying.value = false
        choreographer.removeFrameCallback(frameCallback)
        lastFrameNanos = 0L
        elapsedAccumMs = 0f
    }

    fun togglePlayback() {
        if (_isPlaying.value) pause() else play()
    }

    fun seekTo(ms: Long) {
        val proj = _project.value ?: return
        _playheadMs.value = ms.coerceIn(0L, proj.durationMs)
    }

    fun stepFrame(forward: Boolean = true) {
        val proj = _project.value ?: return
        val frameDurationMs = (1000f / proj.fps.coerceAtLeast(1)).toLong().coerceAtLeast(1L)
        val current = _playheadMs.value
        val stepped = if (forward) current + frameDurationMs else current - frameDurationMs
        _playheadMs.value = stepped.coerceIn(0L, proj.durationMs)
    }

    /** Frame-snapped time for magnetic guides */
    fun snapTimeToFrame(timeMs: Long, fps: Int = project.value?.fps ?: 60): Long {
        val frameMs = 1000f / fps.coerceAtLeast(1)
        return (kotlin.math.round(timeMs / frameMs) * frameMs).toLong()
    }

    // ─── Selection ───────────────────────────────────────────────────────────

    fun selectLayer(layerId: String?) {
        _selectedLayerId.value = layerId
    }

    // ─── UI overlay toggles ──────────────────────────────────────────────────

    fun toggleCurveGraph() { _showCurveGraph.update { !it } }
    fun toggleExportDialog(show: Boolean) { _showExportDialog.value = show }
    fun toggleAddLayerSheet(show: Boolean) { _showAddLayerSheet.value = show }
    fun toggleEffectsSheet(show: Boolean) { _showEffectsSheet.value = show }
    fun toggleTemplatesSheet(show: Boolean) { _showTemplatesSheet.value = show }

    // ─── Export state setters (called from export coroutine) ─────────────────

    fun setExporting(exporting: Boolean) { _isExporting.value = exporting }
    fun setExportProgress(progress: Float) { _exportProgress.value = progress }
    fun setExportResult(path: String?) { _exportResultPath.value = path }

    // ─── Lifecycle ───────────────────────────────────────────────────────────

    override fun onCleared() {
        super.onCleared()
        choreographer.removeFrameCallback(frameCallback)
    }
}
