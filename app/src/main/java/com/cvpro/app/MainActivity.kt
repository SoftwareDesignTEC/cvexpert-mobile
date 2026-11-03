package com.cvpro.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cvpro.app.data.notifications.NotificationDirectorySync
import com.cvpro.app.ui.navigation.CvProApp
import com.cvpro.app.ui.theme.CvProTheme
import com.cvpro.app.ui.theme.ThemeViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.cvpro.app.ui.screens.splash.SplashScreen
import com.cvpro.app.ui.screens.splash.SplashViewModel

/**
 * Única Activity de la app: toda la UI es Compose y la navegación será
 * interna con Navigation Component (T2).
 *
 * @AndroidEntryPoint permite que Hilt inyecte dependencias en esta Activity
 * y en los ViewModels que se pidan desde ella.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var notificationDirectorySync: NotificationDirectorySync

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Android recrea la Activity cuando cambia el idioma del sistema: si cambió, se le avisa
        // a Notificaciones para que los avisos lleguen en el idioma nuevo.
        notificationDirectorySync.onLanguageMaybeChanged()
        // Dibuja detrás de las barras de sistema (comportamiento estándar en Android 15+).
        enableEdgeToEdge()
        setContent {
            // El tema guardado manda sobre el ajuste del sistema (Configuración de la app).
            val themeViewModel: ThemeViewModel = hiltViewModel()
            val splashViewModel: SplashViewModel = hiltViewModel()
            val hasSeenSplash by splashViewModel.hasSeenSplash
                .collectAsStateWithLifecycle()
            val themePreference by themeViewModel.themePreference.collectAsStateWithLifecycle()

            CvProTheme(darkTheme = themePreference.isDark(isSystemInDarkTheme())) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    var showSplash by remember {
                        mutableStateOf(true)
                    }

                    if (showSplash) {
                        SplashScreen(
                            canSkip = hasSeenSplash,
                            onFinished = {
                                splashViewModel.markSplashSeen()
                                showSplash = false
                            },
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        CvProApp()
                    }
                }
            }
        }
    }
}
