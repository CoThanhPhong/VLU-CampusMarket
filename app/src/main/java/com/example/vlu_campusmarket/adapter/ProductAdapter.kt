package com.example.vlu_campusmarket.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.vlu_campusmarket.R
import com.example.vlu_campusmarket.data.model.Product
import java.text.NumberFormat
import java.util.Locale

class ProductAdapter(private val items: List<Product>) :
    RecyclerView.Adapter<ProductAdapter.VH>() {

    private val vnd: NumberFormat = NumberFormat.getInstance(Locale.forLanguageTag("vi-VN"))

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_product, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(h: VH, position: Int) {
        val p = items[position]
        val ctx = h.itemView.context
        h.tvTitle.text = p.name
        h.tvPrice.text = ctx.getString(R.string.price_vnd, vnd.format(p.price))

        // Glide tải ảnh bất đồng bộ từ URL, tự cache & tự huỷ request khi item bị recycle
        Glide.with(h.imgPhone.context)
            .load(p.img)
            .placeholder(R.drawable.ic_placeholder)
            .error(R.drawable.ic_placeholder)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .transition(DrawableTransitionOptions.withCrossFade())
            .centerCrop()
            .into(h.imgPhone)
    }

    override fun getItemCount(): Int = items.size

    override fun onViewRecycled(holder: VH) {
        super.onViewRecycled(holder)
        Glide.with(holder.holderImageContext()).clear(holder.imgPhone) // dọn request đang dở
    }

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val imgPhone: ImageView = v.findViewById(R.id.imgPhone)
        val tvTitle: TextView = v.findViewById(R.id.tvTitle)
        val tvPrice: TextView = v.findViewById(R.id.tvPrice)

        fun holderImageContext() = imgPhone.context
    }
}
