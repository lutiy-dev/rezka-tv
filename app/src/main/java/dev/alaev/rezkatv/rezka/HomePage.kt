package dev.alaev.rezkatv.rezka

import android.app.Activity
import android.graphics.Rect
import android.os.Process
import android.os.SystemClock
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.leanback.widget.HorizontalGridView
import androidx.leanback.widget.OnChildViewHolderSelectedListener
import androidx.leanback.widget.VerticalGridView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dev.alaev.rezkatv.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class HomePage(
    private val activity: Activity,
    private val client: RezkaClient,
    private val scope: CoroutineScope,
    private val loader: Loader,
    private val resume: ResumeStore,
    private val lite: Boolean,
) {
    private class ShelfRow(val shelf: Shelf, val adapter: CardAdapter, var nextUrl: String?) {
        var more: Job? = null
        var selected = 0
    }

    private val page: View = activity.findViewById(R.id.home)
    private val rowsView: VerticalGridView = activity.findViewById(R.id.rows)
    private val mobileRows: RecyclerView = activity.findViewById(R.id.mobile_rows)
    private val heroArtBox: View = activity.findViewById(R.id.hero_art_box)
    private val heroArt: ImageView = activity.findViewById(R.id.hero_art)
    private val heroTitle: TextView = activity.findViewById(R.id.hero_title)
    private val heroMeta: TextView = activity.findViewById(R.id.hero_meta)
    private val heroExtra: TextView = activity.findViewById(R.id.hero_extra)
    private val heroDescription: TextView = activity.findViewById(R.id.hero_description)

    private val compact = ResponsiveLayout.isPhone(activity.resources.configuration.smallestScreenWidthDp)
    private val shelves = Shelf.entries.filter { !lite || it.inLiteMode }
    private val rows = mutableListOf<ShelfRow>()
    private val rowsAdapter = RowsAdapter()
    private val cardPool = RecyclerView.RecycledViewPool()

    private var focusedCard: Card? = null
    private var heroJob: Job? = null
    private var userNavigated = false

    val isVisible get() = page.visibility == View.VISIBLE
    val isEmpty get() = rows.isEmpty()

    init {
        if (compact) {
            rowsView.visibility = View.GONE
            mobileRows.visibility = View.VISIBLE
            mobileRows.layoutManager = LinearLayoutManager(activity)
            mobileRows.adapter = rowsAdapter
        } else {
            rowsView.adapter = rowsAdapter
        }
        if (ResponsiveLayout.useEdgeAwareRows(activity.resources.configuration.smallestScreenWidthDp)) {
            rowsView.keepFocusInsideEdges()
        } else {
            rowsView.pinFocusToStart(0)
        }
        rowsView.setItemViewCacheSize(shelves.size)
        heroDescription.maxLines = if (lite) 2 else 3
        if (lite || compact) heroArtBox.visibility = View.GONE
    }

    fun show() {
        page.visibility = View.VISIBLE
        if (!lite && !compact) heroArtBox.visibility = View.VISIBLE
        focusedCard?.let(::showHero)
    }

    fun hide() {
        page.visibility = View.GONE
        heroArtBox.visibility = View.GONE
        heroJob?.cancel()
    }

    fun focus() {
        if (!compact) rowsView.requestFocus()
    }

    fun onUserKey() {
        userNavigated = true
    }

    fun load() {
        shelves.forEach { shelf -> client.cachedPage(client.shelfUrl(shelf))?.let { show(shelf, it.value) } }
        shelves.forEach { shelf ->
            val cached = client.cachedPage(client.shelfUrl(shelf))
            if (cached == null || cached.age() > shelf.ttl()) refresh(shelf)
        }
        if (rows.isEmpty()) loader.showStatus(R.string.loading)
    }

    fun refresh(shelf: Shelf) {
        if (shelf !in shelves) return
        scope.launch {
            val page = loader.load { client.page(client.shelfUrl(shelf)) } ?: return@launch
            show(shelf, page)
        }
    }

    fun onBack(): Boolean {
        val canScroll = if (compact) mobileRows.computeVerticalScrollOffset() > 0 else rowsView.selectedPosition > 0
        if (canScroll) {
            if (compact) mobileRows.smoothScrollToPosition(0) else rowsView.setSelectedPositionSmooth(0)
        }
        return canScroll
    }

    private fun show(shelf: Shelf, page: Page) {
        val row = rows.find { it.shelf == shelf }
        if (row == null) {
            if (page.cards.isNotEmpty()) addRow(shelf, page)
            return
        }
        val focused = row.adapter.items.getOrNull(row.selected)?.url?.takeIf { row.selected > 0 }
        if (row.adapter.replace(page.cards)) {
            row.more?.cancel()
            row.nextUrl = page.nextUrl
            row.selected = if (focused == null) 0 else page.cards.indexOfFirst { it.url == focused }.coerceAtLeast(0)
            rowsAdapter.notifyItemChanged(rows.indexOf(row))
        }
    }

    private fun addRow(shelf: Shelf, page: Page) {
        val adapter = CardAdapter(
            onFocus = ::showHero,
            onLongClick = { activity.openDetails(it) },
            progressOf = ::progress,
            onClick = { card -> if (shelf == Shelf.CONTINUE) activity.playFromCard(card) else activity.openDetails(card) },
        )
        adapter.append(page.cards)
        val row = ShelfRow(shelf, adapter, page.nextUrl)
        adapter.onNearEnd = { loadMore(row) }

        val index = rows.indexOfFirst { it.shelf.ordinal > shelf.ordinal }.takeIf { it >= 0 } ?: rows.size
        rows.add(index, row)
        rowsAdapter.notifyItemInserted(index)
        loader.hideStatus()
        if (rows.size == 1) Log.i(TAG, "first row: ${SystemClock.elapsedRealtime() - Process.getStartElapsedRealtime()} ms after process start")
        if (!compact && !userNavigated && isVisible) {
            rowsView.selectedPosition = 0
            rowsView.requestFocus()
        }
    }

    private fun loadMore(row: ShelfRow) {
        val next = row.nextUrl ?: return
        if (row.more?.isActive == true) return
        row.more = scope.launch {
            val page = loader.load { client.page(next) } ?: return@launch
            if (row.nextUrl != next) return@launch
            row.nextUrl = page.nextUrl
            row.adapter.append(page.cards)
        }
    }

    private fun progress(card: Card): Int? {
        val (season, episode) = RezkaParser.seasonEpisode(card.badge) ?: (0 to 0)
        val entry = resume.get(card.id, season, episode) ?: return null
        if (entry.durationMs <= 0) return null
        return (entry.positionMs * 100 / entry.durationMs).toInt().coerceIn(1, 100)
    }

    private fun showHero(card: Card) {
        focusedCard = card
        heroTitle.text = card.title
        heroMeta.text = card.subtitle
        heroExtra.text = card.badge.orEmpty()
        heroDescription.text = ""

        heroJob?.cancel()
        val cached = client.cachedDetails(card.url)?.value
        if (cached != null) showHeroDetails(card, cached)
        heroJob = scope.launch {
            delay(HERO_ART_DELAY_MS)
            if (!lite && !compact) Images.art(heroArt, cached?.poster.orEmpty(), card.poster)
            if (cached != null) return@launch
            delay(HERO_DETAILS_DELAY_MS)
            val details = loader.quietly { client.details(card.url) }
            if (details != null && focusedCard == card) {
                showHeroDetails(card, details)
                if (!lite && !compact) Images.art(heroArt, details.poster, card.poster)
            }
        }
    }

    private fun showHeroDetails(card: Card, details: Details) {
        if (details.meta.isNotEmpty()) heroMeta.text = details.meta
        heroExtra.text = listOfNotNull(details.ratings, card.badge?.substringBefore(" (")).joinToString("  ·  ")
        heroDescription.text = details.description
    }

    private fun Shelf.ttl(): Long = when (this) {
        Shelf.CONTINUE -> 0
        Shelf.FAVORITES -> 2 * 60_000L
        else -> 30 * 60_000L
    }

    private inner class RowHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.row_title)
        val cards: RecyclerView = view.findViewById(R.id.row_cards)
        var row: ShelfRow? = null

        init {
            cards.setRecycledViewPool(cardPool)
            if (compact) {
                cards.layoutManager = LinearLayoutManager(activity, RecyclerView.HORIZONTAL, false)
            } else {
                val tvCards = cards as HorizontalGridView
                tvCards.pinFocusToStart(tvCards.paddingStart)
                val overflow = activity.resources.getDimensionPixelSize(R.dimen.card_focus_overflow)
                tvCards.clipBounds = Rect(tvCards.paddingStart - overflow, -CLIP_FAR, CLIP_FAR, CLIP_FAR)
                tvCards.horizontalSpacing = activity.resources.getDimensionPixelSize(R.dimen.card_spacing)
                tvCards.setOnChildViewHolderSelectedListener(object : OnChildViewHolderSelectedListener() {
                    override fun onChildViewHolderSelected(
                        parent: RecyclerView, child: RecyclerView.ViewHolder?, position: Int, subposition: Int,
                    ) {
                        row?.selected = position
                    }
                })
            }
        }
    }

    private inner class RowsAdapter : RecyclerView.Adapter<RowHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RowHolder {
            val layout = if (compact) R.layout.item_row_mobile else R.layout.item_row
            return RowHolder(LayoutInflater.from(parent.context).inflate(layout, parent, false))
        }

        override fun onBindViewHolder(holder: RowHolder, position: Int) {
            val row = rows[position]
            holder.row = row
            holder.title.text = row.shelf.title
            if (holder.cards.adapter !== row.adapter) holder.cards.adapter = row.adapter
            if (!compact) (holder.cards as HorizontalGridView).selectedPosition = row.selected
        }

        override fun getItemCount() = rows.size
    }

    private companion object {
        const val HERO_ART_DELAY_MS = 150L
        const val HERO_DETAILS_DELAY_MS = 550L
        const val CLIP_FAR = 100_000
        const val TAG = "RezkaTv"
    }
}
