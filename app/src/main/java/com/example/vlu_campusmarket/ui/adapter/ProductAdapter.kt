package com.example.vlu_campusmarket.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.vlu_campusmarket.R
import com.example.vlu_campusmarket.data.models.ProductModel
import com.example.vlu_campusmarket.databinding.ProductItemBinding
import com.example.vlu_campusmarket.utils.loadImageWithGlide
import java.text.DecimalFormat

class ProductAdapter(
    private var userNamesMap: Map<String, String> = emptyMap(),
    private var userAvatarsMap: Map<String, String> = emptyMap(),
    private val onRequestFetchUser: (String) -> Unit,
    private val onAuthorClick: (String) -> Unit,
    private val onProductClick: ((ProductModel) -> Unit)? = null
) : ListAdapter<ProductModel, ProductAdapter.ProductViewHolder>(ProductDiffCallback()) {

    private val formatter = DecimalFormat("#,###")

    fun updateUserNames(names: Map<String, String>) {
        if (this.userNamesMap == names) return
        this.userNamesMap = names
        notifyDataSetChanged()
    }

    fun updateUserAvatars(avatars: Map<String, String>) {
        if (this.userAvatarsMap == avatars) return
        this.userAvatarsMap = avatars
        notifyDataSetChanged()
    }

    inner class ProductViewHolder(val binding: ProductItemBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.tvAuthor.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onAuthorClick(getItem(position).authorId)
                }
            }

            binding.imgAuthorAvatar.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onAuthorClick(getItem(position).authorId)
                }
            }

            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onProductClick?.invoke(getItem(position))
                }
            }
        }

        fun bind(product: ProductModel) {
            binding.tvProductName.text = product.name
            binding.tvProductPrice.text = "${formatter.format(product.price)} \u20ab"
            binding.tvDeviceType.text = product.deviceType
            binding.tvProductLocal.text = if (product.local.startsWith("Cơ sở")) product.local else "Cơ sở: ${product.local}"
            binding.tvProductAmount.text = "Kho: ${product.amount}"

            // Load ảnh sản phẩm sử dụng Glide với override giới hạn kích thước tối ưu RAM
            binding.imgProduct.loadImageWithGlide(product.imageUrl, maxWidth = 350, maxHeight = 350)

            // Load avatar tác giả (ưu tiên từ product object, nếu không có lấy từ cache userAvatarsMap)
            val avatarUrl = product.authorAvatar ?: userAvatarsMap[product.authorId]
            binding.imgAuthorAvatar.loadImageWithGlide(avatarUrl, placeholderRes = R.drawable.ic_user_profile, maxWidth = 100, maxHeight = 100)

            // Hiển thị tên người đăng từ cache hoặc từ product object
            val cachedName = userNamesMap[product.authorId]
            val authorDisplayName = product.authorName ?: cachedName
            if (!authorDisplayName.isNullOrEmpty() && authorDisplayName != "...") {
                binding.tvAuthor.text = authorDisplayName
            } else {
                binding.tvAuthor.text = "${product.authorId.take(6)}..."
                onRequestFetchUser(product.authorId)
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
