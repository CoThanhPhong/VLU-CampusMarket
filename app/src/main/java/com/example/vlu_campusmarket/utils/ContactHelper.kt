package com.example.vlu_campusmarket.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

object ContactHelper {

    fun makePhoneCall(context: Context, phoneNumber: String?) {
        if (phoneNumber.isNullOrBlank() || phoneNumber == "Chưa cập nhật" || phoneNumber == "---") {
            Toast.makeText(context, "Số điện thoại không hợp lệ hoặc chưa được cập nhật", Toast.LENGTH_SHORT).show()
            return
        }

        val cleanedNumber = phoneNumber.trim().replace(Regex("[^0-9+]"), "")
        if (cleanedNumber.length < 3) {
            Toast.makeText(context, "Số điện thoại không hợp lệ: $phoneNumber", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$cleanedNumber")
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Không thể mở ứng dụng gọi điện", Toast.LENGTH_SHORT).show()
        }
    }

    fun sendEmail(context: Context, email: String?, productName: String?) {
        if (email.isNullOrBlank() || !email.contains("@")) {
            Toast.makeText(context, "Email không hợp lệ hoặc chưa được cập nhật", Toast.LENGTH_SHORT).show()
            return
        }

        val cleanEmail = email.trim()
        val subject = "Xác nhận giao dịch sản phẩm - ${productName ?: "Sản phẩm VLU"}"
        val body = "Xin chào,\n\nTôi muốn liên hệ để trao đổi thông tin giao dịch về sản phẩm: ${productName ?: "Sản phẩm"}.\n\nVui lòng phản hồi lại giúp tôi.\n\nTrân trọng,"

        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:$cleanEmail")
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, body)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Không tìm thấy ứng dụng email", Toast.LENGTH_SHORT).show()
        }
    }
}
