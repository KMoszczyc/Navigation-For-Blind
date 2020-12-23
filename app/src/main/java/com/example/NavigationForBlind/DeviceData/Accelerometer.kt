package com.example.NavigationForBlind.DeviceData

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.appcompat.app.AppCompatActivity
import com.example.NavigationForBlind.SettingsUtils.PreferredLanguage
import com.example.NavigationForBlind.SettingsUtils.Settings

class Accelerometer(context: Context) : SensorEventListener {
    private val SMOOTH_SENSORS_ALPHA = 0.2f
    // Sensor data
    private var gravity: FloatArray? = FloatArray(3)
    private var geomagnetic: FloatArray? = FloatArray(3)
    private var rArr = FloatArray(9)
    private var iArr = FloatArray(9)
    var orientation = FloatArray(3) // orientation contains: azimuth, pitch and roll
    private var unsmoothedOrientation = FloatArray(3)
    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private var magnetometer: Sensor? = null


    init {
        sensorManager = context.getSystemService(AppCompatActivity.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager!!.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        magnetometer = sensorManager!!.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        sensorManager!!.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
        sensorManager!!.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_NORMAL)
    }

    @SuppressLint("SetTextI18n")
    override fun onSensorChanged(event: SensorEvent) {

        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            gravity = event.values
        }
        if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) geomagnetic = event.values
        if (gravity != null && geomagnetic != null) {
            val success = SensorManager.getRotationMatrix(rArr, iArr, gravity, geomagnetic)
            if (success) {
                SensorManager.getOrientation(rArr, unsmoothedOrientation)
                orientation[0] = addRadians(Math.PI/2, orientation[0].toDouble()).toFloat()
                orientation = smoothOrientation(orientation, unsmoothedOrientation)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }

    // smoothing filter for orientation sensors
    protected fun smoothOrientation(input: FloatArray, output: FloatArray?): FloatArray {
        if (output == null) return input
        for (i in input.indices) {
            output[i] = output[i] + SMOOTH_SENSORS_ALPHA * (input[i] - output[i])
        }
        return output
    }

    fun getCompassDirection() : String
    {
        var direction = ""
        val directionsEN = listOf("North", "North east", "East", "South east", "South", "South west", "West", "North west")
        val directionsPL = listOf("Północ", "Północny wschód", "Wschód", "Południowy wschód", "Południe", "Południowy zachód", "Zachód", "Północny zachód")

        val directions: List<String>

        if (Settings.language == PreferredLanguage.Polish)
            directions = directionsPL
        else
            directions = directionsEN

        if(orientation[0]<Math.PI/8 && orientation[0]>= -Math.PI/8)
            direction = directions[0]
        else if(orientation[0]>=Math.PI/8 && orientation[0] < Math.PI*3/8)
            direction = directions[1]
        else if(orientation[0]>=Math.PI*3/8 && orientation[0] < Math.PI*5/8)
            direction = directions[2]
        else if(orientation[0]>=Math.PI*5/8 && orientation[0] < Math.PI*7/8)
            direction = directions[3]
        else if(orientation[0]<= -Math.PI*7/8 || orientation[0] >= Math.PI*7/8)
            direction = directions[4]
        else if(orientation[0]<= -Math.PI*5/8 && orientation[0] > -Math.PI*7/8)
            direction = directions[5]
        else if(orientation[0]<= -Math.PI*3/8 && orientation[0] > -Math.PI*5/8)
            direction = directions[6]
        else if(orientation[0]<= -Math.PI/8 && orientation[0] > -Math.PI*3/8)
            direction = directions[7]

        return direction
    }

    fun addRadians(angle1: Double, angle2: Double) : Double
    {
        return (angle1 + angle2 + Math.PI.toFloat()) % (2*Math.PI.toFloat()) - Math.PI.toFloat()
    }

    fun getAzimuth() : Float {
        return orientation[0]
    }
}
