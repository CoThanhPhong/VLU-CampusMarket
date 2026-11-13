package com.example.vlu_campusmarket.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.vlu_campusmarket.R
import com.example.vlu_campusmarket.data.models.ProductModel
import com.example.vlu_campusmarket.databinding.ProductItemBinding
import java.util.Locale

class ProductAdapter(
    // Lấy map tên MỚI NHẤT mỗi lần bind (trước đây truyền 1 map rỗng cố định nên tên không bao giờ cập nhật)
    private val userNamesProvider: () -> Map<String, String>,
    private val onRequestFetchUser: (String) -> Unit,
    private val onAuthorClick: (String) -> Unit
) : ListAdapter<ProductModel, ProductAdapter.ProductViewHolder>(ProductDiffCallback()) {

    inner class ProductViewHolder(val binding: ProductItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(product: ProductModel) {
            binding.tvProductName.text = product.name
            // Layout đã có sẵn ký hiệu ₫ phía trước nên chỉ hiển thị số
            binding.tvProductPrice.text = String.format(Locale.US, "%,.0f", product.price)
            binding.tvDeviceType.text = product.deviceType
            binding.tvProductLocal.text = product.local
            binding.tvProductAmount.text = "Kho: ${product.amount}"

            if (product.img.startsWith("http")) {
                binding.imgProduct.load(product.img) {
                    crossfade(true)
                    error(R.drawable.ic_launcher_foreground)
                }
            } else {
                binding.imgProduct.setImageResource(R.drawable.ic_launcher_foreground)
            }

            val authorName = userNamesProvider()[product.author]
            if (authorName != null) {
                binding.tvAuthor.text = "👤 $authorName"
            } else {
                binding.tvAuthor.text = "👤 Người đăng: ${product.author.take(6)}..."
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
