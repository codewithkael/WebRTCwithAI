package com.codewithkael.webrtcwithai.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.codewithkael.webrtcwithai.R
import com.codewithkael.webrtcwithai.ui.states.FiltersUiState
import com.codewithkael.webrtcwithai.utils.persistence.FilterStorage

@Composable
fun FiltersDialog(
    state: FiltersUiState,
    onCancel: () -> Unit,
    onSave: (FilterStorage.Config) -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Filters") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                FilterTile(
                    title = "Face Detect",
                    subtitle = "Draw face oval (ML Kit)",
                    checked = state.faceDetect,
                    imageRes = R.drawable.ic_face_filter,
                    onToggle = { state.faceDetect = it }
                )

                FilterTile(
                    title = "Background Blur",
                    subtitle = "Blur background (segmentation)",
                    checked = state.blurBackground,
                    imageRes = R.drawable.ic_blur_filter,
                    onToggle = { state.blurBackground = it }
                )

                FilterTile(
                    title = "Watermark",
                    subtitle = "Show watermark on camera",
                    checked = state.watermark,
                    imageRes = R.drawable.ic_watermark_filter,
                    onToggle = { state.watermark = it }
                )

                FilterTile(
                    title = "Face Mesh",
                    subtitle = "468-point mesh overlay (ML Kit)",
                    checked = state.faceMesh,
                    imageRes = R.drawable.ic_face_filter,
                    onToggle = { enabled ->
                        state.faceMesh = enabled
                        if (enabled) state.faceDetect = false // optional mutual exclusion
                    }
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(
                    FilterStorage.Config(
                        faceDetect = state.faceDetect,
                        blurBackground = state.blurBackground,
                        watermark = state.watermark,
                        faceMesh = state.faceMesh
                    )
                )
            }) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text("Cancel") }
        }
    )
}
