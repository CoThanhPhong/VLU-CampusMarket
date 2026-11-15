package com.example.vlu_campusmarket.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.example.vlu_campusmarket.data.models.UserModel
import com.google.gson.Gson

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = createEncryptedPreferences(context)
    private val gson = Gson()

    companion object {
        private const val KEY_USER_OBJECT = "key_user_object"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"

        private fun createEncryptedPreferences(context: Context): SharedPreferences {
            return try {
                val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
                EncryptedSharedPreferences.create(
                    "vlu_campus_market_encrypted_session",
                    masterKeyAlias,
                    context,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
            } catch (e: Exception) {
                context.getSharedPreferences("vlu_campus_market_session", Context.MODE_PRIVATE)
            }
        }
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
