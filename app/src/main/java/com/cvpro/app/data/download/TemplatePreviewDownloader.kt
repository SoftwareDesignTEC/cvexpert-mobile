package com.cvpro.app.data.download

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Baja la imagen de vista previa de una plantilla y la deja en Descargas.
 *
 * Una aclaración importante para defender esto: el servicio **no** entrega el archivo de la
 * plantilla. El contrato solo expone `GET /templates`, y de cada plantilla lo único
 * descargable es su `previewUrl`, que es la miniatura. Así que esto guarda la miniatura, no
 * una plantilla lista para llenar. Cuando exista un endpoint que entregue el archivo real,
 * se cambia la URL de origen y el resto de la clase sirve igual.
 *
 * La conexión se abre con HttpURLConnection y no con el OkHttp de la app a propósito: ese
 * cliente lleva el interceptor que adjunta el token de Google, y la miniatura puede estar
 * alojada en un dominio ajeno al APIM. No hay razón para mandarle el token a un tercero.
 *
 * El archivo va a Descargas igual que el PDF del currículum: con MediaStore desde Android
 * 10, y en la carpeta propia de la app en versiones anteriores. Ninguno de los dos caminos
 * pide permisos.
 */
@Singleton
class TemplatePreviewDownloader @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {

    /**
     * Guarda la vista previa y devuelve dónde quedó, lista para abrirse.
     *
     * @throws IllegalStateException si la plantilla no trae vista previa o el servidor no
     * la entrega. La pantalla convierte ese error en un mensaje para el usuario.
     */
    suspend fun download(
        templateName: String,
        previewUrl: String,
    ): Uri = withContext(Dispatchers.IO) {

        check(previewUrl.isNotBlank()) { "Esta plantilla no trae vista previa" }

        val connection = (URL(previewUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            instanceFollowRedirects = true
        }

        try {
            check(connection.responseCode in 200..299) {
                "El servidor respondió ${connection.responseCode}"
            }

            val mimeType = connection.contentType?.substringBefore(';')?.trim()
                ?.takeIf { it.startsWith("image/") }
                ?: DEFAULT_MIME

            val bytes = connection.inputStream.use { it.readBytes() }

            save(
                fileName = fileNameFor(templateName, mimeType),
                mimeType = mimeType,
                bytes = bytes,
            )
        } finally {
            connection.disconnect()
        }
    }

    /** "Creativo UI UX.png": sin caracteres que el sistema de archivos no acepta. */
    private fun fileNameFor(templateName: String, mimeType: String): String {
        val base = templateName
            .replace(Regex("[^A-Za-z0-9 ]"), "")
            .trim()
            .ifBlank { "plantilla" }

        val extension = when (mimeType) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            "image/jpeg" -> "jpg"
            else -> "jpg"
        }

        return "$base.$extension"
    }

    private fun save(fileName: String, mimeType: String, bytes: ByteArray): Uri =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, mimeType)
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = requireNotNull(
                resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values),
            ) { "No se pudo crear el archivo en Descargas" }

            resolver.openOutputStream(uri)?.use { it.write(bytes) }
            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            uri
        } else {
            val carpeta = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            val archivo = File(carpeta, fileName)
            archivo.outputStream().use { it.write(bytes) }
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", archivo)
        }

    private companion object {
        const val TIMEOUT_MS = 20_000
        const val DEFAULT_MIME = "image/jpeg"
    }
}
