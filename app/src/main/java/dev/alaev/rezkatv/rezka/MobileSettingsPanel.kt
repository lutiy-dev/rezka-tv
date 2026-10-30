package dev.alaev.rezkatv.rezka

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import dev.alaev.rezkatv.R

internal open class MobileSettingsPanel(protected val activity: Activity) {
    protected val background = Color.rgb(16, 19, 24)
    protected val surface = Color.rgb(32, 37, 45)
    protected val accent = activity.getColor(R.color.accent)
    protected val muted = Color.rgb(162, 168, 179)
    protected val text = Color.WHITE

    protected fun section(root: LinearLayout, title: String) {
        root.addView(label(title.uppercase(), 12f, accent, true).apply {
            setPadding(dp(2), dp(22), 0, dp(10))
        })
    }

    protected fun panel(root: LinearLayout): LinearLayout {
        val panel = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(surface)
                cornerRadius = dp(16).toFloat()
            }
            setPadding(dp(14), dp(2), dp(14), dp(2))
        }
        root.addView(panel, LinearLayout.LayoutParams(-1, -2))
        return panel
    }

    protected fun row(
        parent: LinearLayout, title: String, trailing: View? = null, detail: String? = null,
    ): LinearLayout {
        val outer = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            minimumHeight = dp(if (detail == null) 56 else 74)
            isClickable = true
            isFocusable = true
            setPadding(dp(4), dp(8), dp(4), dp(8))
        }
        val words = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
        words.addView(label(title, 16f, text, true))
        if (detail != null) words.addView(label(detail, 12f, muted).apply {
            setPadding(0, dp(3), 0, 0)
        })
        outer.addView(words, LinearLayout.LayoutParams(0, -2, 1f))
        if (trailing != null) outer.addView(trailing)
        parent.addView(outer, LinearLayout.LayoutParams(-1, -2))
        return outer
    }

    protected fun divider(parent: LinearLayout) {
        parent.addView(View(activity).apply { setBackgroundColor(Color.rgb(53, 59, 68)) },
            LinearLayout.LayoutParams(-1, dp(1)))
    }

    protected fun label(value: String, size: Float, color: Int, bold: Boolean = false) =
        TextView(activity).apply {
            text = value
            textSize = size
            setTextColor(color)
            if (bold) setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER_VERTICAL
        }

    protected fun dp(value: Int) = (value * activity.resources.displayMetrics.density + 0.5f).toInt()
}
