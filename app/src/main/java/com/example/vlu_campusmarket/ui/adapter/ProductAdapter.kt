package com.example.vlu_campusmarket.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.vlu_campusmarket.data.models.ProductModel
import com.example.vlu_campusmarket.databinding.ProductItemBinding
import com.example.vlu_campusmarket.utils.loadImageWithGlide
import java.text.DecimalFormat

class ProductAdapter(
    private var userNamesMap: Map<String, String> = emptyMap(),
    private val onRequestFetchUser: (String) -> Unit,
    private val onAuthorClick: (String) -> Unit,
    private val onProductClick: ((ProductModel) -> Unit)? = null
) : ListAdapter<ProductModel, ProductAdapter.ProductViewHolder>(ProductDiffCallback()) {

    private val formatter = DecimalFormat("#,###")

    fun updateUserNames(names: Map<String, String>) {
        this.userNamesMap = names
        notifyDataSetChanged()
    }

    inner class ProductViewHolder(val binding: ProductItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(product: ProductModel) {
            binding.tvProductName.text = product.name
            binding.tvProductPrice.text = "${formatter.format(product.price)} \u20ab"
            binding.tvDeviceType.text = product.deviceType
            binding.tvProductLocal.text = if (product.local.startsWith("Cơ sở")) product.local else "Cơ sở: ${product.local}"
            binding.tvProductAmount.text = "Kho: ${product.amount}"

            // Load ảnh sản phẩm sử dụng Glide
            binding.imgProduct.loadImageWithGlide(product.imageUrl)

            // Hiển thị tên người đăng từ cache
            val authorName = userNamesMap[product.authorId]
            if (authorName != null) {
                binding.tvAuthor.text = "👤 $authorName"
            } else {
                binding.tvAuthor.text = "👤 ${product.authorId.take(6)}..."
                onRequestFetchUser(product.authorId)
            }

            binding.tvAuthor.setOnClickListener {
                onAuthorClick(product.authorId)
            }

            binding.root.setOnClickListener {
                onProductClick?.invoke(product)
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
