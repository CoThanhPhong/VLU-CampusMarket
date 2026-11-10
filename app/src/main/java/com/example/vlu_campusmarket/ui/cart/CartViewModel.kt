package com.example.vlu_campusmarket.ui.cart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vlu_campusmarket.data.local.dao.ProductDao
import com.example.vlu_campusmarket.data.models.OrderItemModel
import com.example.vlu_campusmarket.data.models.ProductModel
import com.example.vlu_campusmarket.data.models.ShippingInfoModel
import com.example.vlu_campusmarket.data.repository.CartRepository
import com.example.vlu_campusmarket.data.repository.OrderRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class CartItemUIModel(
    val productId: String,
    val quantity: Int,
    val product: ProductModel?
)

class CartViewModel(
    private val cartRepository: CartRepository,
    private val orderRepository: OrderRepository,
    private val productDao: ProductDao
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _cartItems = MutableStateFlow<List<CartItemUIModel>>(emptyList())
    val cartItems: StateFlow<List<CartItemUIModel>> = _cartItems.asStateFlow()

    val totalPrice: StateFlow<Double> = _cartItems.map { list ->
        list.sumOf { (it.product?.price ?: 0.0) * it.quantity }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    init {
        refreshCart()
    }

    fun refreshCart() {
        viewModelScope.launch {
            _isLoading.value = true
            val userId = cartRepository.getLoggedInUserId()
            if (userId != null) {
                cartRepository.fetchRemoteCart()
                cartRepository.getCartLocal(userId).collect { rawItems ->
                    productDao.getAllProducts().collect { allProducts ->
                        val productMap = allProducts.associateBy { it._id }
                        val uiList = rawItems.map { item ->
                            val entity = productMap[item.product]
                            val model = entity?.let { e ->
                                ProductModel(
                                    _id = e._id,
                                    name = e.name,
                                    description = e.description,
                                    price = e.price,
                                    amount = e.amount,
                                    local = e.local,
                                    img = e.img,
                                    startDate = null,
                                    status = e.status,
                                    deviceType = e.deviceType,
                                    endDate = null,
                                    author = e.author
                                )
                            }
                            CartItemUIModel(
                                productId = item.product,
                                quantity = item.quantity,
                                product = model
                            )
                        }
                        _cartItems.value = uiList
                        _isLoading.value = false
                    }
                }
            } else {
                _cartItems.value = emptyList()
                _isLoading.value = false
            }
        }
    }

    fun updateQuantity(productId: String, newQty: Int) {
        viewModelScope.launch {
            cartRepository.updateQuantity(productId, newQty)
        }
    }

    fun checkout(shippingInfo: ShippingInfoModel, paymentMethod: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val items = _cartItems.value
            if (items.isEmpty()) {
                onResult(false, "Giỏ hàng đang trống")
                return@launch
            }

            val orderItems = items.map { item ->
                OrderItemModel(
                    product = item.productId,
                    quantity = item.quantity,
                    price = item.product?.price ?: 0.0,
                    local = item.product?.local ?: "Cơ sở 1"
                )
            }

            val total = items.sumOf { (it.product?.price ?: 0.0) * it.quantity }

            _isLoading.value = true
            val success = orderRepository.createOrder(orderItems, shippingInfo, paymentMethod, total)
            _isLoading.value = false

            if (success) {
                // Xóa các sản phẩm trong giỏ sau khi đặt hàng thành công
                items.forEach { cartRepository.updateQuantity(it.productId, 0) }
                onResult(true, "Đặt hàng thành công!")
            } else {
                onResult(false, "Đặt hàng thất bại. Vui lòng thử lại!")
            }
        }
    }
}
