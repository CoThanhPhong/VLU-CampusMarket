package com.example.vlu_campusmarket.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.vlu_campusmarket.data.models.ProductModel
import com.example.vlu_campusmarket.databinding.CartHeaderItemBinding
import com.example.vlu_campusmarket.databinding.CartItemBinding
import com.example.vlu_campusmarket.ui.cart.CartItemUIModel
import com.example.vlu_campusmarket.utils.loadImageWithGlide
import java.text.DecimalFormat

sealed class CartUiRow {
    data class Header(val authorName: String, val isSelected: Boolean) : CartUiRow()
    data class Item(val cartItem: CartItemUIModel, val isSelected: Boolean) : CartUiRow()
}

class CartAdapter(
    private val onItemChecked: (String, Boolean) -> Unit,
    private val onAuthorChecked: (String, Boolean) -> Unit,
    private val onQuantityChanged: (String, Int) -> Unit,
    private val onDeleteItem: (String) -> Unit,
    private val onItemClick: (ProductModel) -> Unit
) : ListAdapter<CartUiRow, RecyclerView.ViewHolder>(CartRowDiffCallback()) {

    private val formatter = DecimalFormat("#,###")

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ITEM = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is CartUiRow.Header -> TYPE_HEADER
            is CartUiRow.Item -> TYPE_ITEM
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_HEADER) {
            val binding = CartHeaderItemBinding.inflate(inflater, parent, false)
            HeaderViewHolder(binding)
        } else {
            val binding = CartItemBinding.inflate(inflater, parent, false)
            ItemViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = getItem(position)) {
            is CartUiRow.Header -> (holder as HeaderViewHolder).bind(row)
            is CartUiRow.Item -> (holder as ItemViewHolder).bind(row)
        }
    }

    inner class HeaderViewHolder(private val binding: CartHeaderItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(header: CartUiRow.Header) {
            binding.tvAuthorName.text = header.authorName
            binding.cbAuthorHeader.setOnCheckedChangeListener(null)
            binding.cbAuthorHeader.isChecked = header.isSelected
            binding.cbAuthorHeader.setOnCheckedChangeListener { _, isChecked ->
                onAuthorChecked(header.authorName, isChecked)
            }
        }
    }

    inner class ItemViewHolder(private val binding: CartItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(row: CartUiRow.Item) {
            val item = row.cartItem
            val product = item.product

            binding.root.setOnClickListener {
                if (product != null) {
                    onItemClick(product)
                }
            }

            binding.cbCartItem.setOnCheckedChangeListener(null)
            binding.cbCartItem.isChecked = row.isSelected
            binding.cbCartItem.setOnCheckedChangeListener { _, isChecked ->
                onItemChecked(item.productId, isChecked)
            }

            binding.tvCartProductName.text = product?.name ?: "Sản phẩm (${item.productId.take(6)})"
            val price = product?.price ?: 0.0
            binding.tvCartProductPrice.text = "${formatter.format(price)} \u20ab"
            binding.tvCartProductCampus.text = "📍 ${product?.local ?: "Cơ sở 1"}"
            binding.tvCartProductStock.text = "📦 Kho: ${product?.amount ?: 1}"
            binding.tvQuantity.text = item.quantity.toString()

            binding.imgCartProduct.loadImageWithGlide(product?.imageUrl, maxWidth = 250, maxHeight = 250)

            binding.btnPlus.setOnClickListener {
                onQuantityChanged(item.productId, item.quantity + 1)
            }

            binding.btnMinus.setOnClickListener {
                if (item.quantity > 1) {
                    onQuantityChanged(item.productId, item.quantity - 1)
                } else {
                    onDeleteItem(item.productId)
                }
            }

            binding.btnDelete.setOnClickListener {
                onDeleteItem(item.productId)
            }
        }
    }

    class CartRowDiffCallback : DiffUtil.ItemCallback<CartUiRow>() {
        override fun areItemsTheSame(oldItem: CartUiRow, newItem: CartUiRow): Boolean {
            return when {
                oldItem is CartUiRow.Header && newItem is CartUiRow.Header -> oldItem.authorName == newItem.authorName
                oldItem is CartUiRow.Item && newItem is CartUiRow.Item -> oldItem.cartItem.productId == newItem.cartItem.productId
                else -> false
            }
        }

        override fun areContentsTheSame(oldItem: CartUiRow, newItem: CartUiRow): Boolean {
            return oldItem == newItem
        }
    }
}
