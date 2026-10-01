package com.cvpro.app.data.mapper

import com.cvpro.app.data.remote.dto.CourseDto
import com.cvpro.app.data.remote.dto.SkillsGapDto
import com.cvpro.app.domain.model.CourseRecommendation
import com.cvpro.app.domain.model.SkillGapItem
import com.cvpro.app.domain.model.SkillsGapAnalysis

fun List<SkillsGapDto>.toDomain(): SkillsGapAnalysis {
    return SkillsGapAnalysis(
        skills = map { it.toSkillGapItem() },
        recommendations = flatMap { item ->
            item.courses.map { it.toDomain() }
        },
    )
}

private fun SkillsGapDto.toSkillGapItem(): SkillGapItem {
    return SkillGapItem(
        name = skillName,
        userLevel = userLevel,
        marketLevel = marketLevel,
    )
}

private fun CourseDto.toDomain(): CourseRecommendation {
    return CourseRecommendation(
        id = id,
        title = title,
        platform = platform,
        url = url,
        logoUrl = logoUrl,
        duration = "$durationHours horas",
    )
}