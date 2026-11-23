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
import com.example.vlu_campusmarket.data.models.UserModel
import com.example.vlu_campusmarket.databinding.OrderItemBinding
import com.example.vlu_campusmarket.databinding.OrderSubItemBinding
import com.example.vlu_campusmarket.utils.loadImageWithGlide
import java.text.DecimalFormat

class OrderAdapter(
    private var userNamesMap: Map<String, String> = emptyMap(),
    private var sellerDetailsMap: Map<String, UserModel> = emptyMap(),
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

    fun updateSellerDetails(sellers: Map<String, UserModel>) {
        if (this.sellerDetailsMap == sellers) return
        this.sellerDetailsMap = sellers
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
            binding.btnSellerArrived.visibility = View.GONE
            binding.layoutContactSection.visibility = View.GONE // Chỉ người bán liên hệ người mua
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
            val isArrived = statusLower.contains("đến") || statusLower.contains("arrived") || statusLower == "seller_arrived"
            val isOrderConfirmed = statusLower in listOf("đã xác nhận", "approved", "completed", "hoàn thành") || allItemsConfirmed
            val isOrderCancelled = statusLower in listOf("đã hủy", "cancelled", "đã từ chối", "từ chối") || allItemsRejected

            when {
                isOrderCancelled -> {
                    binding.tvOrderStatus.text = "Đã hủy"
                    binding.tvOrderStatus.setTextColor(Color.parseColor("#DC2626"))
                    binding.tvOrderStatus.setBackgroundColor(Color.parseColor("#FEE2E2"))
                    binding.btnCancelOrder.visibility = View.GONE
                }
                isArrived -> {
                    binding.tvOrderStatus.text = "Đã đến địa điểm giao hàng"
                    binding.tvOrderStatus.setTextColor(Color.parseColor("#2563EB"))
                    binding.tvOrderStatus.setBackgroundColor(Color.parseColor("#DBEAFE"))
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

            // Nếu người bán đã đến nơi, hiển thị thông báo nổi bật cho Buyer
            if (isArrived) {
                binding.tvShippingDetails.text = "${binding.tvShippingDetails.text}\n\n📍 THÔNG BÁO: Người bán đã có mặt tại địa điểm giao hàng! Vui lòng liên hệ trực tiếp để nhận sản phẩm."
            }

            // Hiển thị chi tiết từng sản phẩm
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
                    "Người bán: $authorDisplayName"
                } else if (item.authorId.isNotEmpty()) {
                    onRequestFetchUser(item.authorId)
                    "Người bán: Đang tải..."
                } else {
                    ""
                }

                subBinding.tvSubProductName.text = productName
                subBinding.tvSubProductDetails.text = "SL: ${item.quantity} | ${formatter.format(item.price)} đ | ${item.local}"
                
                val itemStatus = item.currentStatus
                val resolvedItemStatus = when {
                    itemStatus.isNotEmpty() -> itemStatus
                    isOrderConfirmed -> "Đã xác nhận"
                    isOrderCancelled -> "Đã từ chối"
                    else -> "Chờ xác nhận"
                }

                val statusDisplay = "Trạng thái: $resolvedItemStatus"
                val combinedText = if (authorText.isNotEmpty()) {
                    "$authorText • $statusDisplay"
                } else {
                    statusDisplay
                }

                subBinding.tvSubProductStatus.visibility = View.VISIBLE
                subBinding.tvSubProductStatus.text = combinedText

                val isItemConfirmed = resolvedItemStatus.equals("Đã xác nhận", ignoreCase = true) ||
                                      resolvedItemStatus.equals("approved", ignoreCase = true) ||
                                      resolvedItemStatus.equals("Hoàn thành", ignoreCase = true) ||
                                      resolvedItemStatus.equals("completed", ignoreCase = true)
                val isItemRejected = resolvedItemStatus.equals("Từ chối", ignoreCase = true) ||
                                     resolvedItemStatus.equals("Đã từ chối", ignoreCase = true) ||
                                     resolvedItemStatus.equals("rejected", ignoreCase = true) ||
                                     resolvedItemStatus.equals("Đã hủy", ignoreCase = true) ||
                                     resolvedItemStatus.equals("cancelled", ignoreCase = true)

                val statusColor = when {
                    isItemConfirmed -> "#16A34A"
                    isItemRejected -> "#DC2626"
                    else -> "#D97706"
                }
                subBinding.tvSubProductStatus.setTextColor(Color.parseColor(statusColor))

                subBinding.imgSubProduct.loadImageWithGlide(item.productImg, maxWidth = 150, maxHeight = 150)
                subBinding.ivSubItemMenu.visibility = View.GONE
                subBinding.root.setOnClickListener {
                    val productModel = ProductModel(
                        _id = item.productId,
                        name = item.productName ?: "Sản phẩm",
                        price = item.price,
                        local = item.local,
                        img = item.productImg ?: "",
                        amount = item.quantity,
                        author = item.authorId
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
