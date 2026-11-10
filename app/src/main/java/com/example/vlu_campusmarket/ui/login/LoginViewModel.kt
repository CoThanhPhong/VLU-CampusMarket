package com.example.vlu_campusmarket.ui.login

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.vlu_campusmarket.data.local.AppDatabase
import com.example.vlu_campusmarket.data.local.entity.UserEntity
import com.example.vlu_campusmarket.data.remote.RetrofitClient
import com.example.vlu_campusmarket.data.remote.response.LoginRequest
import com.example.vlu_campusmarket.utils.SessionManager // 1. Import SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginViewModel(application: Application) : AndroidViewModel(application) {
    private val userDao = AppDatabase.getDatabase(application).userDao()
    private val apiService = RetrofitClient.getInstance(application)
    private val sessionManager = SessionManager(application) // 2. Khởi tạo SessionManager

    fun checkAutoLogin(onAutoLoginResult: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val user = userDao.getLoggedInUser().firstOrNull()
            val autoLogin = user != null && user.isLoggedIn
            withContext(Dispatchers.Main) {
                onAutoLoginResult(autoLogin)
            }
        }
    }

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

                val userData = body.user // UserModel từ server trả về
                if (userData != null) {
                    // 1. Lưu vào SessionManager để đọc nhanh bằng Gson
                    sessionManager.saveUserSession(userData)

                    // 2. Trích xuất Cookie và lưu vào Room DB như bình thường
                    val rawCookieHeader = response.headers().values("Set-Cookie").firstOrNull()
                    val cleanCookie = rawCookieHeader?.substringBefore(";") ?: (if (rememberMe) "persistent_session" else "temporary_session")

                    val userEntity = UserEntity(
                        id = userData.id,
                        mssv = userData.mssv,
                        email = userData.email,
                        role = userData.role,
                        cookie = cleanCookie,
                        isLoggedIn = rememberMe
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