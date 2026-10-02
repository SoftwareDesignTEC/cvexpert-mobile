package com.cvpro.app.domain.model

/**
 * Un canal de contacto del centro de ayuda.
 *
 * @param type clave del canal ("support", "general", "privacy"). No se muestra: sirve para
 * distinguirlos si más adelante cada uno necesita un ícono o un trato distinto.
 * @param label texto que ve el usuario, ya en español desde el servicio.
 * @param email dirección a la que se escribe.
 */
data class HelpContact(
    val type: String,
    val label: String,
    val email: String,
)
