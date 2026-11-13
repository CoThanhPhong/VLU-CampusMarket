package com.example.vlu_campusmarket.data.repository

import com.example.vlu_campusmarket.data.local.dao.ProductDao
import com.example.vlu_campusmarket.data.local.entity.ProductEntity
import com.example.vlu_campusmarket.data.models.ProductModel
import com.example.vlu_campusmarket.data.models.UserModel
import com.example.vlu_campusmarket.data.remote.ApiService
import com.example.vlu_campusmarket.data.remote.response.ProductListResponse
import com.example.vlu_campusmarket.data.remote.response.UserIdRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.IOException

import com.google.gson.Gson

class ProductRepository(
    private val productDao: ProductDao,
    private val apiService: ApiService
) {
<<<<<<< HEAD
    // 1. Room (ProductEntity) -> ProductModel cho ViewModel
=======
>>>>>>> features/optimize-leakCanary
    val localProducts: Flow<List<ProductModel>> = productDao.getAllProducts().map { entities ->
        entities.map { entity ->
            ProductModel(
                _id = entity._id,
                name = entity.name,
                price = entity.price,
                deviceType = entity.deviceType,
                author = entity.author,
                img = entity.img,
                description = entity.description,
                amount = entity.amount,
                local = entity.local,
                status = entity.status
            )
        }
    }

<<<<<<< HEAD
    // 2. Đồng bộ từ API về Room. Trả về null nếu thành công, ngược lại trả về thông báo lỗi.
    suspend fun fetchFromRemoteAndSave(): String? {
        return try {
            val response = apiService.getAllProducts()
            if (!response.success) {
                return "Server không trả về danh sách sản phẩm"
            }

            val entities = response.data.orEmpty().map { model ->
                ProductEntity(
                    _id = model._id,
                    name = model.name,
                    price = model.price,
                    deviceType = model.deviceType,
                    author = model.author,
                    img = model.img,
                    description = model.description,
                    amount = model.amount,
                    local = model.local,
                    status = model.status
                )
            }
            productDao.insertProducts(entities)
            null
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            "Không thể kết nối đến server. Đang hiển thị dữ liệu đã lưu"
        } catch (e: Exception) {
            e.printStackTrace()
            "Không thể tải danh sách sản phẩm"
        }
    }

    // 3. Lấy thông tin chi tiết user
    suspend fun getUserDetail(userId: String): UserModel? {
        return try {
            val response = apiService.getUserById(UserIdRequest(userId))
            response.body()?.data
        } catch (e: CancellationException) {
            throw e
=======
    suspend fun saveProductToLocal(model: ProductModel) {
        val imgString = if (model.imageList.isNotEmpty()) {
            Gson().toJson(model.imageList)
        } else {
            model.imageUrl
        }
        val entity = ProductEntity(
            _id = model.safeId,
            name = model.name,
            price = model.price,
            deviceType = model.deviceType,
            author = model.authorId,
            img = imgString,
            description = model.description,
            amount = model.amount,
            local = model.local,
            status = model.status
        )
        if (entity._id.isNotEmpty()) {
            productDao.insertProducts(listOf(entity))
        }
    }

    suspend fun deleteProductFromLocal(id: String) {
        if (id.isNotEmpty()) {
            productDao.deleteProductById(id)
        }
    }

    suspend fun fetchFromRemoteAndSave(page: Int = 1): ProductListResponse? {
        return try {
            val response = apiService.getAllProducts(page)
            if (response.isSuccessful) {
                val body = response.body()
                val productList = body?.data ?: emptyList()
                val entities = productList.map { model ->
                    ProductEntity(
                        _id = model.safeId,
                        name = model.name,
                        price = model.price,
                        deviceType = model.deviceType,
                        author = model.authorId,
                        img = if (model.imageList.isNotEmpty()) Gson().toJson(model.imageList) else model.imageUrl,
                        description = model.description,
                        amount = model.amount,
                        local = model.local,
                        status = model.status
                    )
                }.filter { it._id.isNotEmpty() }
                productDao.insertProducts(entities)
                body
            } else {
                null
            }
>>>>>>> features/optimize-leakCanary
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
<<<<<<< HEAD
=======

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
>>>>>>> features/optimize-leakCanary
}
