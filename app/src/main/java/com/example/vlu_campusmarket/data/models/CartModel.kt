package com.example.vlu_campusmarket.data.models

data class CartItemModel(
    val product: Any? = "",
    val quantity: Int = 1
) {
    val productId: String
        get() = when (product) {
            null -> ""
            is String -> product
            is Map<*, *> -> (product["_id"] ?: product["id"])?.toString() ?: ""
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
}

data class CartModel(
    val _id: String = "",
    val user: Any? = "",
    val items: List<CartItemModel> = emptyList()
) {
    val userId: String
        get() = when (user) {
            null -> ""
            is String -> user
            is Map<*, *> -> (user["_id"] ?: user["id"])?.toString() ?: ""
            else -> {
                val str = user.toString()
                if (str != "null") str else ""
            }
        }
}
