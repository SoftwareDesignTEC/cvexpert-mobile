package com.cvpro.app.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cvpro.app.R
import com.cvpro.app.ui.theme.CvProTheme

/**
 * Pantalla inicial provisional del scaffold (T1).
 *
 * Es un composable "sin estado" (stateless): no conoce ViewModels ni BuildConfig,
 * solo recibe datos planos. Así se puede previsualizar y reutilizar.
 * En T2 esta pantalla se reemplaza por el grafo de navegación real.
 */
@Composable
fun HomeScreen(
    versionName: String,
    versionCode: Int,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            // Deja libres las zonas del sistema (barra de estado y de gestos).
            .safeDrawingPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = stringResource(R.string.home_tagline),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
        // Requisito de la rúbrica: el número de versión debe verse dentro de la app.
        Text(
            text = stringResource(R.string.home_version, versionName, versionCode),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 24.dp),
        )
    }
}

@Preview(name = "Claro", showBackground = true)
@Composable
private fun HomeScreenPreviewLight() {
    CvProTheme(darkTheme = false) {
        HomeScreen(versionName = "0.1.0", versionCode = 1)
    }
}

@Preview(name = "Oscuro", showBackground = true)
@Composable
private fun HomeScreenPreviewDark() {
    CvProTheme(darkTheme = true) {
        HomeScreen(versionName = "0.1.0", versionCode = 1)
    }
}
