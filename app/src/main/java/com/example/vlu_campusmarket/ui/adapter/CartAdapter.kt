package com.example.vlu_campusmarket.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.vlu_campusmarket.databinding.CartItemBinding
import com.example.vlu_campusmarket.ui.cart.CartItemUIModel
import com.example.vlu_campusmarket.utils.loadImageWithGlide
import java.text.DecimalFormat

class CartAdapter(
    private val onQuantityChanged: (String, Int) -> Unit,
    private val onDeleteItem: (String) -> Unit
) : ListAdapter<CartItemUIModel, CartAdapter.CartViewHolder>(CartDiffCallback()) {

    private val formatter = DecimalFormat("#,###")

    inner class CartViewHolder(val binding: CartItemBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.btnPlus.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    val item = getItem(pos)
                    onQuantityChanged(item.productId, item.quantity + 1)
                }
            }

            binding.btnMinus.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    val item = getItem(pos)
                    if (item.quantity > 1) {
                        onQuantityChanged(item.productId, item.quantity - 1)
                    } else {
                        onDeleteItem(item.productId)
                    }
                }
            }

            binding.btnDelete.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    val item = getItem(pos)
                    onDeleteItem(item.productId)
                }
            }
        }

        fun bind(item: CartItemUIModel) {
            val product = item.product
            binding.tvCartProductName.text = product?.name ?: "Sản phẩm (${item.productId.take(6)})"
            val price = product?.price ?: 0.0
            binding.tvCartProductPrice.text = "${formatter.format(price)} \u20ab"
            binding.tvQuantity.text = item.quantity.toString()

            binding.imgCartProduct.loadImageWithGlide(product?.imageUrl, maxWidth = 250, maxHeight = 250)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val binding = CartItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CartViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class CartDiffCallback : DiffUtil.ItemCallback<CartItemUIModel>() {
        override fun areItemsTheSame(oldItem: CartItemUIModel, newItem: CartItemUIModel): Boolean {
            return oldItem.productId == newItem.productId
        }

        override fun areContentsTheSame(oldItem: CartItemUIModel, newItem: CartItemUIModel): Boolean {
            return oldItem == newItem
        }
    }
}
