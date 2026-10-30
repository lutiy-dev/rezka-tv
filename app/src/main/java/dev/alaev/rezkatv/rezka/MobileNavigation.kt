package dev.alaev.rezkatv.rezka

import android.app.AlertDialog
import android.content.res.ColorStateList
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
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
        val labels = secondary.map { bar.context.getString(it.label) }.toTypedArray()
        AlertDialog.Builder(bar.context).setItems(labels) { _, index -> select(secondary[index]) }.show()
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
