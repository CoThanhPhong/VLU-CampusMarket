package com.example.vlu_campusmarket.data.models

data class ProductModel(
    val _id: String = "",
    val name: String = "",
    val description: String? = "",
    val price: Double = 0.0,
    val amount: Int = 1,
    val local: String = "Cơ sở 1",
    val img: Any = emptyList<String>(),
    val startDate: String? = null,
    val status: String? = "AVAILABLE",
    val deviceType: String = "Khác",
    val endDate: String? = null,
    val author: Any = ""
) {
    val imageUrl: String
        get() = when (img) {
            is List<*> -> img.firstOrNull()?.toString() ?: ""
            is String -> img
            else -> img.toString()
        }

    val authorId: String
        get() = when (author) {
            is String -> author
            is Map<*, *> -> (author["_id"] ?: author["id"])?.toString() ?: ""
            else -> {
                try {
                    val idField = author::class.java.getDeclaredField("id")
                    idField.isAccessible = true
                    idField.get(author)?.toString() ?: ""
                } catch (e: Exception) {
                    try {
                        val idField = author::class.java.getDeclaredField("_id")
                        idField.isAccessible = true
                        idField.get(author)?.toString() ?: ""
                    } catch (e2: Exception) {
                        author.toString()
                    }
                }
            }
        }
}
