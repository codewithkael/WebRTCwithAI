package com.codewithkael.webrtcwithai.ui.states

import android.content.Context
import androidx.compose.runtime.*
import com.codewithkael.webrtcwithai.utils.persistence.FilterStorage

class FiltersUiState(initial: FilterStorage.Config) {
    var showDialog by mutableStateOf(false)

    var faceDetect by mutableStateOf(initial.faceDetect)
    var blurBackground by mutableStateOf(initial.blurBackground)
    var watermark by mutableStateOf(initial.watermark)
    var faceMesh by mutableStateOf(initial.faceMesh)

    fun reloadFromStorage(context: Context) {
        val cfg = FilterStorage.load(context)
        faceDetect = cfg.faceDetect
        blurBackground = cfg.blurBackground
        watermark = cfg.watermark
        faceMesh = cfg.faceMesh
    }
}

@Composable
fun rememberFiltersUiState(initial: FilterStorage.Config): FiltersUiState {
    return remember(initial) { FiltersUiState(initial) }
}
