package com.cvpro.app.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.cvpro.app.R
import com.cvpro.app.domain.model.Cv
import com.cvpro.app.domain.model.JobOffer
import com.cvpro.app.domain.model.SkillsGapAnalysis
import com.cvpro.app.ui.components.RadarChart
import com.cvpro.app.ui.components.RadarSeries
import com.cvpro.app.ui.components.VersionLabel
import com.cvpro.app.ui.theme.CvProTheme
import com.cvpro.app.ui.theme.extraColors

/** Panel Principal: la pantalla de entrada después del login. */
@Composable
fun HomeScreen(
    versionName: String,
    versionCode: Int,
    onTemplatesClick: () -> Unit,
    onSkillsGapClick: () -> Unit,
    onJobsClick: () -> Unit,
    onCvsClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    HomeContent(
        uiState = uiState,
        versionName = versionName,
        versionCode = versionCode,
        onTemplatesClick = onTemplatesClick,
        onSkillsGapClick = onSkillsGapClick,
        onJobsClick = onJobsClick,
        onCvsClick = onCvsClick,
        modifier = modifier,
    )
}

/**
 * Panel Principal, con las cuatro secciones del diseño de Samuel: los currículums recientes,
 * la barra para crear uno, el análisis de habilidades y las ofertas recomendadas.
 *
 * Cada sección es una tarjeta blanca sobre el fondo azulado del tema.
 */
@Composable
fun HomeContent(
    uiState: HomeUiState,
    versionName: String,
    versionCode: Int,
    onTemplatesClick: () -> Unit,
    onSkillsGapClick: () -> Unit,
    onJobsClick: () -> Unit,
    onCvsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RecentCvsSection(cvs = uiState.recentCvs, onSeeAllClick = onCvsClick)

        CreateCvSection(onOfflineClick = onTemplatesClick)

        SkillsSection(analysis = uiState.skillsGap, onSeeAreasClick = onSkillsGapClick)

        RecommendedOffersSection(offers = uiState.recommendedOffers, onSeeAllClick = onJobsClick)

        VersionLabel(
            versionName = versionName,
            versionCode = versionCode,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
        )
    }
}

// ─── Currículums recientes ────────────────────────────────────────────────────

@Composable
private fun RecentCvsSection(
    cvs: List<Cv>,
    onSeeAllClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SectionCard(modifier = modifier) {
        SectionHeader(
            title = stringResource(R.string.home_recent_cvs_title),
            actionLabel = stringResource(R.string.home_recent_cvs_see_all),
            onActionClick = onSeeAllClick,
        )

        if (cvs.isEmpty()) {
            EmptySectionText(stringResource(R.string.home_recent_cvs_empty))
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(top = 12.dp),
            ) {
                items(items = cvs, key = { it.id }) { cv -> RecentCvCard(cv = cv) }
            }
        }
    }
}

@Composable
private fun RecentCvCard(
    cv: Cv,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.width(190.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column {
            // El modelo del CV no trae imagen de vista previa: por ahora va el recuadro gris
            // del diseño.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.home_cv_preview),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = cv.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(R.string.home_cv_updated_at, cv.updatedAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 10.dp),
                ) {
                    // TODO: sin acción. Falta la pantalla de edición del currículum.
                    Button(
                        onClick = {},
                        contentPadding = TIGHT_BUTTON_PADDING,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = stringResource(R.string.home_cv_edit),
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                        )
                    }
                    // TODO: sin acción. La exportación a PDF no entra en la Fase I.
                    Button(
                        onClick = {},
                        contentPadding = TIGHT_BUTTON_PADDING,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.onSurface,
                            contentColor = MaterialTheme.colorScheme.surface,
                        ),
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = stringResource(R.string.home_cv_download),
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

// ─── Crear currículum ─────────────────────────────────────────────────────────

/** Barra con los dos caminos para crear un currículum: con IA o sin conexión. */
@Composable
private fun CreateCvSection(
    onOfflineClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.home_create_cv_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.home_create_cv_subtitle),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            // TODO: sin acción. La generación con IA no entra en la Fase I.
            Button(onClick = {}, contentPadding = TIGHT_BUTTON_PADDING) {
                Text(stringResource(R.string.home_create_cv_ai))
            }
            OutlinedButton(
                onClick = onOfflineClick,
                contentPadding = TIGHT_BUTTON_PADDING,
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            ) {
                Text(stringResource(R.string.home_create_cv_offline))
            }
        }
    }
}

// ─── Análisis de habilidades ──────────────────────────────────────────────────

