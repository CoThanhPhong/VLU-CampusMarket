package com.example.vlu_campusmarket.ui.product

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vlu_campusmarket.data.local.dao.ProductDao
import com.example.vlu_campusmarket.data.local.dao.UserDao
import com.example.vlu_campusmarket.data.local.entity.ProductEntity
import com.example.vlu_campusmarket.data.models.ProductModel
import com.example.vlu_campusmarket.data.remote.ApiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class CreatePostViewModel(
    private val apiService: ApiService,
    private val productDao: ProductDao,
    private val userDao: UserDao
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun createProduct(
        name: String,
        price: Double,
        deviceType: String,
        local: String,
        amount: Int,
        description: String,
        img: String,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val authorId = userDao.getLoggedInUser().firstOrNull()?.id ?: "anonymous"
                val newProduct = ProductModel(
                    _id = "",
                    name = name,
                    description = description,
                    price = price,
                    amount = amount,
                    local = local,
                    img = img,
                    startDate = null,
                    status = "AVAILABLE",
                    deviceType = deviceType,
                    endDate = null,
                    author = authorId
                )

                val response = apiService.createProducts(listOf(newProduct))
                _isLoading.value = false

                if (response.isSuccessful && response.body()?.success == true) {
                    val createdList = response.body()?.data ?: emptyList()
                    val entities = createdList.map { model ->
                        ProductEntity(
                            _id = model._id,
                            name = model.name,
                            description = model.description,
                            price = model.price,
                            amount = model.amount,
                            local = model.local,
                            img = model.imageUrl,
                            status = model.status,
                            deviceType = model.deviceType,
                            author = model.authorId
                        )
                    }
                    if (entities.isNotEmpty()) {
                        productDao.insertProducts(entities)
                    }
                    onResult(true, "Tạo bài đăng thành công!")
                } else {
                    val msg = response.body()?.message ?: "Tạo bài đăng thất bại"
                    onResult(false, msg)
                }
            } catch (e: Exception) {
                _isLoading.value = false
                onResult(false, e.message ?: "Lỗi kết nối đến server")
            }
        }
    }
}
