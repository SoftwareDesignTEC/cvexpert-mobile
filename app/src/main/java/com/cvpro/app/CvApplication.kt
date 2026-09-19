package com.cvpro.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Clase Application de la app.
 *
 * @HiltAndroidApp le dice a Hilt que genere aquí el contenedor de dependencias
 * de toda la app. Sin esta anotación ningún @AndroidEntryPoint funciona.
 * Se registra en el AndroidManifest con android:name=".CvApplication".
 */
@HiltAndroidApp
class CvApplication : Application()
