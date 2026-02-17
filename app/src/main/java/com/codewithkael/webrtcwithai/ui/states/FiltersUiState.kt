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
    var imageLabeling by mutableStateOf(initial.imageLabeling)
    var objectDetection by mutableStateOf(initial.objectDetection)
    var poseDetection by mutableStateOf(initial.poseDetection)
    var textRecognition by mutableStateOf(initial.textRecognition)

    fun reloadFromStorage(context: Context) {
        val cfg = FilterStorage.load(context)
        faceDetect = cfg.faceDetect
        blurBackground = cfg.blurBackground
        watermark = cfg.watermark
        faceMesh = cfg.faceMesh
        imageLabeling = cfg.imageLabeling
        poseDetection = cfg.poseDetection
        textRecognition = cfg.textRecognition
    }
}

@Composable
fun rememberFiltersUiState(initial: FilterStorage.Config): FiltersUiState {
    return remember(initial) { FiltersUiState(initial) }
}
