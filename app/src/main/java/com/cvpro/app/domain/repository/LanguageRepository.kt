package com.cvpro.app.domain.repository

/** Idioma en el que se ve la app, elegido en Configuración. */
interface LanguageRepository {

    /** False en Android 12 o anterior: ahí la app sigue siempre el idioma del teléfono. */
    val canChangeLanguage: Boolean

    fun current(): AppLanguage

    /** Lo aplica de inmediato (Android recrea la pantalla) y el sistema lo recuerda. */
    fun set(language: AppLanguage)
}

/**
 * Idiomas de la app.
 *
 * @property tag código BCP 47 que se le pasa a Android; null = seguir el idioma del teléfono.
 */
enum class AppLanguage(val tag: String?) {
    SYSTEM(null),
    SPANISH("es"),
    ENGLISH("en"),
}
