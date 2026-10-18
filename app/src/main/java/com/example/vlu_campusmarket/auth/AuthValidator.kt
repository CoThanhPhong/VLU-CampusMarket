package com.example.vlu_campusmarket.auth

object AuthValidator {
    // Đổi lại theo đuôi email sinh viên thực tế; để "" nếu không giới hạn (vd: khi test bằng Gmail)
    const val ALLOWED_DOMAIN = "@vanlanguni.vn"

    private val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$")

    fun isValidEmail(email: String?): Boolean {
        if (email.isNullOrBlank()) return false
        val trimmed = email.trim()
        if (!EMAIL_REGEX.matches(trimmed)) return false
        return ALLOWED_DOMAIN.isEmpty() || trimmed.lowercase().endsWith(ALLOWED_DOMAIN)
    }

    fun isValidPassword(pw: String?): Boolean =
        pw != null && pw.length >= 6 // Firebase yêu cầu tối thiểu 6 ký tự
}
