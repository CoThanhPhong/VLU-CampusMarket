package com.example.vlu_campusmarket.utils

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.example.vlu_campusmarket.data.models.UserModel

class SessionManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("vlu_campus_market_session", Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val KEY_USER_OBJECT = "key_user_object"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
    }

    fun saveUserSession(user: UserModel, rememberMe: Boolean = true) {
        val jsonString = gson.toJson(user)
        prefs.edit().apply {
            putString(KEY_USER_OBJECT, jsonString)
            putBoolean(KEY_IS_LOGGED_IN, rememberMe)
            apply()
        }
    }

    fun fetchUser(): UserModel? {
        val jsonString = prefs.getString(KEY_USER_OBJECT, null)
        return if (jsonString != null) {
            try {
                gson.fromJson(jsonString, UserModel::class.java)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    fun logout() {
        prefs.edit().clear().apply()
    }
}
