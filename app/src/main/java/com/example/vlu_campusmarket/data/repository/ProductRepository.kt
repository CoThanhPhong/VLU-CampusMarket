package com.example.vlu_campusmarket.data.repository

import com.example.vlu_campusmarket.data.local.dao.ProductDao
import com.example.vlu_campusmarket.data.local.entity.ProductEntity
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

    suspend fun fetchFromRemoteAndSave() {
        try {
            val response = apiService.getAllProducts()
            if (response.isSuccessful) {
                val productList = response.body()?.data ?: emptyList()
                val entities = productList.map { model ->
                    ProductEntity(
                        _id = model._id,
                        name = model.name,
                        price = model.price,
                        deviceType = model.deviceType,
                        author = model.authorId,
                        img = model.imageUrl,
                        description = model.description,
                        amount = model.amount,
                        local = model.local,
                        status = model.status
                    )
                }
                productDao.insertProducts(entities)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun getUserDetail(userId: String): UserModel? {
        return try {
            val requestBody = UserIdRequest(userId)
            val response = apiService.getUserById(requestBody)
            val userResponse = response.body()?.data

            if (userResponse != null) {
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
