package com.cvpro.app.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.cvpro.app.R
import com.cvpro.app.ui.screens.cvs.PhotoPhase

/**
 * La foto del currículum en el editor: la foto actual (o una silueta), "Elegir foto" y "Quitar".
 *
 * Mientras trabaja dice en qué está, "Preparando…" o "Subiendo…", porque duran distinto:
 * recortar es instantáneo y subir depende de la red. Un solo "Cargando" dejaría al usuario sin
 * saber si se quedó pegado. Es la misma idea que el campo de la web.
 *
 * @param hasPhoto si el CV tiene foto, aunque todavía no se haya podido mostrar.
 */
@Composable
fun CvPhotoField(
    preview: Bitmap?,
    hasPhoto: Boolean,
    phase: PhotoPhase,
    errorMessage: String?,
    onPickClick: () -> Unit,
    onRemoveClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val busy = phase != PhotoPhase.IDLE
    Column(modifier = modifier.padding(vertical = 4.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            ) {
                when {
                    busy -> CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                    preview != null -> Image(
                        bitmap = preview.asImageBitmap(),
                        contentDescription = stringResource(R.string.cv_photo_description),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                    else -> Icon(
                        imageVector = Icons.Outlined.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(32.dp),
                    )
                }
            }
            Column {
                Text(
                    text = stringResource(R.string.cv_photo_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = onPickClick, enabled = !busy, colors = outlinedOnCardColors()) {
                        Icon(Icons.Outlined.AddAPhoto, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(
                            text = stringResource(
                                when (phase) {
                                    PhotoPhase.PREPARING -> R.string.cv_photo_preparing
                                    PhotoPhase.UPLOADING -> R.string.cv_photo_uploading
                                    PhotoPhase.IDLE -> if (hasPhoto) R.string.cv_photo_change else R.string.cv_photo_pick
                                },
                            ),
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                    if (hasPhoto && !busy) {
                        TextButton(onClick = onRemoveClick) {
                            Text(stringResource(R.string.cv_photo_remove), color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
        Text(
            text = errorMessage ?: stringResource(R.string.cv_photo_hint),
            style = MaterialTheme.typography.bodySmall,
            color = if (errorMessage != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}
