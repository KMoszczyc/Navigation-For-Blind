package com.example.NavigationForBlind.DeviceData

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.NavigationForBlind.SettingsUtils.PreferredLanguage
import com.example.NavigationForBlind.SettingsUtils.Utils
import com.example.NavigationForBlind.SettingsUtils.Settings
import com.example.NavigationForBlind.Speech.SpeechSynthesizer
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class DeviceDataManager(private val context: Context) {
    var accelerometer: Accelerometer = Accelerometer(context)
    lateinit var currentLocation: CurrentLocation

    init {
        if(checkLocationPermissions() && checkInternetPermission())
            currentLocation = CurrentLocation(context)
    }

    fun speakCompassDirection()
    {
        SpeechSynthesizer(context, accelerometer.getCompassDirection())
    }

    fun speakCurrentLocation() = GlobalScope.launch()
    {
        SpeechSynthesizer(context, currentLocation.getAddress())
    }

    fun speakTime()
    {
        var pattern = "HH:mm"
        val calendarTime = Calendar.getInstance().time

        if(Settings.language == PreferredLanguage.English)
            pattern = "h:mm a"

        val timeFormat = SimpleDateFormat(pattern, Utils.languageToLocale(Settings.language))

        SpeechSynthesizer(context, timeFormat.format(calendarTime))
    }

    fun speakDate()
    {
        val calendarTime = Calendar.getInstance().time
        val dateFormat = SimpleDateFormat(
            "EEEE, d MMMM, yyyy",
            Utils.languageToLocale(Settings.language)
        )
        SpeechSynthesizer(context, dateFormat.format(calendarTime))
    }

    fun checkLocationPermissions() : Boolean
    {
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )  == PackageManager.PERMISSION_GRANTED
    }

    fun checkInternetPermission() : Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.INTERNET) == PackageManager.PERMISSION_GRANTED
    }
}