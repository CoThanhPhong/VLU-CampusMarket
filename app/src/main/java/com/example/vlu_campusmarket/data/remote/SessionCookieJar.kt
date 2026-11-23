package com.example.vlu_campusmarket.data.remote

import android.content.Context
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import java.util.concurrent.ConcurrentHashMap

class SessionCookieJar(context: Context) : CookieJar {
    private val prefs = context.getSharedPreferences("vlu_cookie_store", Context.MODE_PRIVATE)
    private val cookieStore = ConcurrentHashMap<String, MutableList<Cookie>>()

    @Synchronized
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        if (cookies.isEmpty()) return
<<<<<<< HEAD
        val current = cookieStore.getOrPut(url.host) { mutableListOf() }
        // Gộp theo tên cookie: cookie mới ghi đè cookie cũ cùng tên
        cookies.forEach { newCookie ->
            current.removeAll { it.name == newCookie.name }
            current.add(newCookie)
        }
=======

        val host = url.host
        val existingCookies = cookieStore[host] ?: mutableListOf()
        val now = System.currentTimeMillis()

        // Lọc bỏ các cookie đã hết hạn
        val updatedCookies = existingCookies.filter { it.expiresAt > now }.toMutableList()

        for (newCookie in cookies) {
            // Cập nhật cookie trùng name hoặc thêm mới chứ không xóa sạch danh sách cookie cũ
            updatedCookies.removeAll { it.name == newCookie.name }
            if (newCookie.expiresAt > now) {
                updatedCookies.add(newCookie)
            }
        }

        cookieStore[host] = updatedCookies
>>>>>>> features/optimize-leakCanary
    }

    @Synchronized
    override fun loadForRequest(url: HttpUrl): List<Cookie> {
<<<<<<< HEAD
        val now = System.currentTimeMillis()
        val list = cookieStore[url.host] ?: return emptyList()
        list.removeAll { it.expiresAt < now }
        return list.toList()
    }
=======
        val host = url.host
        val now = System.currentTimeMillis()
        val existingCookies = cookieStore[host] ?: return emptyList()

        val validCookies = existingCookies.filter { it.expiresAt > now }.toMutableList()
        cookieStore[host] = validCookies

        return validCookies
    }

    fun saveTokenCookie(host: String, tokenValue: String) {
        if (host.isBlank() || tokenValue.isBlank()) return

        try {
            val tokenCookie = Cookie.Builder()
                .name("token")
                .value(tokenValue)
                .domain(host)
                .path("/")
                .build()

            saveFromResponse(
                HttpUrl.Builder().scheme("http").host(host).build(),
                listOf(tokenCookie)
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun clear() {
        cookieStore.clear()
        prefs.edit().clear().apply()
    }
>>>>>>> features/optimize-leakCanary
}
