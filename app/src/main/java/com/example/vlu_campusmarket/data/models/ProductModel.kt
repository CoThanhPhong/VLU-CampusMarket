package com.example.vlu_campusmarket.data.models

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

data class ProductModel(
    @SerializedName(value = "_id", alternate = ["id", "productId"])
    val _id: String? = null,
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
    val safeId: String
        get() = _id ?: ""

    val imageUrl: String
        get() = imageList.firstOrNull() ?: ""

    val imageList: List<String>
        get() = when (img) {
            is List<*> -> img.filterIsInstance<String>().filter { it.isNotEmpty() }
            is String -> {
                val trimmed = img.trim()
                if (trimmed.startsWith("[")) {
                    try {
                        val parsed = Gson().fromJson(trimmed, Array<String>::class.java).toList()
                        parsed.filter { it.isNotEmpty() }
                    } catch (e: Exception) {
                        if (trimmed.isNotEmpty()) listOf(trimmed) else emptyList()
                    }
                } else if (trimmed.isNotEmpty()) {
                    listOf(trimmed)
                } else {
                    emptyList()
                }
            }
            else -> if (img.toString().isNotEmpty()) listOf(img.toString()) else emptyList()
        }

    val authorId: String
        get() = when (author) {
            is String -> author
            is Map<*, *> -> (author["_id"] ?: author["id"] ?: author["userId"])?.toString() ?: ""
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

    val authorName: String?
        get() = when (author) {
            is Map<*, *> -> {
                val profile = author["profile"] as? Map<*, *>
                profile?.get("name")?.toString() ?: author["name"]?.toString() ?: author["email"]?.toString()
            }
            is UserModel -> author.profile?.name ?: author.email
            else -> null
        }

    val authorAvatar: String?
        get() = when (author) {
            is Map<*, *> -> {
                val profile = author["profile"] as? Map<*, *>
                profile?.get("avatar")?.toString() ?: author["avatar"]?.toString()
            }
            is UserModel -> author.profile?.avatar
            else -> null
        }
}
