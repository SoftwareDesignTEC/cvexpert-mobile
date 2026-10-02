package com.cvpro.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter

/**
 * Imagen que viene del servicio, con algo que mostrar cuando no se puede mostrar.
 *
 * Son dos casos distintos y los dos terminan igual de mal si no se cubren:
 *
 * 1. La URL llega vacía o nula. Antes se revisaba con `!= null`, que deja pasar la cadena
 *    vacía: el mock manda "" y la imagen se dibujaba sin nada adentro.
 * 2. La URL llega pero la imagen no carga —404, sin internet, el dominio caído—. Acá no
 *    había defensa en ninguna pantalla: quedaba el recuadro de color vacío.
 *
 * Es el mismo criterio que usa la app web, que revisa la URL y además escucha `onError`
 * para caer a un ícono. Mantenerlo igual es parte de la paridad web–móvil.
 *
 * @param fallback qué dibujar cuando no hay imagen. Cada pantalla pasa lo suyo: la inicial
 * de la empresa en las tarjetas, un ícono en las plantillas.
 */
@Composable
fun RemoteImage(
    url: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    fallback: @Composable () -> Unit,
) {
    // Se reinicia si cambia la URL: una imagen que falló no debe condenar a la siguiente.
    var failed by remember(url) { mutableStateOf(false) }

    if (url.isNullOrBlank() || failed) {
        fallback()
        return
    }

    AsyncImage(
        model = url,
        contentDescription = contentDescription,
        contentScale = contentScale,
        onState = { state ->
            if (state is AsyncImagePainter.State.Error) failed = true
        },
        modifier = modifier,
    )
}
