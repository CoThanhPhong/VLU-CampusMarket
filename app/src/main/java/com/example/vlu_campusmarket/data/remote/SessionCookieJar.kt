package com.example.vlu_campusmarket.data.remote

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

class SessionCookieJar : CookieJar {
    private val cookieStore = HashMap<String, MutableList<Cookie>>()

    @Synchronized
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        if (cookies.isEmpty()) return
        val current = cookieStore.getOrPut(url.host) { mutableListOf() }
        // Gộp theo tên cookie: cookie mới ghi đè cookie cũ cùng tên
        cookies.forEach { newCookie ->
            current.removeAll { it.name == newCookie.name }
            current.add(newCookie)
        }
    }

    @Synchronized
    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val now = System.currentTimeMillis()
        val list = cookieStore[url.host] ?: return emptyList()
        list.removeAll { it.expiresAt < now }
        return list.toList()
    }
}
