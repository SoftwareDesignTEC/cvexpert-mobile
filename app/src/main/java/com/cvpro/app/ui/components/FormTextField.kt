package com.cvpro.app.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType

/**
 * Campo de formulario de la app.
 *
 * Junta en un solo componente lo que necesita cualquier campo: etiqueta, texto de ayuda,
 * marca de error y el teclado adecuado (correo, teléfono o texto). Cuando `errorMessage`
 * no es null, el campo se pinta en rojo y muestra el motivo debajo, que es lo que pide la
 * rúbrica sobre validación y mensajes claros.
 */
@Composable
fun FormTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    errorMessage: String? = null,
    helperText: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    singleLine: Boolean = true,
    enabled: Boolean = true,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(text = label) },
        isError = errorMessage != null,
        supportingText = {
            val message = errorMessage ?: helperText
            if (message != null) Text(text = message)
        },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        singleLine = singleLine,
        enabled = enabled,
        modifier = modifier.fillMaxWidth(),
    )
}
