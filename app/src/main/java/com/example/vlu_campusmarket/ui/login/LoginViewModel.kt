package com.example.vlu_campusmarket.ui.login

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.vlu_campusmarket.data.local.AppDatabase
import com.example.vlu_campusmarket.data.local.entity.UserEntity
import com.example.vlu_campusmarket.data.remote.RetrofitClient
import com.example.vlu_campusmarket.data.remote.response.LoginRequest
<<<<<<< HEAD
import com.example.vlu_campusmarket.data.remote.response.LoginResponse
import com.google.gson.Gson
import kotlinx.coroutines.CancellationException
=======
import com.example.vlu_campusmarket.utils.SessionManager
>>>>>>> features/optimize-leakCanary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

class LoginViewModel(application: Application) : AndroidViewModel(application) {
    private val userDao = AppDatabase.getDatabase(application).userDao()
    private val apiService = RetrofitClient.getInstance(application)
    private val sessionManager = SessionManager(application)

    fun checkAutoLogin(onAutoLoginResult: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val user = userDao.getLoggedInUser().firstOrNull()
            val sessionRemembered = sessionManager.isLoggedIn()
            val autoLogin = user != null && user.isLoggedIn && sessionRemembered
            withContext(Dispatchers.Main) {
                onAutoLoginResult(autoLogin)
            }
        }
    }

    private suspend fun saveOfflineLogin(mssv: String, rememberMe: Boolean) {
        val userEntity = UserEntity(
            id = mssv,
            mssv = mssv,
            email = "$mssv@vlu.edu.vn",
            role = "student",
            cookie = if (rememberMe) "offline_session" else "temporary_session",
            isLoggedIn = true
        )
        userDao.logoutAll()
        userDao.insertUser(userEntity)
    }

    private suspend fun loginOnline(mssv: String, pass: String, rememberMe: Boolean): Pair<Boolean, String> {
        val response = apiService.login(LoginRequest(mssv, pass))
        val body = response.body()

        if (!response.isSuccessful || body == null || !body.success) {
            // Server trả 401/400... thì thông báo nằm trong errorBody
            val serverMessage = body?.message ?: runCatching {
                Gson().fromJson(response.errorBody()?.string(), LoginResponse::class.java)?.message
            }.getOrNull()
            return false to (serverMessage ?: "Sai MSSV hoặc mật khẩu")
        }

        body.user?.let { userData ->
            val userEntity = UserEntity(
                id = userData.id.ifBlank { mssv },
                mssv = userData.mssv.ifBlank { mssv },
                email = userData.email,
                role = userData.role,
                cookie = if (rememberMe) "persistent_session" else "temporary_session",
                isLoggedIn = true
            )
            userDao.logoutAll()
            userDao.insertUser(userEntity)
        }
        return true to "Đăng nhập thành công"
    }

    fun login(mssv: String, pass: String, rememberMe: Boolean, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
<<<<<<< HEAD
            val result: Pair<Boolean, String> = try {
                loginOnline(mssv, pass, rememberMe)
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                // Chỉ lỗi mạng (không kết nối được server) mới cho vào chế độ offline
                try {
                    saveOfflineLogin(mssv, rememberMe)
                    true to "Đăng nhập thành công (chế độ offline)"
                } catch (dbError: Exception) {
                    false to "Không thể kết nối đến server và không lưu được phiên offline"
=======
            try {
                // Xóa cookie cũ trước khi tiến hành login mới
                RetrofitClient.clearCookies()

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
                    sessionManager.saveUserSession(userData, rememberMe)

                    val setCookieHeaders = response.headers().values("Set-Cookie")
                    val tokenCookieHeader = setCookieHeaders.firstOrNull { it.trim().startsWith("token=") }
                        ?: setCookieHeaders.firstOrNull()
                    val cleanCookie = tokenCookieHeader?.substringBefore(";") ?: (if (rememberMe) "persistent_session" else "temporary_session")

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
>>>>>>> features/optimize-leakCanary
                }
            } catch (e: Exception) {
                false to "Lỗi xử lý dữ liệu từ server. Vui lòng thử lại"
            }

            withContext(Dispatchers.Main) {
                onResult(result.first, result.second)
            }
        }
    }
}
