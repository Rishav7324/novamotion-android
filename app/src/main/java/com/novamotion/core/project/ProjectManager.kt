package com.novamotion.core.project

import com.novamotion.core.model.Project
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

enum class AspectRatioPreset(val title: String, val width: Int, val height: Int, val description: String) {
    REELS_9_16("9:16", 1080, 1920, "Reels / TikTok / Shorts"),
    CINEMA_16_9("16:9", 1920, 1080, "YouTube / Landscape Video"),
    SQUARE_1_1("1:1", 1080, 1080, "Instagram Post"),
    FEED_4_5("4:5", 1080, 1350, "Portrait Feed"),
    ULTRAWIDE_21_9("21:9", 2560, 1080, "Cinematic Anamorphic")
}

object ProjectManager {

    private val _projects = MutableStateFlow<List<Project>>(emptyList())
    val projects: StateFlow<List<Project>> = _projects.asStateFlow()

    private val _activeProject = MutableStateFlow<Project?>(null)
    val activeProject: StateFlow<Project?> = _activeProject.asStateFlow()

    // Undo / Redo history stacks
    private val undoStack = mutableListOf<Project>()
    private val redoStack = mutableListOf<Project>()
    private const val MAX_HISTORY = 50

    fun createProject(
        title: String = "Untitled Project",
        aspectRatio: AspectRatioPreset = AspectRatioPreset.REELS_9_16,
        fps: Int = 60,
        durationMs: Long = 10000L
    ): Project {
        val newProj = Project(
            id = UUID.randomUUID().toString(),
            title = title,
            width = aspectRatio.width,
            height = aspectRatio.height,
            fps = fps,
            durationMs = durationMs,
            layers = emptyList()
        )
        _projects.value = listOf(newProj) + _projects.value
        setActiveProject(newProj)
        return newProj
    }

    fun setActiveProject(project: Project) {
        _activeProject.value = project
        undoStack.clear()
        redoStack.clear()
    }

    fun updateActiveProject(updated: Project, recordHistory: Boolean = true) {
        val current = _activeProject.value
        if (current != null && recordHistory) {
            if (undoStack.size >= MAX_HISTORY) undoStack.removeAt(0)
            undoStack.add(current)
            redoStack.clear()
        }
        _activeProject.value = updated
        _projects.value = _projects.value.map { if (it.id == updated.id) updated else it }
    }

    fun undo(): Boolean {
        if (undoStack.isEmpty()) return false
        val previous = undoStack.removeAt(undoStack.lastIndex)
        val current = _activeProject.value
        if (current != null) redoStack.add(current)
        _activeProject.value = previous
        _projects.value = _projects.value.map { if (it.id == previous.id) previous else it }
        return true
    }

    fun redo(): Boolean {
        if (redoStack.isEmpty()) return false
        val next = redoStack.removeAt(redoStack.lastIndex)
        val current = _activeProject.value
        if (current != null) undoStack.add(current)
        _activeProject.value = next
        _projects.value = _projects.value.map { if (it.id == next.id) next else it }
        return true
    }

    fun canUndo(): Boolean = undoStack.isNotEmpty()
    fun canRedo(): Boolean = redoStack.isNotEmpty()
}
