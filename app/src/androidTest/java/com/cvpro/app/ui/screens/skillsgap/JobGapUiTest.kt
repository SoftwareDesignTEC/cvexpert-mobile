package com.cvpro.app.ui.screens.skillsgap

import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.test.platform.app.InstrumentationRegistry
import com.cvpro.app.domain.model.SkillLevel
import com.cvpro.app.ui.theme.CvProTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.Locale

class JobGapUiTest {
    @get:Rule val compose = createComposeRule()

    private fun content(locale: String = "es", fontScale: Float = 1f, block: @Composable () -> Unit) {
        compose.setContent {
            val context = LocalContext.current
            val config = Configuration(LocalConfiguration.current).apply { setLocale(Locale.forLanguageTag(locale)) }
            CompositionLocalProvider(LocalContext provides context.createConfigurationContext(config),
                LocalConfiguration provides config,
                LocalDensity provides Density(LocalDensity.current.density, fontScale)) {
                CvProTheme { Surface { block() } }
            }
        }
    }

    @Test fun acceptanceSummaryIsVisibleAndEvidenceIsVoluntary() {
        content { SkillsGapModeContent(SkillsGapMode.OFFER, {}, offerContent = {
            JobSkillsGapContent(acceptancePreviewState(), {}, {}, {}, {}, {})
        }, marketContent = {}) }
        compose.onNodeWithText("Documentadas: 2 / 7").assertIsDisplayed()
        compose.onNodeWithText("28,57 % de cobertura documental").assertExists()
        compose.onNodeWithText("Cumples el nivel indicado").assertExists()
        compose.onNodeWithText("Descripción: «SQL básico»").assertDoesNotExist()
        screenshot("gap-result-es.png")
        compose.onNodeWithTag("gap-details").performScrollTo().performClick()
        compose.onNodeWithText("Descripción: «SQL básico»").assertExists()
        compose.onNodeWithTag("gap-details").performScrollTo().performClick()
        compose.onNodeWithText("Descripción: «SQL básico»").assertDoesNotExist()
    }

    @Test fun missingCoursesAreSummarizedOnceAndNamesCanBeExpanded() {
        content { JobSkillsGapContent(acceptancePreviewState(), {}, {}, {}, {}, {}) }
        compose.onNodeWithText("Habilidades sin cursos asociados: 5").assertDoesNotExist()
        compose.onNodeWithTag("gap-courses").performScrollTo().performClick()
        compose.onAllNodesWithText("Habilidades sin cursos asociados: 5").assertCountEquals(1)
        compose.onNodeWithTag("gap-course-skills").performScrollTo().performClick()
        compose.onNodeWithText("Comunicación, Logística, Office, Soporte técnico, Trabajo en equipo").performScrollTo().assertIsDisplayed()
        screenshot("gap-courses-es.png")
    }

    @Test fun unknownRequiredLevelDoesNotDrawAZeroOrInventBeginner() {
        content { SkillLevelComparisonChart(listOf(SkillComparison("Excel", SkillLevel.INTERMEDIATE, null, null))) }
        compose.onNodeWithText("No especificado").assertIsDisplayed()
        compose.onNodeWithText("Principiante").assertDoesNotExist()
        compose.onNodeWithText("0").assertDoesNotExist()
    }

    @Test fun mainModeAllowsExplicitCvAndOfferSelection() {
        val fixture = acceptancePreviewState()
        var analyzes = 0
        content {
            var state by remember { mutableStateOf(JobSkillsGapUiState(cvs = fixture.cvs, cvPage = 1)) }
            JobSkillsGapContent(state, {}, { id -> state = state.copy(selectedCvId = id,
                selectedCv = fixture.selectedCv, offers = listOf(fixture.offer!!), offerPage = 1) },
                { analyzes++ }, {}, {}, onSelectOffer = { state = state.copy(offer = fixture.offer) })
        }
        compose.onNodeWithTag("gap-analyze").assertDoesNotExist()
        compose.onNodeWithText("Mi CV de tecnología").performClick()
        compose.onNodeWithTag("gap-offer-search").assertExists()
        screenshot("gap-selection-es.png")
        compose.onNodeWithText("Asistente de TI").performClick()
        compose.onNodeWithTag("gap-analyze").performScrollTo().performClick()
        assertEquals(1, analyzes)
    }

