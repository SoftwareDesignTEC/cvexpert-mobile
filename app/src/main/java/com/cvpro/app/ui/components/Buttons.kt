package com.cvpro.app.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Botón principal de la app: la acción más importante de cada pantalla.
 *
 * Mientras `isLoading` es true muestra una rueda y queda deshabilitado, así el usuario no
 * puede tocar dos veces y no hace falta repetir ese manejo en cada pantalla.
 */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
) {
    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        modifier = modifier.fillMaxWidth(),
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(20.dp),
            )
        } else {
            Text(text = text)
        }
    }
}

/** Botón secundario: acciones alternativas, menos importantes que la principal. */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(text = text)
    }
}

/**
 * Colores de los botones sin relleno.
 *
 * El texto va siempre en el azul de la marca. Se define acá y no en cada pantalla para
 * que todos los "Ver todos", "Ver detalles" y demás se vean iguales, y para cambiarlos
 * en un solo lugar si la paleta cambia.
 */
@Composable
fun outlinedOnCardColors(): ButtonColors = ButtonDefaults.outlinedButtonColors(
    containerColor = MaterialTheme.colorScheme.surface,
    contentColor = MaterialTheme.colorScheme.primary,
)
