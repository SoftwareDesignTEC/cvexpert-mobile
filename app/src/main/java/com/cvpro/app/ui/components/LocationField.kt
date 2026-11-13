package com.cvpro.app.ui.components

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import com.cvpro.app.domain.model.CostaRicaPlaces

/**
 * Campo de ubicación con sugerencias, como un buscador: al escribir "ques" aparece
 * "Ciudad Quesada, San Carlos, Alajuela" y basta con tocarla.
 *
 * Se puede escribir cualquier otra cosa (un lugar fuera de la lista o de otro país): las
 * sugerencias ayudan, no obligan. La lista sale de [CostaRicaPlaces].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    errorMessage: String? = null,
    placeholder: String? = null,
    imeAction: ImeAction = ImeAction.Next,
) {
    // Se abre al escribir y se cierra al elegir; así no tapa el campo de abajo sin motivo.
    var expanded by remember { mutableStateOf(false) }
    val suggestions = remember(value) {
        CostaRicaPlaces.search(value).filterNot { it.equals(value.trim(), ignoreCase = true) }
    }

    ExposedDropdownMenuBox(
        expanded = expanded && suggestions.isNotEmpty(),
        onExpandedChange = { expanded = it },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {
                onValueChange(it)
                expanded = true
            },
            label = { Text(text = label) },
            placeholder = placeholder?.let { { Text(text = it) } },
            leadingIcon = { Icon(Icons.Outlined.Place, contentDescription = null) },
            isError = errorMessage != null,
            supportingText = { if (errorMessage != null) Text(text = errorMessage) },
            keyboardOptions = KeyboardOptions(imeAction = imeAction),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable),
        )
        ExposedDropdownMenu(
            expanded = expanded && suggestions.isNotEmpty(),
            onDismissRequest = { expanded = false },
        ) {
            suggestions.forEach { place ->
                DropdownMenuItem(
                    text = { Text(place) },
                    leadingIcon = { Icon(Icons.Outlined.Place, contentDescription = null) },
                    onClick = {
                        onValueChange(place)
                        expanded = false
                    },
                )
            }
        }
    }
}
