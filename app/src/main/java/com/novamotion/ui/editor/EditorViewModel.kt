package com.novamotion.ui.editor

import android.view.Choreographer
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novamotion.core.model.Layer
import com.novamotion.core.model.Project
import com.novamotion.core.project.ProjectManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

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

    // ─── Choreographer clock ─────────────────────────────────────────────────

    /** Last nanoTime when a frame callback fired during playback. */
    private var lastFrameNanos = 0L

    private val choreographer: Choreographer by lazy { Choreographer.getInstance() }

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!_isPlaying.value) return

            val proj = _project.value ?: return

            if (lastFrameNanos != 0L) {
                // Compute actual elapsed time between display frames
                val elapsedMs = (frameTimeNanos - lastFrameNanos) / 1_000_000L
                val newPlayheadMs = (_playheadMs.value + elapsedMs) % proj.durationMs
                _playheadMs.value = newPlayheadMs
            }
            lastFrameNanos = frameTimeNanos

            // Schedule next frame
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
        _isPlaying.value = true
        choreographer.postFrameCallback(frameCallback)
    }

    fun pause() {
        _isPlaying.value = false
        choreographer.removeFrameCallback(frameCallback)
        lastFrameNanos = 0L
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
        val frameDurationMs = 1000L / proj.fps
        val current = _playheadMs.value
        val stepped = if (forward) current + frameDurationMs else current - frameDurationMs
        _playheadMs.value = stepped.coerceIn(0L, proj.durationMs)
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
