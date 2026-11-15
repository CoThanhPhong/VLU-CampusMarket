package com.example.vlu_campusmarket.data.repository

import com.example.vlu_campusmarket.data.local.Converters
import com.example.vlu_campusmarket.data.local.dao.CartDao
import com.example.vlu_campusmarket.data.local.dao.UserDao
import com.example.vlu_campusmarket.data.local.entity.CartEntity
import com.example.vlu_campusmarket.data.models.CartItemModel
import com.example.vlu_campusmarket.data.remote.ApiService
import com.example.vlu_campusmarket.data.remote.response.CartItemPayload
import com.example.vlu_campusmarket.data.remote.response.UpdateCartRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

class CartRepository(
    private val cartDao: CartDao,
    private val apiService: ApiService,
    private val userDao: UserDao
) {

    suspend fun getLoggedInUserId(): String? {
        return userDao.getLoggedInUser().firstOrNull()?.id
    }

    fun getCartLocal(userId: String): Flow<List<CartItemModel>> {
        return cartDao.getCart(userId).map { entity ->
            entity?.items?.map {
                CartItemModel(product = it.product, quantity = it.quantity)
            } ?: emptyList()
        }
    }

    suspend fun fetchRemoteCart(): Boolean {
        return try {
            val userId = getLoggedInUserId() ?: return false
            val response = apiService.getCart()
            if (response.isSuccessful && response.body()?.success == true) {
                val items = response.body()?.data ?: emptyList()
                val embeddedItems = items.map {
                    Converters.EmbeddedCartItem(product = it.productId, quantity = it.quantity)
                }
                val entity = CartEntity(
                    id = "cart_$userId",
                    userId = userId,
                    items = embeddedItems
                )
                cartDao.insertCart(entity)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun addToCart(productId: String, quantity: Int): Boolean {
        val userId = getLoggedInUserId() ?: return false
        val currentCartEntity = cartDao.getCart(userId).firstOrNull()
        val currentItems = currentCartEntity?.items?.toMutableList() ?: mutableListOf()

        val existingIndex = currentItems.indexOfFirst { it.product == productId }
        if (existingIndex >= 0) {
            val existing = currentItems[existingIndex]
            currentItems[existingIndex] = Converters.EmbeddedCartItem(
                product = productId,
                quantity = existing.quantity + quantity
            )
        } else {
            currentItems.add(Converters.EmbeddedCartItem(product = productId, quantity = quantity))
        }

        return syncCartWithRemoteAndLocal(userId, currentItems)
    }

    suspend fun updateQuantity(productId: String, newQuantity: Int): Boolean {
        val userId = getLoggedInUserId() ?: return false
        val currentCartEntity = cartDao.getCart(userId).firstOrNull() ?: return false
        val currentItems = currentCartEntity.items.toMutableList()

        if (newQuantity <= 0) {
            currentItems.removeAll { it.product == productId }
        } else {
            val index = currentItems.indexOfFirst { it.product == productId }
            if (index >= 0) {
                currentItems[index] = Converters.EmbeddedCartItem(product = productId, quantity = newQuantity)
            }
        }

        return syncCartWithRemoteAndLocal(userId, currentItems)
    }

    private suspend fun syncCartWithRemoteAndLocal(
        userId: String,
        items: List<Converters.EmbeddedCartItem>
    ): Boolean {
        val payload = items.map { CartItemPayload(product = it.product, quantity = it.quantity) }
        return try {
            val response = apiService.updateCart(UpdateCartRequest(payload))
            if (response.isSuccessful) {
                val entity = CartEntity(
                    id = "cart_$userId",
                    userId = userId,
                    items = items
                )
                cartDao.insertCart(entity)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            // Lưu tạm vào Room nếu ngoại lệ mạng
            val entity = CartEntity(
                id = "cart_$userId",
                userId = userId,
                items = items
            )
            cartDao.insertCart(entity)
            true
        }
    }
}
