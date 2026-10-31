package dev.alaev.rezkatv.rezka

internal object FavoritesRequest {
    fun addPost(postId: String, categoryId: String): List<Pair<String, String>> {
        require(postId.isNotEmpty() && postId.all(Char::isDigit))
        require(categoryId.isNotEmpty() && categoryId.all(Char::isDigit))
        return listOf("post_id" to postId, "cat_id" to categoryId, "action" to "add_post")
    }
}
