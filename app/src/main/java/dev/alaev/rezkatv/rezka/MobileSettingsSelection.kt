package dev.alaev.rezkatv.rezka

internal object MobileSettingsSelection {
    fun index(values: Array<String>, current: String?): Int =
        values.indexOf(current).coerceAtLeast(0)
}
