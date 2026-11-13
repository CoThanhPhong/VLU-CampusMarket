package com.example.vlu_campusmarket.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vlu_campusmarket.data.local.dao.ProductDao
import com.example.vlu_campusmarket.data.models.OrderModel
import com.example.vlu_campusmarket.data.models.ProductModel
import com.example.vlu_campusmarket.data.models.UserModel
import com.example.vlu_campusmarket.data.remote.ApiService
import com.example.vlu_campusmarket.data.remote.response.ProductIdRequest
import com.example.vlu_campusmarket.data.remote.response.UpdateOrderStatusRequest
import com.example.vlu_campusmarket.data.remote.response.UserIdRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

import com.example.vlu_campusmarket.data.local.entity.ProductEntity
import com.google.gson.Gson

class AdminViewModel(
    private val apiService: ApiService,
    private val productDao: ProductDao
) : ViewModel() {

    private val _allProducts = MutableStateFlow<List<ProductModel>>(emptyList())
    val allProducts: StateFlow<List<ProductModel>> = _allProducts.asStateFlow()

    private val _allOrders = MutableStateFlow<List<OrderModel>>(emptyList())
    val allOrders: StateFlow<List<OrderModel>> = _allOrders.asStateFlow()

    private val _allUsers = MutableStateFlow<List<UserModel>>(emptyList())
    val allUsers: StateFlow<List<UserModel>> = _allUsers.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        refreshAllData()
    }

    fun refreshAllData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Fetch Products
                val prodResponse = apiService.getAllProducts()
                if (prodResponse.isSuccessful) {
                    val prods = prodResponse.body()?.data ?: emptyList()
                    _allProducts.value = prods
                    val entities = prods.map { model ->
                        ProductEntity(
                            _id = model.safeId,
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
                    }.filter { it._id.isNotEmpty() }
                    if (entities.isNotEmpty()) {
                        productDao.insertProducts(entities)
                    }
                }

                // Fetch Orders
                val orderResponse = apiService.getAllOrders()
                if (orderResponse.isSuccessful && orderResponse.body()?.success == true) {
                    _allOrders.value = orderResponse.body()?.data ?: emptyList()
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
            val currentProducts = _allProducts.value.associateBy { it._id }

            productIds.forEach { id ->
                val currentModel = currentProducts[id]
                if (currentModel != null) {
                    val updatedModel = currentModel.copy(status = newStatus)
                    val response = apiService.updateProduct(updatedModel)
                    if (response.isSuccessful) {
                        successCount++
                    }
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
                var response = apiService.updateOrderStatus(req)
                if (!response.isSuccessful) {
                    response = apiService.updateOrderStatusStatus(req)
                }
                if (!response.isSuccessful) {
                    response = apiService.updateOrderStatusUpdate(req)
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
}
