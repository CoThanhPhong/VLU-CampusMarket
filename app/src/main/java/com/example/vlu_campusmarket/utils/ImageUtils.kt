package com.example.vlu_campusmarket.utils

import android.util.Base64
import android.widget.ImageView
import androidx.appcompat.content.res.AppCompatResources
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.vlu_campusmarket.R
import com.example.vlu_campusmarket.data.remote.RetrofitClient

fun ImageView.loadImageWithGlide(imagePath: String?, placeholderRes: Int = R.drawable.ic_placeholder_product) {
    val placeholder = AppCompatResources.getDrawable(context, placeholderRes)
    if (imagePath.isNullOrEmpty()) {
        setImageDrawable(placeholder)
        return
    }

    if (imagePath.startsWith("data:image")) {
        try {
            val base64Data = imagePath.substringAfter(",")
            val imageBytes = Base64.decode(base64Data, Base64.DEFAULT)
            Glide.with(context)
                .load(imageBytes)
                .placeholder(placeholder)
                .error(placeholder)
                .into(this)
            return
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    val fullUrl = RetrofitClient.getFullImageUrl(imagePath)

    Glide.with(context)
        .load(fullUrl)
        .placeholder(placeholder)
        .error(placeholder)
        .diskCacheStrategy(DiskCacheStrategy.ALL)
        .into(this)
}
