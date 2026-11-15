package com.example.vlu_campusmarket.ui.cart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vlu_campusmarket.data.local.dao.ProductDao
import com.example.vlu_campusmarket.data.models.OrderItemModel
import com.example.vlu_campusmarket.data.models.ProductModel
import com.example.vlu_campusmarket.data.models.ShippingInfoModel
import com.example.vlu_campusmarket.data.repository.CartRepository
import com.example.vlu_campusmarket.data.repository.OrderRepository
import com.example.vlu_campusmarket.ui.adapter.CartUiRow
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

    private val rawCartItems = MutableStateFlow<List<CartItemUIModel>>(emptyList())
    private val selectedProductIds = MutableStateFlow<Set<String>>(emptySet())

    private val userNamesMap = MutableStateFlow<Map<String, String>>(emptyMap())
    private val userCache = mutableMapOf<String, String>()

    private fun fetchUserNameIfNeeded(userId: String) {
        if (userId.isBlank() || userCache.containsKey(userId)) return
        userCache[userId] = "Sinh viên VLU"
        viewModelScope.launch {
            try {
                val user = orderRepository.getUserDetail(userId)
                if (user != null) {
                    val name = user.profile?.name ?: user.email ?: "Sinh viên VLU"
                    userCache[userId] = name
                }
            } catch (e: Exception) {
                userCache[userId] = "Sinh viên VLU"
            }
            userNamesMap.value = userCache.toMap()
        }
    }

    // UI rows grouped by author
    val cartUiRows: StateFlow<List<CartUiRow>> = combine(
        rawCartItems,
        selectedProductIds,
        userNamesMap
    ) { items, selectedIds, namesMap ->
        val rows = mutableListOf<CartUiRow>()
        val grouped = items.groupBy { item ->
            val product = item.product
            val directName = product?.authorName
            val authId = product?.authorId ?: ""
            val resolvedName = namesMap[authId]

            when {
                !directName.isNullOrBlank() && directName != authId -> directName
                !resolvedName.isNullOrBlank() -> resolvedName
                authId.isNotBlank() -> {
                    fetchUserNameIfNeeded(authId)
                    "Sinh viên VLU"
                }
                else -> "Người bán Campus Market"
            }
        }

        for ((authorName, authorItems) in grouped) {
            val authorItemIds = authorItems.map { it.productId }
            val isAllAuthorSelected = authorItemIds.isNotEmpty() && authorItemIds.all { it in selectedIds }
            rows.add(CartUiRow.Header(authorName, isAllAuthorSelected))

            for (item in authorItems) {
                rows.add(CartUiRow.Item(item, item.productId in selectedIds))
            }
        }
        rows
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalItemCount: StateFlow<Int> = rawCartItems.map { list ->
        list.sumOf { it.quantity }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val selectedCount: StateFlow<Int> = combine(rawCartItems, selectedProductIds) { items, selectedIds ->
        items.filter { it.productId in selectedIds }.sumOf { it.quantity }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalPrice: StateFlow<Double> = combine(rawCartItems, selectedProductIds) { items, selectedIds ->
        items.filter { it.productId in selectedIds }.sumOf { (it.product?.price ?: 0.0) * it.quantity }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val isAllSelected: StateFlow<Boolean> = combine(rawCartItems, selectedProductIds) { items, selectedIds ->
        items.isNotEmpty() && items.all { it.productId in selectedIds }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        refreshCart()
    }

    fun refreshCart() {
        viewModelScope.launch {
            _isLoading.value = true
            val userId = cartRepository.getLoggedInUserId()
            if (userId != null) {
                cartRepository.fetchRemoteCart()
                combine(
                    cartRepository.getCartLocal(userId),
                    productDao.getAllProducts()
                ) { rawItems, allProducts ->
                    val productMap = allProducts.associateBy { it._id }
                    rawItems.map { item ->
                        val entity = productMap[item.productId]
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
                            productId = item.productId,
                            quantity = item.quantity,
                            product = model
                        )
                    }
                }.collect { uiList ->
                    rawCartItems.value = uiList
                    selectedProductIds.value = uiList.map { it.productId }.toSet()
                    _isLoading.value = false
                }
            } else {
                rawCartItems.value = emptyList()
                selectedProductIds.value = emptySet()
                _isLoading.value = false
            }
        }
    }

    fun toggleItemSelection(productId: String, isSelected: Boolean) {
        val current = selectedProductIds.value.toMutableSet()
        if (isSelected) current.add(productId) else current.remove(productId)
        selectedProductIds.value = current
    }

    fun toggleAuthorSelection(authorName: String, isSelected: Boolean) {
        val current = selectedProductIds.value.toMutableSet()
        val authorItems = rawCartItems.value.filter { item ->
            val product = item.product
            val directName = product?.authorName
            val authId = product?.authorId ?: ""
            val resolvedName = userNamesMap.value[authId]
            val groupName = when {
                !directName.isNullOrBlank() && directName != authId -> directName
                !resolvedName.isNullOrBlank() -> resolvedName
                else -> "Sinh viên VLU"
            }
            groupName == authorName
        }
        for (item in authorItems) {
            if (isSelected) current.add(item.productId) else current.remove(item.productId)
        }
        selectedProductIds.value = current
    }

    fun toggleSelectAll(isSelected: Boolean) {
        if (isSelected) {
            selectedProductIds.value = rawCartItems.value.map { it.productId }.toSet()
        } else {
            selectedProductIds.value = emptySet()
        }
    }

    fun updateQuantity(productId: String, newQty: Int) {
        val currentList = rawCartItems.value.toMutableList()
        if (newQty <= 0) {
            currentList.removeAll { it.productId == productId }
            val currentSelected = selectedProductIds.value.toMutableSet()
            currentSelected.remove(productId)
            selectedProductIds.value = currentSelected
        } else {
            val idx = currentList.indexOfFirst { it.productId == productId }
            if (idx >= 0) {
                currentList[idx] = currentList[idx].copy(quantity = newQty)
            }
        }
        rawCartItems.value = currentList

        viewModelScope.launch {
            cartRepository.updateQuantity(productId, newQty)
        }
    }

    fun removeItem(productId: String, onResult: (Boolean, String) -> Unit) {
        updateQuantity(productId, 0)
        onResult(true, "Đã xóa sản phẩm khỏi giỏ hàng")
    }

    fun deleteSelectedItems(onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val selectedIds = selectedProductIds.value
            if (selectedIds.isEmpty()) {
                onResult(false, "Vui lòng chọn sản phẩm cần xóa")
                return@launch
            }
            _isLoading.value = true
            selectedIds.forEach { id ->
                cartRepository.updateQuantity(id, 0)
            }
            refreshCart()
            _isLoading.value = false
            onResult(true, "Đã xóa ${selectedIds.size} sản phẩm đã chọn")
        }
    }

    fun checkout(shippingInfo: ShippingInfoModel, paymentMethod: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val allItems = rawCartItems.value
            val selectedIds = selectedProductIds.value
            val itemsToCheckout = allItems.filter { it.productId in selectedIds }

            if (itemsToCheckout.isEmpty()) {
                onResult(false, "Vui lòng chọn ít nhất một sản phẩm để thanh toán")
                return@launch
            }

            val orderItems = itemsToCheckout.map { item ->
                OrderItemModel(
                    product = item.productId,
                    quantity = item.quantity,
                    price = item.product?.price ?: 0.0,
                    local = item.product?.local ?: "Cơ sở 1"
                )
            }

            val total = itemsToCheckout.sumOf { (it.product?.price ?: 0.0) * it.quantity }

            _isLoading.value = true
            val success = orderRepository.createOrder(orderItems, shippingInfo, paymentMethod, total)
            _isLoading.value = false

            if (success) {
                itemsToCheckout.forEach { item ->
                    cartRepository.updateQuantity(item.productId, 0)
                }
                refreshCart()
                onResult(true, "Đặt hàng thành công!")
            } else {
                onResult(false, "Đặt hàng thất bại. Vui lòng thử lại!")
            }
        }
    }
}
