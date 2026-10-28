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
import android.widget.Toast
import dev.alaev.rezkatv.BuildConfig
import dev.alaev.rezkatv.R
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread
import android.provider.Settings as AndroidSettings

object UpdateManager {
    private const val RELEASE_API = "https://api.github.com/repos/lutiy-dev/rezka-tv/releases/latest"
    private const val PREFS = "rezka_update"
    private const val KEY_LAST_CHECK = "last_check"
    private const val CHECK_INTERVAL_MS = 24L * 60L * 60L * 1000L
    private const val NETWORK_TIMEOUT_MS = 8_000
    private const val MAX_HTTP_SUCCESS = 299
    private const val APK_MIME = "application/vnd.android.package-archive"

    data class Release(val version: String, val apkUrl: String, val pageUrl: String)

    fun check(activity: Activity, force: Boolean = false) {
        val prefs = activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        if (!force && now - prefs.getLong(KEY_LAST_CHECK, 0L) < CHECK_INTERVAL_MS) {
            return
        }
        prefs.edit().putLong(KEY_LAST_CHECK, now).apply()
        thread(name = "rezka-update-check") {
            runCatching { fetchLatestRelease() }
                .onSuccess { release -> handleCheckSuccess(activity, force, release) }
                .onFailure { handleCheckFailure(activity, force) }
        }
    }

    private fun handleCheckSuccess(activity: Activity, force: Boolean, release: Release?) {
        if (activity.isFinishing || activity.isDestroyed) {
            return
        }
        activity.runOnUiThread {
            when {
                release != null && isNewer(release.version, BuildConfig.VERSION_NAME) ->
                    showUpdateDialog(activity, release)
                force -> Toast.makeText(activity, R.string.update_up_to_date, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleCheckFailure(activity: Activity, force: Boolean) {
        if (force && !activity.isFinishing && !activity.isDestroyed) {
            activity.runOnUiThread {
                Toast.makeText(activity, R.string.update_check_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun fetchLatestRelease(): Release? {
        val connection = URL(RELEASE_API).openConnection() as HttpURLConnection
        connection.connectTimeout = NETWORK_TIMEOUT_MS
        connection.readTimeout = NETWORK_TIMEOUT_MS
        connection.setRequestProperty("Accept", "application/vnd.github+json")
        connection.setRequestProperty("User-Agent", "Rezka-TV/${BuildConfig.VERSION_NAME}")
        return try {
            if (connection.responseCode in HttpURLConnection.HTTP_OK..MAX_HTTP_SUCCESS) {
                parseRelease(JSONObject(connection.inputStream.bufferedReader().use { it.readText() }))
            } else {
                null
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun parseRelease(json: JSONObject): Release? {
        val assets = json.optJSONArray("assets")
        val apkUrl = assets?.let { array ->
            (0 until array.length())
                .asSequence()
                .mapNotNull { array.optJSONObject(it) }
                .firstOrNull { it.optString("name").endsWith(".apk", ignoreCase = true) }
                ?.optString("browser_download_url")
        }
        val version = json.optString("tag_name").removePrefix("v")
        val pageUrl = json.optString("html_url")
        val usable = !json.optBoolean("draft") &&
            !json.optBoolean("prerelease") &&
            version.isNotBlank() &&
            !apkUrl.isNullOrBlank() &&
            pageUrl.isNotBlank()
        return if (usable) Release(version, requireNotNull(apkUrl), pageUrl) else null
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !activity.packageManager.canRequestPackageInstalls()
        ) {
            Toast.makeText(activity, R.string.update_allow_install, Toast.LENGTH_LONG).show()
            activity.startActivity(
                Intent(
                    AndroidSettings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${activity.packageName}"),
                ),
            )
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
        registerDownloadReceiver(activity, manager, downloadId)
    }

    private fun registerDownloadReceiver(
        activity: Activity,
        manager: DownloadManager,
        downloadId: Long,
    ) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == DownloadManager.ACTION_DOWNLOAD_COMPLETE &&
                    intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L) == downloadId
                ) {
                    runCatching { activity.unregisterReceiver(this) }
                    installDownloadedApk(activity, manager, downloadId)
                }
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

    private fun installDownloadedApk(
        activity: Activity,
        manager: DownloadManager,
        downloadId: Long,
    ) {
        val uri = manager.getUriForDownloadedFile(downloadId)
        if (uri == null) {
            Toast.makeText(activity, R.string.update_download_failed, Toast.LENGTH_LONG).show()
        } else {
            activity.startActivity(
                Intent(Intent.ACTION_VIEW)
                    .setDataAndType(uri, APK_MIME)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION),
            )
        }
    }

    internal fun isNewer(candidate: String, current: String): Boolean {
        val left = parseVersion(candidate)
        val right = parseVersion(current)
        return if (left == null || right == null) false else compareVersions(left, right) > 0
    }

    private fun compareVersions(left: List<Int>, right: List<Int>): Int {
        var result = 0
        for (index in 0 until maxOf(left.size, right.size)) {
            if (result == 0) {
                result = left.getOrElse(index) { 0 }.compareTo(right.getOrElse(index) { 0 })
            }
        }
        return result
    }

    private fun parseVersion(value: String): List<Int>? {
        val parts = value.trim().removePrefix("v").substringBefore('-').split('.')
        return if (parts.isEmpty() || parts.any { it.toIntOrNull() == null }) {
            null
        } else {
            parts.map { requireNotNull(it.toIntOrNull()) }
        }
    }
}
