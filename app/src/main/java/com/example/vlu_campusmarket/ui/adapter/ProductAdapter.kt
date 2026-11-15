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

    companion object {
        private const val PAYLOAD_USER_DATA = "PAYLOAD_USER_DATA"
    }

    fun updateUserNames(names: Map<String, String>) {
        if (this.userNamesMap == names) return
        this.userNamesMap = names
        if (itemCount > 0) {
            notifyItemRangeChanged(0, itemCount, PAYLOAD_USER_DATA)
        }
    }

    fun updateUserAvatars(avatars: Map<String, String>) {
        if (this.userAvatarsMap == avatars) return
        this.userAvatarsMap = avatars
        if (itemCount > 0) {
            notifyItemRangeChanged(0, itemCount, PAYLOAD_USER_DATA)
        }
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
            binding.tvDeviceType.text = product.formattedDeviceType
            binding.tvProductLocal.text = if (product.local.startsWith("Cơ sở")) product.local else "Cơ sở: ${product.local}"
            binding.tvProductAmount.text = "Kho: ${product.amount}"

            binding.imgProduct.loadImageWithGlide(product.imageUrl, maxWidth = 350, maxHeight = 350)
            bindUserData(product)
        }

        fun bindUserData(product: ProductModel) {
            val avatarUrl = product.authorAvatar ?: userAvatarsMap[product.authorId]
            binding.imgAuthorAvatar.loadImageWithGlide(avatarUrl, placeholderRes = R.drawable.ic_user_profile, maxWidth = 100, maxHeight = 100)

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

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isNotEmpty() && payloads.contains(PAYLOAD_USER_DATA)) {
            holder.bindUserData(getItem(position))
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    class ProductDiffCallback : DiffUtil.ItemCallback<ProductModel>() {
        override fun areItemsTheSame(oldItem: ProductModel, newItem: ProductModel): Boolean {
            val oldId = oldItem.safeId
            val newId = newItem.safeId
            return if (oldId.isNotEmpty() && newId.isNotEmpty()) {
                oldId == newId
            } else {
                oldItem == newItem
            }
        }

        override fun areContentsTheSame(oldItem: ProductModel, newItem: ProductModel): Boolean {
            return oldItem == newItem
        }
    }
}
