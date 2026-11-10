package com.example.vlu_campusmarket.data.repository

import com.example.vlu_campusmarket.data.local.entity.ProductEntity
import com.example.vlu_campusmarket.data.local.dao.ProductDao
import com.example.vlu_campusmarket.data.models.ProductModel
import com.example.vlu_campusmarket.data.models.UserModel
import com.example.vlu_campusmarket.data.remote.ApiService
import com.example.vlu_campusmarket.data.remote.response.UserIdRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ProductRepository(
    private val productDao: ProductDao,
    private val apiService: ApiService
) {
    // 1. Map từ Flow<List<ProductEntity>> của Room sang Flow<List<ProductModel>> cho ViewModel
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
                status = entity.status,
                startDate = null,
                endDate = null
            )
        }
    }

    // 2. Đồng bộ dữ liệu từ Retrofit Response về Room Database
    suspend fun fetchFromRemoteAndSave() {
        try {
            val response = apiService.getAllProducts()
            // Lấy danh sách từ body của Response (giả định trả về ProductListResponse hoặc List)
            val productList = response.data.let {
                // Nếu response là ProductListResponse chứa list sản phẩm, thay thế cho phù hợp
                // Hoặc nếu response trực tiếp là List thì dùng trực tiếp
                it as? List<ProductModel> ?: emptyList()
            } ?: emptyList()

            // Map từ ProductModel sang ProductEntity để lưu vào Room
            val entities = productList.map { model ->
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
                    status = model.status,
                )
            }
            productDao.insertProducts(entities)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // 3. Lấy thông tin chi tiết user và unwrap Response
    suspend fun getUserDetail(userId: String): UserModel? {
        return try {
            // Nếu API yêu cầu truyền vào object UserIdRequest thay vì String trực tiếp:
             val requestBody = UserIdRequest(userId)
             val response = apiService.getUserById(requestBody)

            // Hoặc nếu truyền String trực tiếp nhưng API trả về Response<UserModel> hoặc Response<UserDetailResponse>:
//            val response = apiService.getUserById(userId)
            val userResponse = response.body()?.data

            if (userResponse != null) {
                // Map dữ liệu sang UserModel của bạn
                UserModel(
                    id = userResponse.id,
                    mssv = userResponse.mssv,
                    email = userResponse.email,
                    role = userResponse.role,
                    profile = userResponse.profile
                )
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}