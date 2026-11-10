package com.example.vlu_campusmarket.data.remote.response

import com.example.vlu_campusmarket.data.models.OrderModel
import com.example.vlu_campusmarket.data.models.OrderItemModel
import com.example.vlu_campusmarket.data.models.ShippingInfoModel

data class ValidateCheckoutRequest(
    val orderItems: List<OrderItemModel>
)

data class CreateOrderRequest(
    val orderItems: List<OrderItemModel>,
    val shippingInfo: ShippingInfoModel,
    val paymentMethod: String,
    val totalPrice: Double
)

data class OrderCreateResponse(
    val success: Boolean,
    val data: OrderModel
)

data class OrderListResponse(
    val success: Boolean,
    val data: List<OrderModel>
)

data class CancelOrderRequest(
    val orderId: String
)

data class CancelOrderResponse(
    val success: Boolean,
    val message: String,
    val data: OrderModel
)

data class UpdateOrderStatusRequest(
    val orderId: String,
    val orderStatus: String
)

data class BaseApiResponse(
    val success: Boolean,
    val message: String
)
