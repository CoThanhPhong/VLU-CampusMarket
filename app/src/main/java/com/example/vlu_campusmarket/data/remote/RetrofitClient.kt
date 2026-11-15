package com.example.vlu_campusmarket.data.remote

import android.content.Context
import android.os.Build
import com.example.vlu_campusmarket.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    // Server Web / Backend Render API
    private const val RENDER_HOST = "https://vlu-campusmarketweb.onrender.com"
    private const val RENDER_BASE_URL = "https://vlu-campusmarketweb.onrender.com/api/"

    val BASE_URL: String = RENDER_BASE_URL

    val SERVER_HOST: String = RENDER_HOST

    @Volatile
    private var instance: ApiService? = null

    @Volatile
    var cookieJarInstance: SessionCookieJar? = null
        private set

    fun clearCookies() {
        cookieJarInstance?.clear()
    }

    fun getInstance(context: Context): ApiService {
        val appContext = context.applicationContext
        if (cookieJarInstance == null) {
            synchronized(this) {
                if (cookieJarInstance == null) {
                    cookieJarInstance = SessionCookieJar(appContext)
                }
            }
        }
        return instance ?: synchronized(this) {
            val logging = HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) {
                    HttpLoggingInterceptor.Level.BODY
                } else {
                    HttpLoggingInterceptor.Level.NONE
                }
            }

            val okHttpClient = OkHttpClient.Builder()
                .cookieJar(cookieJarInstance!!)
                .addInterceptor(AuthInterceptor(appContext))
                .addInterceptor(logging)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()

            Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ApiService::class.java)
                .also { instance = it }
        }
    }

    fun getFullImageUrl(imagePath: String?): String {
        if (imagePath.isNullOrEmpty()) return ""
        
        var cleanPath = imagePath.replace("\\", "/")

        // 1. Chuyển đổi SVG từ DiceBear sang PNG để Glide đọc được trực tiếp
        if (cleanPath.contains("api.dicebear.com")) {
            cleanPath = cleanPath.replace("/svg?", "/png?").replace("/svg", "/png").replace(".svg", ".png")
        }

        // 2. Thay thế khoảng trắng bằng %20 để tránh lỗi URL malformed
        cleanPath = cleanPath.replace(" ", "%20")

        if (cleanPath.contains("localhost")) {
            cleanPath = cleanPath.replace("http://localhost:5000", SERVER_HOST)
                .replace("https://localhost:5000", SERVER_HOST)
                .replace("http://localhost", SERVER_HOST)
                .replace("https://localhost", SERVER_HOST)
                .replace("localhost:5000", "vlu-campusmarketweb.onrender.com")
                .replace("localhost", "vlu-campusmarketweb.onrender.com")
        }
        if (cleanPath.contains("10.0.2.2:5000")) {
            cleanPath = cleanPath.replace("http://10.0.2.2:5000", SERVER_HOST)
                .replace("https://10.0.2.2:5000", SERVER_HOST)
        }

        return when {
            cleanPath.startsWith("http://") || cleanPath.startsWith("https://") -> cleanPath
            cleanPath.startsWith("/") -> "$SERVER_HOST$cleanPath"
            else -> "$SERVER_HOST/$cleanPath"
        }
    }
}
