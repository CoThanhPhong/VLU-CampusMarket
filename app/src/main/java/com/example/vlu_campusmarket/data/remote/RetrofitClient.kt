package com.example.vlu_campusmarket.data.remote

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    // 1. Android Emulator: 10.0.2.2 = localhost của máy tính đang chạy server.
    //    Server Node phải listen ở 0.0.0.0 (hoặc IPv4 127.0.0.1) cổng 5000.
    // 2. Thiết bị thật: đổi thành IP LAN của máy tính, ví dụ "http://192.168.1.10:5000/api/"
    private const val BASE_URL = "http://10.0.2.2:5000/api/"

    // Dùng CHUNG 1 cookie jar + 1 ApiService cho toàn app để session đăng nhập không bị mất
    private val cookieJar = SessionCookieJar()

    @Volatile
    private var apiService: ApiService? = null

    fun getInstance(@Suppress("UNUSED_PARAMETER") context: Context): ApiService {
        return apiService ?: synchronized(this) {
            apiService ?: buildApi().also { apiService = it }
        }
    }

    private fun buildApi(): ApiService {
        // BASIC: chỉ log dòng request/response, KHÔNG log body (tránh lộ mật khẩu trong Logcat)
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        val okHttpClient = OkHttpClient.Builder()
            .cookieJar(cookieJar)
            .addInterceptor(logging)
            .connectTimeout(10, TimeUnit.SECONDS) // báo lỗi nhanh hơn, trước đây phải chờ 30s
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
