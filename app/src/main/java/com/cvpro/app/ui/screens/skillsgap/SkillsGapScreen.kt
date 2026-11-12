package com.cvpro.app.ui.screens.skillsgap
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun SkillsGapScreen(
    onCvsClick: () -> Unit = {},
    onExploreCourses: (String?) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    JobSkillsGapOfferScreen(onCorrectCv = onCvsClick, onExploreCourses = onExploreCourses, modifier = modifier)
}
