package com.cvpro.app.ui.screens.skillsgap

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.cvpro.app.data.mapper.toDomain
import com.cvpro.app.data.remote.dto.CvDto
import com.cvpro.app.data.remote.dto.JobOfferDto
import com.cvpro.app.domain.model.*
import com.cvpro.app.ui.theme.CvProTheme

/** Acceptance fixture for previews/instrumented rendering only. Never a source of selectable offers. */
internal fun acceptancePreviewState(): JobSkillsGapUiState {
    val missing = listOf("Comunicación", "Logística", "Office", "Soporte técnico", "Trabajo en equipo")
    val cv = CvDto(id = "preview-cv", title = "Mi CV de tecnología").toDomain()
    val offer = JobOfferDto(id = "preview-job", title = "Asistente de TI", company = "Empresa de tecnología").toDomain()
    val analysis = JobSkillsGapAnalysis(cv.id, offer.id, JobCoverageStatus.PARTIAL, 7, 2, 28.57,
        listOf(DocumentedJobSkill("SQL", "intermediate"), DocumentedJobSkill("Excel", "intermediate")),
        missing, JobCatalogStatus.CONSULTED, missing.map { JobSkillCourses(it, SkillCatalogMatch.NOT_FOUND, emptyList()) },
        listOf(JobLevelAssessment("SQL", SkillLevel.INTERMEDIATE, SkillLevel.BEGINNER, "description", "SQL básico",
            "explicit", null, "documented_skill_meets_required", 0),
            JobLevelAssessment("Excel", SkillLevel.INTERMEDIATE, null, null, null, "unknown", "no_explicit_evidence", "unknown_required_level", null)) +
            missing.map { JobLevelAssessment(it, null, null, null, null, "unknown", "no_explicit_evidence", "not_documented", null) },
        JobLevelAnalysis(1, 0, 1, 5, 1, 0))
    return JobSkillsGapUiState(offer = offer, cvs = listOf(cv), cvPage = 1,
        selectedCvId = cv.id, selectedCv = cv, analysis = analysis)
}

@Preview(name = "Asistente TI · español", locale = "es", widthDp = 360, heightDp = 800, showBackground = true)
@Preview(name = "IT · English", locale = "en", widthDp = 360, heightDp = 800, showBackground = true)
@Preview(name = "Fuente grande", locale = "es", widthDp = 360, heightDp = 800, fontScale = 1.6f, showBackground = true)
@Composable
internal fun JobGapAcceptancePreview() {
    CvProTheme {
        Surface {
            JobSkillsGapContent(acceptancePreviewState(), {}, {}, {}, {}, {})
        }
    }
}
