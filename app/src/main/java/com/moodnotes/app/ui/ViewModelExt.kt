package com.moodnotes.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.moodnotes.app.MoodNotesApplication
import com.moodnotes.app.data.MoodRepository

/**
 * 简单的 ViewModel 工厂：从 Application 容器里取 Repository。
 */
@Composable
inline fun <reified VM : ViewModel> moodNotesViewModel(
    crossinline create: (MoodRepository) -> VM,
): VM {
    val app = LocalContext.current.applicationContext as MoodNotesApplication
    return viewModel(factory = viewModelFactory {
        initializer { create(app.repository) }
    })
}
