package dev.alaev.rezkatv.rezka

internal object VoiceSearchQuery {
    fun first(results: ArrayList<String>?): String? = results?.firstOrNull()?.takeIf { it.isNotBlank() }
}
