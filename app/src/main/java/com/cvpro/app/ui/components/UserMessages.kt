package com.cvpro.app.ui.components

import androidx.annotation.StringRes
import com.cvpro.app.R
import retrofit2.HttpException
import java.io.IOException

/**
 * Convierte un error en un mensaje que el usuario entienda, en el idioma de la app.
 *
 * Antes las pantallas mostraban `exception.message` tal cual, y el usuario veía cosas como
 * "HTTP 404 Resource Not Found". El ViewModel guarda solo el id del texto ([StringRes]) y la
 * pantalla lo resuelve con `stringResource`, así el ViewModel no necesita un Context.
 */
@StringRes
fun Throwable.toUserMessage(): Int = when {
    // 400: el servicio rechazó lo que mandamos (un campo vacío o con formato inválido).
    this is HttpException && code() == HTTP_BAD_REQUEST -> R.string.error_invalid
    // 404 y 5xx: en la práctica es que el servicio o la ruta no están disponibles.
    this is HttpException && (code() == HTTP_NOT_FOUND || code() >= HTTP_SERVER_ERROR) ->
        R.string.error_unavailable
    // Sin internet, el servidor no contestó a tiempo o se cortó la conexión.
    this is IOException -> R.string.error_network
    else -> R.string.error_generic
}

/**
 * Aviso de una sola vez (snackbar) que el ViewModel arma sin tener un Context: el id del
 * texto y los valores que reemplazan sus `%1$s`.
 */
data class UiMessage(
    @param:StringRes val res: Int,
    val args: List<String> = emptyList(),
)

private const val HTTP_BAD_REQUEST = 400
private const val HTTP_NOT_FOUND = 404
private const val HTTP_SERVER_ERROR = 500
