package dev.alaev.rezkatv.rezka

internal object MobileSettingsSelection {
    fun index(values: Array<String>, current: String?): Int {
        return values.indexOf(current).coerceAtLeast(0)
    }
}
