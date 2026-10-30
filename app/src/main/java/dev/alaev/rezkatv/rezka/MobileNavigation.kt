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
        val density = context.resources.displayMetrics.density
        fun dp(value: Int) = (value * density + 0.5f).toInt()
        val accent = context.getColor(R.color.accent)
        val surface = Color.rgb(32, 37, 45)
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(18), dp(16), dp(14))
            background = GradientDrawable().apply {
                setColor(surface)
                cornerRadius = dp(16).toFloat()
            }
        }
        container.addView(TextView(context).apply {
            text = context.getString(R.string.mobile_more)
            textSize = 20f
            setTextColor(accent)
            setTypeface(null, Typeface.BOLD)
            setPadding(dp(12), 0, dp(12), dp(12))
        })
        val dialog = AlertDialog.Builder(context).setView(ScrollView(context).apply {
            isFillViewport = false
            addView(container)
        }).create()
        secondary.forEachIndexed { index, section ->
            val item = LinearLayout(context).apply {
                gravity = Gravity.CENTER_VERTICAL
                orientation = LinearLayout.HORIZONTAL
                minimumHeight = dp(56)
                setPadding(dp(12), dp(4), dp(12), dp(4))
                isClickable = true
                isFocusable = true
                contentDescription = context.getString(section.label)
                setOnClickListener {
                    dialog.dismiss()
                    select(section)
                }
            }
            item.addView(ImageView(context).apply {
                setImageResource(section.icon)
                imageTintList = ColorStateList.valueOf(accent)
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }, LinearLayout.LayoutParams(dp(24), dp(24)))
            item.addView(TextView(context).apply {
                setText(section.label)
                textSize = 16f
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER_VERTICAL
            }, LinearLayout.LayoutParams(0, -2, 1f).apply {
                marginStart = dp(16)
            })
            container.addView(item, LinearLayout.LayoutParams(-1, -2))
            if (index < secondary.lastIndex) {
                container.addView(View(context).apply {
                    setBackgroundColor(Color.rgb(53, 59, 68))
                }, LinearLayout.LayoutParams(-1, dp(1)))
            }
        }
        dialog.setCanceledOnTouchOutside(true)
        dialog.show()
        val screenWidth = context.resources.displayMetrics.widthPixels
        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout(minOf(screenWidth - dp(36), dp(440)), -2)
            addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            setDimAmount(0.65f)
        }
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
