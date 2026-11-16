package com.cvpro.app.ui.navigation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * Pide el permiso de avisos (POST_NOTIFICATIONS) una vez por sesión, justo después de entrar.
 *
 * Solo existe desde Android 13: antes los avisos venían permitidos de fábrica. Se pide después
 * del login y no al abrir la app porque los avisos son de la cuenta: antes de entrar no hay
 * nada que avisar. Si el usuario lo niega dos veces, Android deja de mostrar el diálogo por su
 * cuenta, así que no hace falta llevar la cuenta acá. La respuesta no se usa: PushNotifications
 * revisa el permiso cada vez que va a dibujar un aviso.
 */
@Composable
fun NotificationPermissionRequest(userId: String) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    // rememberSaveable: girar la pantalla no lo vuelve a pedir.
    var askedFor by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(userId) {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted && askedFor != userId) {
            askedFor = userId
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
