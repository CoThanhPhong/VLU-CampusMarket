package com.example.vlu_campusmarket.data.models

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

data class ProductModel(
    @SerializedName(value = "_id", alternate = ["id", "productId"])
    val _id: Any? = null,
    val name: String = "",
    val description: String? = "",
    val price: Double = 0.0,
    val amount: Int = 1,
    val local: String = "Cơ sở 1",
    val img: Any? = emptyList<String>(),
    val startDate: String? = null,
    val status: String? = "AVAILABLE",
    val deviceType: String = "Khác",
    val endDate: String? = null,
    val author: Any? = ""
) {
    val safeId: String
        get() = when (val id = _id) {
            null -> ""
            is String -> id
            is Map<*, *> -> (id["\$oid"] ?: id["_id"] ?: id["id"])?.toString() ?: ""
            else -> {
                val str = id.toString()
                if (str != "null") str else ""
            }
        }

    val formattedDeviceType: String
        get() {
            val type = deviceType.trim().lowercase()
            return when (type) {
                "mobile", "phone", "điện thoại" -> "Mobile"
                "tablet" -> "Tablet"
                "mac", "macbook", "mac book" -> "Macbook"
                "laptop" -> "Laptop"
                else -> if (type.isBlank()) "Khác" else deviceType.trim().replaceFirstChar { it.uppercase() }
            }
        }

    val imageUrl: String
        get() = imageList.firstOrNull() ?: ""

    val imageList: List<String>
        get() = when (val i = img) {
            null -> emptyList()
            is List<*> -> {
                i.mapNotNull { item ->
                    item?.toString()?.trim()?.removeSurrounding("\"", "\"")?.removeSurrounding("'", "'")
                }.filter { it.isNotEmpty() && it != "null" }
            }
            is String -> {
                val trimmed = i.trim()
                if (trimmed.startsWith("[")) {
                    try {
                        val parsed = Gson().fromJson(trimmed, Array<String>::class.java).toList()
                        parsed.mapNotNull { item ->
                            item?.toString()?.trim()?.removeSurrounding("\"", "\"")?.removeSurrounding("'", "'")
                        }.filter { it.isNotEmpty() && it != "null" }
                    } catch (_: Exception) {
                        val clean = trimmed.removeSurrounding("[", "]").removeSurrounding("\"", "\"").trim()
                        if (clean.isNotEmpty() && clean != "null") listOf(clean) else emptyList()
                    }
                } else if (trimmed.isNotEmpty()) {
                    val clean = trimmed.removeSurrounding("\"", "\"").trim()
                    if (clean.isNotEmpty() && clean != "null") listOf(clean) else emptyList()
                } else {
                    emptyList()
                }
            }
            else -> {
                val str = i.toString().trim().removeSurrounding("\"", "\"")
                if (str.isNotEmpty() && str != "null") listOf(str) else emptyList()
            }
        }

    val authorId: String
        get() = when (val a = author) {
            null -> ""
            is String -> a
            is Map<*, *> -> (a["_id"] ?: a["id"] ?: a["userId"])?.toString() ?: ""
            is UserModel -> a.id
            else -> {
                try {
                    val idField = a.javaClass.getDeclaredField("id")
                    idField.isAccessible = true
                    idField.get(a)?.toString() ?: ""
                } catch (_: Exception) {
                    try {
                        val idField = a.javaClass.getDeclaredField("_id")
                        idField.isAccessible = true
                        idField.get(a)?.toString() ?: ""
                    } catch (_: Exception) {
                        val str = a.toString()
                        if (str != "null") str else ""
                    }
                }
            }
        }

    val authorName: String?
        get() = when (val a = author) {
            null -> null
            is Map<*, *> -> {
                val profile = a["profile"] as? Map<*, *>
                profile?.get("name")?.toString() ?: a["name"]?.toString() ?: a["email"]?.toString()
            }
            is UserModel -> a.profile?.name ?: a.email
            else -> null
        }

    val authorAvatar: String?
        get() = when (val a = author) {
            null -> null
            is Map<*, *> -> {
                val profile = a["profile"] as? Map<*, *>
                profile?.get("avatar")?.toString() ?: a["avatar"]?.toString()
            }
            is UserModel -> a.profile?.avatar
            else -> null
        }
}
