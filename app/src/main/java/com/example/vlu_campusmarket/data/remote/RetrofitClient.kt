package com.example.vlu_campusmarket.data.remote

import android.content.Context
import android.os.Build
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    // 💡 Cấu hình BASE_URL tự động nhận diện Máy ảo (Emulator) và Máy thật (Physical Device)
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

    fun getInstance(context: Context): ApiService {
        return instance ?: synchronized(this) {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val okHttpClient = OkHttpClient.Builder()
                .cookieJar(SessionCookieJar())
                .addInterceptor(AuthInterceptor(context.applicationContext))
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

        if (cleanPath.contains("localhost:5000")) {
            return cleanPath.replace("localhost:5000", if (isEmulator) "10.0.2.2:5000" else PHYSICAL_DEVICE_IP)
        }
        if (cleanPath.contains("127.0.0.1:5000")) {
            return cleanPath.replace("127.0.0.1:5000", if (isEmulator) "10.0.2.2:5000" else PHYSICAL_DEVICE_IP)
        }

        if (cleanPath.startsWith("http://") || cleanPath.startsWith("https://") || cleanPath.startsWith("data:image")) {
            return cleanPath
        }

        if (!cleanPath.startsWith("/")) {
            cleanPath = "/$cleanPath"
        }
        return "$SERVER_HOST$cleanPath"
    }
}
