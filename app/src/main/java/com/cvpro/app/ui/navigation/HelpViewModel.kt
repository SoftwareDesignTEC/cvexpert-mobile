package com.cvpro.app.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cvpro.app.domain.repository.SettingsContentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Correo de soporte para el "Centro de ayuda" del menú lateral.
 *
 * Es un ViewModel aparte y mínimo a propósito: el menú vive fuera de las pantallas, así que
 * no puede usar el de Configuración, y traerse ese entero solo por una dirección de correo
 * cargaría sesiones, políticas y términos que el menú no necesita.
 *
 * Si la llamada falla, la dirección queda en null y el botón abre el correo sin
 * destinatario en vez de no hacer nada.
 */
@HiltViewModel
class HelpViewModel @Inject constructor(
    private val repository: SettingsContentRepository,
) : ViewModel() {

    private val _supportEmail = MutableStateFlow<String?>(null)
    val supportEmail: StateFlow<String?> = _supportEmail.asStateFlow()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            runCatching { repository.getHelpContacts() }
                .onSuccess { contacts ->
                    // El canal de soporte es el que corresponde al menú de ayuda. Si el
                    // servicio no lo trae, sirve cualquiera antes que ninguno.
                    _supportEmail.value = contacts
                        .firstOrNull { it.type == SUPPORT_TYPE }
                        ?.email
                        ?: contacts.firstOrNull()?.email
                }
        }
    }

    private companion object {
        const val SUPPORT_TYPE = "support"
    }
}
