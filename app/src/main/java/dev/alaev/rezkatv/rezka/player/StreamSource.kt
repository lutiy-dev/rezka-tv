package dev.alaev.rezkatv.rezka.player

object StreamSource {
    fun isHls(url: String): Boolean =
        url.substringBefore('#').substringBefore('?').endsWith(".m3u8", ignoreCase = true)
}
