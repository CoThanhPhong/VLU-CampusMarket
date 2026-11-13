package com.example.vlu_campusmarket.utils

import android.content.Context
import com.bumptech.glide.Glide
import com.example.vlu_campusmarket.data.local.AppDatabase
import com.example.vlu_campusmarket.data.remote.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object LogoutHelper {
    suspend fun performCompleteLogout(context: Context) {
        val appContext = context.applicationContext

        // 0. Clear OkHttp / Retrofit Cookies
        try {
            RetrofitClient.clearCookies()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 1. Clear SessionManager (SharedPreferences)
        try {
            SessionManager(appContext).logout()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Clear all tables in Room Database (users, products, cart, orders)
        try {
            AppDatabase.getDatabase(appContext).clearAllTables()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 3. Clear Glide Memory and Disk Caches (clearMemory must run on Main thread)
        try {
            withContext(Dispatchers.Main) {
                Glide.get(appContext).clearMemory()
            }
            Thread {
                try {
                    Glide.get(appContext).clearDiskCache()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 4. Clear App Cache and Temp files
        try {
            appContext.cacheDir?.let { clearDirectory(it) }
            appContext.externalCacheDir?.let { clearDirectory(it) }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun clearDirectory(dir: File): Boolean {
        var success = true
        if (dir.isDirectory) {
            val children = dir.list()
            if (children != null) {
                for (child in children) {
                    val successChild = clearDirectory(File(dir, child))
                    if (!successChild) {
                        success = false
                    }
                }
            }
        }
        return dir.delete() && success
    }
}
