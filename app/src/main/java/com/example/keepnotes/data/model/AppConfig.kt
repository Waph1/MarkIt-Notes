package com.example.keepnotes.data.model

data class AppConfig(
    var fileColors: java.util.HashMap<String, Long> = java.util.HashMap(), // Filename -> Color
    var pinnedFiles: java.util.HashSet<String> = java.util.HashSet(),       // Filenames
    var customTimestamps: java.util.HashMap<String, Long> = java.util.HashMap() // Filename -> Timestamp
)
