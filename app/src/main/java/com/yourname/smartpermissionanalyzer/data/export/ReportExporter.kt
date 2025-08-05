package com.yourname.smartpermissionanalyzer.data.export

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
import com.yourname.smartpermissionanalyzer.domain.entities.AppEntity
import com.yourname.smartpermissionanalyzer.domain.entities.RiskLevelEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReportExporter @Inject constructor() {

    // ✅ ENHANCED: Export functions with format selection
    suspend fun exportFullReport(apps: List<AppEntity>, context: Context): Boolean {
        return exportFullReportAsText(apps, context)
    }

    // ✅ NEW: Advanced export with multiple formats
    suspend fun exportAdvancedReport(
        apps: List<AppEntity>,
        format: ReportFormat,
        includeSettings: Boolean = false,
        settingsData: Map<String, Any> = emptyMap(),
        context: Context
    ): ExportResult {
        return withContext(Dispatchers.IO) {
            try {
                val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(Date())
                val baseFileName = "SmartPermissionAnalyzer_Report_$timestamp"

                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) {
                    downloadsDir.mkdirs()
                }

                val result = when (format) {
                    ReportFormat.CSV -> exportToCsv(apps, baseFileName, downloadsDir, context)
                    ReportFormat.TEXT -> exportToText(apps, includeSettings, settingsData, baseFileName, downloadsDir, context)
                    ReportFormat.PDF -> exportToPdf(apps, includeSettings, settingsData, baseFileName, downloadsDir, context)
                    ReportFormat.EXECUTIVE_SUMMARY -> exportExecutiveSummary(apps, baseFileName, downloadsDir, context)
                    ReportFormat.COMPREHENSIVE -> exportComprehensiveReport(apps, settingsData, baseFileName, downloadsDir, context)
                }

                result
            } catch (e: Exception) {
                ExportResult.Error("Export failed: ${e.message}")
            }
        }
    }

    // ✅ NEW: Export individual app report with enhanced details
    suspend fun exportEnhancedAppReport(
        app: AppEntity,
        format: ReportFormat = ReportFormat.TEXT,
        includeTrendData: Boolean = false,
        context: Context
    ): ExportResult {
        return withContext(Dispatchers.IO) {
            try {
                val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(Date())
                val fileName = "App_Report_${app.appName.replace(" ", "_")}_$timestamp"

                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) {
                    downloadsDir.mkdirs()
                }

                when (format) {
                    ReportFormat.PDF -> exportAppToPdf(app, fileName, downloadsDir, context)
                    ReportFormat.TEXT -> exportAppToText(app, includeTrendData, fileName, downloadsDir, context)
                    else -> exportAppToText(app, includeTrendData, fileName, downloadsDir, context)
                }
            } catch (e: Exception) {
                ExportResult.Error("App export failed: ${e.message}")
            }
        }
    }

    // ✅ NEW: PDF Export with professional formatting
    private fun exportToPdf(
        apps: List<AppEntity>,
        includeSettings: Boolean,
        settingsData: Map<String, Any>,
        baseFileName: String,
        downloadsDir: File,
        context: Context
    ): ExportResult {
        return try {
            val fileName = "$baseFileName.pdf"
            val file = File(downloadsDir, fileName)

            val document = PdfDocument()
            var pageNumber = 1

            // Page 1: Executive Summary
            val page1 = document.startPage(PdfDocument.PageInfo.Builder(595, 842, pageNumber++).create())
            drawExecutiveSummaryPage(page1.canvas, apps, includeSettings, settingsData)
            document.finishPage(page1)

            // Page 2: Risk Analysis
            val page2 = document.startPage(PdfDocument.PageInfo.Builder(595, 842, pageNumber++).create())
            drawRiskAnalysisPage(page2.canvas, apps)
            document.finishPage(page2)

            // Page 3+: Detailed App Reports
            val criticalAndHighRiskApps = apps.filter {
                it.riskLevel == RiskLevelEntity.CRITICAL || it.riskLevel == RiskLevelEntity.HIGH
            }.sortedByDescending { it.riskScore }

            criticalAndHighRiskApps.chunked(3).forEach { appsChunk ->
                val page = document.startPage(PdfDocument.PageInfo.Builder(595, 842, pageNumber++).create())
                drawAppDetailsPage(page.canvas, appsChunk)
                document.finishPage(page)
            }

            FileOutputStream(file).use { stream ->
                document.writeTo(stream)
            }
            document.close()

            notifyMediaScanner(context, file)

            ExportResult.Success(fileName, file.absolutePath, file.length())
        } catch (e: Exception) {
            ExportResult.Error("PDF export failed: ${e.message}")
        }
    }

    private fun exportAppToPdf(
        app: AppEntity,
        fileName: String,
        downloadsDir: File,
        context: Context
    ): ExportResult {
        return try {
            val pdfFileName = "$fileName.pdf"
            val file = File(downloadsDir, pdfFileName)

            val document = PdfDocument()
            val page = document.startPage(PdfDocument.PageInfo.Builder(595, 842, 1).create())

            drawSingleAppPage(page.canvas, app)
            document.finishPage(page)

            FileOutputStream(file).use { stream ->
                document.writeTo(stream)
            }
            document.close()

            notifyMediaScanner(context, file)

            ExportResult.Success(pdfFileName, file.absolutePath, file.length())
        } catch (e: Exception) {
            ExportResult.Error("App PDF export failed: ${e.message}")
        }
    }

    // ✅ NEW: Executive Summary Export
    private fun exportExecutiveSummary(
        apps: List<AppEntity>,
        baseFileName: String,
        downloadsDir: File,
        context: Context
    ): ExportResult {
        return try {
            val fileName = "${baseFileName}_Executive_Summary.txt"
            val file = File(downloadsDir, fileName)

            val content = generateExecutiveSummary(apps)

            FileWriter(file).use { writer ->
                writer.write(content)
            }

            notifyMediaScanner(context, file)

            ExportResult.Success(fileName, file.absolutePath, file.length())
        } catch (e: Exception) {
            ExportResult.Error("Executive summary export failed: ${e.message}")
        }
    }

    // ✅ NEW: Comprehensive Multi-Format Export
    private fun exportComprehensiveReport(
        apps: List<AppEntity>,
        settingsData: Map<String, Any>,
        baseFileName: String,
        downloadsDir: File,
        context: Context
    ): ExportResult {
        return try {
            // Create a folder for comprehensive report
            val reportFolder = File(downloadsDir, "${baseFileName}_Comprehensive")
            if (!reportFolder.exists()) {
                reportFolder.mkdirs()
            }

            // Export multiple formats
            val csvFile = File(reportFolder, "data_analysis.csv")
            val textFile = File(reportFolder, "detailed_report.txt")
            val summaryFile = File(reportFolder, "executive_summary.txt")

            // Generate all formats
            FileWriter(csvFile).use { it.write(generateEnhancedCSVReport(apps)) }
            FileWriter(textFile).use { it.write(generateCompleteReportWithSettings(apps, settingsData)) }
            FileWriter(summaryFile).use { it.write(generateExecutiveSummary(apps)) }

            // Create index file
            val indexFile = File(reportFolder, "README.txt")
            FileWriter(indexFile).use { writer ->
                writer.write(generateIndexFile(apps.size, settingsData))
            }

            notifyMediaScanner(context, csvFile)
            notifyMediaScanner(context, textFile)
            notifyMediaScanner(context, summaryFile)
            notifyMediaScanner(context, indexFile)

            ExportResult.Success(
                reportFolder.name,
                reportFolder.absolutePath,
                reportFolder.walkTopDown().filter { it.isFile }.map { it.length() }.sum()
            )
        } catch (e: Exception) {
            ExportResult.Error("Comprehensive export failed: ${e.message}")
        }
    }

    // ✅ Enhanced CSV with trend analysis
    private fun exportToCsv(
        apps: List<AppEntity>,
        baseFileName: String,
        downloadsDir: File,
        context: Context
    ): ExportResult {
        return try {
            val fileName = "$baseFileName.csv"
            val file = File(downloadsDir, fileName)

            val content = generateEnhancedCSVReport(apps)

            FileWriter(file).use { writer ->
                writer.write(content)
            }

            notifyMediaScanner(context, file)

            ExportResult.Success(fileName, file.absolutePath, file.length())
        } catch (e: Exception) {
            ExportResult.Error("CSV export failed: ${e.message}")
        }
    }

    private fun exportToText(
        apps: List<AppEntity>,
        includeSettings: Boolean,
        settingsData: Map<String, Any>,
        baseFileName: String,
        downloadsDir: File,
        context: Context
    ): ExportResult {
        return try {
            val fileName = "$baseFileName.txt"
            val file = File(downloadsDir, fileName)

            val content = if (includeSettings) {
                generateCompleteReportWithSettings(apps, settingsData)
            } else {
                generateDetailedTextReport(apps)
            }

            FileWriter(file).use { writer ->
                writer.write(content)
            }

            notifyMediaScanner(context, file)

            ExportResult.Success(fileName, file.absolutePath, file.length())
        } catch (e: Exception) {
            ExportResult.Error("Text export failed: ${e.message}")
        }
    }

    private fun exportAppToText(
        app: AppEntity,
        includeTrendData: Boolean,
        fileName: String,
        downloadsDir: File,
        context: Context
    ): ExportResult {
        return try {
            val textFileName = "$fileName.txt"
            val file = File(downloadsDir, textFileName)

            val content = if (includeTrendData) {
                generateEnhancedAppReport(app)
            } else {
                generateAppDetailReport(app)
            }

            FileWriter(file).use { writer ->
                writer.write(content)
            }

            notifyMediaScanner(context, file)

            ExportResult.Success(textFileName, file.absolutePath, file.length())
        } catch (e: Exception) {
            ExportResult.Error("App text export failed: ${e.message}")
        }
    }

    // ✅ Legacy compatibility methods
    suspend fun exportFullReportWithSettings(
        apps: List<AppEntity>,
        settingsData: Map<String, Any>,
        context: Context
    ): Boolean {
        return when (val result = exportAdvancedReport(apps, ReportFormat.TEXT, true, settingsData, context)) {
            is ExportResult.Success -> true
            is ExportResult.Error -> false
        }
    }

    suspend fun exportFullReportAsCsv(apps: List<AppEntity>, context: Context): Boolean {
        return when (val result = exportAdvancedReport(apps, ReportFormat.CSV, false, emptyMap(), context)) {
            is ExportResult.Success -> true
            is ExportResult.Error -> false
        }
    }

    suspend fun exportFullReportAsText(apps: List<AppEntity>, context: Context): Boolean {
        return when (val result = exportAdvancedReport(apps, ReportFormat.TEXT, false, emptyMap(), context)) {
            is ExportResult.Success -> true
            is ExportResult.Error -> false
        }
    }

    suspend fun exportAppReport(app: AppEntity, context: Context): Boolean {
        return when (val result = exportEnhancedAppReport(app, ReportFormat.TEXT, false, context)) {
            is ExportResult.Success -> true
            is ExportResult.Error -> false
        }
    }

    fun shareAppReport(app: AppEntity, context: Context): Intent {
        val reportContent = generateEnhancedAppReport(app)

        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Security Report: ${app.appName}")
            putExtra(Intent.EXTRA_TEXT, reportContent)
        }

        return Intent.createChooser(shareIntent, "Share App Security Report")
    }

    // ✅ NEW: Share comprehensive report
    fun shareComprehensiveReport(apps: List<AppEntity>, context: Context): Intent {
        val reportContent = generateExecutiveSummary(apps)

        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Device Security Analysis Report")
            putExtra(Intent.EXTRA_TEXT, reportContent)
        }

        return Intent.createChooser(shareIntent, "Share Security Analysis")
    }

    // ✅ NEW: Generate Executive Summary
    private fun generateExecutiveSummary(apps: List<AppEntity>): String {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

        return buildString {
            appendLine("🛡️ DEVICE SECURITY EXECUTIVE SUMMARY")
            appendLine("=" .repeat(50))
            appendLine("Generated: $timestamp")
            appendLine()

            // Key metrics
            val criticalCount = apps.count { it.riskLevel == RiskLevelEntity.CRITICAL }
            val highCount = apps.count { it.riskLevel == RiskLevelEntity.HIGH }
            val mediumCount = apps.count { it.riskLevel == RiskLevelEntity.MEDIUM }
            val avgRiskScore = apps.map { it.riskScore }.average()
            val avgTrustScore = apps.map { it.trustScore }.average() * 100

            appendLine("📊 KEY SECURITY METRICS")
            appendLine("-".repeat(30))
            appendLine("Total Apps Analyzed: ${apps.size}")
            appendLine("Average Risk Score: ${avgRiskScore.toInt()}/100")
            appendLine("Average Trust Score: ${avgTrustScore.toInt()}%")
            appendLine()

            // Risk distribution
            appendLine("🎯 RISK DISTRIBUTION")
            appendLine("-".repeat(30))
            appendLine("🔴 Critical Risk: $criticalCount apps (${((criticalCount.toFloat() / apps.size) * 100).toInt()}%)")
            appendLine("🟠 High Risk: $highCount apps (${((highCount.toFloat() / apps.size) * 100).toInt()}%)")
            appendLine("🟡 Medium Risk: $mediumCount apps (${((mediumCount.toFloat() / apps.size) * 100).toInt()}%)")

            val lowCount = apps.count { it.riskLevel == RiskLevelEntity.LOW }
            val safeCount = apps.count { it.riskLevel == RiskLevelEntity.MINIMAL }
            appendLine("🟢 Low Risk: $lowCount apps (${((lowCount.toFloat() / apps.size) * 100).toInt()}%)")
            appendLine("✅ Safe: $safeCount apps (${((safeCount.toFloat() / apps.size) * 100).toInt()}%)")
            appendLine()

            // Immediate actions required
            if (criticalCount > 0 || highCount > 0) {
                appendLine("⚠️ IMMEDIATE ACTIONS REQUIRED")
                appendLine("-".repeat(30))

                if (criticalCount > 0) {
                    appendLine("🔥 CRITICAL: $criticalCount apps require immediate attention")
                    apps.filter { it.riskLevel == RiskLevelEntity.CRITICAL }
                        .sortedByDescending { it.riskScore }
                        .take(3)
                        .forEach { app ->
                            appendLine("   • ${app.appName} (Risk: ${app.riskScore}/100)")
                        }
                    if (criticalCount > 3) {
                        appendLine("   • ... and ${criticalCount - 3} more critical apps")
                    }
                    appendLine()
                }

                if (highCount > 0) {
                    appendLine("⚠️ HIGH PRIORITY: $highCount apps need review")
                    apps.filter { it.riskLevel == RiskLevelEntity.HIGH }
                        .sortedByDescending { it.riskScore }
                        .take(3)
                        .forEach { app ->
                            appendLine("   • ${app.appName} (Risk: ${app.riskScore}/100)")
                        }
                    if (highCount > 3) {
                        appendLine("   • ... and ${highCount - 3} more high-risk apps")
                    }
                    appendLine()
                }
            }

            // Privacy analysis
            val appsWithCamera = apps.count { it.permissions.any { perm -> perm.contains("CAMERA") } }
            val appsWithMic = apps.count { it.permissions.any { perm -> perm.contains("RECORD_AUDIO") } }
            val appsWithLocation = apps.count { it.permissions.any { perm -> perm.contains("LOCATION") } }
            val appsWithContacts = apps.count { it.permissions.any { perm -> perm.contains("CONTACTS") } }

            appendLine("🔒 PRIVACY EXPOSURE ANALYSIS")
            appendLine("-".repeat(30))
            appendLine("📷 Camera Access: $appsWithCamera apps (${((appsWithCamera.toFloat() / apps.size) * 100).toInt()}%)")
            appendLine("🎤 Microphone Access: $appsWithMic apps (${((appsWithMic.toFloat() / apps.size) * 100).toInt()}%)")
            appendLine("📍 Location Access: $appsWithLocation apps (${((appsWithLocation.toFloat() / apps.size) * 100).toInt()}%)")
            appendLine("👥 Contacts Access: $appsWithContacts apps (${((appsWithContacts.toFloat() / apps.size) * 100).toInt()}%)")
            appendLine("🌐 Internet Access: ${apps.count { it.hasInternetAccess }} apps (${((apps.count { it.hasInternetAccess }.toFloat() / apps.size) * 100).toInt()}%)")
            appendLine()

            // Recommendations
            appendLine("💡 KEY RECOMMENDATIONS")
            appendLine("-".repeat(30))

            when {
                criticalCount > 0 -> {
                    appendLine("1. 🔥 URGENT: Review and consider uninstalling $criticalCount critical risk apps")
                    appendLine("2. ⚠️ Review permissions for $highCount high-risk apps")
                    appendLine("3. 🔒 Enable real-time monitoring for ongoing protection")
                }
                highCount > 0 -> {
                    appendLine("1. ⚠️ Review and restrict permissions for $highCount high-risk apps")
                    appendLine("2. 🛡️ Continue regular security monitoring")
                    appendLine("3. 📱 Consider app alternatives for risky applications")
                }
                else -> {
                    appendLine("1. ✅ Your device security looks good!")
                    appendLine("2. 🔄 Continue regular monitoring to maintain security")
                    appendLine("3. 📲 Review new app installations carefully")
                }
            }

            appendLine()
            appendLine("📈 SECURITY SCORE: ${(100 - avgRiskScore).toInt()}/100")

            val securityGrade = when {
                avgRiskScore < 20 -> "A+ (Excellent)"
                avgRiskScore < 40 -> "A (Very Good)"
                avgRiskScore < 60 -> "B (Good)"
                avgRiskScore < 80 -> "C (Fair - Needs Attention)"
                else -> "D (Poor - Immediate Action Required)"
            }

            appendLine("🏆 SECURITY GRADE: $securityGrade")
            appendLine()
            appendLine("Generated by Smart Permission Analyzer")
            appendLine("For detailed analysis, review the comprehensive report")
        }
    }

    // ✅ NEW: Enhanced CSV with trend analysis
    private fun generateEnhancedCSVReport(apps: List<AppEntity>): String {
        val header = buildString {
            append("App Name,Package Name,Risk Score,Risk Level,Trust Score (%),Category,")
            append("Total Permissions,Suspicious Permissions,Critical Permissions,")
            append("System App,Internet Access,Has Camera,Has Microphone,Has Location,")
            append("Has Contacts,Install Date,Last Update,Version,Target SDK,")
            append("Permission Density,Risk Factors Count,Enabled Status,File Size (MB)\n")
        }

        val rows = apps.joinToString("\n") { app ->
            val installDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(app.installTime))
            val updateDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(app.lastUpdateTime))
            val hasCamera = if (app.permissions.any { it.contains("CAMERA") }) "Yes" else "No"
            val hasMic = if (app.permissions.any { it.contains("RECORD_AUDIO") }) "Yes" else "No"
            val hasLocation = if (app.permissions.any { it.contains("LOCATION") }) "Yes" else "No"
            val hasContacts = if (app.permissions.any { it.contains("CONTACTS") }) "Yes" else "No"

            "\"${app.appName}\"," +
                    "\"${app.packageName}\"," +
                    "${app.riskScore}," +
                    "\"${app.riskLevel.name}\"," +
                    "${(app.trustScore * 100).toInt()}," +
                    "\"${app.appCategory.name.replace("_", " ")}\"," +
                    "${app.permissions.size}," +
                    "${app.suspiciousPermissionCount}," +
                    "${app.criticalPermissionCount}," +
                    "\"${if (app.isSystemApp) "Yes" else "No"}\"," +
                    "\"${if (app.hasInternetAccess) "Yes" else "No"}\"," +
                    "\"$hasCamera\"," +
                    "\"$hasMic\"," +
                    "\"$hasLocation\"," +
                    "\"$hasContacts\"," +
                    "\"$installDate\"," +
                    "\"$updateDate\"," +
                    "\"${app.versionName}\"," +
                    "${app.targetSdkVersion}," +
                    "${String.format("%.2f", app.permissionDensity)}," +
                    "${app.riskFactors.size}," +
                    "\"${if (app.isEnabled) "Enabled" else "Disabled"}\"," +
                    "${String.format("%.2f", app.appSize / (1024.0 * 1024.0))}"
        }

        return header + rows
    }

    // ✅ NEW: Enhanced app report with trend analysis
    private fun generateEnhancedAppReport(app: AppEntity): String {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

        return buildString {
            appendLine("🛡️ COMPREHENSIVE APP SECURITY REPORT")
            appendLine("=" .repeat(50))
            appendLine("Report Generated: $timestamp")
            appendLine()

            appendLine("📱 APP INFORMATION")
            appendLine("-".repeat(30))
            appendLine("Name: ${app.appName}")
            appendLine("Package: ${app.packageName}")
            appendLine("Version: ${app.versionName} (Code: ${app.versionCode})")
            appendLine("Category: ${app.appCategory.name.replace("_", " ")}")
            appendLine("Developer: Unknown")
            appendLine("System App: ${if (app.isSystemApp) "Yes" else "No"}")
            appendLine("Status: ${if (app.isEnabled) "Enabled" else "Disabled"}")
            appendLine("Size: ${String.format("%.2f", app.appSize / (1024.0 * 1024.0))} MB")
            appendLine("Target SDK: ${app.targetSdkVersion}")
            appendLine("Install Date: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(app.installTime))}")
            appendLine("Last Update: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(app.lastUpdateTime))}")
            appendLine()

            appendLine("🎯 SECURITY ANALYSIS")
            appendLine("-".repeat(30))
            appendLine("Risk Score: ${app.riskScore}/100")
            appendLine("Risk Level: ${app.riskLevel.name}")
            appendLine("Trust Score: ${(app.trustScore * 100).toInt()}%")
            appendLine("Security Grade: ${getSecurityGrade(app.riskScore)}")
            appendLine("Internet Access: ${if (app.hasInternetAccess) "Yes" else "No"}")
            appendLine()

            appendLine("🔒 PERMISSION ANALYSIS")
            appendLine("-".repeat(30))
            appendLine("Total Permissions: ${app.permissions.size}")
            appendLine("Suspicious Permissions: ${app.suspiciousPermissionCount}")
            appendLine("Critical Permissions: ${app.criticalPermissionCount}")
            appendLine("Permission Density: ${String.format("%.1f", app.permissionDensity * 100)}%")
            appendLine()

            // Privacy-sensitive permissions
            val sensitivePermissions = mapOf(
                "Camera" to app.permissions.any { it.contains("CAMERA") },
                "Microphone" to app.permissions.any { it.contains("RECORD_AUDIO") },
                "Location" to app.permissions.any { it.contains("LOCATION") },
                "Contacts" to app.permissions.any { it.contains("CONTACTS") },
                "SMS" to app.permissions.any { it.contains("SMS") },
                "Phone" to app.permissions.any { it.contains("CALL") },
                "Calendar" to app.permissions.any { it.contains("CALENDAR") },
                "Storage" to app.permissions.any { it.contains("STORAGE") }
            )

            appendLine("🔐 PRIVACY-SENSITIVE ACCESS")
            appendLine("-".repeat(30))
            sensitivePermissions.forEach { (permission, hasAccess) ->
                val status = if (hasAccess) "✅ Granted" else "❌ Not Granted"
                appendLine("$permission: $status")
            }
            appendLine()

            if (app.suspiciousPermissions.isNotEmpty()) {
                appendLine("⚠️ SUSPICIOUS PERMISSIONS")
                appendLine("-".repeat(30))
                app.suspiciousPermissions.forEach { permission ->
                    appendLine("🔴 ${permission.substringAfterLast(".")}")
                    appendLine("   └─ ${getPermissionDescription(permission)}")
                }
                appendLine()
            }

            if (app.riskFactors.isNotEmpty()) {
                appendLine("⚠️ IDENTIFIED RISK FACTORS")
                appendLine("-".repeat(30))
                app.riskFactors.forEachIndexed { index, factor ->
                    appendLine("${index + 1}. $factor")
                }
                appendLine()
            }

            appendLine("💡 PERSONALIZED RECOMMENDATIONS")
            appendLine("-".repeat(30))
            generateAppRecommendations(app).forEach { recommendation ->
                appendLine("• $recommendation")
            }
            appendLine()

            appendLine("📊 ALL PERMISSIONS (${app.permissions.size} total)")
            appendLine("-".repeat(30))
            val groupedPermissions = app.permissions.groupBy { getPermissionCategory(it) }
            groupedPermissions.forEach { (category, perms) ->
                appendLine("$category:")
                perms.sorted().forEach { permission ->
                    val isRisky = permission in app.suspiciousPermissions
                    val marker = if (isRisky) "  🔴" else "  ✅"
                    appendLine("$marker ${permission.substringAfterLast(".")}")
                }
                appendLine()
            }

            appendLine("=" .repeat(50))
            appendLine("Report generated by Smart Permission Analyzer")
            appendLine("For device-wide analysis, generate a comprehensive report")
        }
    }

    // ✅ PDF Drawing functions
    private fun drawExecutiveSummaryPage(canvas: Canvas, apps: List<AppEntity>, includeSettings: Boolean, settingsData: Map<String, Any>) {
        val paint = Paint().apply {
            textSize = 24f
            color = Color.BLACK
            isAntiAlias = true
        }

        val titlePaint = Paint().apply {
            textSize = 32f
            color = Color.BLACK
            isFakeBoldText = true
            isAntiAlias = true
        }

        var y = 80f

        // Title
        canvas.drawText("Device Security Report", 50f, y, titlePaint)
        y += 60f

        // Timestamp
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        canvas.drawText("Generated: $timestamp", 50f, y, paint)
        y += 40f

        // Key metrics
        paint.textSize = 20f
        canvas.drawText("Total Apps Analyzed: ${apps.size}", 50f, y, paint)
        y += 30f

        val criticalCount = apps.count { it.riskLevel == RiskLevelEntity.CRITICAL }
        val highCount = apps.count { it.riskLevel == RiskLevelEntity.HIGH }

        paint.color = if (criticalCount > 0) Color.RED else Color.BLACK
        canvas.drawText("Critical Risk Apps: $criticalCount", 50f, y, paint)
        y += 30f

        paint.color = if (highCount > 0) Color.rgb(255, 140, 0) else Color.BLACK
        canvas.drawText("High Risk Apps: $highCount", 50f, y, paint)
        y += 30f

        // Additional metrics continue...
        paint.color = Color.BLACK
        val avgRisk = apps.map { it.riskScore }.average()
        canvas.drawText("Average Risk Score: ${avgRisk.toInt()}/100", 50f, y, paint)
    }

    private fun drawRiskAnalysisPage(canvas: Canvas, apps: List<AppEntity>) {
        val paint = Paint().apply {
            textSize = 20f
            color = Color.BLACK
            isAntiAlias = true
        }

        val titlePaint = Paint().apply {
            textSize = 28f
            color = Color.BLACK
            isFakeBoldText = true
            isAntiAlias = true
        }

        var y = 80f

        canvas.drawText("Risk Analysis", 50f, y, titlePaint)
        y += 50f

        // Draw risk distribution
        val riskCounts = mapOf(
            "Critical" to apps.count { it.riskLevel == RiskLevelEntity.CRITICAL },
            "High" to apps.count { it.riskLevel == RiskLevelEntity.HIGH },
            "Medium" to apps.count { it.riskLevel == RiskLevelEntity.MEDIUM },
            "Low" to apps.count { it.riskLevel == RiskLevelEntity.LOW },
            "Minimal" to apps.count { it.riskLevel == RiskLevelEntity.MINIMAL }
        )

        riskCounts.forEach { (level, count) ->
            paint.color = when (level) {
                "Critical" -> Color.RED
                "High" -> Color.rgb(255, 140, 0)
                "Medium" -> Color.rgb(255, 193, 7)
                "Low" -> Color.rgb(76, 175, 80)
                else -> Color.rgb(46, 125, 50)
            }

            canvas.drawText("$level Risk: $count apps", 50f, y, paint)
            y += 30f
        }
    }

    private fun drawAppDetailsPage(canvas: Canvas, apps: List<AppEntity>) {
        val paint = Paint().apply {
            textSize = 16f
            color = Color.BLACK
            isAntiAlias = true
        }

        val titlePaint = Paint().apply {
            textSize = 24f
            color = Color.BLACK
            isFakeBoldText = true
            isAntiAlias = true
        }

        var y = 80f

        canvas.drawText("App Details", 50f, y, titlePaint)
        y += 40f

        apps.forEach { app ->
            paint.textSize = 18f
            paint.isFakeBoldText = true
            canvas.drawText(app.appName, 50f, y, paint)
            y += 25f

            paint.textSize = 14f
            paint.isFakeBoldText = false
            canvas.drawText("Risk: ${app.riskScore}/100 (${app.riskLevel.name})", 70f, y, paint)
            y += 20f
            canvas.drawText("Permissions: ${app.permissions.size}", 70f, y, paint)
            y += 20f
            canvas.drawText("Package: ${app.packageName}", 70f, y, paint)
            y += 30f
        }
    }

    private fun drawSingleAppPage(canvas: Canvas, app: AppEntity) {
        val paint = Paint().apply {
            textSize = 16f
            color = Color.BLACK
            isAntiAlias = true
        }

        val titlePaint = Paint().apply {
            textSize = 28f
            color = Color.BLACK
            isFakeBoldText = true
            isAntiAlias = true
        }

        var y = 80f

        canvas.drawText("App Security Report", 50f, y, titlePaint)
        y += 60f

        paint.textSize = 20f
        paint.isFakeBoldText = true
        canvas.drawText(app.appName, 50f, y, paint)
        y += 40f

        paint.textSize = 16f
        paint.isFakeBoldText = false
        canvas.drawText("Package: ${app.packageName}", 50f, y, paint)
        y += 25f
        canvas.drawText("Risk Score: ${app.riskScore}/100", 50f, y, paint)
        y += 25f
        canvas.drawText("Risk Level: ${app.riskLevel.name}", 50f, y, paint)
        y += 25f
        canvas.drawText("Trust Score: ${(app.trustScore * 100).toInt()}%", 50f, y, paint)
        y += 25f
        canvas.drawText("Total Permissions: ${app.permissions.size}", 50f, y, paint)
        y += 25f
        canvas.drawText("Suspicious: ${app.suspiciousPermissionCount}", 50f, y, paint)
    }

    // ✅ Helper functions (keeping your existing ones and adding new ones)
    private fun generateCompleteReportWithSettings(apps: List<AppEntity>, settingsData: Map<String, Any>): String {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

        return buildString {
            appendLine("===============================================")
            appendLine("SMART PERMISSION ANALYZER - COMPLETE REPORT")
            appendLine("===============================================")
            appendLine()
            appendLine("Report Generated: $timestamp")
            appendLine("Total Apps Analyzed: ${apps.size}")
            appendLine()

            // Settings section
            if (settingsData.isNotEmpty()) {
                appendLine("CURRENT SETTINGS CONFIGURATION:")
                appendLine("=" .repeat(50))
                settingsData.forEach { (key, value) ->
                    val displayKey = key.replace("_", " ").split(" ").joinToString(" ") {
                        it.replaceFirstChar { char -> char.uppercase() }
                    }
                    appendLine("$displayKey: $value")
                }
                appendLine()
            }

            // Security Summary
            val criticalApps = apps.count { it.riskLevel == RiskLevelEntity.CRITICAL }
            val highRiskApps = apps.count { it.riskLevel == RiskLevelEntity.HIGH }
            val mediumRiskApps = apps.count { it.riskLevel == RiskLevelEntity.MEDIUM }
            val lowRiskApps = apps.count { it.riskLevel == RiskLevelEntity.LOW }
            val safeApps = apps.count { it.riskLevel == RiskLevelEntity.MINIMAL }

            appendLine("SECURITY SUMMARY:")
            appendLine("=" .repeat(50))
            appendLine("Critical Risk Apps: $criticalApps")
            appendLine("High Risk Apps: $highRiskApps")
            appendLine("Medium Risk Apps: $mediumRiskApps")
            appendLine("Low Risk Apps: $lowRiskApps")
            appendLine("Safe Apps: $safeApps")
            appendLine()

            // Real-time scanning status
            val realtimeScanning = settingsData["real_time_scanning"] as? Boolean ?: false
            if (realtimeScanning) {
                val scanFreq = settingsData["scan_frequency_minutes"] as? Int ?: 30
                appendLine("🛡️ REAL-TIME PROTECTION STATUS:")
                appendLine("Real-time scanning: ACTIVE")
                appendLine("Scan frequency: Every $scanFreq minutes")
                appendLine("Background monitoring: Enabled")
                appendLine()
            }

            // Add detailed analysis
            append(generateDetailedTextReport(apps).substringAfter("DETAILED APP ANALYSIS:"))
        }
    }

    private fun generateDetailedTextReport(apps: List<AppEntity>): String {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

        return buildString {
            appendLine("===============================================")
            appendLine("SMART PERMISSION ANALYZER - SECURITY REPORT")
            appendLine("===============================================")
            appendLine()
            appendLine("Report Generated: $timestamp")
            appendLine("Total Apps Analyzed: ${apps.size}")
            appendLine()

            // Security Summary
            val criticalApps = apps.count { it.riskLevel == RiskLevelEntity.CRITICAL }
            val highRiskApps = apps.count { it.riskLevel == RiskLevelEntity.HIGH }
            val mediumRiskApps = apps.count { it.riskLevel == RiskLevelEntity.MEDIUM }
            val lowRiskApps = apps.count { it.riskLevel == RiskLevelEntity.LOW }
            val safeApps = apps.count { it.riskLevel == RiskLevelEntity.MINIMAL }

            appendLine("SECURITY SUMMARY:")
            appendLine("Critical Risk Apps: $criticalApps")
            appendLine("High Risk Apps: $highRiskApps")
            appendLine("Medium Risk Apps: $mediumRiskApps")
            appendLine("Low Risk Apps: $lowRiskApps")
            appendLine("Safe Apps: $safeApps")
            appendLine()

            // Top Risk Apps
            if (criticalApps > 0 || highRiskApps > 0) {
                appendLine("⚠️ HIGH PRIORITY APPS REQUIRING ATTENTION:")
                appendLine("=" .repeat(50))

                apps.filter {
                    it.riskLevel == RiskLevelEntity.CRITICAL ||
                            it.riskLevel == RiskLevelEntity.HIGH
                }.sortedByDescending { it.riskScore }.forEach { app ->
                    appendLine()
                    appendLine("🔴 ${app.appName}")
                    appendLine("   Package: ${app.packageName}")
                    appendLine("   Risk Score: ${app.riskScore}/100 (${app.riskLevel.name})")
                    appendLine("   Permissions: ${app.permissions.size} total, ${app.suspiciousPermissionCount} suspicious")
                    appendLine("   Category: ${app.appCategory.name.replace("_", " ")}")
                    appendLine("   Internet Access: ${if (app.hasInternetAccess) "YES" else "NO"}")

                    if (app.riskFactors.isNotEmpty()) {
                        appendLine("   Risk Factors:")
                        app.riskFactors.forEach { factor ->
                            appendLine("   - $factor")
                        }
                    }
                }
                appendLine()
            }

            // Detailed App List
            appendLine("DETAILED APP ANALYSIS:")
            appendLine("=" .repeat(50))

            apps.sortedByDescending { it.riskScore }.forEach { app ->
                appendLine()
                appendLine("📱 ${app.appName}")
                appendLine("   Package: ${app.packageName}")
                appendLine("   Risk Level: ${app.riskLevel.name} (${app.riskScore}/100)")
                appendLine("   Category: ${app.appCategory.name.replace("_", " ")}")
                appendLine("   Version: ${app.versionName}")
                appendLine("   System App: ${if (app.isSystemApp) "Yes" else "No"}")
                appendLine("   Internet Access: ${if (app.hasInternetAccess) "Yes" else "No"}")
                appendLine("   Trust Score: ${(app.trustScore * 100).toInt()}%")
                appendLine("   Install Date: ${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(app.installTime))}")
                appendLine("   Last Update: ${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(app.lastUpdateTime))}")

                appendLine("   Permissions (${app.permissions.size} total):")
                if (app.suspiciousPermissions.isNotEmpty()) {
                    appendLine("   ⚠️ Suspicious Permissions:")
                    app.suspiciousPermissions.forEach { permission ->
                        appendLine("      - ${permission.substringAfterLast(".")}")
                    }
                }

                val normalPermissions = app.permissions.filter { it !in app.suspiciousPermissions }
                if (normalPermissions.isNotEmpty()) {
                    appendLine("   ✓ Normal Permissions:")
                    normalPermissions.take(5).forEach { permission ->
                        appendLine("      - ${permission.substringAfterLast(".")}")
                    }
                    if (normalPermissions.size > 5) {
                        appendLine("      ... and ${normalPermissions.size - 5} more")
                    }
                }

                appendLine("   " + "-".repeat(40))
            }

            // Recommendations
            appendLine()
            appendLine("SECURITY RECOMMENDATIONS:")
            appendLine("=" .repeat(50))

            if (criticalApps > 0) {
                appendLine("🔥 IMMEDIATE ACTION REQUIRED:")
                appendLine("- Review and consider uninstalling $criticalApps critical risk apps")
                appendLine("- Disable unnecessary permissions for high-risk apps")
            }

            if (highRiskApps > 0) {
                appendLine("⚠️ HIGH PRIORITY:")
                appendLine("- Review $highRiskApps high-risk apps carefully")
                appendLine("- Monitor these apps for unusual behavior")
            }

            val appsWithCamera = apps.count { it.permissions.any { perm -> perm.contains("CAMERA") } }
            val appsWithMicrophone = apps.count { it.permissions.any { perm -> perm.contains("RECORD_AUDIO") } }
            val appsWithLocation = apps.count { it.permissions.any { perm -> perm.contains("LOCATION") } }

            appendLine("📊 PRIVACY ANALYSIS:")
            appendLine("- $appsWithCamera apps can access your camera")
            appendLine("- $appsWithMicrophone apps can access your microphone")
            appendLine("- $appsWithLocation apps can access your location")
            appendLine("- ${apps.count { it.hasInternetAccess }} apps have internet access")

            appendLine()
            appendLine("===============================================")
            appendLine("Report generated by Smart Permission Analyzer")
            appendLine("===============================================")
        }
    }

    private fun generateAppDetailReport(app: AppEntity): String {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

        return buildString {
            appendLine("===============================================")
            appendLine("APP SECURITY REPORT")
            appendLine("===============================================")
            appendLine()
            appendLine("Report Generated: $timestamp")
            appendLine()
            appendLine("APP INFORMATION:")
            appendLine("Name: ${app.appName}")
            appendLine("Package: ${app.packageName}")
            appendLine("Version: ${app.versionName} (${app.versionCode})")
            appendLine("Category: ${app.appCategory.name.replace("_", " ")}")
            appendLine("System App: ${if (app.isSystemApp) "Yes" else "No"}")
            appendLine("Status: ${if (app.isEnabled) "Enabled" else "Disabled"}")
            appendLine()
            appendLine("SECURITY ANALYSIS:")
            appendLine("Risk Score: ${app.riskScore}/100")
            appendLine("Risk Level: ${app.riskLevel.name}")
            appendLine("Trust Score: ${(app.trustScore * 100).toInt()}%")
            appendLine("Internet Access: ${if (app.hasInternetAccess) "Yes" else "No"}")
            appendLine()
            appendLine("PERMISSIONS SUMMARY:")
            appendLine("Total Permissions: ${app.permissions.size}")
            appendLine("Suspicious Permissions: ${app.suspiciousPermissionCount}")
            appendLine("Critical Permissions: ${app.criticalPermissionCount}")
            appendLine("Permission Density: ${(app.permissionDensity * 100).toInt()}%")
            appendLine()

            if (app.suspiciousPermissions.isNotEmpty()) {
                appendLine("SUSPICIOUS PERMISSIONS:")
                app.suspiciousPermissions.forEach { permission ->
                    appendLine("⚠️ ${permission.substringAfterLast(".")}")
                }
                appendLine()
            }

            if (app.riskFactors.isNotEmpty()) {
                appendLine("RISK FACTORS:")
                app.riskFactors.forEach { factor ->
                    appendLine("- $factor")
                }
                appendLine()
            }

            appendLine("ALL PERMISSIONS:")
            app.permissions.sorted().forEach { permission ->
                val isRisky = permission in app.suspiciousPermissions
                val marker = if (isRisky) "⚠️ " else "✓ "
                appendLine("$marker ${permission.substringAfterLast(".")}")
            }
            appendLine()
            appendLine("===============================================")
        }
    }

    private fun generateIndexFile(appCount: Int, settingsData: Map<String, Any>): String {
        return buildString {
            appendLine("SMART PERMISSION ANALYZER - COMPREHENSIVE REPORT")
            appendLine("=" .repeat(50))
            appendLine("Generated: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}")
            appendLine()
            appendLine("This folder contains your complete device security analysis:")
            appendLine()
            appendLine("📊 data_analysis.csv")
            appendLine("   - Machine-readable data for analysis")
            appendLine("   - Import into Excel, Google Sheets, or analytics tools")
            appendLine("   - Contains detailed metrics for all $appCount apps")
            appendLine()
            appendLine("📄 detailed_report.txt")
            appendLine("   - Complete human-readable security report")
            appendLine("   - Includes app-by-app analysis")
            appendLine("   - Contains recommendations and risk factors")
            appendLine()
            appendLine("📋 executive_summary.txt")
            appendLine("   - High-level overview for decision makers")
            appendLine("   - Key metrics and immediate actions")
            appendLine("   - Security grade and recommendations")
            appendLine()
            appendLine("Report covers $appCount applications analyzed with Smart Permission Analyzer")
            appendLine("For questions or support, visit our documentation.")
        }
    }

    private fun generateAppRecommendations(app: AppEntity): List<String> {
        val recommendations = mutableListOf<String>()

        when (app.riskLevel) {
            RiskLevelEntity.CRITICAL -> {
                recommendations.add("🔥 URGENT: Consider uninstalling this app immediately")
                recommendations.add("If needed, find a safer alternative with similar functionality")
                recommendations.add("Review what data this app may have already collected")
            }
            RiskLevelEntity.HIGH -> {
                recommendations.add("⚠️ Restrict unnecessary permissions in Android settings")
                recommendations.add("Monitor this app's behavior closely")
                recommendations.add("Consider disabling background activity")
                if (app.hasInternetAccess) {
                    recommendations.add("Block network access if app doesn't need internet")
                }
            }
            RiskLevelEntity.MEDIUM -> {
                recommendations.add("📋 Review granted permissions and revoke unnecessary ones")
                recommendations.add("Check privacy settings within the app")
                recommendations.add("Keep the app updated to latest version")
            }
            else -> {
                recommendations.add("✅ This app appears safe to use")
                recommendations.add("Continue monitoring through regular security scans")
                recommendations.add("Keep app permissions minimal as best practice")
            }
        }

        // Add specific recommendations based on permissions
        if (app.permissions.any { it.contains("CAMERA") }) {
            recommendations.add("📸 Only grant camera permission when actively using camera features")
        }

        if (app.permissions.any { it.contains("LOCATION") }) {
            recommendations.add("📍 Consider using 'While using app' location setting instead of 'Always'")
        }

        if (app.permissions.any { it.contains("CONTACTS") }) {
            recommendations.add("👥 Review why this app needs access to your contacts")
        }

        return recommendations
    }

    private fun getSecurityGrade(riskScore: Int): String {
        return when {
            riskScore < 20 -> "A+ (Excellent)"
            riskScore < 40 -> "A (Very Good)"
            riskScore < 60 -> "B (Good)"
            riskScore < 80 -> "C (Fair)"
            else -> "D (Poor)"
        }
    }

    private fun getPermissionCategory(permission: String): String {
        return when {
            permission.contains("CAMERA") || permission.contains("RECORD_AUDIO") -> "Media & Camera"
            permission.contains("LOCATION") -> "Location Services"
            permission.contains("CONTACTS") || permission.contains("CALENDAR") -> "Personal Information"
            permission.contains("SMS") || permission.contains("CALL") -> "Communication"
            permission.contains("STORAGE") || permission.contains("WRITE") || permission.contains("READ") -> "Storage & Files"
            permission.contains("INTERNET") || permission.contains("NETWORK") -> "Network & Internet"
            permission.contains("BLUETOOTH") || permission.contains("NFC") -> "Device Connectivity"
            else -> "System & Other"
        }
    }

    private fun getPermissionDescription(permission: String): String {
        return when {
            permission.contains("CAMERA") -> "Can take photos and record videos"
            permission.contains("RECORD_AUDIO") -> "Can record audio and access microphone"
            permission.contains("ACCESS_FINE_LOCATION") -> "Can access precise GPS location"
            permission.contains("ACCESS_COARSE_LOCATION") -> "Can access approximate location"
            permission.contains("READ_CONTACTS") -> "Can read your contact list"
            permission.contains("WRITE_CONTACTS") -> "Can modify your contacts"
            permission.contains("READ_SMS") -> "Can read your text messages"
            permission.contains("SEND_SMS") -> "Can send text messages"
            permission.contains("READ_CALL_LOG") -> "Can read your call history"
            permission.contains("CALL_PHONE") -> "Can make phone calls"
            permission.contains("INTERNET") -> "Can access the internet"
            permission.contains("WAKE_LOCK") -> "Can prevent device from sleeping"
            else -> "Android system permission"
        }
    }

    private fun notifyMediaScanner(context: Context, file: File) {
        val intent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
        intent.data = Uri.fromFile(file)
        context.sendBroadcast(intent)
    }
}

// ✅ NEW: Data classes for enhanced export functionality
enum class ReportFormat {
    CSV, TEXT, PDF, EXECUTIVE_SUMMARY, COMPREHENSIVE
}

sealed class ExportResult {
    data class Success(val fileName: String, val filePath: String, val fileSizeBytes: Long) : ExportResult()
    data class Error(val message: String) : ExportResult()
}
