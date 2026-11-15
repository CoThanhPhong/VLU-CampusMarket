package com.example.vlu_campusmarket.data.repository

import com.example.vlu_campusmarket.data.local.Converters
import com.example.vlu_campusmarket.data.local.dao.OrderDao
import com.example.vlu_campusmarket.data.local.dao.ProductDao
import com.example.vlu_campusmarket.data.local.dao.UserDao
import com.example.vlu_campusmarket.data.local.entity.OrderEntity
import com.example.vlu_campusmarket.data.models.OrderItemModel
import com.example.vlu_campusmarket.data.models.OrderModel
import com.example.vlu_campusmarket.data.models.ShippingInfoModel
import com.example.vlu_campusmarket.data.models.UserModel
import com.example.vlu_campusmarket.data.remote.ApiService
import com.example.vlu_campusmarket.data.remote.response.CancelOrderRequest
import com.example.vlu_campusmarket.data.remote.response.CreateOrderRequest
import com.example.vlu_campusmarket.data.remote.response.UserIdRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

class OrderRepository(
    private val orderDao: OrderDao,
    private val apiService: ApiService,
    private val userDao: UserDao,
    private val productDao: ProductDao? = null
) {

    suspend fun getLoggedInUserId(): String? {
        return userDao.getLoggedInUser().firstOrNull()?.id
    }

    fun getOrdersLocal(userId: String): Flow<List<OrderModel>> {
        return orderDao.getOrders(userId).map { list ->
            list.map { entity ->
                OrderModel(
                    _id = entity.id,
                    user = entity.userId,
                    orderItems = entity.orderItems.map { it ->
                        val localProd = productDao?.getProductById(it.product)
                        val authorIdToUse = localProd?.author ?: ""

                        OrderItemModel(
                            product = mapOf(
                                "_id" to it.product,
                                "name" to (it.productName ?: localProd?.name ?: ""),
                                "img" to (it.productImg ?: localProd?.img ?: ""),
                                "author" to mapOf(
                                    "_id" to authorIdToUse,
                                    "profile" to mapOf("name" to (it.authorName ?: ""))
                                )
                            ),
                            quantity = it.quantity,
                            price = it.price,
                            local = it.local
                        ) 
                    },
                    shippingInfo = ShippingInfoModel(entity.shippingInfo.fullName, entity.shippingInfo.phone, entity.shippingInfo.local),
                    paymentMethod = entity.paymentMethod,
                    totalPrice = entity.totalPrice,
                    orderStatus = entity.orderStatus,
                    isPaid = entity.isPaid,
                    paidAt = null
                )
            }
        }
    }

    suspend fun fetchRemoteOrders(): Boolean {
        return try {
            val userId = getLoggedInUserId() ?: return false
            val response = apiService.getMyOrders()
            if (response.isSuccessful && response.body()?.success == true) {
                val orders = response.body()?.data ?: emptyList()
                orders.forEach { model ->
                    val entity = OrderEntity(
                        id = model._id,
                        userId = userId,
                        orderItems = model.orderItems.map { item ->
                            val localProd = productDao?.getProductById(item.productId)
                            Converters.EmbeddedOrderItem(
                                product = item.productId,
                                quantity = item.quantity,
                                price = item.price,
                                local = item.local,
                                productName = item.productName ?: localProd?.name,
                                productImg = item.productImg ?: localProd?.img,
                                authorName = item.authorName
                            ) 
                        },
                        shippingInfo = Converters.EmbeddedShippingInfo(model.shippingInfo.fullName, model.shippingInfo.phone,model.shippingInfo.local),
                        paymentMethod = model.paymentMethod,
                        totalPrice = model.totalPrice,
                        orderStatus = model.orderStatus,
                        isPaid = model.isPaid
                    )
                    orderDao.insertOrder(entity)
                }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun createOrder(
        orderItems: List<OrderItemModel>,
        shippingInfo: ShippingInfoModel,
        paymentMethod: String,
        totalPrice: Double
    ): Boolean {
        return try {
            val userId = getLoggedInUserId() ?: return false
            val request = CreateOrderRequest(
                orderItems = orderItems,
                shippingInfo = shippingInfo,
                paymentMethod = paymentMethod,
                totalPrice = totalPrice
            )
            val response = apiService.createOrder(request)
            if (response.isSuccessful && response.body()?.success == true) {
                val order = response.body()?.data
                if (order != null) {
                    val entity = OrderEntity(
                        id = order._id,
                        userId = userId,
                        orderItems = order.orderItems.map { 
                            Converters.EmbeddedOrderItem(
                                product = it.productId,
                                quantity = it.quantity,
                                price = it.price,
                                local = it.local,
                                productName = it.productName,
                                productImg = it.productImg,
                                authorName = it.authorName
                            ) 
                        },
                        shippingInfo = Converters.EmbeddedShippingInfo(order.shippingInfo.fullName, order.shippingInfo.phone, order.shippingInfo.local),
                        paymentMethod = order.paymentMethod,
                        totalPrice = order.totalPrice,
                        orderStatus = order.orderStatus,
                        isPaid = order.isPaid
                    )
                    orderDao.insertOrder(entity)
                }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun cancelOrder(orderId: String): Boolean {
        return try {
            val response = apiService.cancelOrder(CancelOrderRequest(orderId))
            if (response.isSuccessful && response.body()?.success == true) {
                fetchRemoteOrders()
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun getUserDetail(userId: String): UserModel? {
        return try {
            val requestBody = UserIdRequest(userId)
            val response = apiService.getUserById(requestBody)
            if (response.isSuccessful) {
                val userResponse = response.body()?.data
                if (userResponse != null) {
                    UserModel(
                        id = userResponse.id,
                        mssv = userResponse.mssv,
                        email = userResponse.email,
                        role = userResponse.role,
                        profile = userResponse.profile
                    )
                } else null
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}
