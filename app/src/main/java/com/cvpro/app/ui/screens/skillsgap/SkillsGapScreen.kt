package com.cvpro.app.ui.screens.skillsgap

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.cvpro.app.R
import com.cvpro.app.ui.components.PlaceholderScreen

/** Análisis de Brechas: habilidades a mejorar frente al mercado y cursos recomendados. */
@Composable
fun SkillsGapScreen(modifier: Modifier = Modifier) {
    PlaceholderScreen(
        title = stringResource(R.string.destination_skills_gap),
        description = stringResource(R.string.placeholder_skills_gap),
        icon = Icons.Default.Star,
        modifier = modifier,
    )
}
