package com.example.vlu_campusmarket.data.models

data class OrderItemModel(
    val product: Any? = "",
    val quantity: Int = 1,
    val price: Double = 0.0,
    val local: String = "",
    val status: String? = null
) {
    val currentStatus: String
        get() {
            if (!status.isNullOrEmpty()) return status
            return when (product) {
                null -> ""
                is Map<*, *> -> product["itemStatus"]?.toString() ?: product["status"]?.toString() ?: ""
                else -> ""
            }
        }

    val productId: String
        get() = when (product) {
            null -> ""
            is String -> product
            is Map<*, *> -> (product["_id"] ?: product["id"] ?: product["userId"])?.toString() ?: ""
            else -> {
                try {
                    val idField = product::class.java.getDeclaredField("id")
                    idField.isAccessible = true
                    idField.get(product)?.toString() ?: ""
                } catch (e: Exception) {
                    try {
                        val idField = product::class.java.getDeclaredField("_id")
                        idField.isAccessible = true
                        idField.get(product)?.toString() ?: ""
                    } catch (e2: Exception) {
                        val str = product.toString()
                        if (str != "null") str else ""
                    }
                }
            }
        }

    val productName: String?
        get() = when (product) {
            null -> null
            is Map<*, *> -> product["name"]?.toString()
            else -> null
        }

    val productImg: String?
        get() = when (product) {
            null -> null
            is Map<*, *> -> {
                val img = product["img"]
                when (img) {
                    null -> null
                    is List<*> -> img.firstOrNull()?.toString()
                    is String -> img
                    else -> img.toString()
                }
            }
            else -> null
        }

    val authorId: String
        get() = when (product) {
            null -> ""
            is String -> product
            is Map<*, *> -> {
                val author = product["author"]
                when (author) {
                    null -> {
                        // Thử lấy trực tiếp từ product nếu product là map thông tin author
                        (product["_id"] ?: product["id"] ?: product["userId"] ?: product["mssv"] ?: product["email"])?.toString() ?: ""
                    }
                    is String -> author
                    is Map<*, *> -> (author["_id"] ?: author["id"] ?: author["userId"] ?: author["mssv"] ?: author["email"])?.toString() ?: ""
                    else -> author.toString()
                }
            }
            else -> {
                val str = product.toString()
                if (str != "null") str else ""
            }
        }

    val authorName: String?
        get() = when (product) {
            null -> null
            is Map<*, *> -> {
                val author = product["author"]
                when (author) {
                    null -> null
                    is Map<*, *> -> {
                        val profile = author["profile"] as? Map<*, *>
                        profile?.get("name")?.toString() ?: author["name"]?.toString() ?: author["email"]?.toString()
                    }
                    else -> null
                }
            }
            else -> null
        }
}

data class ShippingInfoModel(
    val fullName: String = "",
    val phone: String = "",
    val local: String = "",
    val email: String = ""
)

data class OrderModel(
    val _id: String = "",
    val user: Any? = "",
    val orderItems: List<OrderItemModel> = emptyList(),
    val shippingInfo: ShippingInfoModel = ShippingInfoModel(),
    val paymentMethod: String = "COD",
    val totalPrice: Double = 0.0,
    val orderStatus: String = "Chờ xác nhận",
    val isPaid: Boolean = false,
    val paidAt: String? = null
) {
    val userId: String
        get() = when (user) {
            null -> ""
            is String -> user
            is Map<*, *> -> (user["_id"] ?: user["id"] ?: user["userId"])?.toString() ?: ""
            else -> {
                val str = user.toString()
                if (str != "null") str else ""
            }
        }

    val customerName: String?
        get() = when (user) {
            null -> null
            is Map<*, *> -> {
                val profile = user["profile"] as? Map<*, *>
                profile?.get("name")?.toString() ?: user["name"]?.toString() ?: user["email"]?.toString()
            }
            else -> null
        }
}
