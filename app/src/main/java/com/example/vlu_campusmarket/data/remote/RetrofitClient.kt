package com.example.vlu_campusmarket.data.remote

import android.content.Context
import android.os.Build
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

<<<<<<< HEAD
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
=======
    // Đường dẫn IP của Máy ảo (Android Emulator)
    private const val EMULATOR_BASE_URL = "http://10.0.2.2:5000/api/"
    private const val EMULATOR_HOST = "http://10.0.2.2:5000"

    // 👉 Khi test trên MÁY THẬT qua Wi-Fi chung, hãy thay IP dưới đây thành IP LAN của máy tính chạy MERN Web (VD: "192.168.1.5:5000")
    private const val PHYSICAL_DEVICE_IP = "192.168.1.137:5000"
    private const val DEVICE_BASE_URL = "http://$PHYSICAL_DEVICE_IP/api/"
    private const val DEVICE_HOST = "http://$PHYSICAL_DEVICE_IP"

    // Hàm kiểm tra tự động thiết bị đang chạy là Máy ảo hay Máy thật
    private val isEmulator: Boolean
        get() = Build.FINGERPRINT.startsWith("generic") ||
                Build.FINGERPRINT.startsWith("unknown") ||
                Build.MODEL.contains("google_sdk") ||
                Build.MODEL.contains("Emulator") ||
                Build.MODEL.contains("Android SDK built for x86") ||
                Build.HARDWARE.contains("goldfish") ||
                Build.HARDWARE.contains("ranchu") ||
                Build.PRODUCT.contains("sdk")

    val BASE_URL: String
        get() = if (isEmulator) EMULATOR_BASE_URL else DEVICE_BASE_URL

    val SERVER_HOST: String
        get() = if (isEmulator) EMULATOR_HOST else DEVICE_HOST

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
                level = HttpLoggingInterceptor.Level.BODY
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
>>>>>>> features/optimize-leakCanary

        if (cleanPath.contains("localhost:5000")) {
            cleanPath = cleanPath.replace("http://localhost:5000", SERVER_HOST)
                .replace("https://localhost:5000", SERVER_HOST)
        }
        if (cleanPath.contains("10.0.2.2:5000") && !isEmulator) {
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
