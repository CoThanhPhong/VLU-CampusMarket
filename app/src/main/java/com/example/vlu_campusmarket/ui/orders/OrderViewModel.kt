package com.example.vlu_campusmarket.ui.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vlu_campusmarket.data.models.OrderModel
import com.example.vlu_campusmarket.data.repository.OrderRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class OrderViewModel(private val orderRepository: OrderRepository) : ViewModel() {

    private val _orders = MutableStateFlow<List<OrderModel>>(emptyList())
    val orders: StateFlow<List<OrderModel>> = _orders.asStateFlow()

    private val userCache = mutableMapOf<String, String>()
    private val _userNames = MutableStateFlow(mapOf<String, String>())
    val userNames: StateFlow<Map<String, String>> = _userNames.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _notificationEvent = MutableSharedFlow<String>(replay = 0)
    val notificationEvent: SharedFlow<String> = _notificationEvent.asSharedFlow()

    private var previousConfirmedOrderIds = setOf<String>()

    fun fetchUserNameIfNeeded(userId: String) {
        if (userId.isBlank() || userCache.containsKey(userId)) return
        userCache[userId] = "Sinh viên VLU"

        viewModelScope.launch {
            try {
                val user = orderRepository.getUserDetail(userId)
                if (user != null) {
                    val authorName = user.profile?.name ?: "Sinh viên VLU"
                    userCache[userId] = authorName
                }
            } catch (e: Exception) {
                userCache[userId] = "Sinh viên VLU"
            }
            _userNames.value = userCache.toMap()
        }
    }

    init {
        refreshOrders()
        startRealtimeUserOrderSync()
    }

    private fun startRealtimeUserOrderSync() {
        viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                delay(5000) // Polling ngầm mỗi 5 giây
                try {
                    val userId = orderRepository.getLoggedInUserId()
                    if (userId != null) {
                        orderRepository.fetchRemoteOrders()
                        val currentOrders = (orderRepository.getOrdersLocal(userId).firstOrNull() ?: emptyList()).sortedByDescending { it._id }
                        _orders.value = currentOrders

                        val currentConfirmedIds = currentOrders
                            .filter { it.orderStatus.equals("Đã xác nhận", ignoreCase = true) || it.orderStatus.equals("approved", ignoreCase = true) }
                            .map { it._id }
                            .toSet()

                        val brandNewConfirmed = currentConfirmedIds - previousConfirmedOrderIds
                        if (brandNewConfirmed.isNotEmpty() && previousConfirmedOrderIds.isNotEmpty()) {
                            _notificationEvent.emit("🎉 Hóa đơn của bạn vừa được cửa hàng xác nhận thành công!")
                        }
                        previousConfirmedOrderIds = currentConfirmedIds
                    }
                } catch (e: Exception) {
                    // Ignored
                }
            }
        }
    }

    fun refreshOrders() {
        viewModelScope.launch {
            _isRefreshing.value = true
            val userId = orderRepository.getLoggedInUserId()
            if (userId != null) {
                orderRepository.fetchRemoteOrders()
                orderRepository.getOrdersLocal(userId).collect { list ->
                    _orders.value = list.sortedByDescending { it._id }
                    previousConfirmedOrderIds = list
                        .filter { it.orderStatus.equals("Đã xác nhận", ignoreCase = true) || it.orderStatus.equals("approved", ignoreCase = true) }
                        .map { it._id }
                        .toSet()
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
