package com.qb.secondbrain.context

import android.annotation.SuppressLint
import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import javax.inject.Inject
import kotlin.coroutines.resume

class LocationProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val locationManager: LocationManager
        get() = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): Location? {
        // Try last known location first
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        for (provider in providers) {
            try {
                val lastKnown = locationManager.getLastKnownLocation(provider)
                if (lastKnown != null) {
                    return lastKnown
                }
            } catch (_: Exception) {
                continue
            }
        }

        // Request single update
        return suspendCancellableCoroutine { continuation ->
            val listener = object : android.location.LocationListener {
                override fun onLocationChanged(location: Location) {
                    locationManager.removeUpdates(this)
                    if (continuation.isActive) {
                        continuation.resume(location)
                    }
                }

                override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) {}

                override fun onProviderEnabled(provider: String) {}

                override fun onProviderDisabled(provider: String) {}
            }

            continuation.invokeOnCancellation {
                try {
                    locationManager.removeUpdates(listener)
                } catch (_: Exception) {
                    // Ignore
                }
            }

            for (provider in providers) {
                try {
                    if (locationManager.isProviderEnabled(provider)) {
                        locationManager.requestSingleUpdate(provider, listener, null)
                        return@suspendCancellableCoroutine
                    }
                } catch (_: Exception) {
                    continue
                }
            }

            // No provider available
            if (continuation.isActive) {
                continuation.resume(null)
            }
        }
    }

    suspend fun getAddress(latitude: Double, longitude: Double): String? {
        return try {
            val geocoder = Geocoder(context, Locale.CHINA)
            val addresses: List<Address>? = withContextSafely {
                geocoder.getFromLocation(latitude, longitude, 1)
            }
            val address = addresses?.firstOrNull()
            address?.let {
                buildString {
                    it.locality?.let { append(it) }
                    it.subLocality?.let { append(it) }
                    it.thoroughfare?.let { append(it) }
                    it.subThoroughfare?.let { append(it) }
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    @Suppress("DEPRECATION")
    private suspend fun <T> withContextSafely(block: () -> T): T {
        // Geocoder.getFromLocation is synchronous; wrap for coroutine compatibility
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            block()
        }
    }
}
