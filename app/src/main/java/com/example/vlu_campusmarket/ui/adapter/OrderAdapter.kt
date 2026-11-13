package com.example.vlu_campusmarket.ui.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.vlu_campusmarket.data.models.OrderModel
import com.example.vlu_campusmarket.databinding.OrderItemBinding
import com.example.vlu_campusmarket.databinding.OrderSubItemBinding
import com.example.vlu_campusmarket.utils.loadImageWithGlide
import java.text.DecimalFormat

class OrderAdapter(
    private val onCancelOrder: (String) -> Unit
) : ListAdapter<OrderModel, OrderAdapter.OrderViewHolder>(OrderDiffCallback()) {

    private val formatter = DecimalFormat("#,###")

    inner class OrderViewHolder(val binding: OrderItemBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.btnCancelOrder.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    onCancelOrder(getItem(pos)._id)
                }
            }
        }

        fun bind(order: OrderModel) {
            binding.layoutOrderItemsContainer.removeAllViews()
            binding.tvOrderId.text = "Mã đơn: #${order._id.take(8)}"
            binding.tvOrderStatus.text = order.orderStatus
            
            when (order.orderStatus.lowercase()) {
                "đang xử lý", "pending", "processing", "chờ xác nhận" -> {
                    binding.tvOrderStatus.text = "Chờ xử lý"
                    binding.tvOrderStatus.setTextColor(Color.parseColor("#EAB308"))
                    binding.tvOrderStatus.setBackgroundColor(Color.parseColor("#FEF9C3"))
                    binding.btnCancelOrder.visibility = View.VISIBLE
                }
                "đã xác nhận", "approved", "completed", "hoàn thành" -> {
                    binding.tvOrderStatus.text = "Đã xác nhận"
                    binding.tvOrderStatus.setTextColor(Color.parseColor("#16A34A"))
                    binding.tvOrderStatus.setBackgroundColor(Color.parseColor("#DCFCE7"))
                    binding.btnCancelOrder.visibility = View.GONE
                }
                "đã hủy", "cancelled" -> {
                    binding.tvOrderStatus.text = "Đã hủy"
                    binding.tvOrderStatus.setTextColor(Color.parseColor("#DC2626"))
                    binding.tvOrderStatus.setBackgroundColor(Color.parseColor("#FEE2E2"))
                    binding.btnCancelOrder.visibility = View.GONE
                }
                else -> {
                    binding.tvOrderStatus.text = order.orderStatus
                    binding.btnCancelOrder.visibility = View.GONE
                }
            }

            val ship = order.shippingInfo
            binding.tvShippingDetails.text = "Người nhận: ${ship.fullName} (${ship.phone})\nĐịa chỉ: ${ship.local}"
            
            val totalQty = order.orderItems.sumOf { it.quantity }
            binding.tvOrderItemsCount.text = "Tổng số lượng sản phẩm: $totalQty"
            binding.tvOrderTotal.text = "Tổng tiền: ${formatter.format(order.totalPrice)} \u20ab"

            // Hiển thị chi tiết từng sản phẩm và tên người đăng trong đơn hàng
            val inflater = LayoutInflater.from(binding.root.context)
            order.orderItems.forEach { item ->
                val subBinding = OrderSubItemBinding.inflate(inflater, binding.layoutOrderItemsContainer, false)
                val productName = item.productName ?: "Sản phẩm (${item.productId.take(6)})"
                val authorText = if (!item.authorName.isNullOrEmpty()) "Người đăng: ${item.authorName}" else ""
                subBinding.tvSubProductName.text = productName
                subBinding.tvSubProductDetails.text = "SL: ${item.quantity} | ${formatter.format(item.price)} đ | ${item.local}${authorText}"
                subBinding.imgSubProduct.loadImageWithGlide(item.productImg, maxWidth = 150, maxHeight = 150)
                binding.layoutOrderItemsContainer.addView(subBinding.root)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val binding = OrderItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return OrderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class OrderDiffCallback : DiffUtil.ItemCallback<OrderModel>() {
        override fun areItemsTheSame(oldItem: OrderModel, newItem: OrderModel): Boolean {
            return oldItem._id == newItem._id
        }

        override fun areContentsTheSame(oldItem: OrderModel, newItem: OrderModel): Boolean {
            return oldItem == newItem
        }
    }
}
