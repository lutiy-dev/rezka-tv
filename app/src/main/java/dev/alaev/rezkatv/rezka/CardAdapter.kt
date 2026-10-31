package dev.alaev.rezkatv.rezka

import android.content.res.Configuration
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import dev.alaev.rezkatv.R

class CardAdapter(
    private val onFocus: (Card) -> Unit = {},
    private val onLongClick: ((Card) -> Unit)? = null,
    private val progressOf: (Card) -> Int? = { null },
    private val onClick: (Card) -> Unit,
    private val catalogMode: Boolean = false,
    private val searchMode: Boolean = false,
) : RecyclerView.Adapter<CardAdapter.Holder>() {

    private val cards = mutableListOf<Card>()

    var onNearEnd: (() -> Unit)? = null

    val items: List<Card> get() = cards

    fun append(more: List<Card>) {
        val start = cards.size
        cards += more
        notifyItemRangeInserted(start, more.size)
    }

    fun replace(all: List<Card>): Boolean {
        if (all == cards) return false
        cards.clear()
        cards += all
        notifyDataSetChanged()
        return true
    }

    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val poster: ImageView = view.findViewById(R.id.poster)
        val badge: TextView = view.findViewById(R.id.badge)
        val progress: ProgressBar = view.findViewById(R.id.progress)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_card, parent, false)
        view.clipToOutline = true
        val phone = !ModeSelection.isTv(parent.context)
        if ((catalogMode || searchMode) && phone) {
            val widthDp = parent.resources.configuration.screenWidthDp
            val landscape = parent.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
            val availableDp = widthDp - if (landscape) MOBILE_RAIL_DP else 0
            val columns = if (searchMode) {
                ResponsiveLayout.searchColumns(widthDp)
            } else {
                ResponsiveLayout.mobileCatalogColumns(availableDp, landscape)
            }
            val gapDp = if (searchMode) {
                SEARCH_GAP_DP
            } else {
                CATALOG_GAP_DP
            }
            val itemWidthDp = if (searchMode) {
                ResponsiveLayout.searchPosterWidthDp(widthDp)
            } else {
                (availableDp - CATALOG_PADDING_DP * 2 - gapDp * (columns - 1)) / columns
            }
            val density = parent.resources.displayMetrics.density
            val width = (itemWidthDp * density).toInt()
            view.layoutParams = RecyclerView.LayoutParams(width, width * POSTER_HEIGHT_RATIO / POSTER_WIDTH_RATIO)
        }
        if (!ModeSelection.cardFocusable(ModeSelection.effective(parent.context))) {
            view.isFocusable = false
            view.isFocusableInTouchMode = false
        }
        view.setOnFocusChangeListener { v, focused ->
            val scale = if (focused) FOCUS_SCALE else 1f
            v.animate().scaleX(scale).scaleY(scale).setDuration(120).start()
            if (focused) (v.tag as? Card)?.let(onFocus)
        }
        return Holder(view)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val card = cards[position]
        holder.itemView.tag = card
        Images.poster(holder.poster, card.poster)
        val badge = card.badge?.substringBefore(" (")
        holder.badge.text = badge
        holder.badge.visibility = if (badge.isNullOrEmpty()) View.GONE else View.VISIBLE
        val progress = progressOf(card)
        holder.progress.visibility = if (progress == null) View.GONE else View.VISIBLE
        holder.progress.progress = progress ?: 0
        holder.itemView.setOnClickListener { onClick(card) }
        holder.itemView.setOnLongClickListener(onLongClick?.let { handler -> View.OnLongClickListener { handler(card); true } })
        if (position >= cards.size - PRELOAD_AHEAD) onNearEnd?.invoke()
    }

    override fun getItemCount() = cards.size

    private companion object {
        const val FOCUS_SCALE = 1.08f
        const val PRELOAD_AHEAD = 8
        const val CATALOG_GAP_DP = 12
        const val SEARCH_GAP_DP = 14
        const val MOBILE_RAIL_DP = 72
        const val CATALOG_PADDING_DP = 12
        const val POSTER_HEIGHT_RATIO = 3
        const val POSTER_WIDTH_RATIO = 2
    }
}
