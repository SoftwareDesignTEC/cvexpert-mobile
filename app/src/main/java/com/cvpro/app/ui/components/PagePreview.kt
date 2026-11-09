package com.cvpro.app.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale

/**
 * Primera hoja de un currículum, ya dibujada (ver data/pdf/CvPreviewRenderer.kt).
 *
 * Se recorta desde arriba: en una tarjeta baja lo que importa es el encabezado con el nombre
 * y las primeras secciones, no el pie de la hoja.
 */
@Composable
fun PagePreview(
    page: Bitmap,
    modifier: Modifier = Modifier,
) {
    // asImageBitmap no copia los píxeles, pero igual se recuerda para no envolverlo en cada
    // recomposición.
    val image = remember(page) { page.asImageBitmap() }
    Image(
        bitmap = image,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        alignment = Alignment.TopCenter,
        modifier = modifier,
    )
}
