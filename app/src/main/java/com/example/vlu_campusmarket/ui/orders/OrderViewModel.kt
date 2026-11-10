package com.example.vlu_campusmarket.ui.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vlu_campusmarket.data.models.OrderModel
import com.example.vlu_campusmarket.data.repository.OrderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class OrderViewModel(private val orderRepository: OrderRepository) : ViewModel() {

    private val _orders = MutableStateFlow<List<OrderModel>>(emptyList())
    val orders: StateFlow<List<OrderModel>> = _orders.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        refreshOrders()
    }

    fun refreshOrders() {
        viewModelScope.launch {
            _isRefreshing.value = true
            val userId = orderRepository.getLoggedInUserId()
            if (userId != null) {
                orderRepository.fetchRemoteOrders()
                orderRepository.getOrdersLocal(userId).collect { list ->
                    _orders.value = list
                    _isRefreshing.value = false
                }
            } else {
                _orders.value = emptyList()
                _isRefreshing.value = false
            }
        }
    }

    fun cancelOrder(orderId: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _isRefreshing.value = true
            val success = orderRepository.cancelOrder(orderId)
            _isRefreshing.value = false
            if (success) {
                onResult(true, "Đã hủy đơn hàng thành công")
            } else {
                onResult(false, "Không thể hủy đơn hàng này")
            }
        }
    }
}
