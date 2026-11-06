package com.cvpro.app.data.locale

import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList
import com.cvpro.app.domain.repository.AppLanguage
import com.cvpro.app.domain.repository.LanguageRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cambia el idioma de la app con LocaleManager, que viene con Android 13 (API 33).
 *
 * El sistema guarda la elección por su cuenta (sobrevive a cerrar la app y a reiniciar el
 * teléfono) y recrea la Activity, así que la app no guarda nada ni repinta a mano. Es el mismo
 * ajuste que aparece en Ajustes del teléfono > Apps > CVExpert > Idioma, gracias a
 * res/xml/locales_config.xml.
 *
 * En Android 8 a 12 no existe idioma por app sin agregar la librería appcompat, así que ahí
 * la app sigue el idioma del teléfono y la opción se muestra deshabilitada.
 */
@Singleton
class SystemLanguageRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) : LanguageRepository {

    override val canChangeLanguage: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    override fun current(): AppLanguage {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return AppLanguage.SYSTEM
        val locales = context.getSystemService(LocaleManager::class.java).applicationLocales
        if (locales.isEmpty) return AppLanguage.SYSTEM
        return when (locales[0].language) {
            "es" -> AppLanguage.SPANISH
            "en" -> AppLanguage.ENGLISH
            else -> AppLanguage.SYSTEM
        }
    }

    override fun set(language: AppLanguage) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        context.getSystemService(LocaleManager::class.java).applicationLocales =
            language.tag?.let { LocaleList.forLanguageTags(it) } ?: LocaleList.getEmptyLocaleList()
    }
}
