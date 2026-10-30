package dev.alaev.rezkatv.rezka

import android.app.Activity
import android.app.AlertDialog
import android.content.res.ColorStateList
import android.view.View
import android.webkit.CookieManager
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.preference.PreferenceManager
import com.bumptech.glide.Glide
import dev.alaev.rezkatv.BuildConfig
import dev.alaev.rezkatv.R
import kotlin.concurrent.thread

@Suppress("LongMethod", "MagicNumber")
internal class MobileSettingsView(activity: Activity) : MobileSettingsPanel(activity) {
    private val prefs = PreferenceManager.getDefaultSharedPreferences(activity)
    private lateinit var accountStatus: TextView

    fun create(): View {
        val scroll = ScrollView(activity).apply {
            setBackgroundColor(background)
            isFillViewport = true
            clipToPadding = false
        }
        val root = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(22), dp(20), dp(32))
        }
        scroll.addView(root)
        val back = label("‹   " + activity.getString(R.string.settings), 28f, text, true).apply {
            setPadding(0, dp(4), 0, dp(14))
            isClickable = true
            isFocusable = true
            contentDescription = activity.getString(R.string.settings)
            setOnClickListener { activity.finish() }
        }
        root.addView(back)
        root.addView(
            label("REZKA TV · МОБИЛЬНОЕ ПРИЛОЖЕНИЕ", 11f, muted).apply {
                setPadding(0, 0, 0, dp(22))
            },
        )

        val account = panel(root)
        accountStatus = label("", 12f, accent)
        val accountRow = row(account, activity.getString(R.string.pref_login), accountStatus)
        accountRow.setOnClickListener { activity.openLogin() }
        refreshAccount()

        section(root, activity.getString(R.string.pref_playback))
        val playback = panel(root)
        choice(playback, R.string.pref_quality, "quality", "auto", R.array.quality_titles, R.array.quality_values)
        divider(playback)
        toggle(playback, R.string.pref_autoplay, R.string.pref_autoplay_summary, "autoplay_next", true)
        divider(playback)
        choice(playback, R.string.pref_seek_step, "seek_step", "10", R.array.seek_titles, R.array.seek_values)

        section(root, activity.getString(R.string.pref_interface))
        val interfacePanel = panel(root)
        toggle(interfacePanel, R.string.pref_lite, R.string.pref_lite_summary, "lite_mode", false)

        section(root, activity.getString(R.string.pref_site))
        val site = panel(root)
        choice(site, R.string.pref_mirror, "mirror", "https://rezka.fi/", R.array.mirror_titles, R.array.mirror_values)
        divider(site)
        row(site, activity.getString(R.string.open_site)).setOnClickListener { activity.openSite() }
        divider(site)
        row(site, activity.getString(R.string.pref_clear_cache)).setOnClickListener {
            val context = activity.applicationContext
            RezkaClient.get(context).clearCaches()
            Glide.get(context).clearMemory()
            thread { Glide.get(context).clearDiskCache() }
            Toast.makeText(context, R.string.cache_cleared, Toast.LENGTH_SHORT).show()
        }
        divider(site)
        row(site, activity.getString(R.string.pref_logout)).setOnClickListener {
            AlertDialog.Builder(activity)
                .setMessage(R.string.pref_logout)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    RezkaClient.get(activity).logout()
                    CookieManager.getInstance().removeAllCookies(null)
                    refreshAccount()
                    Toast.makeText(activity, R.string.logout_done, Toast.LENGTH_SHORT).show()
                }.show()
        }

        section(root, "ПРИЛОЖЕНИЕ")
        val application = panel(root)
        row(application, activity.getString(R.string.pref_check_updates), label("↻", 24f, muted))
            .setOnClickListener { UpdateManager.check(activity, force = true) }
        divider(application)
        val version = row(application, activity.getString(R.string.pref_version))
        version.addView(label(BuildConfig.VERSION_NAME, 14f, muted))
        return scroll
    }

    fun refreshAccount() {
        if (::accountStatus.isInitialized) {
            accountStatus.text = if (RezkaClient.get(activity).isLoggedIn) "Подключён" else "Не подключён"
        }
    }

    private fun choice(group: LinearLayout, title: Int, key: String, default: String, titlesId: Int, valuesId: Int) {
        val titles = activity.resources.getStringArray(titlesId)
        val values = activity.resources.getStringArray(valuesId)
        val summary = label("", 13f, muted)
        val item = row(group, activity.getString(title), summary)
        fun update() {
            val index = MobileSettingsSelection.index(values, prefs.getString(key, default))
            summary.text = titles.getOrNull(index).orEmpty() + "  ›"
        }
        update()
        item.setOnClickListener {
            val selected = MobileSettingsSelection.index(values, prefs.getString(key, default))
            AlertDialog.Builder(activity)
                .setTitle(title)
                .setSingleChoiceItems(titles, selected) { dialog, index ->
                    prefs.edit().putString(key, values[index]).apply()
                    update()
                    dialog.dismiss()
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }
    }

    private fun toggle(group: LinearLayout, title: Int, detail: Int, key: String, default: Boolean) {
        val control = Switch(activity).apply {
            text = ""
            isChecked = prefs.getBoolean(key, default)
            thumbTintList = ColorStateList.valueOf(accent)
            setOnCheckedChangeListener { _, checked -> prefs.edit().putBoolean(key, checked).apply() }
        }
        val item = row(group, activity.getString(title), control, activity.getString(detail))
        item.setOnClickListener { control.isChecked = !control.isChecked }
    }
}
