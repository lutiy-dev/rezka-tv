package dev.alaev.rezkatv.rezka

import android.app.Activity
import android.app.AlertDialog
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings as AndroidSettings
import android.widget.Toast
import dev.alaev.rezkatv.BuildConfig
import dev.alaev.rezkatv.R
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

object UpdateManager {

    private const val RELEASE_API = "https://api.github.com/repos/lutiy-dev/rezka-tv/releases/latest"
    private const val PREFS = "rezka_update"
    private const val KEY_LAST_CHECK = "last_check"
    private const val CHECK_INTERVAL_MS = 24L * 60L * 60L * 1000L
    private const val APK_MIME = "application/vnd.android.package-archive"

    data class Release(
        val version: String,
        val apkUrl: String,
        val pageUrl: String,
    )

    fun check(activity: Activity, force: Boolean = false) {
        val prefs = activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val lastCheck = prefs.getLong(KEY_LAST_CHECK, 0L)
        if (!force && now - lastCheck < CHECK_INTERVAL_MS) return
        prefs.edit().putLong(KEY_LAST_CHECK, now).apply()

        thread(name = "rezka-update-check") {
            runCatching { fetchLatestRelease() }
                .onSuccess { release ->
                    if (!activity.isFinishing && !activity.isDestroyed) {
                        activity.runOnUiThread {
                            when {
                                release != null && isNewer(release.version, BuildConfig.VERSION_NAME) ->
                                    showUpdateDialog(activity, release)
                                force -> Toast.makeText(
                                    activity,
                                    R.string.update_up_to_date,
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }
                        }
                    }
                }
                .onFailure {
                    if (force && !activity.isFinishing && !activity.isDestroyed) {
                        activity.runOnUiThread {
                            Toast.makeText(activity, R.string.update_check_failed, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
        }
    }

    private fun fetchLatestRelease(): Release? {
        val connection = URL(RELEASE_API).openConnection() as HttpURLConnection
        connection.connectTimeout = 8_000
        connection.readTimeout = 8_000
        connection.setRequestProperty("Accept", "application/vnd.github+json")
        connection.setRequestProperty("User-Agent", "Rezka-TV/${BuildConfig.VERSION_NAME}")
        return try {
            if (connection.responseCode !in 200..299) return null
            val json = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
            if (json.optBoolean("draft") || json.optBoolean("prerelease")) return null

            val tag = json.optString("tag_name").removePrefix("v")
            val pageUrl = json.optString("html_url")
            val assets = json.optJSONArray("assets") ?: return null
            var apkUrl = ""
            for (index in 0 until assets.length()) {
                val asset = assets.optJSONObject(index) ?: continue
                val name = asset.optString("name")
                if (name.endsWith(".apk", ignoreCase = true)) {
                    apkUrl = asset.optString("browser_download_url")
                    if (apkUrl.isNotBlank()) break
                }
            }
            if (tag.isBlank() || apkUrl.isBlank() || pageUrl.isBlank()) null else Release(tag, apkUrl, pageUrl)
        } finally {
            connection.disconnect()
        }
    }

    private fun showUpdateDialog(activity: Activity, release: Release) {
        AlertDialog.Builder(activity)
            .setTitle(R.string.update_available_title)
            .setMessage(activity.getString(R.string.update_available_message, release.version))
            .setPositiveButton(R.string.update_action) { _, _ -> downloadAndInstall(activity, release) }
            .setNegativeButton(R.string.update_later, null)
            .setNeutralButton(R.string.update_release_page) { _, _ ->
                activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(release.pageUrl)))
            }
            .show()
    }

    private fun downloadAndInstall(activity: Activity, release: Release) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !activity.packageManager.canRequestPackageInstalls()) {
            Toast.makeText(activity, R.string.update_allow_install, Toast.LENGTH_LONG).show()
            val intent = Intent(
                AndroidSettings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${activity.packageName}"),
            )
            activity.startActivity(intent)
            return
        }

        val manager = activity.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val fileName = "Rezka-TV-${release.version}.apk"
        val request = DownloadManager.Request(Uri.parse(release.apkUrl))
            .setTitle(fileName)
            .setDescription(activity.getString(R.string.update_downloading))
            .setMimeType(APK_MIME)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(activity, Environment.DIRECTORY_DOWNLOADS, fileName)

        val downloadId = manager.enqueue(request)
        Toast.makeText(activity, R.string.update_downloading, Toast.LENGTH_SHORT).show()

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action != DownloadManager.ACTION_DOWNLOAD_COMPLETE) return
                if (intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L) != downloadId) return
                runCatching { activity.unregisterReceiver(this) }

                val uri = manager.getUriForDownloadedFile(downloadId)
                if (uri == null) {
                    Toast.makeText(activity, R.string.update_download_failed, Toast.LENGTH_LONG).show()
                    return
                }

                val install = Intent(Intent.ACTION_VIEW)
                    .setDataAndType(uri, APK_MIME)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
                activity.startActivity(install)
            }
        }

        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            activity.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            activity.registerReceiver(receiver, filter)
        }
    }

    internal fun isNewer(candidate: String, current: String): Boolean {
        val left = parseVersion(candidate) ?: return false
        val right = parseVersion(current) ?: return false
        val count = maxOf(left.size, right.size)
        for (index in 0 until count) {
            val a = left.getOrElse(index) { 0 }
            val b = right.getOrElse(index) { 0 }
            if (a != b) return a > b
        }
        return false
    }

    private fun parseVersion(value: String): List<Int>? {
        val clean = value.trim().removePrefix("v").substringBefore('-')
        if (clean.isBlank()) return null
        val parts = clean.split('.')
        if (parts.any { it.toIntOrNull() == null }) return null
        return parts.map { requireNotNull(it.toIntOrNull()) }
    }
}
