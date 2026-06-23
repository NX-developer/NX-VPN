package com.nxvpn.app.util

import java.util.Locale

/** Human-readable byte counts, e.g. 1.4 MB. */
fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val units = arrayOf("KB", "MB", "GB", "TB")
    var value = bytes / 1024.0
    var unit = 0
    while (value >= 1024 && unit < units.lastIndex) {
        value /= 1024.0
        unit++
    }
    return String.format(Locale.US, "%.1f %s", value, units[unit])
}

/** Elapsed seconds rendered as HH:MM:SS. */
fun formatDuration(totalSeconds: Long): String {
    val s = totalSeconds.coerceAtLeast(0)
    val h = s / 3600
    val m = (s % 3600) / 60
    val sec = s % 60
    return String.format(Locale.US, "%02d:%02d:%02d", h, m, sec)
}
