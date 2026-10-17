package com.cvpro.app.data.auth

import android.util.Log
import org.json.JSONObject
import java.util.Base64

/**
 * Lee los datos (claims) que trae el id_token de Google.
 *
 * Un id_token es un JWT: tres partes separadas por puntos —cabecera, contenido y firma—,
 * cada una en Base64 de URL. Los datos van en la segunda parte, como JSON.
 *
 * Acá **no se valida la firma** y no hace falta: de eso se encarga el APIM con su política
 * de validate-jwt. La app solo lee lo que necesita: cuándo vence (ver [JwtExpiry]) y quién
 * es el usuario.
 */
object IdTokenClaims {

    /** El contenido del token como JSON, o null si no tiene el formato de un JWT. */
    fun payload(idToken: String): JSONObject? = try {
        val part = idToken.split('.').getOrNull(1)
        // Base64 de URL y sin relleno, que es como viaja dentro de un JWT.
        part?.let { JSONObject(String(Base64.getUrlDecoder().decode(it))) }
    } catch (exception: Exception) {
        // Un token con formato raro no debería tumbar la app.
        Log.w(TAG, "No se pudo leer el contenido del token", exception)
        null
    }

    /**
     * El `sub` del token: el identificador de la cuenta de Google, que no cambia aunque el
     * usuario cambie de correo.
     *
     * Es el id con el que el servicio de Usuarios guarda el perfil, el mismo que usa la web.
     */
    fun subject(idToken: String): String? =
        payload(idToken)?.optString(SUBJECT_CLAIM)?.takeIf { it.isNotBlank() }

    private const val SUBJECT_CLAIM = "sub"

    private const val TAG = "IdTokenClaims"
}
