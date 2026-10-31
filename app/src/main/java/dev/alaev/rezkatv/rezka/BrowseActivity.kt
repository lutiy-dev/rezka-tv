package dev.alaev.rezkatv.rezka

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.os.Process
import android.view.FocusFinder
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.leanback.widget.BrowseFrameLayout
import dev.alaev.rezkatv.R
import dev.alaev.rezkatv.rezka.lists.ListsAssets
import dev.alaev.rezkatv.rezka.lists.OscarPage
import dev.alaev.rezkatv.rezka.lists.TitleMarks
import dev.alaev.rezkatv.rezka.lists.TopPage
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel

@Suppress("TooManyFunctions")
class BrowseActivity : Activity() {

    private val scope = MainScope()
    private lateinit var client: RezkaClient
    private lateinit var settings: Settings
    private lateinit var rail: NavRail
    private lateinit var home: HomePage
    private lateinit var grid: GridPage
    private lateinit var top: TopPage
    private lateinit var oscar: OscarPage
    private lateinit var choice: ChoicePanel
    private lateinit var root: BrowseFrameLayout

    private val phoneMode by lazy { !ModeSelection.isTv(this) }
    private var displayedOrientation = Configuration.ORIENTATION_UNDEFINED
    private var mobileNavigation: MobileNavigation? = null
    private var liteMode = false
    private var baseUrl = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        intent.getStringExtra(EXTRA_BASE)?.let { RezkaClient.setBaseUrl(this, it) }
        settings = Settings(this)
        client = RezkaClient.get(this)
        liteMode = settings.liteMode
        baseUrl = client.baseUrl
        setContentView(R.layout.activity_browse)
        displayedOrientation = resources.configuration.orientation

        root = findViewById(R.id.root)
        if (phoneMode) {
            findViewById<View>(R.id.clock).visibility = View.GONE
            findViewById<View>(R.id.hero_text).visibility = View.GONE
            findViewById<View>(R.id.hero_art_box).visibility = View.GONE
            findViewById<LinearLayout>(R.id.grid_header).orientation = LinearLayout.VERTICAL
        }
        val loader = Loader(this, findViewById<TextView>(R.id.status))
        home = HomePage(this, client, scope, loader, ResumeStore(this), liteMode)
        grid = GridPage(this, client, scope, loader)
        choice = ChoicePanel(findViewById(R.id.choice_panel))
        val marks = TitleMarks(this, client)
        top = TopPage(this, client, scope, marks, choice)
        oscar = OscarPage(this, client, scope, marks)
        rail = NavRail(findViewById(R.id.rail), findViewById(R.id.rail_scrim), ::onSection) { ListsAssets.preload(this) }

        if (phoneMode) configureMobileNavigation() else configureTvNavigation()
        savedInstanceState?.getString(STATE_SECTION)?.let { saved ->
            Section.entries.find { it.name == saved && it != Section.SEARCH && it != Section.SETTINGS }
                ?.takeIf { it != Section.HOME }?.let(::onSection)
        }

