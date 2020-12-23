package com.example.NavigationForBlind.Activities

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Bundle
import android.util.Log
import android.view.SurfaceView
import android.view.Window
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.obstacledetection.*
import com.example.NavigationForBlind.DeviceData.CurrentLocation
import com.example.NavigationForBlind.DeviceData.DeviceDataManager
import com.example.NavigationForBlind.ObstacleDetection.ObstacleDetection
import com.example.NavigationForBlind.Speech.*
import com.example.NavigationForBlind.SettingsUtils.*
import kotlinx.android.synthetic.main.activity_main.*
import org.opencv.android.BaseLoaderCallback
import org.opencv.android.OpenCVLoader


// Camera renderer set size

class MainActivity : AppCompatActivity() {
    lateinit var deviceDataManager: DeviceDataManager
    private lateinit var obstacleDetection: ObstacleDetection
    private lateinit var voiceCommandManager: VoiceCommandManager
    private lateinit var audioManager: AudioManager
    private val REQUEST_CODE = 101
    private val TAG = "ONIPOT DEBUG"

    private var baseLoaderCallback: BaseLoaderCallback = object : BaseLoaderCallback(this) {
        override fun onManagerConnected(status: Int) {
            when (status) {
                SUCCESS -> {
                    javaCameraView!!.enableView()
                }
                else -> {
                    super.onManagerConnected(status)
                }
            }
        }
    }

    @SuppressLint("InvalidWakeLockTag")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
//        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
//        val wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"MyWakelockTag")
//
//        wakeLock.acquire(10*60*1000L /*10 minutes*/)

        requestAllPermissions()

        requestWindowFeature(Window.FEATURE_NO_TITLE)
        supportActionBar!!.hide()
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        setContentView(R.layout.activity_main)
        OpenCVLoader.initDebug()

        setupSettings()
        setupUI()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        SpeechSynthesizer(this, "")
        deviceDataManager = DeviceDataManager( this)

        if(checkMicrophonePermission())
            voiceCommandManager = VoiceCommandManager(audioManager, deviceDataManager,this)

        if(checkCameraPermission())
            initObstacleDetection()
    }

    private fun initObstacleDetection()
    {
        if(Settings.obstacleDetectionEnabled){
            obstacleDetection = ObstacleDetection(this, deviceDataManager.accelerometer)
            javaCameraView!!.visibility = SurfaceView.VISIBLE
            javaCameraView!!.alpha = 1f
            javaCameraView!!.setCvCameraViewListener(obstacleDetection)
        }
        else{
            javaCameraView!!.disableView()
            javaCameraView!!.alpha = 0f
        }
    }

    private fun setupSettings()
    {
        val languageStr = SharedPreferencesUtils(this).getStoredString("language")
        val obstacleDetectionWidthStr = SharedPreferencesUtils(this).getStoredString("obstacleDetectionWidth")
        val obstacleDetectionHeightStr = SharedPreferencesUtils(this).getStoredString("obstacleDetectionHeight")

        val language = Utils.enumValueOfOrNull<PreferredLanguage>(languageStr!!)
        if (language != null) {
            Settings.language = language
        }

        Settings.obstacleDetectionEnabled = SharedPreferencesUtils(this).getStoredBool("obstacleDetectionEnabled")!!

        val obstacleDetectionWidth = Utils.enumValueOfOrNull<DetectionWidth>(
            obstacleDetectionWidthStr!!
        )
        if (obstacleDetectionWidth != null) {
            Settings.detectionWidth = obstacleDetectionWidth
        }

        val obstacleDetectionHeight = Utils.enumValueOfOrNull<DetectionHeight>(
            obstacleDetectionHeightStr!!
        )
        if (obstacleDetectionHeight != null) {
            Settings.detectionHeight = obstacleDetectionHeight
        }
    }


    private fun setupUI() {
        voiceCommandsButton.setOnClickListener {
            speechDetectionCount =0
            voiceCommandManager.recognizeSpeech(VoiceCommandsEN.default)
//            voiceCommandManager.notificationsVolume = audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION);
//            voiceCommandManager.systemVolume = audioManager.getStreamVolume(AudioManager.STREAM_SYSTEM);
//            voiceCommandManager.musicVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        }

        locationButton.setOnClickListener {
            deviceDataManager.speakCurrentLocation()
        }

        compassButton.setOnClickListener {
            deviceDataManager.speakCompassDirection()
        }

        timeButton.setOnClickListener {
            deviceDataManager.speakTime()
        }

        dateButton.setOnClickListener {
            deviceDataManager.speakDate()
        }

        settingsButton.setOnClickListener {
//            var intent = Intent(this, SettingsActivity::class.java)
            var intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    fun requestAllPermissions()
    {
        val isFineLocationDenied = ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        )  == PackageManager.PERMISSION_DENIED
        val isCourseLocationDenied = ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )  == PackageManager.PERMISSION_DENIED
        val isRecordAudioDenied = checkCallingOrSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_DENIED
        val isCameraDenied = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED
        val isInternetDenied = ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET) == PackageManager.PERMISSION_DENIED

        val permissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA,
            Manifest.permission.INTERNET
        )

        if (isFineLocationDenied || isCourseLocationDenied || isRecordAudioDenied || isCameraDenied || isInternetDenied) {
            requestPermissions(permissions, REQUEST_CODE)
        }
    }

    fun checkMicrophonePermission() : Boolean
    {
        return checkCallingOrSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }

    fun checkCameraPermission() : Boolean
    {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(requestCode: Int,  permissions: Array<out String>, grantResults: IntArray ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Log.wtf("onRequestPermissionsResult", grantResults.toString())

        voiceCommandManager = VoiceCommandManager(audioManager, deviceDataManager, this)
        deviceDataManager.currentLocation = CurrentLocation(this)
        initObstacleDetection()
    }


    override fun onPointerCaptureChanged(hasCapture: Boolean) {}

    override fun onDestroy() {
        super.onDestroy()
        if (javaCameraView != null) {
            javaCameraView!!.disableView()
        }
//        myLocation.stopLocationUpdates()
    }

    override fun onPause() {
        super.onPause()
        if (javaCameraView != null) {
            javaCameraView!!.disableView()
        }
//        myLocation.stopLocationUpdates()
    }

    override fun onResume() {
        super.onResume()
        if (OpenCVLoader.initDebug()) {
            Log.d(TAG, "OpenCV is working correctly")
            baseLoaderCallback.onManagerConnected(BaseLoaderCallback.SUCCESS)
        } else {
            Log.d(TAG, "OpenCV is not working correctly")
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0, this, baseLoaderCallback)
        }
//        myLocation.startLocationUpdates()
    }


    companion object {
        var speechDetectionCount = 0;
        var speechResult = ""
        var lastChosenStreet = ""
    }
}