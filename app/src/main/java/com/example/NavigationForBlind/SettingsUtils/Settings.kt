package com.example.NavigationForBlind.SettingsUtils

class Settings {
    companion object{
        var language: PreferredLanguage = PreferredLanguage.English
        var obstacleDetectionEnabled = true
        var detectionHeight: DetectionHeight = DetectionHeight.Normal
        var detectionWidth: DetectionWidth = DetectionWidth.Normal
    }
}

enum class DetectionHeight {
    High,
    Normal,
    Small
}

enum class DetectionWidth {
    Wide,
    Normal,
    Narrow
}

enum class PreferredLanguage
{
    English,
    Polish
}
