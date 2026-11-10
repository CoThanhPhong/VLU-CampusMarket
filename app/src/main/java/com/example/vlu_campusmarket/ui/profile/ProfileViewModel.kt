package com.example.vlu_campusmarket.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vlu_campusmarket.data.local.dao.UserDao
import com.example.vlu_campusmarket.data.local.entity.UserEntity
import com.example.vlu_campusmarket.data.models.ProductModel
import com.example.vlu_campusmarket.data.models.UserModel
import com.example.vlu_campusmarket.data.remote.ApiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val userDao: UserDao,
    private val apiService: ApiService
) : ViewModel() {

    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    private val _userDetail = MutableStateFlow<UserModel?>(null)
    val userDetail: StateFlow<UserModel?> = _userDetail.asStateFlow()

    private val _userProducts = MutableStateFlow<List<ProductModel>>(emptyList())
    val userProducts: StateFlow<List<ProductModel>> = _userProducts.asStateFlow()

    init {
        loadUser()
    }

    fun loadUser() {
        viewModelScope.launch {
            userDao.getLoggedInUser().collect { user ->
                _currentUser.value = user
                if (user != null) {
                    try {
                        val response = apiService.getCurrentUser()
                        if (response.isSuccessful && response.body()?.success == true) {
                            val userModel = response.body()?.data
                            _userDetail.value = userModel

                            val userId = userModel?.id ?: user.id
                            val productResponse = apiService.getAllProducts()
                            if (productResponse.isSuccessful) {
                                val allProducts = productResponse.body()?.data ?: emptyList()
                                _userProducts.value = allProducts.filter { it.authorId == userId }
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
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
