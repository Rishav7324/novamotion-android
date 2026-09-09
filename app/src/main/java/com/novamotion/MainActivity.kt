package com.novamotion

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import com.novamotion.core.model.Project
import com.novamotion.ui.home.HomeScreen
import com.novamotion.ui.layout.StudioWorkspace
import com.novamotion.ui.settings.SettingsScreen
import com.novamotion.ui.splash.SplashScreen
import com.novamotion.ui.theme.NovaMotionTheme

enum class AppScreen {
    SPLASH,
    HOME,
    STUDIO,
    SETTINGS
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NovaMotionTheme {
                var currentScreen by remember { mutableStateOf(AppScreen.SPLASH) }
                var activeProject by remember { mutableStateOf<Project?>(null) }

                when (currentScreen) {
                    AppScreen.SPLASH -> {
                        SplashScreen(
                            onContinue = { currentScreen = AppScreen.HOME }
                        )
                    }
                    AppScreen.HOME -> {
                        HomeScreen(
                            onOpenProject = { project ->
                                activeProject = project
                                currentScreen = AppScreen.STUDIO
                            },
                            onOpenSettings = {
                                currentScreen = AppScreen.SETTINGS
                            }
                        )
                    }
                    AppScreen.STUDIO -> {
                        StudioWorkspace(
                            initialProject = activeProject,
                            onBackToHome = {
                                currentScreen = AppScreen.HOME
                            }
                        )
                    }
                    AppScreen.SETTINGS -> {
                        SettingsScreen(
                            onBack = { currentScreen = AppScreen.HOME }
                        )
                    }
                }
            }
        }
    }
}
