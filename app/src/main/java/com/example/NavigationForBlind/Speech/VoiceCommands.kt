package com.example.NavigationForBlind.Speech

import java.util.*

enum class VoiceCommandsEN {
    navigation,
    compass,
    location,
    time,
    date,
    help,
    default
}

enum class VoiceCommandsPL {
    nawigacja,
    kompas,
    lokalizacja,
    czas,
    data,
    pomoc,
    domyslne
}


enum class NavigationVoiceCommands {
    StreetName,
    ConfirmStreet,
    TransitType
}

enum class TransitTypes {
    walking,
    driving,
    bicycling,
    transit
}
