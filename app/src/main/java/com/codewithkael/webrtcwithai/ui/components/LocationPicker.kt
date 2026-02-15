package com.codewithkael.webrtcwithai.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.codewithkael.webrtcwithai.webrt.WebRTCFactory.*

@Composable
fun LocationPicker(
    value: WatermarkLocation,
    onChange: (WatermarkLocation) -> Unit
) {
    val options = listOf(
        WatermarkLocation.TOP_LEFT,
        WatermarkLocation.TOP_RIGHT,
        WatermarkLocation.CENTER,
        WatermarkLocation.BOTTOM_LEFT,
        WatermarkLocation.BOTTOM_RIGHT
    )

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        options.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { opt ->
                    FilterChip(
                        selected = value == opt,
                        onClick = { onChange(opt) },
                        label = { Text(opt.name.replace("_", " ")) }
                    )
                }
            }
        }
    }
}
