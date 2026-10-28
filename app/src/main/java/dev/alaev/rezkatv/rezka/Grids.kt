package dev.alaev.rezkatv.rezka

import androidx.leanback.widget.BaseGridView

fun BaseGridView.pinFocusToStart(offsetPx: Int) {
    itemAnimator = null
    windowAlignment = BaseGridView.WINDOW_ALIGN_NO_EDGE
    windowAlignmentOffsetPercent = 0f
    windowAlignmentOffset = offsetPx
    itemAlignmentOffsetPercent = 0f
    itemAlignmentOffset = 0
}

fun BaseGridView.keepFocusInsideEdges() {
    itemAnimator = null
    windowAlignment = BaseGridView.WINDOW_ALIGN_BOTH_EDGE
    windowAlignmentOffsetPercent = CENTER_PERCENT
    itemAlignmentOffsetPercent = CENTER_PERCENT
}

private const val CENTER_PERCENT = 50f
