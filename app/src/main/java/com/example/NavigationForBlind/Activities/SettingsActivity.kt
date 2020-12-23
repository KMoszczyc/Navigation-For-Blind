package com.example.NavigationForBlind.Activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.preference.*
import com.example.NavigationForBlind.SettingsUtils.*
import com.example.obstacledetection.R

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, SettingsFragment())
                .commit()
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onSupportNavigateUp(): Boolean {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
        return true
    }

    class SettingsFragment() : PreferenceFragmentCompat() {

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.settings_fragment, rootKey)

            handleLanguageSettings()
            handleEnableDetectionSettings()
            handleDetectionHeightSettings()
            handleDetectionWidthSettings()
        }

        private fun handleLanguageSettings()
        {
            val languageArr = resources.getStringArray(R.array.languages)
            val languagePreference = findPreference<ListPreference>("language_settings")!!
            val index = PreferredLanguage.valueOf(Settings.language.toString()).ordinal
            languagePreference.value = languageArr[index]

            languagePreference.onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { preference, newValue ->
                    val index = languageArr.indexOf(newValue)
                    Settings.language = PreferredLanguage.values()[index]
                    SharedPreferencesUtils(requireActivity()).writeStoredString("language", newValue.toString())
                    true
                }
        }

        private fun handleEnableDetectionSettings()
        {
            val detectionEnabled = findPreference<SwitchPreferenceCompat>("detection_enabled")!!
            detectionEnabled.isChecked = Settings.obstacleDetectionEnabled
            Log.wtf("preference", detectionEnabled.isChecked.toString())

            detectionEnabled.onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { preference, newValue ->
                    SharedPreferencesUtils(requireActivity()).writeStoredBool("obstacleDetectionEnabled", newValue as Boolean)
                    Log.wtf("preference", newValue.toString())
                    Settings.obstacleDetectionEnabled = newValue
                    true
                }
        }

        private fun handleDetectionHeightSettings()
        {
            val height_arr = resources.getStringArray(R.array.detection_field_height_values)
            val detectionHeight = findPreference<ListPreference>("detection_field_height")!!
            val index = DetectionHeight.valueOf(Settings.detectionHeight.toString()).ordinal
            detectionHeight.value = height_arr[index]

            detectionHeight.onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { preference, newValue ->
                    val index = height_arr.indexOf(newValue)
                    Settings.detectionHeight = DetectionHeight.values()[index]
                    SharedPreferencesUtils(requireActivity()).writeStoredString("obstacleDetectionHeight", Settings.detectionHeight.toString())
                    true
                }
        }

        private fun handleDetectionWidthSettings()
        {
            val width_arr = resources.getStringArray(R.array.detection_field_width_values)
            val detectionWidth = findPreference<ListPreference>("detection_field_width")!!
            val index = DetectionWidth.valueOf(Settings.detectionWidth.toString()).ordinal
            detectionWidth.value = width_arr[index]

            detectionWidth.onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { preference, newValue ->
                    val index = width_arr.indexOf(newValue)
                    Settings.detectionWidth = DetectionWidth.values()[index]
                    SharedPreferencesUtils(requireActivity()).writeStoredString("obstacleDetectionWidth", Settings.detectionWidth.toString())
                    true
                }
        }
    }

}