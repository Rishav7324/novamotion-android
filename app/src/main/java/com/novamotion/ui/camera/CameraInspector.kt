package com.novamotion.ui.camera

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novamotion.core.model.Camera3D
import com.novamotion.ui.theme.*

@Composable
fun CameraInspector(
    camera: Camera3D,
    onCameraChanged: (Camera3D) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(StudioSurface)
            .padding(12.dp)
    ) {
        Text(
            text = "3D Cinematic Camera & Depth of Field",
            color = TextPrimary,
            fontSize = 13.sp,
            style = MaterialTheme.typography.titleSmall
        )

        Spacer(modifier = Modifier.height(10.dp))

        // FOV Slider
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "FOV", color = TextSecondary, fontSize = 11.sp, modifier = Modifier.width(60.dp))
            Slider(
                value = camera.fovDegrees,
                onValueChange = { onCameraChanged(camera.copy(fovDegrees = it)) },
                valueRange = 25f..120f,
                colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = ElectricIndigo),
                modifier = Modifier.weight(1f)
            )
            Text(text = "${camera.fovDegrees.toInt()}°", color = TextMuted, fontSize = 11.sp)
        }

        // Camera Roll Slider
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "Roll", color = TextSecondary, fontSize = 11.sp, modifier = Modifier.width(60.dp))
            Slider(
                value = camera.rollAngle,
                onValueChange = { onCameraChanged(camera.copy(rollAngle = it)) },
                valueRange = -90f..90f,
                colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = ElectricIndigo),
                modifier = Modifier.weight(1f)
            )
            Text(text = "${camera.rollAngle.toInt()}°", color = TextMuted, fontSize = 11.sp)
        }

        // Depth of Field Toggle
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Depth of Field (Bokeh)", color = TextPrimary, fontSize = 12.sp)
            Switch(
                checked = camera.enableDoF,
                onCheckedChange = { onCameraChanged(camera.copy(enableDoF = it)) },
                colors = SwitchDefaults.colors(checkedThumbColor = NeonCyan, checkedTrackColor = ElectricIndigo)
            )
        }
    }
}
