package com.cvpro.app.ui.screens.courses

import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.test.platform.app.InstrumentationRegistry
import com.cvpro.app.domain.model.*
import com.cvpro.app.ui.components.CourseCard
import com.cvpro.app.ui.theme.CvProTheme
import org.junit.*
import org.junit.Assert.*
import java.io.File
import java.util.Locale

/** Compose verification with local state; no API or authenticated E2E claims. */
class CoursesUiTest {
    @get:Rule val compose = createComposeRule()
    private fun content(dark: Boolean = false, english: Boolean = false, large: Boolean = false, block: @Composable () -> Unit) {
        compose.setContent {
            val config = Configuration(LocalConfiguration.current).apply { setLocale(Locale.forLanguageTag(if (english) "en" else "es")) }
            val context = LocalContext.current.createConfigurationContext(config)
            CompositionLocalProvider(LocalContext provides context, LocalConfiguration provides config,
                LocalDensity provides Density(LocalDensity.current.density, if (large) 1.6f else 1f)) {
                CvProTheme(darkTheme = dark) { Surface { block() } }
            }
        }
    }
    @Test fun emptyCatalogAndExactFiltersRenderInSpanish() {
        content { CoursesContent(CoursesUiState(result = CoursePage(emptyList(), 1, 20, 0)), {}, {}, {}) }
        compose.onNodeWithText("Explorar cursos").assertExists()
        compose.onNodeWithTag("courses-catalog").performScrollToNode(hasText("El cat\u00e1logo todav\u00eda no contiene cursos."))
        compose.onNodeWithText("El cat\u00e1logo todav\u00eda no contiene cursos.").assertIsDisplayed()
        screenshot("courses-empty-es.png")
    }
    @Test fun darkEnglishLargeFontRetainsErrorAndRetry() {
        var retries = 0
        content(dark = true, english = true, large = true) {
            CoursesContent(CoursesUiState(error = CourseFailure.UNAVAILABLE), {}, {}, { retries++ })
        }
        compose.onNodeWithTag("courses-catalog").performScrollToNode(hasText("Try again"))
        compose.onNodeWithText("Try again").performClick()
        assertEquals(1, retries)
        screenshot("courses-error-dark-en-large.png")
    }
    @Test fun pageCountComesFromServerAndNextPageUsesCallback() {
        var page = 0
        content { CoursesContent(CoursesUiState(result = CoursePage(emptyList(), 1, 20, 21, 1)), {}, { page = it }, {}) }
        compose.onNodeWithTag("courses-catalog").performScrollToNode(hasText("P\u00e1gina 1 \u00b7 21 registros del servidor"))
        compose.onNodeWithText("P\u00e1gina 1 \u00b7 21 registros del servidor").assertIsDisplayed()
        compose.onNodeWithTag("courses-catalog").performScrollToNode(hasText("Siguiente"))
        compose.onNodeWithText("Siguiente").performClick()
        assertEquals(2, page)
        screenshot("courses-data-defect-es.png")
    }
    @Test fun durationUsesLocalizedNumberAndFakeRecordsAreNotPresented() {
        val course = Course("test-only", "Associated course fixture", "Provider fixture", "https://learn.microsoft.com/training/", null, 2, false, listOf("Python"))
        content(english = true) {
            androidx.compose.foundation.layout.Column {
                CourseCard(course)
                CourseCard(course.copy(id = "demo", title = "[DEMO] Hidden course"))
            }
        }
        compose.onNodeWithText("Duration of 2 h").assertExists()
        compose.onNodeWithText("[DEMO] Hidden course").assertDoesNotExist()
    }
    private fun screenshot(name: String) {
        compose.waitForIdle()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        File(context.getExternalFilesDir(null), name).outputStream().use {
            compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
