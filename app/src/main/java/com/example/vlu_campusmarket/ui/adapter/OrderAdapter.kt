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
import java.text.DecimalFormat

class OrderAdapter(
    private val onCancelOrder: (String) -> Unit
) : ListAdapter<OrderModel, OrderAdapter.OrderViewHolder>(OrderDiffCallback()) {

    private val formatter = DecimalFormat("#,###")

    inner class OrderViewHolder(val binding: OrderItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(order: OrderModel) {
            binding.tvOrderId.text = "Mã đơn: #${order._id.take(8)}"
            binding.tvOrderStatus.text = order.orderStatus
            
            when (order.orderStatus.lowercase()) {
                "đang xử lý", "pending", "processing" -> {
                    binding.tvOrderStatus.text = "Chờ xử lý"
                    binding.tvOrderStatus.setTextColor(Color.parseColor("#EAB308"))
                    binding.tvOrderStatus.setBackgroundColor(Color.parseColor("#FEF9C3"))
                    binding.btnCancelOrder.visibility = View.VISIBLE
                }
                "đã xác nhận", "approved", "completed" -> {
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
            binding.tvShippingDetails.text = "Người nhận: ${ship.fullName} (${ship.phone})\nĐịa chỉ: ${ship.address} - ${ship.local}"
            val totalQty = order.orderItems.sumOf { it.quantity }
            binding.tvOrderItemsCount.text = "Số lượng mặt hàng: $totalQty"
            binding.tvOrderTotal.text = "Tổng tiền: ${formatter.format(order.totalPrice)} \u20ab"

            binding.btnCancelOrder.setOnClickListener {
                onCancelOrder(order._id)
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
