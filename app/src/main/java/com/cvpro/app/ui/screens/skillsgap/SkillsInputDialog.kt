package com.cvpro.app.ui.screens.skillsgap

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.cvpro.app.R
import com.cvpro.app.domain.model.SkillLevel

@Composable
internal fun SkillsInputDialog(
    state: SkillsGapUiState,
    viewModel: SkillsGapViewModel,
) {
    val reviewing = state.inputStage == SkillsInputStage.REVIEW
    AlertDialog(
        onDismissRequest = viewModel::closeInput,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(if (reviewing) R.string.skills_gap_review_title else R.string.skills_gap_input_title))
                state.inputError?.let { message ->
                    Text(
                        stringResource(message),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        text = {
            val listState = key(reviewing) { rememberLazyListState() }
            LazyColumn(
                state = listState,
                modifier = Modifier.heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Text(stringResource(R.string.skills_gap_replace_notice))
                }
                if (reviewing) {
                    items(state.draft) { skill ->
                        Text(stringResource(R.string.skills_gap_review_skill, skill.skillName, levelLabel(skill.level)))
                    }
                } else {
                    itemsIndexed(state.draft) { index, skill ->
                        Column {
                            OutlinedTextField(
                                value = skill.skillName,
                                onValueChange = { viewModel.changeName(index, it) },
                                label = { Text(stringResource(R.string.skills_gap_skill_name)) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                            )
                            Row {
                                SkillLevelMenu(skill.level) { viewModel.changeLevel(index, it) }
                                TextButton(onClick = { viewModel.removeSkill(index) }) {
                                    Text(stringResource(R.string.skills_gap_remove_skill))
                                }
                            }
                        }
                    }
                    item {
                        OutlinedButton(onClick = viewModel::addSkill, enabled = state.draft.size < 500) {
                            Text(stringResource(R.string.skills_gap_add_skill))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = if (reviewing) viewModel::confirmInput else viewModel::reviewInput) {
                Text(stringResource(if (reviewing) R.string.skills_gap_save_skills else R.string.skills_gap_review))
            }
        },
        dismissButton = {
            TextButton(onClick = if (reviewing) viewModel::editInput else viewModel::closeInput) {
                Text(stringResource(if (reviewing) R.string.skills_gap_edit_draft else R.string.skills_gap_close_editor))
            }
        },
    )
}

@Composable
private fun SkillLevelMenu(level: SkillLevel?, onChange: (SkillLevel?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }) {
            Text(levelLabel(level))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            (SkillLevel.entries + null).forEach { option ->
                DropdownMenuItem(
                    text = { Text(levelLabel(option)) },
                    onClick = {
                        expanded = false
                        onChange(option)
                    },
                )
            }
        }
    }
}

@Composable
private fun levelLabel(level: SkillLevel?): String =
    stringResource(level?.labelRes ?: R.string.skills_gap_unknown_level)
