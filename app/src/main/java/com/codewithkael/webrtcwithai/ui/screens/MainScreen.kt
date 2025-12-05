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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.codewithkael.webrtcwithai.R
import com.codewithkael.webrtcwithai.ui.components.SurfaceViewRendererComposable
import com.codewithkael.webrtcwithai.ui.viewmodel.MainViewModel
import com.codewithkael.webrtcwithai.utils.MyApplication

@Composable
fun MainScreen() {
    val viewModel: MainViewModel = hiltViewModel()
    val callState = viewModel.callState.collectAsState()
    val context = LocalContext.current
    var callId by remember { mutableStateOf("") }

    // Permissions
    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (!permissions.all { it.value }) {
            Toast.makeText(context, "Camera and Microphone permissions are required", Toast.LENGTH_SHORT).show()
        } else {
            viewModel.permissionsGranted()
        }
    }

    LaunchedEffect(Unit) {
        requestPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.CAMERA
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFEAEAEA))
            .padding(top = 28.dp)
    ) {
        // Top User ID Display
        Text(
            text = "Your ID: ${MyApplication.UserID}",
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            textAlign = TextAlign.Center,
            color = Color.Black,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium
        )

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

        if (callState.value){
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .weight(2.5f),
                horizontalArrangement = Arrangement.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {

                    // Remote Video
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(Color.White, RoundedCornerShape(12.dp))
                            .padding(6.dp)
                    ) {
                        Text(
                            text = "Remote",
                            modifier = Modifier.padding(4.dp),
                            fontWeight = FontWeight.Bold,
                            color = Color.DarkGray
                        )

                        SurfaceViewRendererComposable(
                            modifier = Modifier
                                .fillMaxSize(),
                            onSurfaceReady = { renderer ->
                                viewModel.initRemoteSurfaceView(renderer)
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Local Video
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(Color.White, RoundedCornerShape(12.dp))
                            .padding(6.dp)
                    ) {
                        Text(
                            text = "Local",
                            modifier = Modifier.padding(4.dp),
                            fontWeight = FontWeight.Bold,
                            color = Color.DarkGray
                        )

                        if (callState.value) {
                            SurfaceViewRendererComposable(
                                modifier = Modifier.fillMaxSize(),
                                onSurfaceReady = { renderer ->
                                    viewModel.startLocalStream(renderer)
                                }
                            )
                        }
                    }
                }
            }

        }

        // Footer
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.6f)
                .padding(bottom = 15.dp, start = 5.dp, end = 5.dp)
                .clickable {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/@codewithkael"))
                    context.startActivity(intent)
                }
                .background(color = Color(0xA4D6DFE5), shape = RoundedCornerShape(8.dp)),
            horizontalArrangement = Arrangement.Absolute.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
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
