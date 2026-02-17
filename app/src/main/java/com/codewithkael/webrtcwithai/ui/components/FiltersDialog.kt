package com.codewithkael.webrtcwithai.ui.components

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
    val scroll = rememberScrollState()

    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Filters") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(scroll),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
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
                    onToggle = { state.faceMesh = it }
                )

                FilterTile(
                    title = "Image Labeling",
                    subtitle = "Scene labels (ML Kit)",
                    checked = state.imageLabeling,
                    imageRes = R.drawable.ic_face_filter,
                    onToggle = { state.imageLabeling = it }
                )

                FilterTile(
                    title = "Object Detection",
                    subtitle = "Boxes + labels (ML Kit)",
                    checked = state.objectDetection,
                    imageRes = R.drawable.ic_face_filter,
                    onToggle = { state.objectDetection = it }
                )

                FilterTile(
                    title = "Pose Detection",
                    subtitle = "Skeleton + landmarks (ML Kit)",
                    checked = state.poseDetection,
                    imageRes = R.drawable.ic_face_filter,
                    onToggle = { state.poseDetection = it }
                )

                FilterTile(
                    title = "Text Recognition (OCR)",
                    subtitle = "Detect & draw text (ML Kit v2)",
                    checked = state.textRecognition,
                    imageRes = R.drawable.ic_face_filter, // replace later
                    onToggle = { state.textRecognition = it }
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
                        faceMesh = state.faceMesh,
                        objectDetection = state.objectDetection,
                        imageLabeling = state.imageLabeling,
                        poseDetection = state.poseDetection,
                        textRecognition = state.textRecognition
                    )
                )
            }) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text("Cancel") }
        }
    )
}
