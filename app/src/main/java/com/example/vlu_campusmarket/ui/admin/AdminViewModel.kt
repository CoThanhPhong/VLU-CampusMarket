package com.example.vlu_campusmarket.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vlu_campusmarket.data.local.dao.ProductDao
import com.example.vlu_campusmarket.data.local.dao.UserDao
import com.example.vlu_campusmarket.data.local.entity.ProductEntity
import com.example.vlu_campusmarket.data.models.OrderModel
import com.example.vlu_campusmarket.data.models.ProductModel
import com.example.vlu_campusmarket.data.models.UserModel
import com.example.vlu_campusmarket.data.remote.ApiService
import com.example.vlu_campusmarket.data.remote.response.OrderCreateResponse
import com.example.vlu_campusmarket.data.remote.response.ProductIdRequest
import com.example.vlu_campusmarket.data.remote.response.UpdateOrderStatusRequest
import com.example.vlu_campusmarket.data.remote.response.UserIdRequest
import com.example.vlu_campusmarket.utils.SessionManager
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import retrofit2.Response

class AdminViewModel(
    private val apiService: ApiService,
    private val productDao: ProductDao,
    private val userDao: UserDao? = null,
    private val sessionManager: SessionManager? = null
) : ViewModel() {

    private val _allProducts = MutableStateFlow<List<ProductModel>>(emptyList())
    val allProducts: StateFlow<List<ProductModel>> = _allProducts.asStateFlow()

    private val _totalProductsCount = MutableStateFlow(0)
    val totalProductsCount: StateFlow<Int> = _totalProductsCount.asStateFlow()

    val sortedAdminProducts: StateFlow<List<ProductModel>> = allProducts.map { products ->
        products.sortedWith(
            compareBy<ProductModel> { product ->
                val st = product.status?.trim() ?: "Chờ duyệt"
                val isSoldOut = st.equals("Hết hàng", ignoreCase = true) || product.amount == 0
                when {
                    st.equals("Chờ duyệt", ignoreCase = true) || st.isEmpty() -> 1
                    st.equals("Bị từ chối", ignoreCase = true) || st.equals("REJECTED", ignoreCase = true) -> 2
                    !isSoldOut && (st.equals("Còn hàng", ignoreCase = true) || st.equals("Đã duyệt", ignoreCase = true) || st.equals("AVAILABLE", ignoreCase = true)) -> 3
                    else -> 4
                }
            }.thenByDescending { it.safeId }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _productSearchQuery = MutableStateFlow("")
    fun setProductSearchQuery(query: String) {
        _productSearchQuery.value = query.trim()
    }

    val filteredAdminProducts: StateFlow<List<ProductModel>> = combine(
        sortedAdminProducts,
        _productSearchQuery
    ) { products: List<ProductModel>, query: String ->
        if (query.isBlank()) products
        else products.filter { product ->
            product.name.contains(query, ignoreCase = true) || product.deviceType.contains(query, ignoreCase = true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _allOrders = MutableStateFlow<List<OrderModel>>(emptyList())
    val allOrders: StateFlow<List<OrderModel>> = _allOrders.asStateFlow()

    private val _allUsers = MutableStateFlow<List<UserModel>>(emptyList())
    val allUsers: StateFlow<List<UserModel>> = _allUsers.asStateFlow()

    private val _userSearchQuery = MutableStateFlow("")
    fun setUserSearchQuery(query: String) {
        _userSearchQuery.value = query.trim()
    }

    val filteredAdminUsers: StateFlow<List<UserModel>> = combine(
        allUsers,
        _userSearchQuery
    ) { users: List<UserModel>, query: String ->
        if (query.isBlank()) users
        else users.filter { user ->
            val name = user.profile?.name ?: ""
            val mssv = user.mssv ?: ""
            val email = user.email ?: ""
            name.contains(query, ignoreCase = true) || mssv.contains(query, ignoreCase = true) || email.contains(query, ignoreCase = true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _notificationEvent = MutableSharedFlow<String>(replay = 0)
    val notificationEvent: SharedFlow<String> = _notificationEvent.asSharedFlow()

    private var previousPendingOrderIds = setOf<String>()

    init {
        observeLocalDatabase()
        refreshAllData()
        startRealtimeOrderSync()
    }

    private suspend fun filterOrdersForCurrentRole(orders: List<OrderModel>): List<OrderModel> {
        val sessionUser = sessionManager?.fetchUser()
        val entityUser = userDao?.getLoggedInUser()?.firstOrNull()

        val currentUserId = (sessionUser?.id ?: entityUser?.id ?: "").trim()
        val currentMssv = (sessionUser?.mssv ?: entityUser?.mssv ?: "").trim()
        val currentEmail = (sessionUser?.email ?: entityUser?.email ?: "").trim()

        if (currentUserId.isEmpty() && currentMssv.isEmpty() && currentEmail.isEmpty()) {
            return orders
        }

        val filteredList = mutableListOf<OrderModel>()
        for (order in orders) {
            val matchingItems = order.orderItems.filter { item ->
                val productId = item.productId
                val localProd = productDao.getProductById(productId)
                val authorId = (localProd?.author ?: item.authorId).trim()

                authorId.isNotEmpty() && (
                    authorId.equals(currentUserId, ignoreCase = true) ||
                    authorId.equals(currentMssv, ignoreCase = true) ||
                    authorId.equals(currentEmail, ignoreCase = true)
                )
            }
            if (matchingItems.isNotEmpty()) {
                val sellerOrder = order.copy(
                    orderItems = matchingItems,
                    totalPrice = matchingItems.sumOf { it.price * it.quantity }
                )
                filteredList.add(sellerOrder)
            }
        }
        return filteredList
    }

    private fun observeLocalDatabase() {
        viewModelScope.launch {
            productDao.getAllProducts().collect { entities ->
                val models = entities.map { entity ->
                    val parsedImg: Any = if (entity.img.startsWith("[")) {
                        try {
                            Gson().fromJson(entity.img, Array<String>::class.java).toList()
                        } catch (_: Exception) {
                            entity.img
                        }
                    } else {
                        entity.img
                    }
                    ProductModel(
                        _id = entity._id,
                        name = entity.name,
                        price = entity.price,
                        deviceType = entity.deviceType,
                        author = entity.author,
                        img = parsedImg,
                        description = entity.description,
                        amount = entity.amount,
                        local = entity.local,
                        status = entity.status,
                        startDate = null,
                        endDate = null
                    )
                }
                _allProducts.value = models
            }
        }
    }

    private fun startRealtimeOrderSync() {
        viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                delay(5000) // Tự động kiểm tra API mỗi 5 giây
                try {
                    val orderResponse = apiService.getAllOrders()
                    if (orderResponse.isSuccessful && orderResponse.body()?.success == true) {
                        val rawOrders = orderResponse.body()?.data ?: emptyList()
                        val filteredOrders = filterOrdersForCurrentRole(rawOrders).sortedByDescending { it._id }
                        _allOrders.value = filteredOrders

                        val currentPendingIds = filteredOrders
                            .filter { it.orderStatus.equals("Chờ xác nhận", ignoreCase = true) || it.orderStatus.equals("pending", ignoreCase = true) }
                            .map { it._id }
                            .toSet()

                        val brandNewPending = currentPendingIds - previousPendingOrderIds
                        if (brandNewPending.isNotEmpty() && previousPendingOrderIds.isNotEmpty()) {
                            _notificationEvent.emit("🔔 Thông báo: Có ${brandNewPending.size} đơn hàng mới vừa được đặt/thanh toán!")
                        }
                        previousPendingOrderIds = currentPendingIds
                    }
                } catch (_: Exception) {
                    // Bỏ qua lỗi kết nối tạm thời khi sync ngầm
                }
            }
        }
    }

    fun refreshAllData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                var totalCountFromApi = 0
                val allProds = mutableListOf<ProductModel>()
                var page = 1
                while (true) {
                    val prodResponse = apiService.getAllProducts(page)
                    if (prodResponse.isSuccessful && prodResponse.body() != null) {
                        val body = prodResponse.body()!!
                        val list = body.data
                        if (list.isEmpty()) break
                        allProds.addAll(list)
                        if (body.totalProducts > 0) {
                            totalCountFromApi = body.totalProducts
                        } else if (body.count > 0) {
                            totalCountFromApi = body.count
                        }
                        if (page >= body.totalPages) break
                    } else {
                        break
                    }
                    page++
                    if (page > 50) break
                }

                _totalProductsCount.value = if (totalCountFromApi > 0) totalCountFromApi else allProds.size

                val entities = allProds.mapIndexed { index, model ->
                    val finalId = if (model.safeId.isNotEmpty()) model.safeId else "prod_${index}_${System.identityHashCode(model)}"
                    ProductEntity(
                        _id = finalId,
                        name = model.name,
                        description = model.description,
                        price = model.price,
                        amount = model.amount,
                        local = model.local,
                        img = if (model.imageList.isNotEmpty()) Gson().toJson(model.imageList) else model.imageUrl,
                        status = model.status,
                        deviceType = model.deviceType,
                        author = model.authorId
                    )
                }.distinctBy { it._id }

                if (entities.isNotEmpty()) {
                    productDao.insertProducts(entities)
                }

                // Fetch Orders & Filter by Role
                val orderResponse = apiService.getAllOrders()
                if (orderResponse.isSuccessful && orderResponse.body()?.success == true) {
                    val rawOrders = orderResponse.body()?.data ?: emptyList()
                    val filteredOrders = filterOrdersForCurrentRole(rawOrders).sortedByDescending { it._id }
                    _allOrders.value = filteredOrders
                    previousPendingOrderIds = filteredOrders
                        .filter { it.orderStatus.equals("Chờ xác nhận", ignoreCase = true) || it.orderStatus.equals("pending", ignoreCase = true) }
                        .map { it._id }
                        .toSet()
                }

                // Fetch Users
                val userResponse = apiService.getAllUsers()
                if (userResponse.isSuccessful && userResponse.body()?.success == true) {
                    _allUsers.value = userResponse.body()?.data ?: emptyList()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteProduct(productId: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val response = apiService.deleteProduct(ProductIdRequest(productId))
                if (response.isSuccessful) {
                    productDao.deleteProductById(productId)
                    refreshAllData()
                    onResult(true, "Đã xóa sản phẩm thành công")
                } else {
                    onResult(false, "Không thể xóa sản phẩm này")
                }
            } catch (e: Exception) {
                onResult(false, e.message ?: "Lỗi kết nối")
            }
        }
    }

    fun updateProductStatusBatch(productIds: List<String>, newStatus: String, onResult: (Boolean, String) -> Unit) {
        if (productIds.isEmpty()) {
            onResult(false, "Vui lòng chọn sản phẩm")
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            var successCount = 0

            productIds.forEach { id ->
                val response = apiService.updateProduct(ProductModel(_id = id, status = newStatus))
                if (response.isSuccessful) {
                    successCount++
                }
            }
            refreshAllData()
            onResult(true, "Đã cập nhật $successCount/${productIds.size} sản phẩm")
        }
    }

    fun deleteProductsBatch(productIds: List<String>, onResult: (Boolean, String) -> Unit) {
        if (productIds.isEmpty()) {
            onResult(false, "Vui lòng chọn sản phẩm")
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            var successCount = 0
            productIds.forEach { id ->
                val response = apiService.deleteProduct(ProductIdRequest(id))
                if (response.isSuccessful) {
                    productDao.deleteProductById(id)
                    successCount++
                }
            }
            refreshAllData()
            onResult(true, "Đã xóa $successCount/${productIds.size} sản phẩm")
        }
    }

    fun updateOrderStatus(orderId: String, newStatus: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val req = UpdateOrderStatusRequest(orderId, newStatus)
                val response: Response<OrderCreateResponse> = when {
                    newStatus.equals("Đã xác nhận", ignoreCase = true) || newStatus.equals("approved", ignoreCase = true) -> {
                        apiService.confirmOrder(req)
                    }
                    newStatus.equals("Đã đến địa điểm giao hàng", ignoreCase = true) || newStatus.equals("seller_arrived", ignoreCase = true) -> {
                        apiService.confirmOrder(req)
                    }
                    newStatus.equals("Đã hủy", ignoreCase = true) || newStatus.equals("Đã từ chối", ignoreCase = true) || newStatus.equals("cancelled", ignoreCase = true) || newStatus.equals("rejected", ignoreCase = true) -> {
                        apiService.rejectOrder(req)
                    }
                    else -> {
                        apiService.confirmOrder(req)
                    }
                }

                if (response.isSuccessful) {
                    refreshAllData()
                    onResult(true, "Đã cập nhật trạng thái đơn hàng")
                } else {
                    onResult(false, "Cập nhật đơn hàng thất bại")
                }
            } catch (e: Exception) {
                onResult(false, e.message ?: "Lỗi kết nối")
            }
        }
    }

    fun confirmOrderItem(orderId: String, productId: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val req = mapOf(
                    "orderId" to orderId,
                    "order_id" to orderId,
                    "productId" to productId,
                    "product_id" to productId,
                    "itemId" to productId,
                    "item_id" to productId,
                    "orderItemId" to productId
                )
                val response = apiService.confirmOrderItem(req)

                if (response.isSuccessful) {
                    refreshAllData()
                    onResult(true, "Đã xác nhận sản phẩm")
                } else {
                    onResult(false, "Xác nhận sản phẩm thất bại")
                }
            } catch (e: Exception) {
                onResult(false, e.message ?: "Lỗi kết nối")
            }
        }
    }

    fun rejectOrderItem(orderId: String, productId: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val req = mapOf(
                    "orderId" to orderId,
                    "order_id" to orderId,
                    "productId" to productId,
                    "product_id" to productId,
                    "itemId" to productId,
                    "item_id" to productId,
                    "orderItemId" to productId
                )
                val response = apiService.rejectOrderItem(req)

                if (response.isSuccessful) {
                    refreshAllData()
                    onResult(true, "Đã từ chối sản phẩm")
                } else {
                    onResult(false, "Từ chối sản phẩm thất bại")
                }
            } catch (e: Exception) {
                onResult(false, e.message ?: "Lỗi kết nối")
            }
        }
    }

    fun deleteUser(userId: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val response = apiService.deleteUser(UserIdRequest(userId))
                if (response.isSuccessful) {
                    refreshAllData()
                    onResult(true, "Đã xóa người dùng khỏi hệ thống")
                } else {
                    onResult(false, "Không thể xóa người dùng này")
                }
            } catch (e: Exception) {
                onResult(false, e.message ?: "Lỗi kết nối")
            }
        }
    }

    fun updateProductDetails(product: ProductModel, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val updatedProduct = product.copy(
                    img = if (product.imageList.isNotEmpty()) product.imageList else product.imageUrl
                )
                val response = apiService.updateProduct(updatedProduct)
                if (response.isSuccessful) {
                    refreshAllData()
                    onResult(true, "Cập nhật sản phẩm thành công")
                } else {
                    onResult(false, "Cập nhật sản phẩm thất bại")
                }
            } catch (e: Exception) {
                onResult(false, e.message ?: "Lỗi kết nối")
            }
        }
    }

    fun updateUserRole(userId: String, newRole: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val request = mapOf("userId" to userId, "role" to newRole)
                val response = apiService.updateUserRole(request)
                if (response.isSuccessful) {
                    refreshAllData()
                    onResult(true, "Đổi role thành công thành $newRole")
                } else {
                    onResult(false, "Đổi role thất bại")
                }
            } catch (e: Exception) {
                onResult(false, e.message ?: "Lỗi kết nối")
            }
        }
    }

    fun deleteUsersBatch(userIds: List<String>, onResult: (Boolean, String) -> Unit) {
        if (userIds.isEmpty()) {
            onResult(false, "Vui lòng chọn tài khoản")
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            var successCount = 0
            userIds.forEach { id ->
                val response = apiService.deleteUser(UserIdRequest(id))
                if (response.isSuccessful) {
                    successCount++
                }
            }
            refreshAllData()
            onResult(true, "Đã xóa $successCount/${userIds.size} tài khoản")
        }
    }

    fun updateUserRoleBatch(userIds: List<String>, newRole: String, onResult: (Boolean, String) -> Unit) {
        if (userIds.isEmpty()) {
            onResult(false, "Vui lòng chọn tài khoản")
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            var successCount = 0
            userIds.forEach { id ->
                val request = mapOf("userId" to id, "role" to newRole)
                val response = apiService.updateUserRole(request)
                if (response.isSuccessful) {
                    successCount++
                }
            }
            refreshAllData()
            onResult(true, "Đã cập nhật role cho $successCount/${userIds.size} tài khoản")
        }
    }
}
