package com.example.vlu_campusmarket.ui.admin

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.vlu_campusmarket.data.models.ProductModel
import com.example.vlu_campusmarket.databinding.ProductItemBinding
import com.example.vlu_campusmarket.utils.loadImageWithGlide
import java.text.DecimalFormat

class AdminProductAdapter(
    private val onDeleteProduct: (String) -> Unit
) : ListAdapter<ProductModel, AdminProductAdapter.AdminProductViewHolder>(ProductDiffCallback()) {

    private val formatter = DecimalFormat("#,###")

    inner class AdminProductViewHolder(val binding: ProductItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(product: ProductModel) {
            binding.tvProductName.text = product.name
            binding.tvProductPrice.text = "${formatter.format(product.price)} \u20ab"
            binding.tvDeviceType.text = product.deviceType
            binding.tvProductLocal.text = "Cơ sở: ${product.local}"
            binding.tvProductAmount.text = "Kho: ${product.amount}"
            binding.tvAuthor.text = "🗑️ Xóa bài đăng"
            binding.tvAuthor.setTextColor(Color.parseColor("#EF4444"))

            binding.imgProduct.loadImageWithGlide(product.imageUrl)

            binding.tvAuthor.setOnClickListener {
                onDeleteProduct(product._id)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdminProductViewHolder {
        val binding = ProductItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AdminProductViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AdminProductViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ProductDiffCallback : DiffUtil.ItemCallback<ProductModel>() {
        override fun areItemsTheSame(oldItem: ProductModel, newItem: ProductModel): Boolean {
            return oldItem._id == newItem._id
        }

        override fun areContentsTheSame(oldItem: ProductModel, newItem: ProductModel): Boolean {
            return oldItem == newItem
        }
    }
}
