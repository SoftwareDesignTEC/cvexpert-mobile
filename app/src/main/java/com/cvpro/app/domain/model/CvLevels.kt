package com.cvpro.app.domain.model

import androidx.annotation.StringRes
import com.cvpro.app.R

/**
 * Niveles que acepta el servicio de CVs (enums `Skill.level` y `Language.level` del contrato).
 *
 * El servidor siempre los manda y los espera en inglés, tal cual el enum (confirmado por Pérez):
 * la traducción es trabajo de cada cliente. Por eso cada nivel guarda su valor del contrato
 * ([apiValue]) y el texto que ve el usuario ([labelRes]), que sale de strings.xml.
 *
 * El texto vive aquí y no en la UI porque lo usan tanto las pantallas como el generador de PDF:
 * así hay una sola tabla de traducción.
 */
interface CvLevel {
    val apiValue: String

    @get:StringRes
    val labelRes: Int
}

enum class SkillLevel(override val apiValue: String, override val labelRes: Int) : CvLevel {
    BEGINNER("beginner", R.string.cv_level_beginner),
    INTERMEDIATE("intermediate", R.string.cv_level_intermediate),
    ADVANCED("advanced", R.string.cv_level_advanced),
    EXPERT("expert", R.string.cv_level_expert),
    ;

    companion object {
        fun fromApi(value: String): SkillLevel? = entries.firstOrNull { it.apiValue == value }
    }
}

enum class LanguageLevel(override val apiValue: String, override val labelRes: Int) : CvLevel {
    BASIC("basic", R.string.cv_level_basic),
    INTERMEDIATE("intermediate", R.string.cv_level_intermediate),
    ADVANCED("advanced", R.string.cv_level_advanced),
    NATIVE("native", R.string.cv_level_native),
    ;

    companion object {
        fun fromApi(value: String): LanguageLevel? = entries.firstOrNull { it.apiValue == value }
    }
}