        home.load()
        if (!ModeSelection.hasChoice(this)) showModePicker()
        UpdateManager.check(this)
    }

    private fun showModePicker() {
        val recommended = ModeSelection.effective(this)
        val options = arrayOf(getString(R.string.mode_tv), getString(R.string.mode_touch))
        val initial = if (recommended == ModeSelection.TV) 0 else 1
        var selected = initial
        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.mode_first_run)
            .setSingleChoiceItems(options, initial) { _, index -> selected = index }
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val chosen = if (selected == 0) ModeSelection.TV else ModeSelection.TOUCH
                ModeSelection.save(this, chosen)
                if (ModeSelection.isTv(this) == phoneMode) recreate()
            }
            .setCancelable(false)
            .create()
        dialog.show()
    }

    private fun configureMobileNavigation() {
        findViewById<View>(R.id.rail).visibility = View.GONE
        findViewById<View>(R.id.rail_scrim).visibility = View.GONE
        val landscape = displayedOrientation == Configuration.ORIENTATION_LANDSCAPE
        val density = resources.displayMetrics.density
        val content = findViewById<View>(R.id.content)
        val params = content.layoutParams as FrameLayout.LayoutParams
        params.leftMargin = if (landscape) (MOBILE_RAIL_DP * density).toInt() else 0
        params.bottomMargin = if (landscape) 0 else (MOBILE_BAR_DP * density).toInt()
        content.layoutParams = params
        val nav = findViewById<LinearLayout>(R.id.mobile_nav)
        val navParams = nav.layoutParams as FrameLayout.LayoutParams
        navParams.width = if (landscape) (MOBILE_RAIL_DP * density).toInt() else FrameLayout.LayoutParams.MATCH_PARENT
        navParams.height = if (landscape) FrameLayout.LayoutParams.MATCH_PARENT else (MOBILE_BAR_DP * density).toInt()
        navParams.gravity = if (landscape) Gravity.START else Gravity.BOTTOM
        nav.layoutParams = navParams
        findViewById<LinearLayout>(R.id.grid_header).orientation =
            if (landscape) LinearLayout.HORIZONTAL else LinearLayout.VERTICAL
        if (landscape) {
            findViewById<View>(R.id.grid_page).setPadding(0, (MOBILE_HEADER_PADDING_DP * density).toInt(), 0, 0)
        }
        mobileNavigation = MobileNavigation(nav, ::onSection, vertical = landscape)
    }

    private fun configureTvNavigation() {
        root.onFocusSearchListener = BrowseFrameLayout.OnFocusSearchListener { focused, direction ->
            when {
                choice.isShowing && (direction == View.FOCUS_LEFT || direction == View.FOCUS_RIGHT) -> focused
                direction == View.FOCUS_LEFT && !rail.contains(focused) -> {
                    val next = FocusFinder.getInstance().findNextFocus(root, focused, direction)
                    if (next == null || rail.contains(next)) rail.selectedView() else null
                }
                direction == View.FOCUS_RIGHT && rail.contains(focused) -> {
                    focusPage()
                    root.findFocus()?.takeUnless { rail.contains(it) }
                }
                else -> null
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (phoneMode && newConfig.orientation != displayedOrientation) recreate()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(STATE_SECTION, rail.selected.name)
        super.onSaveInstanceState(outState)
    }

    override fun onRestart() {
        super.onRestart()
        if (home.isEmpty) home.load() else home.refresh(Shelf.CONTINUE)
        if (top.isVisible) top.refresh()
        if (oscar.isVisible) oscar.refresh()
    }

    override fun onResume() {
        super.onResume()
        if (settings.liteMode != liteMode || RezkaClient.baseUrl(this) != baseUrl || ModeSelection.isTv(this) == phoneMode) recreate()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
        if (isFinishing) Process.killProcess(Process.myPid())
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) home.onUserKey()
        return super.dispatchKeyEvent(event)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (!phoneMode) {
            onTvBack()
            return
        }
        when {
            choice.isShowing -> choice.hide()
            home.isVisible && home.onBack() -> Unit
            grid.isVisible && grid.onBack() -> Unit
            top.isVisible && top.onBack() -> Unit
            oscar.isVisible && oscar.onBack() -> Unit
            rail.selected != Section.HOME -> onSection(Section.HOME)
            else -> finish()
        }
    }

    private fun onTvBack() {
        when {
            choice.isShowing -> choice.hide()
            rail.contains(currentFocus) -> if (rail.selected != Section.HOME) onSection(Section.HOME) else finish()
            home.isVisible && home.onBack() -> Unit
            grid.isVisible && grid.onBack() -> Unit
            top.isVisible && top.onBack() -> Unit
            oscar.isVisible && oscar.onBack() -> Unit
            else -> rail.focus()
        }
    }

    private fun onSection(section: Section) {
        when (section) {
            Section.SEARCH -> openSearch()

            Section.SETTINGS -> startActivity(Intent(this, SettingsActivity::class.java))

            else -> {
                rail.selected = section
                mobileNavigation?.update(section)
                if (section != Section.HOME) home.hide()
                if (section.category == null) grid.hide()
                if (section != Section.TOP) top.hide()
                if (section != Section.OSCAR) oscar.hide()
                when (section) {
                    Section.HOME -> home.show()
                    Section.TOP -> top.show()
                    Section.OSCAR -> oscar.show()
                    else -> grid.show(requireNotNull(section.category))
                }
                focusPage()
            }
        }
    }

    private fun focusPage() {
        when {
            grid.isVisible -> grid.focus()
            top.isVisible -> top.focus()
            oscar.isVisible -> oscar.focus()
            else -> home.focus()
        }
    }

    private companion object {
        const val EXTRA_BASE = "base"
        const val MOBILE_BAR_DP = 64
        const val MOBILE_RAIL_DP = 72
        const val MOBILE_HEADER_PADDING_DP = 8
        const val STATE_SECTION = "section"
    }
}