    @Test fun jobsEntryPreservesOfferAndDoesNotShowAnotherOfferPicker() {
        val fixture = acceptancePreviewState()
        content { JobSkillsGapContent(fixture.copy(offerLocked = true, analysis = null), {}, {}, {}, {}, {}) }
        compose.onNodeWithText("Asistente de TI").assertExists()
        compose.onNodeWithTag("gap-offer-search").assertDoesNotExist()
        compose.onNodeWithText("Cambiar oferta").assertDoesNotExist()
    }

    @Test fun applyingFromJobsStillCallsTheExistingAction() {
        var applications = 0
        val offer = acceptancePreviewState().offer!!
        content {
            com.cvpro.app.ui.screens.jobs.JobsContent(
                uiState = com.cvpro.app.ui.screens.jobs.JobsUiState(offers = listOf(offer)),
                onQueryChange = {}, onFilterChange = {}, onRetryClick = {}, onLoadMore = {},
                onApply = { applications++ })
        }
        compose.onNodeWithText("Postularme").performScrollTo().performClick()
        assertEquals(1, applications)
    }

    @Test fun marketModeIsSeparateAndDoesNotGateOffers() {
        content {
            var mode by remember { mutableStateOf(SkillsGapMode.OFFER) }
            SkillsGapModeContent(mode, { mode = it }, offerContent = { Text("Oferta disponible") },
                marketContent = { SkillsGapContent(SkillsGapUiState(enabled = false), {}) })
        }
        compose.onNodeWithText("Oferta disponible").assertIsDisplayed()
        compose.onNodeWithText("Mercado general").performClick()
        compose.onNodeWithText("Oferta disponible").assertDoesNotExist()
        compose.onNodeWithText("Por oferta").performClick()
        compose.onNodeWithText("Oferta disponible").assertIsDisplayed()
    }

    @Test fun enabledMarketExplainsItsReferenceWithoutPretendingToBeAJob() {
        content { SkillsGapContent(SkillsGapUiState(enabled = true), {}) }
        compose.onNodeWithText("Compara tu instantánea de habilidades con una referencia configurada por el servicio. No es una oferta concreta ni un indicador del mercado en tiempo real.").assertIsDisplayed()
        compose.onNodeWithTag("gap-offer-search").assertDoesNotExist()
    }

    @Test fun retryIsExplicitAndSelectionRemainsVisibleAfterNetworkError() {
        var retries = 0
        content {
            JobSkillsGapContent(acceptancePreviewState().copy(analysis = null,
                errorMessage = com.cvpro.app.R.string.job_gap_network_error), {}, {}, {}, {}, {}, onRetry = { retries++ })
        }
        compose.onNodeWithText("No pudimos completar el análisis").assertExists()
        compose.onNodeWithText("Asistente de TI").assertExists()
        compose.onNodeWithText("CV utilizado: Mi CV de tecnología").assertExists()
        assertEquals(0, retries)
        compose.onNodeWithText("Reintentar").performScrollTo().performClick()
        assertEquals(1, retries)
    }

    @Test fun partialCoursesDoNotPretendThatTheJobLeftLevelsUnspecified() {
        val fixture = acceptancePreviewState()
        content { JobSkillsGapContent(fixture.copy(analysis = fixture.analysis!!.copy(assessments = null,
            levelAnalysis = null), errorMessage = com.cvpro.app.R.string.job_gap_network_error), {}, {}, {}, {}, {}) }
        compose.onNodeWithText("Sin evaluar").assertExists()
        compose.onNodeWithText("Nivel requerido no especificado").assertDoesNotExist()
        compose.onNodeWithTag("gap-courses").assertExists()
    }

    @Test fun englishRenderingAndLargeFontUseTheSameComponents() {
        content(locale = "en", fontScale = 1.6f) {
            JobSkillsGapContent(acceptancePreviewState(), {}, {}, {}, {}, {})
        }
        compose.onNodeWithText("Documented: 2 / 7").assertIsDisplayed()
        compose.onNodeWithText("28.57 % documented coverage").assertExists()
        screenshot("gap-large-font-en.png")
    }

    private fun screenshot(name: String) {
        compose.waitForIdle()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        File(context.getExternalFilesDir(null), name).outputStream().use {
            compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
