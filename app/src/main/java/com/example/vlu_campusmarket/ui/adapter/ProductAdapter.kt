package com.example.vlu_campusmarket.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.vlu_campusmarket.data.models.ProductModel
import com.example.vlu_campusmarket.databinding.ProductItemBinding

class ProductAdapter(
    private val userNamesMap: Map<String, String>,
    private val onRequestFetchUser: (String) -> Unit,
    private val onAuthorClick: (String) -> Unit
) : ListAdapter<ProductModel, ProductAdapter.ProductViewHolder>(ProductDiffCallback()) {

    inner class ProductViewHolder(val binding: ProductItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(product: ProductModel) {
            binding.tvProductName.text = product.name
            binding.tvProductPrice.text = "${product.price} \u20ab"

            // Hiển thị tên từ cache, nếu chưa có thì hiện tạm ID và gọi fetch
            val authorName = userNamesMap[product.author]
            if (authorName != null) {
                binding.tvAuthor.text = authorName
            } else {
                binding.tvAuthor.text = "Người đăng: ${product.author.take(6)}..."
                onRequestFetchUser(product.author)
            }

            binding.tvAuthor.setOnClickListener {
                onAuthorClick(product.author)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val binding = ProductItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ProductViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
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