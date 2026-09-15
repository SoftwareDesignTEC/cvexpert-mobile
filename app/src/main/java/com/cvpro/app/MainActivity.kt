package com.cvpro.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.cvpro.app.ui.screens.home.HomeScreen
import com.cvpro.app.ui.theme.CvProTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Única Activity de la app: toda la UI es Compose y la navegación será
 * interna con Navigation Component (T2).
 *
 * @AndroidEntryPoint permite que Hilt inyecte dependencias en esta Activity
 * y en los ViewModels que se pidan desde ella.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Dibuja detrás de las barras de sistema (comportamiento estándar en Android 15+).
        enableEdgeToEdge()
        setContent {
            CvProTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    // El número de versión sale de BuildConfig, que Gradle genera
                    // a partir del versionName. Requisito de la rúbrica: versión visible.
                    HomeScreen(
                        versionName = BuildConfig.VERSION_NAME,
                        versionCode = BuildConfig.VERSION_CODE,
                    )
                }
            }
        }
    }
}
