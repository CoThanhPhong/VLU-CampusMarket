package com.example.vlu_campusmarket.ui.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.vlu_campusmarket.data.models.OrderModel
import com.example.vlu_campusmarket.data.models.ProductModel
import com.example.vlu_campusmarket.databinding.OrderItemBinding
import com.example.vlu_campusmarket.databinding.OrderSubItemBinding
import com.example.vlu_campusmarket.utils.loadImageWithGlide
import java.text.DecimalFormat

class OrderAdapter(
    private var userNamesMap: Map<String, String> = emptyMap(),
    private val onRequestFetchUser: (String) -> Unit,
    private val onCancelOrder: (String) -> Unit,
    private val onProductClick: (ProductModel) -> Unit
) : ListAdapter<OrderModel, OrderAdapter.OrderViewHolder>(OrderDiffCallback()) {

    private val formatter = DecimalFormat("#,###")

    fun updateUserNames(names: Map<String, String>) {
        if (this.userNamesMap == names) return
        this.userNamesMap = names
        notifyDataSetChanged()
    }

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
            binding.btnConfirmOrder.visibility = View.GONE
            binding.tvOrderId.text = "Mã đơn: #${order._id.take(8)}"

            val hasItems = order.orderItems.isNotEmpty()

            val allItemsRejected = hasItems && order.orderItems.all { item ->
                val st = item.currentStatus
                st.equals("Từ chối", ignoreCase = true) ||
                st.equals("Đã từ chối", ignoreCase = true) ||
                st.equals("rejected", ignoreCase = true) ||
                st.equals("Đã hủy", ignoreCase = true) ||
                st.equals("cancelled", ignoreCase = true)
            }

            val allItemsConfirmed = hasItems && order.orderItems.all { item ->
                val st = item.currentStatus
                st.equals("Đã xác nhận", ignoreCase = true) ||
                st.equals("approved", ignoreCase = true) ||
                st.equals("Hoàn thành", ignoreCase = true) ||
                st.equals("completed", ignoreCase = true)
            }

            val statusLower = order.orderStatus.lowercase()
            val isOrderConfirmed = statusLower in listOf("đã xác nhận", "approved", "completed", "hoàn thành") || allItemsConfirmed
            val isOrderCancelled = statusLower in listOf("đã hủy", "cancelled", "đã từ chối", "từ chối") || allItemsRejected

            when {
                isOrderCancelled -> {
                    binding.tvOrderStatus.text = "Đã hủy"
                    binding.tvOrderStatus.setTextColor(Color.parseColor("#DC2626"))
                    binding.tvOrderStatus.setBackgroundColor(Color.parseColor("#FEE2E2"))
                    binding.btnCancelOrder.visibility = View.GONE
                }
                isOrderConfirmed -> {
                    binding.tvOrderStatus.text = "Đã xác nhận"
                    binding.tvOrderStatus.setTextColor(Color.parseColor("#16A34A"))
                    binding.tvOrderStatus.setBackgroundColor(Color.parseColor("#DCFCE7"))
                    binding.btnCancelOrder.visibility = View.GONE
                }
                statusLower in listOf("đang xử lý", "pending", "processing", "chờ xác nhận", "chờ xử lý") -> {
                    binding.tvOrderStatus.text = "Chờ xác nhận"
                    binding.tvOrderStatus.setTextColor(Color.parseColor("#EAB308"))
                    binding.tvOrderStatus.setBackgroundColor(Color.parseColor("#FEF9C3"))
                    binding.btnCancelOrder.visibility = View.VISIBLE
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

                val cachedName = userNamesMap[item.authorId]
                val authorDisplayName = if (!item.authorName.isNullOrEmpty() && item.authorName != item.authorId) {
                    item.authorName
                } else {
                    cachedName
                }

                val authorText = if (!authorDisplayName.isNullOrEmpty() && authorDisplayName != "...") {
                    "Người đăng: $authorDisplayName"
                } else if (item.authorId.isNotEmpty()) {
                    onRequestFetchUser(item.authorId)
                    "Người đăng: Đang tải..."
                } else {
                    ""
                }

                subBinding.tvSubProductName.text = productName
                subBinding.tvSubProductDetails.text = "SL: ${item.quantity} | ${formatter.format(item.price)} đ | ${item.local}"
                
                if (authorText.isNotEmpty()) {
                    subBinding.tvSubProductStatus.visibility = View.VISIBLE
                    subBinding.tvSubProductStatus.text = authorText
                    subBinding.tvSubProductStatus.setTextColor(Color.parseColor("#94A3B8"))
                } else {
                    subBinding.tvSubProductStatus.visibility = View.GONE
                }

                subBinding.imgSubProduct.loadImageWithGlide(item.productImg, maxWidth = 150, maxHeight = 150)
                subBinding.ivSubItemMenu.visibility = View.GONE
                subBinding.root.setOnClickListener {
                    val productModel = ProductModel(
                        _id = item.productId,
                        name = item.productName ?: "Sản phẩm",
                        price = item.price,
                        local = item.local,
                        img = item.productImg ?: "",
                        amount = item.quantity
                    )
                    onProductClick(productModel)
                }
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
