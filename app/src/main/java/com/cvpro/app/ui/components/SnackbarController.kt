package com.cvpro.app.ui.components

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.compositionLocalOf

/**
 * Snackbar compartido de la app.
 *
 * El Scaffold de la raíz crea un único SnackbarHostState y lo comparte por acá, así
 * cualquier pantalla puede avisar algo ("Perfil guardado") sin montar su propio Scaffold.
 *
 * Es un `compositionLocalOf` sin valor por defecto a propósito: si alguien lo usa fuera de
 * la app, falla de inmediato con un mensaje claro en vez de no mostrar nada.
 */
val LocalSnackbarHostState = compositionLocalOf<SnackbarHostState> {
    error("No hay SnackbarHostState disponible: falta CompositionLocalProvider en CvProApp")
}
