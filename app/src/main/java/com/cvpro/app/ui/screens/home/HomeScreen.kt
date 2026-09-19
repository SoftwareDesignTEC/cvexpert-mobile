package com.cvpro.app.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cvpro.app.R
import com.cvpro.app.domain.model.User
import com.cvpro.app.ui.components.VersionLabel
import com.cvpro.app.ui.theme.CvProTheme

/**
 * Panel Principal: la pantalla de entrada después del login.
 *
 * Por ahora saluda al usuario y deja los dos accesos directos que tiene el prototipo.
 * El contenido real (currículums recientes, análisis de habilidades y ofertas
 * recomendadas) se arma cuando haya datos.
 */
@Composable
fun HomeScreen(
    user: User,
    versionName: String,
    versionCode: Int,
    onTemplatesClick: () -> Unit,
    onSkillsGapClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.home_greeting, user.name),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.home_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
        Button(
            onClick = onTemplatesClick,
            modifier = Modifier
                .padding(top = 32.dp)
                .fillMaxWidth(),
        ) {
            Text(text = stringResource(R.string.home_shortcut_templates))
        }
        OutlinedButton(
            onClick = onSkillsGapClick,
            modifier = Modifier
                .padding(top = 12.dp)
                .fillMaxWidth(),
        ) {
            Text(text = stringResource(R.string.home_shortcut_skills_gap))
        }
        VersionLabel(
            versionName = versionName,
            versionCode = versionCode,
            modifier = Modifier.padding(top = 32.dp),
        )
    }
}

/** Envuelve los previews con el fondo del tema, para que el modo oscuro no salga sobre blanco. */
@Composable
private fun HomePreview(darkTheme: Boolean) {
    CvProTheme(darkTheme = darkTheme) {
        Surface(color = MaterialTheme.colorScheme.background) {
            HomeScreen(
                user = User(name = "María Rodríguez", email = "maria@ejemplo.com", photoUrl = null),
                versionName = "0.1.0",
                versionCode = 1,
                onTemplatesClick = {},
                onSkillsGapClick = {},
            )
        }
    }
}

@Preview(name = "Claro")
@Composable
private fun HomeScreenPreviewLight() = HomePreview(darkTheme = false)

@Preview(name = "Oscuro")
@Composable
private fun HomeScreenPreviewDark() = HomePreview(darkTheme = true)
