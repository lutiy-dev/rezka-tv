package dev.alaev.rezkatv.rezka

import android.app.AlertDialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import dev.alaev.rezkatv.R

class MobileNavigation(
    private val bar: LinearLayout,
    private val onSelect: (Section) -> Unit,
    private val vertical: Boolean = false,
) {
    private val primary = listOf(Section.HOME, Section.FILMS, Section.SERIES, Section.SEARCH)
    private val secondary = Section.entries.filterNot { it in primary }
    private val buttons = mutableMapOf<Section, TextView>()

    init {
        bar.visibility = View.VISIBLE
        bar.orientation = if (vertical) LinearLayout.VERTICAL else LinearLayout.HORIZONTAL
        primary.forEach { section ->
            buttons[section] = addButton(section.label, section.icon) { select(section) }
        }
        addButton(R.string.mobile_more, R.drawable.ic_settings) { showMore() }
        update(Section.HOME)
    }

    fun update(section: Section) {
        buttons.forEach { (key, button) ->
            val color = bar.context.getColor(if (key == section) R.color.accent else R.color.text)
            button.isActivated = key == section
            button.compoundDrawableTintList = ColorStateList.valueOf(color)
            button.setTextColor(color)
        }
    }

    private fun select(section: Section) {
        onSelect(section)
        if (section != Section.SEARCH && section != Section.SETTINGS) update(section)
    }

    private fun showMore() {
        val context = bar.context
        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                dp(MENU_HORIZONTAL_PADDING),
                dp(MENU_VERTICAL_PADDING),
                dp(MENU_HORIZONTAL_PADDING),
                dp(MENU_VERTICAL_PADDING),
            )
            background = GradientDrawable().apply {
                setColor(Color.parseColor(MENU_SURFACE))
                cornerRadius = dp(MENU_CORNER_RADIUS).toFloat()
            }
        }
        content.addView(
            TextView(context).apply {
                setText(R.string.mobile_more)
                textSize = MENU_TITLE_SIZE
                setTextColor(context.getColor(R.color.accent))
                setTypeface(null, Typeface.BOLD)
                setPadding(dp(MENU_ITEM_PADDING), 0, 0, dp(MENU_ITEM_PADDING))
            },
        )
        val scroll = ScrollView(context).apply { addView(content) }
        val dialog = AlertDialog.Builder(context).setView(scroll).create()
        secondary.forEachIndexed { index, section ->
            content.addView(
                menuRow(section) {
                    dialog.dismiss()
                    select(section)
                },
            )
            if (index != secondary.lastIndex) content.addView(menuDivider())
        }
        dialog.setCanceledOnTouchOutside(true)
        dialog.show()
        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout(
                minOf(context.resources.displayMetrics.widthPixels - dp(MENU_SCREEN_MARGIN), dp(MENU_MAX_WIDTH)),
                android.view.WindowManager.LayoutParams.WRAP_CONTENT,
            )
            setDimAmount(MENU_DIM_AMOUNT)
        }
    }

    private fun menuRow(section: Section, onClick: () -> Unit): LinearLayout {
        val context = bar.context
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            minimumHeight = dp(MENU_ROW_HEIGHT)
            setPadding(dp(MENU_ITEM_PADDING), 0, dp(MENU_ITEM_PADDING), 0)
            isClickable = true
            isFocusable = true
            contentDescription = context.getString(section.label)
            setOnClickListener { onClick() }
        }
        row.addView(
            ImageView(context).apply {
                setImageResource(section.icon)
                imageTintList = ColorStateList.valueOf(context.getColor(R.color.accent))
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            },
            LinearLayout.LayoutParams(dp(MENU_ICON_SIZE), dp(MENU_ICON_SIZE)),
        )
        row.addView(
            TextView(context).apply {
                setText(section.label)
                textSize = MENU_ITEM_SIZE
                setTextColor(Color.WHITE)
            },
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = dp(MENU_ITEM_GAP)
            },
        )
        return row
    }

    private fun menuDivider() = View(bar.context).apply {
        setBackgroundColor(Color.parseColor(MENU_DIVIDER))
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            dp(MENU_DIVIDER_HEIGHT),
        )
    }

    private fun dp(value: Int) = (value * bar.resources.displayMetrics.density).toInt()

    private companion object {
        const val MENU_SURFACE = "#20252D"
        const val MENU_DIVIDER = "#353B44"
        const val MENU_HORIZONTAL_PADDING = 16
        const val MENU_VERTICAL_PADDING = 14
        const val MENU_CORNER_RADIUS = 16
        const val MENU_ITEM_PADDING = 12
        const val MENU_ROW_HEIGHT = 56
        const val MENU_ICON_SIZE = 24
        const val MENU_ITEM_GAP = 16
        const val MENU_DIVIDER_HEIGHT = 1
        const val MENU_SCREEN_MARGIN = 36
        const val MENU_MAX_WIDTH = 440
        const val MENU_TITLE_SIZE = 20f
        const val MENU_ITEM_SIZE = 16f
        const val MENU_DIM_AMOUNT = 0.65f
    }

    private fun addButton(label: Int, icon: Int, action: () -> Unit): TextView {
        val button = TextView(bar.context).apply {
            setText(label)
            textSize = 11f
            gravity = Gravity.CENTER
            isClickable = true
            isFocusable = true
            contentDescription = bar.context.getString(label)
            setCompoundDrawablesWithIntrinsicBounds(0, icon, 0, 0)
            compoundDrawablePadding = (3 * resources.displayMetrics.density).toInt()
            setOnClickListener { action() }
        }
        val params = if (vertical) {
            LinearLayout.LayoutParams(-1, 0, 1f)
        } else {
            LinearLayout.LayoutParams(0, -1, 1f)
        }
        bar.addView(button, params)
        return button
    }
}
