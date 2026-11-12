package com.cvpro.app.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.cvpro.app.R

/** Las etiquetas son recursos de UI; solo canonicalValue se escribe en el CV. */
internal enum class CvSkillLevelOption(val canonicalValue: String, @get:StringRes val label: Int) {
    BEGINNER("beginner", R.string.cv_skill_level_beginner),
    INTERMEDIATE("intermediate", R.string.cv_skill_level_intermediate),
    ADVANCED("advanced", R.string.cv_skill_level_advanced),
    EXPERT("expert", R.string.cv_skill_level_expert);

    companion object {
        fun fromValue(value: String): CvSkillLevelOption? = entries.find { it.canonicalValue == value }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CvSkillLevelField(
    value: String,
    onValueChange: (String) -> Unit,
    errorMessage: String?,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = CvSkillLevelOption.fromValue(value)

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            // Los valores anteriores inválidos se muestran tal cual hasta que se corrijan.
            value = selected?.let { stringResource(it.label) } ?: value,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.cv_editor_level)) },
            placeholder = { Text(stringResource(R.string.cv_skill_level_unselected)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            isError = errorMessage != null,
            supportingText = { errorMessage?.let { Text(it) } },
            singleLine = true,
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            CvSkillLevelOption.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(stringResource(option.label)) },
                    onClick = {
                        onValueChange(option.canonicalValue)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
        }
    }
}
