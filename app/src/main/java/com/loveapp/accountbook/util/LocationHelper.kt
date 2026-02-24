package com.loveapp.accountbook.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.LocationManager
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import kotlinx.coroutines.*
import java.util.*

/**
 * 通用自动定位工具，可在任意 Fragment/Activity 中复用。
 * 静默获取当前位置并通过回调返回地址字符串。
 */
class LocationHelper(private val context: Context) {

    private val fusedClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)
    private val timeoutHandler = Handler(Looper.getMainLooper())

    /** 检查是否已有定位权限 */
    fun hasPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    /** 静默获取位置，成功后回调地址字符串 */
    @SuppressLint("MissingPermission")
    fun fetchLocation(onResult: (String) -> Unit) {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val gps = lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val network = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        if (!gps && !network) return

        fusedClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                reverseGeocode(location.latitude, location.longitude, onResult)
            } else {
                val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                    .setMaxUpdates(1)
                    .setDurationMillis(10000)
                    .build()
                val callback = object : LocationCallback() {
                    override fun onLocationResult(result: LocationResult) {
                        fusedClient.removeLocationUpdates(this)
                        timeoutHandler.removeCallbacksAndMessages(null)
                        result.lastLocation?.let { reverseGeocode(it.latitude, it.longitude, onResult) }
                    }
                }
                fusedClient.requestLocationUpdates(request, callback, Looper.getMainLooper())
                timeoutHandler.postDelayed({ fusedClient.removeLocationUpdates(callback) }, 10000)
            }
        }
    }

    private fun reverseGeocode(lat: Double, lng: Double, onResult: (String) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            var address = ""
            try {
                @Suppress("DEPRECATION")
                val results = Geocoder(context, Locale.CHINA).getFromLocation(lat, lng, 1)
                val addr = results?.firstOrNull()
                if (addr != null) {
                    address = addr.getAddressLine(0) ?: buildString {
                        addr.adminArea?.let { append(it) }
                        addr.locality?.let { append(it) }
                        addr.subLocality?.let { append(it) }
                        addr.thoroughfare?.let { append(it) }
                        addr.featureName?.let { f ->
                            if (f != addr.thoroughfare) append(f)
                        }
                    }
                }
            } catch (_: Exception) {}
            if (address.isEmpty()) address = "($lat, $lng)"
            withContext(Dispatchers.Main) {
                onResult(address)
            }
        }
    }
}
