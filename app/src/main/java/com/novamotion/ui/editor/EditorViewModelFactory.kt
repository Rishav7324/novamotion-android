package com.novamotion.ui.editor

import android.content.Context
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.savedstate.SavedStateRegistryOwner

/**
 * Factory for EditorViewModel — injects SavedStateHandle and appContext.
 */
class EditorViewModelFactory(
    owner: SavedStateRegistryOwner,
    private val appContext: Context
) : AbstractSavedStateViewModelFactory(owner, null) {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        key: String,
        modelClass: Class<T>,
        handle: SavedStateHandle
    ): T {
        val viewModel = EditorViewModel(handle)
        viewModel.appContext = appContext.applicationContext
        return viewModel as T
    }
}
