package dev.alaev.rezkatv.rezka

import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.webkit.CookieManager
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.leanback.preference.LeanbackPreferenceFragmentCompat
import androidx.leanback.preference.LeanbackSettingsFragmentCompat
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceScreen
import com.bumptech.glide.Glide
import dev.alaev.rezkatv.BuildConfig
import dev.alaev.rezkatv.R
import kotlin.concurrent.thread

class SettingsActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val mode = getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
        val isTv = mode.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
        if (!isTv) setTheme(R.style.Theme_RezkaTv_Settings_Mobile)
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            val content = if (isTv) SettingsFragment() else MobilePrefsFragment()
            supportFragmentManager.beginTransaction().replace(android.R.id.content, content).commit()
        }
    }

    class SettingsFragment : LeanbackSettingsFragmentCompat() {
        override fun onPreferenceStartInitialScreen() = startPreferenceFragment(PrefsFragment())

        override fun onPreferenceStartFragment(caller: PreferenceFragmentCompat, pref: Preference) = false

        override fun onPreferenceStartScreen(caller: PreferenceFragmentCompat, pref: PreferenceScreen): Boolean {
            startPreferenceFragment(PrefsFragment().apply {
                arguments = Bundle().apply { putString(PreferenceFragmentCompat.ARG_PREFERENCE_ROOT, pref.key) }
            })
            return true
        }
    }

    class PrefsFragment : LeanbackPreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.settings, rootKey)
            bindPreferences(this)
        }

        override fun onResume() {
            super.onResume()
            updateAccount(this)
        }
    }

    class MobilePrefsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.settings, rootKey)
            bindPreferences(this)
        }

        override fun onResume() {
            super.onResume()
            updateAccount(this)
        }
    }

    companion object {
        private fun bindPreferences(fragment: PreferenceFragmentCompat) {
            fragment.findPreference<Preference>("version")?.summary = BuildConfig.VERSION_NAME
            fragment.findPreference<Preference>("check_updates")?.setOnPreferenceClickListener {
                UpdateManager.check(fragment.requireActivity(), force = true)
                true
            }
            fragment.findPreference<Preference>("open_site")?.setOnPreferenceClickListener {
                fragment.requireActivity().openSite()
                true
            }
            fragment.findPreference<Preference>("clear_cache")?.setOnPreferenceClickListener {
                val context = fragment.requireContext().applicationContext
                RezkaClient.get(context).clearCaches()
                Glide.get(context).clearMemory()
                thread { Glide.get(context).clearDiskCache() }
                Toast.makeText(context, R.string.cache_cleared, Toast.LENGTH_SHORT).show()
                true
            }
            fragment.findPreference<Preference>("login")?.setOnPreferenceClickListener {
                fragment.requireActivity().openLogin()
                true
            }
            fragment.findPreference<Preference>("logout")?.setOnPreferenceClickListener {
                RezkaClient.get(fragment.requireContext()).logout()
                CookieManager.getInstance().removeAllCookies(null)
                Toast.makeText(fragment.requireContext(), R.string.logout_done, Toast.LENGTH_SHORT).show()
                updateAccount(fragment)
                true
            }
            updateAccount(fragment)
        }

        private fun updateAccount(fragment: PreferenceFragmentCompat) {
            val loggedIn = RezkaClient.get(fragment.requireContext()).isLoggedIn
            fragment.findPreference<Preference>("login")?.summary =
                fragment.getString(if (loggedIn) R.string.logged_in else R.string.logged_out)
            fragment.findPreference<Preference>("logout")?.isVisible = loggedIn
        }
    }
}
