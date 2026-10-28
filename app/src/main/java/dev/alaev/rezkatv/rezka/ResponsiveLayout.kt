package dev.alaev.rezkatv.rezka

object ResponsiveLayout {
    fun isCompact(widthDp: Int) = widthDp < 600
    fun gridColumns(widthDp: Int) = if (isCompact(widthDp)) 2 else 6
    fun searchColumns(widthDp: Int) = if (isCompact(widthDp)) 2 else 4
}
