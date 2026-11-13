package com.example.vlu_campusmarket.data.remote

import android.content.Context
import com.example.vlu_campusmarket.data.local.AppDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val context: Context) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // Đọc thông tin User đang đăng nhập và lấy Cookie từ Room Database
        val userDao = AppDatabase.getDatabase(context).userDao()
        val loggedInUser = runBlocking {
            userDao.getLoggedInUser().first()
        }

        // Nếu có cookie, tiến hành add vào Header của request
        val requestBuilder = originalRequest.newBuilder()
        loggedInUser?.cookie?.let { cookieValue ->
            val cleanCookie = cookieValue.substringBefore(";")

            val tokenValue = if (cleanCookie.startsWith("token=")) {
                cleanCookie.substringAfter("token=")
            } else {
                cleanCookie
            }

            if (tokenValue.isNotBlank() && tokenValue != "temporary_session" && tokenValue != "persistent_session") {
                // Đảm bảo CookieJar cũng ghi nhớ cookie token cho host này để BridgeInterceptor không đè mất token
                RetrofitClient.cookieJarInstance?.saveTokenCookie(originalRequest.url.host, tokenValue)

                val cookieHeader = "token=$tokenValue"
                requestBuilder.header("Cookie", cookieHeader)
                requestBuilder.header("Authorization", "Bearer $tokenValue")
            } else if (cleanCookie.isNotBlank()) {
                requestBuilder.header("Cookie", cleanCookie)
            }
        }

        return chain.proceed(requestBuilder.build())
    }
}
