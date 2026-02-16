package com.codewithkael.webrtcwithai.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.codewithkael.webrtcwithai.R
import com.codewithkael.webrtcwithai.ui.components.FilterTile
import com.codewithkael.webrtcwithai.ui.components.LocationPicker
import com.codewithkael.webrtcwithai.ui.components.SurfaceViewRendererComposable
import com.codewithkael.webrtcwithai.ui.viewmodel.MainViewModel
import com.codewithkael.webrtcwithai.utils.FilterStorage
import com.codewithkael.webrtcwithai.utils.MyApplication
import com.codewithkael.webrtcwithai.utils.WatermarkStorage
import com.codewithkael.webrtcwithai.webrt.WebRTCFactory.WatermarkLocation


@Composable
fun MainScreen() {
    val viewModel: MainViewModel = hiltViewModel()
    val callState = viewModel.callState.collectAsState()
    val context = LocalContext.current
    var callId by remember { mutableStateOf("") }
    var menuExpanded by remember { mutableStateOf(false) }
    var showWmDialog by remember { mutableStateOf(false) }
    val initialCfg = remember { WatermarkStorage.load(context) }
    var wmUri by remember { mutableStateOf(initialCfg.uri?.let(Uri::parse)) }
    var wmLocation by remember { mutableStateOf(initialCfg.location) }
    var wmMarginDp by remember { mutableFloatStateOf(initialCfg.marginDp) }
    var wmSizeFraction by remember { mutableFloatStateOf(initialCfg.sizeFraction) }

    val pickWmLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            // Persist permission so WebRTCFactory can open it later
            context.contentResolver.takePersistableUriPermission(
                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            wmUri = uri
        }
    }

    var showFiltersDialog by remember { mutableStateOf(false) }

    val initialFilters =
        remember { FilterStorage.load(context) }
    var fltFace by remember { mutableStateOf(initialFilters.faceDetect) }
    var fltBlur by remember { mutableStateOf(initialFilters.blurBackground) }
    var fltWatermark by remember { mutableStateOf(initialFilters.watermark) }
    var fltFaceMesh by remember { mutableStateOf(initialFilters.faceMesh) }


    // Permissions
    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (!permissions.all { it.value }) {
            Toast.makeText(
                context, "Camera and Microphone permissions are required", Toast.LENGTH_SHORT
            ).show()
        } else {
            viewModel.permissionsGranted()
        }
    }

    LaunchedEffect(Unit) {
        requestPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFEAEAEA))
            .padding(top = 28.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Your ID: ${MyApplication.UserID}",
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Start,
                color = Color.Black,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )

            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "Options")
                }

                DropdownMenu(
                    expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(text = { Text("Watermark settings") }, onClick = {
                        menuExpanded = false
                        showWmDialog = true
                    })
                    DropdownMenuItem(text = { Text("Filters") }, onClick = {
                        menuExpanded = false
                        // refresh initial states each time you open
                        val cfg = FilterStorage.load(context)
                        fltFace = cfg.faceDetect
                        fltBlur = cfg.blurBackground
                        fltWatermark = cfg.watermark
                        fltFaceMesh = cfg.faceMesh
                        showFiltersDialog = true
                    })

                }
            }
        }


        // Input layout + call button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .background(Color.White, RoundedCornerShape(10.dp))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = callId,
                onValueChange = { callId = it },
                label = { Text("Enter User ID") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Button(
                onClick = { viewModel.sendStartCallSignal(callId) },
                modifier = Modifier.height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Call")
            }
        }

        if (callState.value) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(2.5f)
                    .padding(8.dp)
                    .background(Color.Black, RoundedCornerShape(12.dp))
            ) {
                // Remote video as full background
                SurfaceViewRendererComposable(
                    modifier = Modifier.fillMaxSize(), onSurfaceReady = { renderer ->
                        viewModel.initRemoteSurfaceView(renderer)
                    })

                // Local video as Picture-in-Picture (bottom-right)
                val pipWidthFraction = 0.42f // ~1/5 of screen width (recommended range: 0.20–0.28)
                SurfaceViewRendererComposable(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp)
                        .fillMaxWidth(pipWidthFraction)
                        // Keep a stable PiP shape (recommended). Choose ONE:
                        .aspectRatio(3f / 4f)   // common for front camera portrait preview
                        // .aspectRatio(16f / 9f) // if you want landscape PiP
                        .shadow(12.dp, RoundedCornerShape(14.dp))
                        .background(Color.Black, RoundedCornerShape(14.dp))
                        .padding(2.dp)
                        .background(Color.Black, RoundedCornerShape(12.dp)),
                    onSurfaceReady = { renderer ->
                        viewModel.startLocalStream(renderer)
                    })

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            brush = Brush.verticalGradient(
                                listOf(Color.Transparent, Color(0xAA000000))
                            )
                        )
                )
            }
        }
        if (showWmDialog) {
            AlertDialog(
                onDismissRequest = { showWmDialog = false },
                title = { Text("Watermark") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = { pickWmLauncher.launch(arrayOf("image/png", "image/jpeg")) },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Select PNG/JPG") }

                        // Location
                        Text("Location")
                        LocationPicker(
                            value = wmLocation, onChange = { wmLocation = it })

                        // Size
                        Text("Size: ${(wmSizeFraction * 100).toInt()}%")
                        Slider(
                            value = wmSizeFraction,
                            onValueChange = { wmSizeFraction = it },
                            valueRange = 0.05f..0.40f
                        )

                        // Margin
                        Text(
                            text = if (wmLocation == WatermarkLocation.CENTER) "Drop (dp): ${wmMarginDp.toInt()}"
                            else "Margin (dp): ${wmMarginDp.toInt()}"
                        )
                        Slider(
                            value = wmMarginDp,
                            onValueChange = { wmMarginDp = it },
                            valueRange = 0f..64f
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        WatermarkStorage.save(
                            context, WatermarkStorage.Config(
                                uri = wmUri?.toString(),
                                location = wmLocation,
                                marginDp = wmMarginDp,
                                sizeFraction = wmSizeFraction
                            )
                        )
                        viewModel.reloadWatermark() // <-- this updates WebRTCFactory instantly
                        showWmDialog = false
                    }) { Text("OK") }
                },
                dismissButton = {
                    TextButton(onClick = { showWmDialog = false }) { Text("Cancel") }
                })
        }
        if (showFiltersDialog) {
            AlertDialog(
                onDismissRequest = { showFiltersDialog = false },
                title = { Text("Filters") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                        FilterTile(
                            title = "Face Detect",
                            subtitle = "Draw face oval (ML Kit)",
                            checked = fltFace,
                            // Use any images you want here
                            imageRes = R.drawable.ic_face_filter,
                            onToggle = { fltFace = it }
                        )

                        FilterTile(
                            title = "Background Blur",
                            subtitle = "Blur background (segmentation)",
                            checked = fltBlur,
                            imageRes = R.drawable.ic_blur_filter,
                            onToggle = { fltBlur = it }
                        )

                        FilterTile(
                            title = "Watermark",
                            subtitle = "Show watermark on camera",
                            checked = fltWatermark,
                            imageRes = R.drawable.ic_watermark_filter,
                            onToggle = { fltWatermark = it }
                        )
                        FilterTile(
                            title = "Face Mesh",
                            subtitle = "468-point mesh overlay (ML Kit)",
                            checked = fltFaceMesh,
                            imageRes = R.drawable.ic_face_filter, // you can make a dedicated icon later
                            onToggle = { enabled ->
                                fltFaceMesh = enabled
                                // Optional: avoid running 2 face pipelines together
                                if (enabled) fltFace = false
                            }
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        FilterStorage.save(
                            context,
                            FilterStorage.Config(
                                faceDetect = fltFace,
                                blurBackground = fltBlur,
                                watermark = fltWatermark,
                                faceMesh = fltFaceMesh
                            )
                        )
                        viewModel.reloadFilters()     // apply instantly
                        showFiltersDialog = false
                    }) { Text("OK") }
                },
                dismissButton = {
                    TextButton(onClick = { showFiltersDialog = false }) { Text("Cancel") }
                }
            )
        }


        // Footer
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.6f)
                .padding(bottom = 15.dp, start = 5.dp, end = 5.dp)
                .clickable {
                    val intent = Intent(
                        Intent.ACTION_VIEW, "https://www.youtube.com/@codewithkael".toUri()
                    )
                    context.startActivity(intent)
                }
                .background(color = Color(0xA4D6DFE5), shape = RoundedCornerShape(8.dp)),
            horizontalArrangement = Arrangement.Absolute.Center,
            verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(id = R.drawable.youtube_logo),
                contentDescription = "YouTube Channel",
                modifier = Modifier
                    .size(54.dp)
                    .weight(1f),
                contentScale = ContentScale.Fit
            )
            Text(
                text = "To Learn how to create this app, Join my Youtube channel now !! \n www.Youtube.com/@CodeWithKael",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 5.dp, start = 10.dp, end = 10.dp)
                    .weight(6f),
                color = Color.Gray,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}
