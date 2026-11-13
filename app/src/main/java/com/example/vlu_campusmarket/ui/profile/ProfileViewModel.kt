package com.example.vlu_campusmarket.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vlu_campusmarket.data.local.dao.ProductDao
import com.example.vlu_campusmarket.data.local.dao.UserDao
import com.example.vlu_campusmarket.data.local.entity.ProductEntity
import com.example.vlu_campusmarket.data.local.entity.UserEntity
import com.example.vlu_campusmarket.data.models.ProductModel
import com.example.vlu_campusmarket.data.models.UserModel
import com.example.vlu_campusmarket.data.remote.ApiService
import com.example.vlu_campusmarket.data.remote.response.UserIdRequest
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val userDao: UserDao,
    private val apiService: ApiService,
    private val targetUserId: String? = null,
    private val productDao: ProductDao? = null
) : ViewModel() {

    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    private val _userDetail = MutableStateFlow<UserModel?>(null)
    val userDetail: StateFlow<UserModel?> = _userDetail.asStateFlow()

    private val _userProducts = MutableStateFlow<List<ProductModel>>(emptyList())
    val userProducts: StateFlow<List<ProductModel>> = _userProducts.asStateFlow()

    init {
        refreshProfileData()
    }

    fun refreshProfileData() {
        viewModelScope.launch {
            if (!targetUserId.isNullOrEmpty()) {
                loadUserById(targetUserId)
            } else {
                loadUser()
            }
        }
    }

    private suspend fun loadUser() {
        try {
            val user = userDao.getLoggedInUser().firstOrNull()
            _currentUser.value = user
            if (user != null) {
                var userModel: UserModel? = null
                var response = apiService.getCurrentUser()
                if (!response.isSuccessful || response.body()?.success != true) {
                    response = apiService.getCurrentUserMe()
                }
                if (!response.isSuccessful || response.body()?.success != true) {
                    response = apiService.getUserMe()
                }
                if (!response.isSuccessful || response.body()?.success != true) {
                    response = apiService.getUsersMe()
                }
                if (!response.isSuccessful || response.body()?.success != true) {
                    response = apiService.getCurrentUserAuthUser()
                }

                if (response.isSuccessful && response.body()?.success == true) {
                    userModel = response.body()?.data
                } else {
                    val idResponse = apiService.getUserById(UserIdRequest(user.id))
                    if (idResponse.isSuccessful && idResponse.body()?.success == true) {
                        userModel = idResponse.body()?.data
                    }
                }

                if (userModel != null) {
                    _userDetail.value = userModel
                    loadAllUserProducts(userModel.id, userModel)
                } else {
                    loadAllUserProducts(user.id, null)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun loadUserById(userId: String) {
        try {
            val response = apiService.getUserById(UserIdRequest(userId))
            if (response.isSuccessful && response.body()?.success == true) {
                val userModel = response.body()?.data
                _userDetail.value = userModel
                loadAllUserProducts(userId, userModel)
            } else {
                loadAllUserProducts(userId, null)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            loadAllUserProducts(userId, null)
        }
    }

    private suspend fun loadAllUserProducts(userId: String, userModel: UserModel?) {
        try {
            val allProducts = mutableListOf<ProductModel>()
            userModel?.profile?.productModels?.let { allProducts.addAll(it) }

            var page = 1
            var totalPages = 1
            do {
                val productResponse = apiService.getAllProducts(page)
                if (productResponse.isSuccessful && productResponse.body() != null) {
                    val body = productResponse.body()!!
                    allProducts.addAll(body.data)
                    totalPages = body.totalPages
                } else {
                    break
                }
                page++
            } while (page <= totalPages)

            val userProductIds = userModel?.profile?.productIds?.toSet() ?: emptySet()
            val fetchedIds = allProducts.map { it.safeId }.toSet()
            val missingIds = userProductIds - fetchedIds
            for (missingId in missingIds) {
                try {
                    val singleRes = apiService.getProductById(missingId)
                    if (singleRes.isSuccessful && singleRes.body()?.success == true) {
                        singleRes.body()?.data?.let { allProducts.add(it) }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            val distinctProducts = allProducts.distinctBy { it.safeId }

            productDao?.let { dao ->
                val entities = distinctProducts.map { model ->
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
                    dao.insertProducts(entities)
                }
            }

            val mssv = userModel?.mssv?.trim() ?: ""
            val email = userModel?.email?.trim() ?: ""
            val userIdStr = userId.trim()

            _userProducts.value = distinctProducts.filter { product ->
                userProductIds.contains(product.safeId) ||
                product.authorId.trim().equals(userIdStr, ignoreCase = true) ||
                product.authorId.trim().equals(mssv, ignoreCase = true) ||
                product.authorId.trim().equals(email, ignoreCase = true) ||
                product.author.toString().trim().equals(userIdStr, ignoreCase = true) ||
                (product.author is Map<*, *> && (
                    product.author["_id"]?.toString()?.trim()?.equals(userIdStr, ignoreCase = true) == true ||
                    product.author["id"]?.toString()?.trim()?.equals(userIdStr, ignoreCase = true) == true ||
                    product.author["mssv"]?.toString()?.trim()?.equals(mssv, ignoreCase = true) == true
                ))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun updateProfile(
        name: String,
        phone: String,
        bio: String,
        avatar: String?,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val request = mapOf(
                    "name" to name,
                    "phone" to phone,
                    "bio" to bio,
                    "avatar" to (avatar ?: "")
                )
                var response = apiService.updateProfile(request)
                if (!response.isSuccessful) {
                    response = apiService.updateUserProfile(request)
                }
                if (!response.isSuccessful) {
                    response = apiService.updateProfileU(request)
                }
                if (!response.isSuccessful) {
                    response = apiService.updateProfileUPost(request)
                }
                if (!response.isSuccessful) {
                    response = apiService.updateUserProfileU(request)
                }
                if (!response.isSuccessful) {
                    response = apiService.updateUserProfileUPost(request)
                }
                if (!response.isSuccessful) {
                    response = apiService.updateProfileUpdate(request)
                }
                if (!response.isSuccessful) {
                    response = apiService.updateProfileUpdatePost(request)
                }
                if (!response.isSuccessful) {
                    response = apiService.updateUsersU(request)
                }
                if (!response.isSuccessful) {
                    response = apiService.updateUsersUpdate(request)
                }

                if (response.isSuccessful && response.body()?.success == true) {
                    val updatedUser = response.body()?.data
                    if (updatedUser != null) {
                        _userDetail.value = updatedUser
                    }
                    onResult(true, "Cập nhật hồ sơ thành công")
                } else {
                    onResult(false, "Cập nhật hồ sơ thất bại")
                }
            } catch (e: Exception) {
                onResult(false, e.message ?: "Lỗi kết nối")
            }
        }
    }

    fun logout(onLogoutSuccess: () -> Unit) {
        viewModelScope.launch {
            userDao.logoutAll()
            onLogoutSuccess()
        }
    }
}
