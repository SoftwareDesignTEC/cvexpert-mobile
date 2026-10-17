package com.cvpro.app.data.auth

import android.util.Log
import org.json.JSONObject
import java.util.Base64

/**
 * Lee cuándo vence un token de Google.
 *
 * Un id_token es un JWT: tres partes separadas por puntos —cabecera, contenido y firma—,
 * cada una en Base64 de URL. El vencimiento viene en el campo `exp` del contenido, en
 * segundos desde 1970.
 *
 * Acá **no se valida la firma** y no hace falta: de eso se encarga el APIM con su política
 * de validate-jwt. La app solo necesita saber si ya venció para no mandar una llamada que
 * sabe que va a fallar, y para avisarle al usuario en vez de mostrarle siete errores.
 *
 * Es la misma lectura que hace la web con jwt-decode, así los dos clientes cortan la sesión
 * en el mismo momento.
 */
object JwtExpiry {

    /**
     * Momento de vencimiento del token, en milisegundos, o null si no se puede leer.
     *
     * Devolver null significa "no sé": quien llama decide. Acá se prefiere dejar pasar el
     * token y que el servicio responda, antes que cerrarle la sesión a alguien por un
     * formato inesperado.
     */
    fun expiresAtMillis(idToken: String): Long? {
        return try {
            val payload = idToken.split('.').getOrNull(1) ?: return null

            // Base64 de URL y sin relleno, que es como viaja dentro de un JWT.
            val json = String(Base64.getUrlDecoder().decode(payload))

            val seconds = JSONObject(json).optLong(EXPIRY_CLAIM, 0L)
            if (seconds <= 0L) null else seconds * 1_000L
        } catch (exception: Exception) {
            // Un token con formato raro no debería tumbar la app.
            Log.w(TAG, "No se pudo leer el vencimiento del token", exception)
            null
        }
    }

    /**
     * true si el token ya venció, o si le queda menos del margen.
     *
     * El margen existe porque entre que se arma la petición y el APIM la valida pasa un
     * rato, y porque el reloj del teléfono y el de Google no son exactamente el mismo. Sin
     * él, un token con tres segundos de vida sale igual y vuelve 401.
     */
    fun isExpired(
        idToken: String,
        nowMillis: Long = System.currentTimeMillis(),
        marginMillis: Long = DEFAULT_MARGIN_MILLIS,
    ): Boolean {
        val expiresAt = expiresAtMillis(idToken) ?: return false
        return expiresAt - marginMillis <= nowMillis
    }

    private const val EXPIRY_CLAIM = "exp"

    /** Margen de seguridad: 30 segundos. */
    private const val DEFAULT_MARGIN_MILLIS = 30_000L

    private const val TAG = "JwtExpiry"
}
