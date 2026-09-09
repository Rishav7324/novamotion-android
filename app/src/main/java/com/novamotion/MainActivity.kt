package com.novamotion

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.novamotion.ui.layout.StudioWorkspace
import com.novamotion.ui.theme.NovaMotionTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NovaMotionTheme {
                StudioWorkspace()
            }
        }
    }
}
