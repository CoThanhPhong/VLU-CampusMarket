package com.example.vlu_campusmarket.data.models

data class OrderItemModel(
    val product: Any = "",
    val quantity: Int = 1,
    val price: Double = 0.0,
    val local: String = ""
) {
    val productId: String
        get() = when (product) {
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
                        product.toString()
                    }
                }
            }
        }

    val productName: String?
        get() = when (product) {
            is Map<*, *> -> product["name"]?.toString()
            else -> null
        }

    val productImg: String?
        get() = when (product) {
            is Map<*, *> -> {
                val img = product["img"]
                when (img) {
                    is List<*> -> img.firstOrNull()?.toString()
                    is String -> img
                    else -> img?.toString()
                }
            }
            else -> null
        }

    val authorName: String?
        get() = when (product) {
            is Map<*, *> -> {
                val author = product["author"]
                when (author) {
                    is Map<*, *> -> {
                        val profile = author["profile"] as? Map<*, *>
                        profile?.get("name")?.toString() ?: author["name"]?.toString() ?: author["email"]?.toString()
                    }
                    is String -> author
                    else -> null
                }
            }
            else -> null
        }
}

data class ShippingInfoModel(
    val fullName: String = "",
    val phone: String = "",
    val local: String = ""
)

data class OrderModel(
    val _id: String = "",
    val user: Any = "",
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
            is String -> user
            is Map<*, *> -> (user["_id"] ?: user["id"] ?: user["userId"])?.toString() ?: ""
            else -> user.toString()
        }

    val customerName: String?
        get() = when (user) {
            is Map<*, *> -> {
                val profile = user["profile"] as? Map<*, *>
                profile?.get("name")?.toString() ?: user["name"]?.toString() ?: user["email"]?.toString()
            }
            else -> null
        }
}
