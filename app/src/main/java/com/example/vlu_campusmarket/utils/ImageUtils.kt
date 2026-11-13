package com.example.vlu_campusmarket.utils

import android.util.Base64
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.vlu_campusmarket.R
import com.example.vlu_campusmarket.data.remote.RetrofitClient

/**
 * Tải và hiển thị ảnh bằng Glide với các cấu hình tối ưu RAM và hiệu năng cuộn RecyclerView:
 * - Dùng DecodeFormat.PREFER_RGB_565 (giảm 50% RAM so với ARGB_8888).
 * - Sử dụng override() để giới hạn kích thước decode phù hợp với UI item, tránh load ảnh gốc 4K vào RAM.
 * - Tự động clear request cũ khi View bị tái sử dụng (recycle) trong RecyclerView.
 */
fun ImageView.loadImageWithGlide(
    imagePath: String?,
    placeholderRes: Int = R.drawable.ic_placeholder_product,
    maxWidth: Int = 400,
    maxHeight: Int = 400
) {
    if (imagePath.isNullOrEmpty()) {
        Glide.with(this).clear(this)
        setImageResource(placeholderRes)
        return
    }

    if (imagePath.startsWith("data:image")) {
        try {
            val base64Data = imagePath.substringAfter(",")
            val imageBytes = Base64.decode(base64Data, Base64.DEFAULT)
            Glide.with(this)
                .load(imageBytes)
                .placeholder(placeholderRes)
                .error(placeholderRes)
                .format(DecodeFormat.PREFER_RGB_565)
                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                .override(maxWidth, maxHeight)
                .centerCrop()
                .into(this)
            return
        } catch (e: Exception) {
            e.printStackTrace()
            Glide.with(this).clear(this)
            setImageResource(placeholderRes)
            return
        }
    }

    val fullUrl = RetrofitClient.getFullImageUrl(imagePath)

    Glide.with(this)
        .load(fullUrl)
        .placeholder(placeholderRes)
        .error(placeholderRes)
        .format(DecodeFormat.PREFER_RGB_565)
        .diskCacheStrategy(DiskCacheStrategy.ALL)
        .override(maxWidth, maxHeight)
        .centerCrop()
        .into(this)
}
