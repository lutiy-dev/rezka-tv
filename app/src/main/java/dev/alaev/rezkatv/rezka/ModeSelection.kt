package dev.alaev.rezkatv.rezka

import android.app.UiModeManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import androidx.preference.PreferenceManager

internal object ModeSelection {
    const val KEY = "control_mode"
    const val AUTO = "auto"
    const val TV = "tv"
    const val TOUCH = "touch"

    fun resolve(saved: String?, television: Boolean, leanback: Boolean): String = when (saved) {
        TV -> TV
        TOUCH -> TOUCH
        else -> if (television || leanback) TV else TOUCH
    }

    fun hasChoice(context: Context): Boolean = PreferenceManager.getDefaultSharedPreferences(context).contains(KEY)

    fun saved(context: Context): String? = PreferenceManager.getDefaultSharedPreferences(context).getString(KEY, null)

    fun save(context: Context, value: String) {
        require(value == AUTO || value == TV || value == TOUCH)
        PreferenceManager.getDefaultSharedPreferences(context).edit().putString(KEY, value).apply()
    }

    fun effective(context: Context): String {
        val manager = context.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
        val television = manager.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
        val leanback = context.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
        return resolve(saved(context), television, leanback)
    }

    fun isTv(context: Context): Boolean = effective(context) == TV

    fun cardFocusable(mode: String): Boolean = mode == TV
}
