package com.example.vlu_campusmarket.ui.admin

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

class AdminOrderAdapter(
    private val onChangeStatus: (String, String) -> Unit
) : ListAdapter<OrderModel, AdminOrderAdapter.AdminOrderViewHolder>(OrderDiffCallback()) {

    private val formatter = DecimalFormat("#,###")

    inner class AdminOrderViewHolder(val binding: OrderItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(order: OrderModel) {
            binding.tvOrderId.text = "Đơn hệ thống: #${order._id.take(8)}"
            binding.tvOrderStatus.text = order.orderStatus

            when (order.orderStatus.lowercase()) {
                "đang xử lý", "pending", "processing" -> {
                    binding.tvOrderStatus.text = "Chờ xử lý"
                    binding.tvOrderStatus.setTextColor(Color.parseColor("#EAB308"))
                    binding.tvOrderStatus.setBackgroundColor(Color.parseColor("#FEF9C3"))
                }
                "đã xác nhận", "approved" -> {
                    binding.tvOrderStatus.text = "Đã xác nhận"
                    binding.tvOrderStatus.setTextColor(Color.parseColor("#16A34A"))
                    binding.tvOrderStatus.setBackgroundColor(Color.parseColor("#DCFCE7"))
                }
                "đang giao", "shipping" -> {
                    binding.tvOrderStatus.text = "Đang giao"
                    binding.tvOrderStatus.setTextColor(Color.parseColor("#2563EB"))
                    binding.tvOrderStatus.setBackgroundColor(Color.parseColor("#EFF6FF"))
                }
                "hoàn thành", "completed" -> {
                    binding.tvOrderStatus.text = "Hoàn thành"
                    binding.tvOrderStatus.setTextColor(Color.parseColor("#0D9488"))
                    binding.tvOrderStatus.setBackgroundColor(Color.parseColor("#CCFBF1"))
                }
                else -> {
                    binding.tvOrderStatus.text = order.orderStatus
                    binding.tvOrderStatus.setTextColor(Color.parseColor("#DC2626"))
                    binding.tvOrderStatus.setBackgroundColor(Color.parseColor("#FEE2E2"))
                }
            }

            val ship = order.shippingInfo
            binding.tvShippingDetails.text = "Khách: ${ship.fullName} (${ship.phone})\nĐịa chỉ: ${ship.address} - ${ship.local}"
            val totalQty = order.orderItems.sumOf { it.quantity }
            binding.tvOrderItemsCount.text = "Số lượng: $totalQty | TT: ${order.paymentMethod}"
            binding.tvOrderTotal.text = "Tổng: ${formatter.format(order.totalPrice)} \u20ab"

            binding.btnCancelOrder.text = "Đổi trạng thái"
            binding.btnCancelOrder.visibility = View.VISIBLE
            binding.btnCancelOrder.setOnClickListener {
                onChangeStatus(order._id, order.orderStatus)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdminOrderViewHolder {
        val binding = OrderItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AdminOrderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AdminOrderViewHolder, position: Int) {
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
