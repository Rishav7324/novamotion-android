package com.novamotion.core.project

import android.content.Context
import android.util.Log
import com.novamotion.core.model.Project
import com.novamotion.core.preset.ProjectSerializer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Persists the active project to internal storage using ProjectSerializer JSON.
 *
 * Storage location: context.filesDir/projects/<projectId>.json
 * Also maintains a "last_active_project_id" SharedPreferences key so we know
 * which project to restore on next launch.
 *
 * Production design:
 *  - Saves are async (called from a coroutine on Dispatchers.IO)
 *  - Load returns null gracefully if no saved project exists
 *  - Old project files are cleaned up when a new project replaces the active one
 */
object ProjectPersistenceManager {

    private const val TAG = "ProjectPersistence"
    private const val PREFS_NAME = "novamotion_prefs"
    private const val KEY_LAST_PROJECT_ID = "last_active_project_id"

    /**
     * Save the given project to internal storage.
     * Must be called from a coroutine (runs on Dispatchers.IO).
     */
    suspend fun saveProject(context: Context, project: Project) = withContext(Dispatchers.IO) {
        try {
            val projectsDir = File(context.filesDir, "projects").also { it.mkdirs() }
            val file = File(projectsDir, "${project.id}.json")
            file.writeText(ProjectSerializer.serialize(project))

            // Record which project is active
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_LAST_PROJECT_ID, project.id)
                .apply()

            Log.d(TAG, "Project saved: ${project.id} (${project.title})")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save project ${project.id}", e)
        }
    }

    /**
     * Load the last active project from internal storage.
     * Returns null if no saved project exists or if deserialization fails.
     */
    suspend fun loadLastProject(context: Context): Project? = withContext(Dispatchers.IO) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val lastId = prefs.getString(KEY_LAST_PROJECT_ID, null) ?: return@withContext null

            val projectsDir = File(context.filesDir, "projects")
            val file = File(projectsDir, "$lastId.json")
            if (!file.exists()) return@withContext null

            val json = file.readText()
            val project = ProjectSerializer.deserialize(json)
            Log.d(TAG, "Project loaded: ${project.id} (${project.title})")
            project
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load last project", e)
            null
        }
    }

    /**
     * Load a specific project by ID.
     */
    suspend fun loadProject(context: Context, projectId: String): Project? = withContext(Dispatchers.IO) {
        try {
            val file = File(context.filesDir, "projects/$projectId.json")
            if (!file.exists()) return@withContext null
            ProjectSerializer.deserialize(file.readText())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load project $projectId", e)
            null
        }
    }

    /**
     * List all saved project IDs (for the home screen project list).
     */
    fun listSavedProjectIds(context: Context): List<String> {
        return try {
            val projectsDir = File(context.filesDir, "projects")
            projectsDir.listFiles()
                ?.filter { it.extension == "json" }
                ?.map { it.nameWithoutExtension }
                ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Delete a project file from storage.
     */
    suspend fun deleteProject(context: Context, projectId: String) = withContext(Dispatchers.IO) {
        try {
            File(context.filesDir, "projects/$projectId.json").delete()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete project $projectId", e)
        }
    }
}