/** El radar de habilidades, centrado y a todo el ancho como en el diseño. */
@Composable
private fun SkillsSection(
    analysis: SkillsGapAnalysis?,
    onSeeAreasClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SectionCard(modifier = modifier) {
        SectionHeader(title = stringResource(R.string.home_skills_title))
        Text(
            text = stringResource(R.string.home_skills_subtitle),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 2.dp),
        )

        if (analysis == null) {
            EmptySectionText(stringResource(R.string.home_skills_empty))
            return@SectionCard
        }

        RadarChart(
            labels = analysis.skills.map { it.name },
            series = listOf(
                RadarSeries(
                    label = stringResource(R.string.skills_gap_series_user),
                    values = analysis.skills.map { it.userLevel },
                    color = MaterialTheme.colorScheme.primary,
                ),
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .padding(top = 12.dp),
        )

        OutlinedButton(
            onClick = onSeeAreasClick,
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
        ) {
            Text(stringResource(R.string.home_shortcut_skills_gap))
        }
    }
}

// ─── Ofertas recomendadas ─────────────────────────────────────────────────────

@Composable
private fun RecommendedOffersSection(
    offers: List<JobOffer>,
    onSeeAllClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SectionCard(modifier = modifier) {
        SectionHeader(
            title = stringResource(R.string.home_offers_title),
            actionLabel = stringResource(R.string.home_offers_see_all),
            onActionClick = onSeeAllClick,
        )

        if (offers.isEmpty()) {
            EmptySectionText(stringResource(R.string.home_offers_empty))
        } else {
            offers.forEach { offer ->
                OfferRow(offer = offer, modifier = Modifier.padding(top = 10.dp))
            }
        }
    }
}

@Composable
private fun OfferRow(
    offer: JobOffer,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(10.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(10.dp),
        ) {
            CompanyLogo(logoUrl = offer.companyLogoUrl, company = offer.company)

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(
                        R.string.home_offer_title_company,
                        offer.title,
                        offer.company,
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = offer.location,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // El porcentaje de compatibilidad va en una píldora verde, como en el diseño.
            offer.matchScore?.let { score ->
                Surface(
                    color = MaterialTheme.extraColors.successContainer,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.padding(start = 6.dp),
                ) {
                    Text(
                        text = stringResource(R.string.home_offer_match, score),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.extraColors.onSuccessContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }
        }
    }
}

// ─── Piezas compartidas ───────────────────────────────────────────────────────

/**
 * Título de sección con la barra azul a la izquierda, y opcionalmente un botón a la derecha.
 */
@Composable
private fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onActionClick: () -> Unit = {},
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth(),
    ) {
        Box(
            modifier = Modifier
                .size(width = 4.dp, height = 18.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.primary),
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp),
        )
        if (actionLabel != null) {
            OutlinedButton(
                onClick = onActionClick,
                contentPadding = TIGHT_BUTTON_PADDING,
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            ) {
                Text(text = actionLabel, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

/** Mensaje de sección sin datos. */
@Composable
private fun EmptySectionText(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
    )
}

/** Tarjeta blanca que envuelve cada sección del panel. */
@Composable
private fun SectionCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(12.dp), content = content)
    }
}

/** Recuadro del logo de la empresa, con su inicial cuando no hay imagen. */
@Composable
private fun CompanyLogo(
    logoUrl: String?,
    company: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        if (logoUrl != null) {
            AsyncImage(
                model = logoUrl,
                contentDescription = stringResource(R.string.applications_logo_description, company),
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp),
            )
        } else {
            Text(
                text = company.take(1).uppercase(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

/** Relleno angosto: en el panel hay varios botones por fila y el de Material no cabe. */
private val TIGHT_BUTTON_PADDING = PaddingValues(horizontal = 10.dp, vertical = 4.dp)

@Preview(name = "Panel · claro", widthDp = 390, heightDp = 1200)
@Composable
private fun HomeScreenPreviewLight() = HomePreview(darkTheme = false)

@Preview(name = "Panel · oscuro", widthDp = 390, heightDp = 1200)
@Composable
private fun HomeScreenPreviewDark() = HomePreview(darkTheme = true)

/** Envuelve los previews con el fondo del tema, para que el modo oscuro no salga sobre blanco. */
@Composable
private fun HomePreview(darkTheme: Boolean) {
    CvProTheme(darkTheme = darkTheme) {
        Surface(color = MaterialTheme.colorScheme.background) {
            HomeContent(
                uiState = HomeUiState(),
                versionName = "0.3.0",
                versionCode = 3,
                onTemplatesClick = {},
                onSkillsGapClick = {},
                onJobsClick = {},
                onCvsClick = {},
            )
        }
    }
}
