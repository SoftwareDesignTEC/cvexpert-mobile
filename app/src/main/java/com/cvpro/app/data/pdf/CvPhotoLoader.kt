package com.cvpro.app.data.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.LruCache
import androidx.core.graphics.scale
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.min

/**
 * Convierte la foto del currículum (`personalInfo.photoUrl`) en una imagen lista para dibujar
 * en el PDF, en la vista previa y en "Ver currículum".
 *
 * Desde el 15 de noviembre la foto puede venir de dos formas, y hay que aguantar ambas porque
 * los CV viejos no se migraron:
 * - **Incrustada** (CV viejos): `data:image/jpeg;base64,...`. Se decodifica acá mismo.
 * - **Enlace** (CV nuevos): `https://cvexpertfotos.blob.core.windows.net/cv-photos/<uuid>.jpg`.
 *   Es público, así que se descarga sin token.
 *
 * Para el enlace se usa un OkHttpClient propio y no el de la app: el de la app le agrega el
 * token de Google a cada pedido, y ese token no tiene por qué salir hacia el almacenamiento.
 *
 * Si la foto no se puede leer (sin internet, enlace roto, formato raro), devuelve null y el CV
 * se dibuja sin foto. Igual que en la web: mejor un PDF sin foto que ningún PDF.
 */
@Singleton
class CvPhotoLoader @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {

    private val client by lazy {
        OkHttpClient.Builder()
            .callTimeout(DOWNLOAD_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Fotos ya preparadas. La clave de una foto incrustada es su hash y su largo: guardar el
     * texto entero (decenas de KB) como clave no aporta nada.
     */
    private val cache = LruCache<String, Bitmap>(MAX_CACHED_PHOTOS)

    /** La foto como un cuadrado de [PHOTO_SIDE_PX], o null si no hay o no se pudo leer. */
    suspend fun load(photoUrl: String?): Bitmap? {
        val source = photoUrl?.trim().orEmpty()
        if (source.isEmpty()) return null
        val key = if (source.startsWith("data:")) "data:${source.hashCode()}:${source.length}" else source
        cache.get(key)?.let { return it }

        return try {
            val bytes = withContext(Dispatchers.IO) { readBytes(source) } ?: return null
            withContext(Dispatchers.Default) { decodeSquare(bytes) }?.also { cache.put(key, it) }
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            null
        }
    }

    /**
     * Prepara una foto elegida del teléfono para subirla: recortada al cuadrado por el centro,
     * de [PHOTO_SIDE_PX] y en JPEG, como hace la web. Una foto de cámara pesa varios MB; así
     * queda en unos 35 KB. Null si lo elegido no es una imagen o no se pudo leer.
     *
     * @param uri lo que devuelve el selector de fotos de Android.
     */
    suspend fun prepareForUpload(uri: Uri): ByteArray? = withContext(Dispatchers.IO) {
        val bytes = context.contentResolver.openInputStream(uri)?.use { input ->
            readLimited(input, MAX_PICKED_BYTES)
        } ?: return@withContext null
        val square = decodeSquare(bytes) ?: return@withContext null
        ByteArrayOutputStream().use { output ->
            square.compress(CompressFormat.JPEG, JPEG_QUALITY, output)
            output.toByteArray()
        }
    }

    /** Recuerda la foto recién subida, para no descargar enseguida lo que ya se tiene. */
    fun remember(photoUrl: String, jpeg: ByteArray) {
        BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size)?.let { cache.put(photoUrl, it) }
    }

    /** Los bytes de la imagen, salgan del texto o de internet. */
    private fun readBytes(source: String): ByteArray? = when {
        // "data:image/png;base64,iVBOR...": lo que va después de la coma es la imagen.
        source.startsWith("data:image", ignoreCase = true) && ";base64," in source ->
            Base64.decode(source.substringAfter(","), Base64.DEFAULT)
        // Solo https: un enlace http dejaría ver la foto a cualquiera en la misma red.
        source.startsWith("https://", ignoreCase = true) -> download(source)
        else -> null
    }

    private fun download(url: String): ByteArray? {
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val body = response.body ?: return null
            // El servicio no acepta fotos de más de 5 MB; algo más grande no es una foto nuestra.
            return body.byteStream().use { input -> readLimited(input, MAX_PHOTO_BYTES) }
        }
    }

    /** Lee todo [input], o null si pasa de [limit] bytes (no se carga en memoria algo enorme). */
    private fun readLimited(input: InputStream, limit: Int): ByteArray? {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(BUFFER_SIZE)
        while (true) {
            val read = input.read(buffer)
            if (read == -1) break
            output.write(buffer, 0, read)
            if (output.size() > limit) return null
        }
        return output.toByteArray()
    }

    /**
     * Recorta al cuadrado por el centro y la deja en [PHOTO_SIDE_PX], como hace la web al
     * subirla. En la hoja ocupa 22 mm: más píxeles solo engordarían el PDF.
     */
    private fun decodeSquare(bytes: ByteArray): Bitmap? {
        // Primero solo las medidas, para no cargar entera una foto de cámara de 12 megapíxeles.
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sampleSize = 1
        while (min(bounds.outWidth, bounds.outHeight) / (sampleSize * 2) >= PHOTO_SIDE_PX) sampleSize *= 2
        val decoded = BitmapFactory.decodeByteArray(
            bytes, 0, bytes.size,
            BitmapFactory.Options().apply { inSampleSize = sampleSize },
        ) ?: return null

        val side = min(decoded.width, decoded.height)
        val square = Bitmap.createBitmap(decoded, (decoded.width - side) / 2, (decoded.height - side) / 2, side, side)
        val target = max(1, min(side, PHOTO_SIDE_PX))
        return if (side == target) square else square.scale(target, target)
    }

    companion object {
        /** El lado de la foto ya preparada, igual que el que sube la web. */
        const val PHOTO_SIDE_PX = 512

        /** Calidad del JPEG que se sube: la misma que usa la web (0,82). */
        private const val JPEG_QUALITY = 82

        private const val MAX_PHOTO_BYTES = 5 * 1024 * 1024

        /** Una foto de cámara puede pesar más que eso; se achica antes de subir. */
        private const val MAX_PICKED_BYTES = 30 * 1024 * 1024
        private const val MAX_CACHED_PHOTOS = 8
        private const val DOWNLOAD_TIMEOUT_SECONDS = 15L
        private const val BUFFER_SIZE = 8 * 1024
    }
}
