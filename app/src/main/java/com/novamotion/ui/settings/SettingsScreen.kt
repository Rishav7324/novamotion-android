package com.novamotion.ui.settings

import android.app.ActivityManager
import android.content.Context
import android.opengl.EGL14
import android.opengl.GLES30
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novamotion.R
import com.novamotion.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences("novamotion_settings", Context.MODE_PRIVATE)
    }

    var showSafeGuides by remember { mutableStateOf(prefs.getBoolean("show_safe_guides", true)) }
    var enableHaptics by remember { mutableStateOf(prefs.getBoolean("enable_haptics", true)) }
    var autoSaveEnabled by remember { mutableStateOf(prefs.getBoolean("auto_save", true)) }
    var defaultFps by remember { mutableStateOf(prefs.getInt("default_fps", 60)) }

    // Device info (computed once)
    val deviceRam = remember {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        am.getMemoryInfo(memInfo)
        "${memInfo.totalMem / 1_073_741_824L} GB RAM"
    }
    val androidVer = remember { "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})" }
    val deviceModel = remember { "${Build.MANUFACTURER} ${Build.MODEL}" }
    val appVersion = remember { "2.1.0 (versionCode 3)" }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings & Hardware", color = TextPrimary, fontSize = 16.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = StudioSurface)
            )
        },
        containerColor = StudioBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── App Branding ──────────────────────────────────────────────────
            Card(
                colors = CardDefaults.cardColors(containerColor = StudioSurfaceVariant),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.app_logo),
                        contentDescription = "NovaMotion Logo",
                        modifier = Modifier.size(54.dp).clip(RoundedCornerShape(12.dp))
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(text = "NovaMotion Pro Studio", color = TextPrimary, fontSize = 15.sp, style = MaterialTheme.typography.titleMedium)
                        Text(text = "Version $appVersion", color = NeonCyan, fontSize = 11.sp)
                        Text(text = "OpenGL ES 3.2 + MediaCodec + ExoPlayer", color = TextMuted, fontSize = 11.sp)
                    }
                }
            }

            // ── Editor Preferences ────────────────────────────────────────────
            Text(text = "Editor Preferences", color = TextSecondary, fontSize = 12.sp, style = MaterialTheme.typography.labelMedium)

            Card(
                colors = CardDefaults.cardColors(containerColor = StudioSurfaceVariant),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    SettingsToggleRow(
                        icon = Icons.Default.GridOn,
                        label = "Show Safe Guides",
                        subtitle = "Canvas safe-area overlay",
                        checked = showSafeGuides,
                        onCheckedChange = {
                            showSafeGuides = it
                            prefs.edit().putBoolean("show_safe_guides", it).apply()
                        }
                    )
                    HorizontalDivider(color = StudioBorder.copy(alpha = 0.4f))
                    SettingsToggleRow(
                        icon = Icons.Default.Vibration,
                        label = "Haptic Feedback",
                        subtitle = "Vibrate on keyframe snap",
                        checked = enableHaptics,
                        onCheckedChange = {
                            enableHaptics = it
                            prefs.edit().putBoolean("enable_haptics", it).apply()
                        }
                    )
                    HorizontalDivider(color = StudioBorder.copy(alpha = 0.4f))
                    SettingsToggleRow(
                        icon = Icons.Default.Save,
                        label = "Auto-Save Projects",
                        subtitle = "Save 1.5s after last edit",
                        checked = autoSaveEnabled,
                        onCheckedChange = {
                            autoSaveEnabled = it
                            prefs.edit().putBoolean("auto_save", it).apply()
                        }
                    )
                    HorizontalDivider(color = StudioBorder.copy(alpha = 0.4f))
                    // Default FPS picker
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Speed, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Default FPS", color = TextPrimary, fontSize = 13.sp)
                                Text("New project frame rate", color = TextMuted, fontSize = 11.sp)
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf(24, 30, 60).forEach { fps ->
                                FilterChip(
                                    selected = defaultFps == fps,
                                    onClick = {
                                        defaultFps = fps
                                        prefs.edit().putInt("default_fps", fps).apply()
                                    },
                                    label = { Text("$fps", fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = ElectricIndigo,
                                        selectedLabelColor = TextPrimary,
                                        containerColor = StudioBackground,
                                        labelColor = TextSecondary
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // ── Hardware Diagnostics ──────────────────────────────────────────
            Text(text = "Hardware Diagnostics", color = TextSecondary, fontSize = 12.sp, style = MaterialTheme.typography.labelMedium)

            Card(
                colors = CardDefaults.cardColors(containerColor = StudioSurfaceVariant),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    DiagRow("Device", deviceModel, Icons.Default.PhoneAndroid)
                    HorizontalDivider(color = StudioBorder.copy(alpha = 0.4f))
                    DiagRow("Android", androidVer, Icons.Default.Android)
                    HorizontalDivider(color = StudioBorder.copy(alpha = 0.4f))
                    DiagRow("Memory", deviceRam, Icons.Default.Memory)
                    HorizontalDivider(color = StudioBorder.copy(alpha = 0.4f))
                    DiagRow("GL Pipeline", "OpenGL ES 3.2 + RENDERMODE_WHEN_DIRTY", Icons.Default.Speed)
                    HorizontalDivider(color = StudioBorder.copy(alpha = 0.4f))
                    DiagRow("Video Decode", "ExoPlayer + SurfaceTexture (OES)", Icons.Default.Videocam)
                    HorizontalDivider(color = StudioBorder.copy(alpha = 0.4f))
                    DiagRow("Export", "MediaCodec H.265/H.264 + MediaStore", Icons.Default.FileDownload)
                    HorizontalDivider(color = StudioBorder.copy(alpha = 0.4f))
                    DiagRow("Max Export", "4K UHD @ 60 FPS / 50 Mbps", Icons.Default.HighQuality)
                }
            }
        }
    }
}

@Composable
private fun SettingsToggleRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(label, color = TextPrimary, fontSize = 13.sp)
                Text(subtitle, color = TextMuted, fontSize = 11.sp)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = TextPrimary,
                checkedTrackColor = ElectricIndigo,
                uncheckedThumbColor = TextMuted,
                uncheckedTrackColor = StudioBackground
            )
        )
    }
}

@Composable
private fun DiagRow(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(label, color = TextSecondary, fontSize = 12.sp)
        }
        Text(value, color = NeonCyan, fontSize = 11.sp)
    }
}
