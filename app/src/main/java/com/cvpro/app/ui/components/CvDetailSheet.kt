package com.cvpro.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.cvpro.app.R
import com.cvpro.app.domain.model.Cv
import com.cvpro.app.domain.model.CvLevel
import com.cvpro.app.domain.model.LanguageLevel
import com.cvpro.app.domain.model.SkillLevel
import com.cvpro.app.domain.model.toCvMonth

/**
 * Contenido completo de un currículum.
 *
 * Es lo que abre "Ver/Editar". Por ahora solo se lee: la edición necesita un editor, que
 * no entra en la Fase I. Aun así muestra todo lo que el servicio devuelve del CV, que
 * hasta ahora no se veía en ninguna pantalla.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CvDetailSheet(
    cv: Cv,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
        ) {
            Text(
                text = cv.title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.home_cv_updated_at, cv.updatedAt),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )

            CvSection(title = stringResource(R.string.cv_detail_personal)) {
                CvLine(cv.personalInfo.name)
                CvLine(cv.personalInfo.email)
                cv.personalInfo.phone?.let { CvLine(it) }
                CvLine(cv.personalInfo.location)
                cv.personalInfo.bio?.takeIf { it.isNotBlank() }?.let { CvLine(it) }
            }

            if (cv.workExperiences.isNotEmpty()) {
                CvSection(title = stringResource(R.string.cv_detail_experience)) {
                    cv.workExperiences.forEach { experience ->
                        CvEntry(
                            title = experience.position,
                            subtitle = experience.company,
                            period = periodOf(
                                experience.startDate,
                                experience.endDate,
                                experience.current,
                            ),
                            detail = experience.description,
                        )
                    }
                }
            }

            if (cv.education.isNotEmpty()) {
                CvSection(title = stringResource(R.string.cv_detail_education)) {
                    cv.education.forEach { study ->
                        CvEntry(
                            title = study.degree,
                            subtitle = study.institution,
                            period = periodOf(study.startDate, study.endDate, study.current),
                            detail = study.field,
                        )
                    }
                }
            }

            if (cv.skills.isNotEmpty()) {
                CvSection(title = stringResource(R.string.cv_detail_skills)) {
                    cv.skills.forEach { skill ->
                        CvLine(nameWithLevel(skill.name, SkillLevel.fromApi(skill.level), skill.level))
                    }
                }
            }

            if (cv.languages.isNotEmpty()) {
                CvSection(title = stringResource(R.string.cv_detail_languages)) {
                    cv.languages.forEach { language ->
                        CvLine(nameWithLevel(language.name, LanguageLevel.fromApi(language.level), language.level))
                    }
                }
            }

            if (cv.certifications.isNotEmpty()) {
                CvSection(title = stringResource(R.string.cv_detail_certifications)) {
                    cv.certifications.forEach { cert ->
                        CvEntry(
                            title = cert.name,
                            subtitle = cert.issuer,
                            period = periodOf(cert.issuedAt, cert.expiresAt, current = false),
                            detail = cert.credentialUrl.orEmpty(),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Aviso de una función que todavía no existe.
 *
 * Se usa en los botones que el diseño dibuja pero la Fase I no cubre: la generación con
 * IA, la exportación a PDF y la descarga de plantillas. Abrir una pantalla que lo explica
 * es más claro que un botón que no responde.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComingSoonSheet(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .padding(bottom = 48.dp),
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(50),
            ) {
                Text(
                    text = stringResource(R.string.coming_soon_badge),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 16.dp),
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

/** Un bloque del currículum, con su título y una línea que lo separa del anterior. */
@Composable
private fun CvSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    Column(modifier = modifier.padding(top = 20.dp)) {
        HorizontalDivider()
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
        )
        content()
    }
}

/** Una entrada con puesto, lugar, periodo y detalle. */
@Composable
private fun CvEntry(
    title: String,
    subtitle: String,
    period: String,
    detail: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(top = 10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = period,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (detail.isNotBlank()) {
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

@Composable
private fun CvLine(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier.padding(top = 2.dp),
    )
}

/**
 * "Kotlin · Avanzado". El servidor manda el nivel en inglés ([level] es el enum ya reconocido,
 * [raw] lo que llegó): si la app no lo conoce se muestra tal cual, y si no hay nivel, solo el nombre.
 */
@Composable
private fun nameWithLevel(name: String, level: CvLevel?, raw: String): String {
    val label = level?.let { stringResource(it.labelRes) } ?: raw
    return if (label.isBlank()) name else "$name · $label"
}

/**
 * "06/2024 — Actual" o "06/2024 — 03/2025", según si el puesto sigue vigente. Sin fecha de
 * fin queda solo la de inicio, y sin ninguna, vacío.
 */
@Composable
private fun periodOf(start: Long?, end: Long?, current: Boolean): String {
    val desde = start.toCvMonth()
    return when {
        desde.isEmpty() -> end.toCvMonth()
        current -> stringResource(R.string.cv_detail_period_current, desde)
        end != null -> stringResource(R.string.cv_detail_period, desde, end.toCvMonth())
        else -> desde
    }
}
