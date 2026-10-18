package com.example.vlu_campusmarket.auth

import android.util.Patterns

object AuthValidator {
    // Đổi lại theo đuôi email sinh viên thực tế; để "" nếu không giới hạn (vd: khi test bằng Gmail)
    const val ALLOWED_DOMAIN = "@vanlanguni.vn"

    fun isValidEmail(email: String?): Boolean {
        if (email == null || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) return false
        return ALLOWED_DOMAIN.isEmpty() || email.lowercase().endsWith(ALLOWED_DOMAIN)
    }

    fun isValidPassword(pw: String?): Boolean =
        pw != null && pw.length >= 6 // Firebase yêu cầu tối thiểu 6 ký tự
}
