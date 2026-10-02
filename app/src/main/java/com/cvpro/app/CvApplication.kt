package com.cvpro.app

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.SvgDecoder
import dagger.hilt.android.HiltAndroidApp

/**
 * Clase Application de la app.
 *
 * @HiltAndroidApp le dice a Hilt que genere aquí el contenedor de dependencias
 * de toda la app. Sin esta anotación ningún @AndroidEntryPoint funciona.
 * Se registra en el AndroidManifest con android:name=".CvApplication".
 *
 * También define cómo carga imágenes toda la app, implementando ImageLoaderFactory.
 */
@HiltAndroidApp
class CvApplication : Application(), ImageLoaderFactory {

    /**
     * Cargador de imágenes de la app, con soporte para SVG.
     *
     * Coil trae de fábrica PNG, JPG y WEBP, pero **no SVG**: baja el archivo, no sabe
     * decodificarlo y la carga falla en silencio. Los logos de empresa que manda el APIM
     * son `.svg`, así que sin este decodificador ninguno se ve y todas las tarjetas caen al
     * respaldo con la inicial.
     *
     * Coil busca esta interfaz en la clase Application, así que con implementarla alcanza:
     * no hay que pasar el cargador a cada AsyncImage.
     */
    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                add(SvgDecoder.Factory())
            }
            .build()
    }
}
