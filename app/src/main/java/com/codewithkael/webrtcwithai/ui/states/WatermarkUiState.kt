package com.codewithkael.webrtcwithai.ui.states

import android.net.Uri
import androidx.compose.runtime.*
import com.codewithkael.webrtcwithai.utils.persistence.WatermarkStorage

class WatermarkUiState(initial: WatermarkStorage.Config) {
    var showDialog by mutableStateOf(false)

    var uriString by mutableStateOf(initial.uri)
        private set

    var location by mutableStateOf(initial.location)
    var marginDp by mutableFloatStateOf(initial.marginDp)
    var sizeFraction by mutableFloatStateOf(initial.sizeFraction)

    fun onPickedUri(uri: Uri) {
        uriString = uri.toString()
    }
}

@Composable
fun rememberWatermarkUiState(initial: WatermarkStorage.Config): WatermarkUiState {
    return remember(initial) { WatermarkUiState(initial) }
}
