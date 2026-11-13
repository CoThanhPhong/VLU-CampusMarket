package com.example.vlu_campusmarket.data.repository

import com.example.vlu_campusmarket.data.local.dao.ProductDao
import com.example.vlu_campusmarket.data.local.entity.ProductEntity
import com.example.vlu_campusmarket.data.models.ProductModel
import com.example.vlu_campusmarket.data.models.UserModel
import com.example.vlu_campusmarket.data.remote.ApiService
import com.example.vlu_campusmarket.data.remote.response.UserIdRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.IOException

class ProductRepository(
    private val productDao: ProductDao,
    private val apiService: ApiService
) {
    // 1. Room (ProductEntity) -> ProductModel cho ViewModel
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
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
