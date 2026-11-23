package com.example.vlu_campusmarket.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
<<<<<<< HEAD
import coil.load
import com.example.vlu_campusmarket.R
import com.example.vlu_campusmarket.data.models.ProductModel
import com.example.vlu_campusmarket.databinding.ProductItemBinding
import java.util.Locale

class ProductAdapter(
    // Lấy map tên MỚI NHẤT mỗi lần bind (trước đây truyền 1 map rỗng cố định nên tên không bao giờ cập nhật)
    private val userNamesProvider: () -> Map<String, String>,
=======
import com.example.vlu_campusmarket.R
import com.example.vlu_campusmarket.data.models.ProductModel
import com.example.vlu_campusmarket.databinding.ProductItemBinding
import com.example.vlu_campusmarket.utils.loadImageWithGlide
import java.text.DecimalFormat

class ProductAdapter(
    private var userNamesMap: Map<String, String> = emptyMap(),
    private var userAvatarsMap: Map<String, String> = emptyMap(),
>>>>>>> features/optimize-leakCanary
    private val onRequestFetchUser: (String) -> Unit,
    private val onAuthorClick: (String) -> Unit,
    private val onProductClick: ((ProductModel) -> Unit)? = null
) : ListAdapter<ProductModel, ProductAdapter.ProductViewHolder>(ProductDiffCallback()) {

<<<<<<< HEAD
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
=======
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
>>>>>>> features/optimize-leakCanary
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
            val localText = product.local ?: "Cơ sở 1"
            binding.tvProductLocal.text = if (localText.startsWith("Cơ sở")) localText else "Cơ sở: $localText"
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
