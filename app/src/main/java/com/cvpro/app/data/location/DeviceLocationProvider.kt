package com.cvpro.app.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Averigua en qué lugar está el teléfono, para llenar la ubicación del perfil sin escribirla.
 *
 * Usa solo lo que trae Android (LocationManager y Geocoder), sin librerías de Google Play. Pide
 * la ubicación **aproximada** (red/wifi): para "Grecia, Alajuela" no hace falta el GPS exacto, y
 * así el permiso que ve el usuario es el menos invasivo.
 *
 * Devuelve null si no hay permiso, si el teléfono no da ubicación a tiempo o si no se pudo pasar
 * a un nombre de lugar. Quien llama decide qué mensaje mostrar.
 */
@Singleton
class DeviceLocationProvider @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {

    /** Si el usuario ya dio el permiso de ubicación aproximada. */
    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    /** "Grecia, Alajuela", o null si no se pudo. */
    suspend fun currentPlace(): String? {
        if (!hasPermission()) return null
        val location = currentLocation() ?: return null
        val address = address(location) ?: return null
        return placeName(address)
    }

    /**
     * Una ubicación del teléfono. Prueba los proveedores en orden (fused, red, GPS) y usa el
     * primero que responda: según el teléfono, la red puede estar activa pero sin datos. Se
     * espera como mucho [LOCATION_TIMEOUT_MS] por cada uno; si ninguno da una nueva, la última
     * conocida.
     */
    @SuppressLint("MissingPermission") // hasPermission() se revisa en currentPlace().
    private suspend fun currentLocation(): Location? {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        val providers = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) add(LocationManager.FUSED_PROVIDER)
            add(LocationManager.NETWORK_PROVIDER)
            add(LocationManager.GPS_PROVIDER)
        }.filter { runCatching { manager.isProviderEnabled(it) }.getOrDefault(false) }

        for (provider in providers) {
            // Con solo el permiso aproximado, algunos Android no dejan pedirle al GPS: se salta.
            val fresh: Location? = try {
                withTimeoutOrNull(LOCATION_TIMEOUT_MS) {
                    suspendCancellableCoroutine<Location?> { continuation ->
                        val signal = CancellationSignal()
                        continuation.invokeOnCancellation { signal.cancel() }
                        LocationManagerCompat.getCurrentLocation(manager, provider, signal, executor) { location ->
                            if (continuation.isActive) continuation.resume(location)
                        }
                    }
                }
            } catch (exception: SecurityException) {
                null
            }
            if (fresh != null) return fresh
        }
        return providers.firstNotNullOfOrNull { runCatching { manager.getLastKnownLocation(it) }.getOrNull() }
    }

    /** Pasa las coordenadas a una dirección. Corre fuera del hilo principal: usa la red. */
    private suspend fun address(location: Location): Address? = withContext(Dispatchers.IO) {
        if (!Geocoder.isPresent()) return@withContext null
        val geocoder = Geocoder(context, Locale.forLanguageTag("es-CR"))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            withTimeoutOrNull(LOCATION_TIMEOUT_MS) {
                suspendCancellableCoroutine { continuation ->
                    geocoder.getFromLocation(location.latitude, location.longitude, 1, object : Geocoder.GeocodeListener {
                        override fun onGeocode(addresses: MutableList<Address>) {
                            if (continuation.isActive) continuation.resume(addresses.firstOrNull())
                        }

                        override fun onError(errorMessage: String?) {
                            if (continuation.isActive) continuation.resume(null)
                        }
                    })
                }
            }
        } else {
            // Antes de Android 13 solo existe la versión que bloquea; por eso va en IO.
            @Suppress("DEPRECATION")
            runCatching { geocoder.getFromLocation(location.latitude, location.longitude, 1)?.firstOrNull() }
                .getOrNull()
        }
    }

    /**
     * "Grecia, Alajuela": cantón o ciudad y provincia, sin "Provincia de" y sin repetir
     * ("Cartago, Cartago" queda "Cartago"). Es el mismo formato de las ofertas.
     */
    private fun placeName(address: Address): String? {
        val parts = listOfNotNull(address.locality ?: address.subAdminArea, address.adminArea)
            .map { it.replace(PROVINCE_PREFIX, "").trim() }
            .filter { it.isNotEmpty() }
            .distinct()
        return parts.joinToString(", ").ifEmpty { null }
    }

    private companion object {
        const val LOCATION_TIMEOUT_MS = 8_000L
        val PROVINCE_PREFIX = Regex("(?i)^provincia de\\s+")
        /** Un solo hilo basta: el callback solo despierta a la corrutina. */
        val executor = Executors.newSingleThreadExecutor()
    }
}
