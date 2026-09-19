package com.cvpro.app.ui.components

import androidx.annotation.StringRes
import com.cvpro.app.R

/**
 * Reglas de validación de los formularios.
 *
 * Son funciones puras: reciben texto y devuelven el id del mensaje de error, o null si el
 * valor está bien. No conocen Compose, así que la misma regla sirve en cualquier pantalla
 * y se puede revisar de un vistazo.
 */
object FormValidation {

    /** Campo obligatorio: no puede quedar vacío ni solo con espacios. */
    @StringRes
    fun required(value: String): Int? =
        if (value.isBlank()) R.string.validation_required else null

    /** Correo electrónico con formato válido. */
    @StringRes
    fun email(value: String): Int? = when {
        value.isBlank() -> R.string.validation_required
        !EMAIL_REGEX.matches(value.trim()) -> R.string.validation_email
        else -> null
    }

    /**
     * Teléfono de Costa Rica: 8 dígitos, con o sin espacios o guiones. Es opcional, así que
     * un campo vacío se considera válido.
     */
    @StringRes
    fun phoneOptional(value: String): Int? {
        if (value.isBlank()) return null
        val digits = value.filter { it.isDigit() }
        return if (digits.length == PHONE_DIGITS) null else R.string.validation_phone
    }

    /** Largo mínimo, para campos como el nombre. */
    @StringRes
    fun minLength(value: String, min: Int): Int? = when {
        value.isBlank() -> R.string.validation_required
        value.trim().length < min -> R.string.validation_min_length
        else -> null
    }

    private const val PHONE_DIGITS = 8

    // Formato básico: algo@algo.dominio, sin espacios.
    private val EMAIL_REGEX = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$")
}
