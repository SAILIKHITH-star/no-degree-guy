package com.example.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.ContextCompat
import com.example.data.InstituteConfig
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.tasks.await
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToLong
import kotlin.math.sin
import kotlin.math.sqrt

data class DeviceCoordinates(
    val latitude: Double,
    val longitude: Double
)

object LocationHelper {

    fun hasCoarseLocationPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    suspend fun fetchCurrentLocation(context: Context): Result<DeviceCoordinates> {
        if (!hasCoarseLocationPermission(context)) {
            return Result.failure(SecurityException("Location permission not granted"))
        }
        return runCatching {
            val client = LocationServices.getFusedLocationProviderClient(context)
            val cts = CancellationTokenSource()
            val fresh = client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token).await()
            val loc = fresh ?: client.lastLocation.await()
                ?: throw IllegalStateException("Location unavailable right now. Please enable GPS or try again.")
            DeviceCoordinates(loc.latitude, loc.longitude)
        }
    }

    fun distanceMeters(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Long {
        val earthRadiusMeters = 6_371_000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return (earthRadiusMeters * c).roundToLong().coerceAtLeast(0L)
    }

    fun formatDistance(meters: Long): String {
        return when {
            meters < 1000L -> "$meters m away"
            meters < 100_000L -> String.format("%.1f km away", meters / 1000.0)
            else -> String.format("%.0f km away", meters / 1000.0)
        }
    }

    private fun campusAddressQuery(config: InstituteConfig): String {
        return "${config.centerName}, ${config.address}, ${config.city}"
    }

    /**
     * Opens Google Maps directly pinned to Rupa IIT / NEET Study Circle campus in NGO Colony, Chinna Chowk, Kadapa.
     */
    fun openCampusInMaps(context: Context, config: InstituteConfig): Boolean {
        val encodedQuery = Uri.encode(campusAddressQuery(config))
        val geoUri = Uri.parse("geo:${config.latitude},${config.longitude}?q=$encodedQuery")
        val geoIntent = Intent(Intent.ACTION_VIEW, geoUri)
        if (runCatching { context.startActivity(geoIntent); true }.getOrDefault(false)) {
            return true
        }
        val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=$encodedQuery")
        return runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
            true
        }.getOrDefault(false)
    }

    /**
     * Launches live Google Maps turn-by-turn directions & route distance directly to the campus location.
     */
    fun openCampusDirections(context: Context, config: InstituteConfig): Boolean {
        val encodedDest = Uri.encode("${config.address}, ${config.city}")
        val dirUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$encodedDest&travelmode=driving")
        if (runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, dirUri)); true }.getOrDefault(false)) {
            return true
        }
        val coordDirUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${config.latitude},${config.longitude}")
        return runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, coordDirUri))
            true
        }.getOrDefault(false)
    }

    fun shareCampusLocation(context: Context, config: InstituteConfig) {
        val encodedDest = Uri.encode("${config.address}, ${config.city}")
        val mapsLink = "https://www.google.com/maps/dir/?api=1&destination=$encodedDest"
        val body = buildString {
            appendLine(config.centerName)
            appendLine("Founder & Director: ${config.directorName}")
            appendLine("${config.address}, ${config.city}")
            appendLine("Timings: ${config.batchTimings}")
            appendLine("Monthly Fee: ${ContactHelper.formatInr(config.monthlyFeeInr)} / month")
            appendLine("Main Phone / WhatsApp: ${ContactHelper.formatPhoneDisplay(config.mainPhone)}")
            val alts = config.alternativePhones()
            if (alts.isNotEmpty()) {
                appendLine("Alt Numbers: ${alts.joinToString(", ") { ContactHelper.formatPhoneDisplay(it) }}")
            }
            append("Directions: $mapsLink")
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "${config.centerName} - Campus Location & Contact")
            putExtra(Intent.EXTRA_TEXT, body)
        }
        runCatching {
            context.startActivity(Intent.createChooser(intent, "Share Campus Location"))
        }
    }
}
