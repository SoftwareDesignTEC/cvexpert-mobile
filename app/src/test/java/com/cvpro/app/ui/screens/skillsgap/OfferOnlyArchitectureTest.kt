package com.cvpro.app.ui.screens.skillsgap

import com.cvpro.app.ui.screens.home.HomeViewModel
import com.cvpro.app.ui.screens.profile.ProfileViewModel
import com.cvpro.app.ui.navigation.CvProDestination
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class OfferOnlyArchitectureTest {
    @Test fun homeAndProfileHaveNoGeneralAnalysisDependencyOrState() {
        for (type in listOf(HomeViewModel::class.java, ProfileViewModel::class.java)) {
            assertTrue(type.declaredConstructors.all { constructor -> constructor.parameterTypes.none { it.simpleName == "SkillsGapRepository" } })
            assertTrue(type.declaredFields.none { it.type.simpleName == "SkillsGapRepository" })
        }
        for (area in listOf("home", "profile")) {
            val screen = source("ui/screens/$area/${area.replaceFirstChar { it.uppercase() }}Screen.kt")
            val vm = source("ui/screens/$area/${area.replaceFirstChar { it.uppercase() }}ViewModel.kt")
            assertFalse(screen.contains("RadarChart"))
            assertFalse(vm.contains("getAnalysis("))
        }
    }
    @Test fun mainEntryCallsExistingOfferFlowAndJobsKeepsItsLockedSheet() {
        val entry = source("ui/screens/skillsgap/SkillsGapScreen.kt")
        assertTrue(entry.contains("JobSkillsGapOfferScreen("))
        assertFalse(entry.contains("SkillsGapMode"))
        assertTrue(source("ui/screens/jobs/JobsScreen.kt").contains("JobSkillsGapSheet("))
        assertTrue(source("ui/screens/jobs/JobsScreen.kt").contains("onExploreCourses = onExploreCourses,"))
        assertTrue(source("ui/screens/skillsgap/JobSkillsGapSheet.kt").contains("viewModel.open(offer)"))
        assertEquals(CvProDestination.COURSES, CvProDestination.fromRoute("cursos?skillName={skillName}"))
        assertEquals(CvProDestination.SKILLS_GAP, CvProDestination.fromRoute("brechas"))
    }
    private fun source(path: String): String {
        val folder = if (File("src/main/java").exists()) File("src/main/java/com/cvpro/app") else File("app/src/main/java/com/cvpro/app")
        return File(folder, path).readText()
    }
}
