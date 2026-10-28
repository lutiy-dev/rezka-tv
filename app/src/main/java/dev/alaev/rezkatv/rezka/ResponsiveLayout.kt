package dev.alaev.rezkatv.rezka

object ResponsiveLayout {
    private const val COMPACT_MAX_WIDTH_DP = 599
    private const val PHONE_GRID_COLUMNS = 2
    private const val TV_GRID_COLUMNS = 6
    private const val PHONE_SEARCH_COLUMNS = 2
    private const val TV_SEARCH_COLUMNS = 4

    fun isCompact(widthDp: Int) = widthDp <= COMPACT_MAX_WIDTH_DP
    fun isPhone(smallestWidthDp: Int) = smallestWidthDp <= COMPACT_MAX_WIDTH_DP
    fun gridColumns(widthDp: Int) = if (isCompact(widthDp)) PHONE_GRID_COLUMNS else TV_GRID_COLUMNS
    fun searchColumns(widthDp: Int) = if (isCompact(widthDp)) PHONE_SEARCH_COLUMNS else TV_SEARCH_COLUMNS
    fun useEdgeAwareRows(widthDp: Int) = isCompact(widthDp)
}
