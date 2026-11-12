package com.cvpro.app.ui.screens.cvs

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Currículum que Mis Currículums tiene que abrir en el editor apenas se muestre.
 *
 * Lo deja Plantillas al crear un CV, para que el usuario caiga directo a llenarlo en vez de
 * tener que buscarlo en la lista. Es un objeto compartido (@Singleton) y no un parámetro de
 * la ruta porque las rutas del menú son fijas: con un parámetro, la barra superior y el menú
 * dejarían de reconocer en qué pantalla está el usuario.
 */
@Singleton
class PendingCvEditor @Inject constructor() {

    private var cvId: String? = null

    /** Pide abrir este currículum la próxima vez que se muestre Mis Currículums. */
    fun request(id: String) {
        cvId = id
    }

    /** Devuelve el pedido y lo borra, para que el editor no se vuelva a abrir solo. */
    fun take(): String? = cvId.also { cvId = null }
}
