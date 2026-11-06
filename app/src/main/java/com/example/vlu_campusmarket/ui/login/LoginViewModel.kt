package com.example.vlu_campusmarket.ui.login

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.vlu_campusmarket.data.local.AppDatabase
import com.example.vlu_campusmarket.data.local.entity.UserEntity
import com.example.vlu_campusmarket.data.remote.RetrofitClient
import com.example.vlu_campusmarket.data.remote.response.LoginRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginViewModel(application: Application) : AndroidViewModel(application) {
    private val userDao = AppDatabase.getDatabase(application).userDao()
    private val apiService = RetrofitClient.getInstance(application)

    fun login(mssv: String, pass: String, rememberMe: Boolean, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Gọi API đăng nhập tới Backend
                val response = apiService.login(LoginRequest(mssv, pass))
                val body = response.body()

                if (!response.isSuccessful || body == null || !body.success) {
                    val errorMsg = body?.message ?: "Đăng nhập thất bại"
                    withContext(Dispatchers.Main) { onResult(false, errorMsg) }
                    return@launch
                }

                val userData = body.user
                if (userData != null) {
                    // Dựa vào rememberMe để xử lý lưu trữ phiên đăng nhập:
                    // - Nếu rememberMe = true: Lưu trạng thái đăng nhập dài hạn vào Room Database.
                    // - Nếu rememberMe = false: Vẫn có thể lưu nhưng đánh dấu phiên ngắn hạn hoặc xử lý xóa khi đóng app.
                    val userEntity = UserEntity(
                        id = userData.id,
                        mssv = userData.mssv,
                        email = userData.email,
                        role = userData.role,
                        cookie = if (rememberMe) "persistent_session" else "temporary_session",
                        isLoggedIn = true // Đánh dấu đã đăng nhập để tự động chuyển trang ở lần mở app sau nếu có Remember Me
                    )

                    userDao.logoutAll()
                    userDao.insertUser(userEntity)
                }

                withContext(Dispatchers.Main) {
                    onResult(true, "Đăng nhập thành công")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onResult(false, e.message ?: "Lỗi kết nối đến server")
                }
            }
        }
    }
}