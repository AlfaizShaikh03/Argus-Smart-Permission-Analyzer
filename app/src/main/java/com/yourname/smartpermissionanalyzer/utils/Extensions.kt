package com.yourname.smartpermissionanalyzer.utils

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import com.yourname.smartpermissionanalyzer.data.models.RiskLevel
import java.text.SimpleDateFormat
import java.util.*

/**
 * Extension functions for the Smart Permission Analyzer app
 */

// Context extensions
fun Context.getAppIcon(packageName: String): Drawable? {
    return try {
        val appInfo = packageManager.getApplicationInfo(packageName, 0)
        appInfo.loadIcon(packageManager)
    } catch (e: Exception) {
        ContextCompat.getDrawable(this, android.R.drawable.sym_def_app_icon)
    }
}

fun Context.getAppName(packageName: String): String {
    return try {
        val appInfo = packageManager.getApplicationInfo(packageName, 0)
        appInfo.loadLabel(packageManager).toString()
    } catch (e: Exception) {
        packageName
    }
}

fun Context.isAppInstalled(packageName: String): Boolean {
    return try {
        packageManager.getApplicationInfo(packageName, 0)
        true
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }
}

// Risk level extensions
val RiskLevel.displayName: String
    get() = when (this) {
        RiskLevel.CRITICAL -> "Critical Risk"
        RiskLevel.HIGH -> "High Risk"
        RiskLevel.MEDIUM -> "Medium Risk"
        RiskLevel.LOW -> "Low Risk"
    }

val RiskLevel.colorHex: String
    get() = when (this) {
        RiskLevel.CRITICAL -> "#D32F2F"
        RiskLevel.HIGH -> "#FF9800"
        RiskLevel.MEDIUM -> "#FFC107"
        RiskLevel.LOW -> "#4CAF50"
    }

// Date formatting extensions
fun Long.formatDate(): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return sdf.format(Date(this))
}

fun Long.formatDateTime(): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(this))
}

fun Long.formatTime(): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(this))
}

// Permission name formatting
fun String.toFriendlyPermissionName(): String {
    return when {
        contains("CAMERA") -> "Camera Access"
        contains("FINE_LOCATION") -> "Precise Location"
        contains("COARSE_LOCATION") -> "Approximate Location"
        contains("READ_CONTACTS") -> "Read Contacts"
        contains("WRITE_CONTACTS") -> "Write Contacts"
        contains("READ_SMS") -> "Read Messages"
        contains("SEND_SMS") -> "Send Messages"
        contains("RECORD_AUDIO") -> "Microphone"
        contains("READ_EXTERNAL_STORAGE") -> "Read Storage"
        contains("WRITE_EXTERNAL_STORAGE") -> "Write Storage"
        contains("INTERNET") -> "Internet Access"
        else -> substringAfterLast(".").replace("_", " ")
            .lowercase().replaceFirstChar { it.uppercase() }
    }
}

// List extensions
fun <T> List<T>.safeGet(index: Int): T? {
    return if (index >= 0 && index < size) this[index] else null
}

// String extensions
fun String.isValidPackageName(): Boolean {
    return matches(Regex("^[a-zA-Z][a-zA-Z0-9_]*(\\.([a-zA-Z][a-zA-Z0-9_]*))*$"))
}

fun String.truncate(maxLength: Int, suffix: String = "..."): String {
    return if (length <= maxLength) this else take(maxLength - suffix.length) + suffix
}
