package dev.alaev.rezkatv.rezka

import android.app.Activity
import android.content.res.Configuration
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.leanback.widget.VerticalGridView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dev.alaev.rezkatv.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class GridPage(
    private val activity: Activity,
    private val client: RezkaClient,
    private val scope: CoroutineScope,
    private val loader: Loader,
) {
    private val page: View = activity.findViewById(R.id.grid_page)
    private val title: TextView = activity.findViewById(R.id.grid_title)
    private val chips: LinearLayout = activity.findViewById(R.id.sort_chips)
    private val info: TextView = activity.findViewById(R.id.grid_info)
    private val grid: VerticalGridView = activity.findViewById(R.id.grid)
    private val mobileGrid: RecyclerView = activity.findViewById(R.id.mobile_grid)
    private val phoneMode = !ModeSelection.isTv(activity)
    private val adapter = CardAdapter(onFocus = ::showInfo, onClick = { activity.openDetails(it) }, catalogMode = true)
    private val columns = ResponsiveLayout.gridColumns(activity.resources.configuration.screenWidthDp)
    private val landscape = activity.resources.configuration.orientation ==
        Configuration.ORIENTATION_LANDSCAPE
    private val mobileColumns = ResponsiveLayout.mobileCatalogColumns(
        activity.resources.configuration.screenWidthDp - if (landscape) MOBILE_RAIL_DP else 0,
        landscape,
    )

    private var category: Category? = null
    private var sort = Sort.POPULAR
    private var nextUrl: String? = null
    private var loadJob: Job? = null
    private var moreJob: Job? = null

    val isVisible get() = page.visibility == View.VISIBLE

    init {
        if (phoneMode) {
            grid.visibility = View.GONE
            mobileGrid.visibility = View.VISIBLE
            mobileGrid.layoutManager = GridLayoutManager(activity, mobileColumns)
            mobileGrid.adapter = adapter
        }
        grid.setNumColumns(columns)
        grid.pinFocusToStart(grid.paddingTop)
        val spacing = activity.resources.getDimensionPixelSize(R.dimen.card_spacing)
        grid.horizontalSpacing = spacing + spacing / 2
        grid.verticalSpacing = spacing + spacing / 2
        if (!phoneMode) grid.adapter = adapter
        adapter.onNearEnd = ::loadMore
    }

    fun show(category: Category) {
        page.visibility = View.VISIBLE
        if (this.category == category) return
        this.category = category
        sort = Sort.POPULAR
        title.text = category.title
        buildChips(category)
        load()
    }

    fun hide() {
        page.visibility = View.GONE
    }

    fun focus() {
        if (adapter.itemCount > 0) {
            if (phoneMode) mobileGrid.requestFocus() else grid.requestFocus()
        } else {
            chips.getChildAt(0)?.requestFocus()
        }
    }

    fun onBack(): Boolean {
        val canScroll = if (phoneMode) {
            mobileGrid.computeVerticalScrollOffset() > 0
        } else {
            grid.selectedPosition >= columns
        }
        if (canScroll) {
            if (phoneMode) {
                mobileGrid.smoothScrollToPosition(0)
            } else {
                grid.setSelectedPositionSmooth(0)
            }
        }
        return canScroll
    }

    private fun buildChips(category: Category) {
        chips.removeAllViews()
        if (!category.sortable) return
        val inflater = LayoutInflater.from(activity)
        Sort.entries.forEach { s ->
            val chip = inflater.inflate(R.layout.item_chip, chips, false) as TextView
            chip.text = s.title
            chip.isActivated = s == sort
            chip.setOnClickListener {
                if (sort == s) return@setOnClickListener
                sort = s
                for (i in 0 until chips.childCount) chips.getChildAt(i).isActivated = i == s.ordinal
                load()
            }
            chips.addView(chip)
        }
    }

    private fun load() {
        val category = category ?: return
        val url = client.categoryUrl(category, sort)
        loadJob?.cancel()
        moreJob?.cancel()
        moreJob = null
        val cached = client.cachedPage(url)
        if (cached != null) showPage(cached.value) else {
            adapter.replace(emptyList())
            info.setText(R.string.loading)
        }
        if (cached != null && cached.age() < CACHE_TTL_MS) return
        loadJob = scope.launch {
            val fresh = loader.quietly { client.page(url) }
            when {
                fresh != null -> showPage(fresh)
                adapter.itemCount == 0 -> info.setText(R.string.load_failed)
            }
        }
    }

    private fun showPage(p: Page) {
        nextUrl = p.nextUrl
        if (adapter.replace(p.cards)) {
            if (phoneMode) mobileGrid.scrollToPosition(0) else grid.selectedPosition = 0
        }
        if (p.cards.isEmpty()) info.setText(R.string.nothing_here) else if (!grid.hasFocus()) info.text = ""
    }

    private fun loadMore() {
        val next = nextUrl ?: return
        if (moreJob?.isActive == true) return
        moreJob = scope.launch {
            val more = loader.quietly { client.page(next) } ?: return@launch
            if (nextUrl != next) return@launch
            nextUrl = more.nextUrl
            adapter.append(more.cards)
        }
    }

    private fun showInfo(card: Card) {
        info.text = listOf(card.title, card.subtitle, card.badge.orEmpty()).filter { it.isNotBlank() }.joinToString("  ·  ")
    }

    private companion object {
        const val CACHE_TTL_MS = 15 * 60_000L
        const val MOBILE_RAIL_DP = 72
    }
}
