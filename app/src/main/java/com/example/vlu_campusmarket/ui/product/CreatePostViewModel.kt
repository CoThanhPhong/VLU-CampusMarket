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

import com.google.gson.Gson

class CreatePostViewModel(
    private val apiService: ApiService,
    private val productDao: ProductDao,
    private val userDao: UserDao
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private fun mapDeviceTypeToEnum(raw: String): String {
        return when (raw.trim().lowercase()) {
            "điện thoại", "mobile", "phone" -> "mobile"
            "tablet" -> "tablet"
            "mac" -> "mac"
            "laptop" -> "laptop"
            else -> "mobile"
        }
    }

    fun createProduct(
        name: String,
        price: Double,
        deviceType: String,
        local: String,
        amount: Int,
        description: String,
        img: Any,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val authorId = userDao.getLoggedInUser().firstOrNull()?.id ?: "anonymous"
                val newProduct = ProductModel(
                    _id = null,
                    name = name,
                    description = description,
                    price = price,
                    amount = amount,
                    local = local,
                    img = img,
                    startDate = null,
                    status = "Còn hàng",
                    deviceType = mapDeviceTypeToEnum(deviceType),
                    endDate = null,
                    author = authorId
                )

                val response = apiService.createProducts(listOf(newProduct))
                _isLoading.value = false

                if (response.isSuccessful && response.body()?.success == true) {
                    val createdList = response.body()?.data ?: emptyList()
                    val entities = createdList.map { model ->
                        val imgStr = if (model.imageList.isNotEmpty()) {
                            Gson().toJson(model.imageList)
                        } else if (img is List<*>) {
                            Gson().toJson(img)
                        } else {
                            img.toString()
                        }
                        ProductEntity(
                            _id = if (model.safeId.isNotEmpty()) model.safeId else "",
                            name = model.name.ifEmpty { name },
                            description = model.description ?: description,
                            price = if (model.price > 0) model.price else price,
                            amount = if (model.amount > 0) model.amount else amount,
                            local = model.local.ifEmpty { local },
                            img = imgStr,
                            status = model.status ?: "Còn hàng",
                            deviceType = model.deviceType.ifEmpty { mapDeviceTypeToEnum(deviceType) },
                            author = if (model.authorId.isNotEmpty()) model.authorId else authorId
                        )
                    }.filter { it._id.isNotEmpty() }

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

    fun updateProduct(
        id: String,
        name: String,
        price: Double,
        deviceType: String,
        local: String,
        amount: Int,
        description: String,
        img: Any,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val authorId = userDao.getLoggedInUser().firstOrNull()?.id ?: "anonymous"
                val updatedProduct = ProductModel(
                    _id = id,
                    name = name,
                    description = description,
                    price = price,
                    amount = amount,
                    local = local,
                    img = img,
                    startDate = null,
                    status = "Còn hàng",
                    deviceType = mapDeviceTypeToEnum(deviceType),
                    endDate = null,
                    author = authorId
                )

                val response = apiService.updateProduct(updatedProduct)
                _isLoading.value = false

                if (response.isSuccessful && response.body()?.success == true) {
                    val model = response.body()?.data
                    val imgStr = if (model != null && model.imageList.isNotEmpty()) {
                        Gson().toJson(model.imageList)
                    } else if (img is List<*>) {
                        Gson().toJson(img)
                    } else {
                        img.toString()
                    }

                    val finalId = if (model != null && model.safeId.isNotEmpty()) model.safeId else id
                    val entity = ProductEntity(
                        _id = finalId,
                        name = model?.name?.ifEmpty { name } ?: name,
                        description = model?.description ?: description,
                        price = if ((model?.price ?: 0.0) > 0) model!!.price else price,
                        amount = if ((model?.amount ?: 0) > 0) model!!.amount else amount,
                        local = model?.local?.ifEmpty { local } ?: local,
                        img = imgStr,
                        status = model?.status ?: "Còn hàng",
                        deviceType = model?.deviceType?.ifEmpty { mapDeviceTypeToEnum(deviceType) } ?: mapDeviceTypeToEnum(deviceType),
                        author = if (model != null && model.authorId.isNotEmpty()) model.authorId else authorId
                    )
                    productDao.insertProducts(listOf(entity))

                    onResult(true, "Cập nhật bài đăng thành công!")
                } else {
                    val msg = response.body()?.message ?: "Cập nhật thất bại"
                    onResult(false, msg)
                }
            } catch (e: Exception) {
                _isLoading.value = false
                onResult(false, e.message ?: "Lỗi kết nối")
            }
        }
    }
}
