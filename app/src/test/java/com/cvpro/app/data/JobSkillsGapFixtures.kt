package com.cvpro.app.data

import com.cvpro.app.domain.model.*

internal fun jobGapCv(id: String = "cv1", level: String = "advanced") = Cv(
    id, "private-user", "CV de prueba", "template", "", "", null,
    UserProfile("private-user", "Nombre privado", "private@example.org", null, "", null, null,
        emptyList(), emptyList(), emptyList()),
    emptyList(), emptyList(), listOf(Skill("skill1", "Python", level, "technical")), emptyList(),
)

internal fun jobGapOffer(id: String = "job1", skills: List<String> = listOf("Python", "SQL")) = JobOffer(
    id, "Oferta de prueba", "Empresa", null, "Costa Rica", JobModality.REMOTE, "", "", "",
    listOf("Texto que no debe convertirse en habilidades"), null, 99, "", skills,
)

internal const val jobGapResponse = """
{
  "cvId":"cv1","dataSource":"client_request","catalogStatus":"consulted",
  "results":[{
    "jobId":"job1","status":"partial","requiredSkillsCount":2,"documentedMatchesCount":1,
    "documentedCoveragePercent":50,
    "matchedSkills":[{"name":"Python","cvLevel":"advanced"}],"notDocumentedSkills":["SQL"],
    "recommendations":[{"skillName":"SQL","catalogMatch":"matched","courses":[{
      "id":"10","title":"Curso de prueba","platform":"Plataforma","url":"https://example.org/course",
      "logoUrl":null,"durationHours":null,"isPaid":false,"skills":["SQL"]
    }]}]
  }]
}
"""
