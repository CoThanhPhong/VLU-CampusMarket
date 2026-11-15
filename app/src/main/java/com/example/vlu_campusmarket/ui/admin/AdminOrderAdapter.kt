package com.example.vlu_campusmarket.ui.admin

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.vlu_campusmarket.data.models.OrderItemModel
import com.example.vlu_campusmarket.data.models.OrderModel
import com.example.vlu_campusmarket.databinding.OrderItemBinding
import com.example.vlu_campusmarket.databinding.OrderSubItemBinding
import com.example.vlu_campusmarket.utils.loadImageWithGlide
import java.text.DecimalFormat

class AdminOrderAdapter(
    private val onConfirmOrder: (String) -> Unit,
    private val onCancelOrder: (String) -> Unit,
    private val onSubItemAction: (OrderModel, OrderItemModel, String) -> Unit
) : ListAdapter<OrderModel, AdminOrderAdapter.AdminOrderViewHolder>(OrderDiffCallback()) {

    private val formatter = DecimalFormat("#,###")
    private val itemStatusMap = mutableMapOf<String, String>()

    inner class AdminOrderViewHolder(val binding: OrderItemBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.btnConfirmOrder.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    val order = getItem(pos)
                    order.orderItems.forEach { item ->
                        val itemKey = "${order._id}_${item.productId}"
                        val currentSt = itemStatusMap[itemKey] ?: item.currentStatus
                        val isRej = currentSt.equals("Từ chối", ignoreCase = true) ||
                                   currentSt.equals("Đã từ chối", ignoreCase = true) ||
                                   currentSt.equals("rejected", ignoreCase = true) ||
                                   currentSt.equals("Đã hủy", ignoreCase = true) ||
                                   currentSt.equals("cancelled", ignoreCase = true)
                        if (!isRej) {
                            itemStatusMap[itemKey] = "Đã xác nhận"
                            onSubItemAction(order, item, "Xác nhận")
                        }
                    }
                    onConfirmOrder(order._id)
                }
            }

            binding.btnCancelOrder.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    val order = getItem(pos)
                    order.orderItems.forEach { item ->
                        val itemKey = "${order._id}_${item.productId}"
                        val currentSt = itemStatusMap[itemKey] ?: item.currentStatus
                        val isConf = currentSt.equals("Đã xác nhận", ignoreCase = true) ||
                                     currentSt.equals("approved", ignoreCase = true) ||
                                     currentSt.equals("Hoàn thành", ignoreCase = true) ||
                                     currentSt.equals("completed", ignoreCase = true)
                        if (!isConf) {
                            itemStatusMap[itemKey] = "Đã từ chối"
                            onSubItemAction(order, item, "Từ chối")
                        }
                    }
                    onCancelOrder(order._id)
                }
            }
        }

        fun bind(order: OrderModel) {
            binding.tvOrderId.text = "Đơn hệ thống: #${order._id.take(8)}"

            val isOrderConfirmedDirect = order.orderStatus.equals("Đã xác nhận", ignoreCase = true) ||
                                         order.orderStatus.equals("approved", ignoreCase = true) ||
                                         order.orderStatus.equals("Hoàn thành", ignoreCase = true) ||
                                         order.orderStatus.equals("completed", ignoreCase = true)

            val isOrderCancelledDirect = order.orderStatus.equals("Đã hủy", ignoreCase = true) ||
                                         order.orderStatus.equals("cancelled", ignoreCase = true) ||
                                         order.orderStatus.equals("Đã từ chối", ignoreCase = true) ||
                                         order.orderStatus.equals("rejected", ignoreCase = true)

            val itemStatusList = order.orderItems.map { item ->
                val itemKey = "${order._id}_${item.productId}"
                val itemStat = itemStatusMap[itemKey] ?: item.currentStatus
                when {
                    itemStat.isNotEmpty() -> itemStat
                    isOrderConfirmedDirect -> "Đã xác nhận"
                    isOrderCancelledDirect -> "Đã từ chối"
                    else -> itemStat
                }
            }

            val hasItems = order.orderItems.isNotEmpty()

            val allItemsRejected = hasItems && itemStatusList.all { st ->
                st.equals("Từ chối", ignoreCase = true) ||
                st.equals("Đã từ chối", ignoreCase = true) ||
                st.equals("rejected", ignoreCase = true) ||
                st.equals("Đã hủy", ignoreCase = true) ||
                st.equals("cancelled", ignoreCase = true)
            }

            val allItemsConfirmed = hasItems && itemStatusList.all { st ->
                st.equals("Đã xác nhận", ignoreCase = true) ||
                st.equals("approved", ignoreCase = true) ||
                st.equals("Hoàn thành", ignoreCase = true) ||
                st.equals("completed", ignoreCase = true)
            }

            val allItemsProcessed = hasItems && itemStatusList.all { st ->
                val isConf = st.equals("Đã xác nhận", ignoreCase = true) || st.equals("approved", ignoreCase = true) || st.equals("Hoàn thành", ignoreCase = true) || st.equals("completed", ignoreCase = true)
                val isRej = st.equals("Từ chối", ignoreCase = true) || st.equals("Đã từ chối", ignoreCase = true) || st.equals("rejected", ignoreCase = true) || st.equals("Đã hủy", ignoreCase = true) || st.equals("cancelled", ignoreCase = true)
                isConf || isRej
            }

            val isOrderConfirmed = isOrderConfirmedDirect || allItemsConfirmed
            val isOrderCancelled = isOrderCancelledDirect || allItemsRejected

            val isFinalState = isOrderConfirmed || isOrderCancelled || allItemsProcessed

            when {
                isOrderCancelled -> {
                    binding.tvOrderStatus.text = "Đã hủy"
                    binding.tvOrderStatus.setTextColor(Color.WHITE)
                    binding.tvOrderStatus.setBackgroundColor(Color.parseColor("#EF4444"))
                }
                isOrderConfirmed || allItemsProcessed -> {
                    binding.tvOrderStatus.text = "Đã xác nhận"
                    binding.tvOrderStatus.setTextColor(Color.WHITE)
                    binding.tvOrderStatus.setBackgroundColor(Color.parseColor("#16A34A"))
                }
                order.orderStatus.lowercase() in listOf("đang giao", "shipping") -> {
                    binding.tvOrderStatus.text = "Đang giao"
                    binding.tvOrderStatus.setTextColor(Color.WHITE)
                    binding.tvOrderStatus.setBackgroundColor(Color.parseColor("#2563EB"))
                }
                else -> {
                    binding.tvOrderStatus.text = "Chờ xử lý"
                    binding.tvOrderStatus.setTextColor(Color.WHITE)
                    binding.tvOrderStatus.setBackgroundColor(Color.parseColor("#D97706"))
                }
            }

            val ship = order.shippingInfo
            binding.tvShippingDetails.text = "Khách: ${ship.fullName} (${ship.phone})\nĐịa chỉ: ${ship.local}"
            
            var calculatedTotal = 0.0
            var calculatedQty = 0
            order.orderItems.forEachIndexed { index, item ->
                val st = itemStatusList[index]
                val isRej = st.equals("Từ chối", ignoreCase = true) ||
                           st.equals("Đã từ chối", ignoreCase = true) ||
                           st.equals("rejected", ignoreCase = true) ||
                           st.equals("Đã hủy", ignoreCase = true) ||
                           st.equals("cancelled", ignoreCase = true)
                if (!isRej) {
                    calculatedTotal += item.price * item.quantity
                    calculatedQty += item.quantity
                }
            }

            binding.tvOrderItemsCount.text = "Số lượng: $calculatedQty | TT: ${order.paymentMethod}"
            binding.tvOrderTotal.text = "Tổng: ${formatter.format(if (calculatedTotal > 0 || allItemsRejected) calculatedTotal else order.totalPrice)} \u20ab"

            if (isFinalState) {
                binding.btnConfirmOrder.visibility = View.GONE
                binding.btnCancelOrder.visibility = View.GONE
            } else {
                binding.btnConfirmOrder.visibility = View.VISIBLE
                binding.btnCancelOrder.visibility = View.VISIBLE
                binding.btnCancelOrder.text = "Hủy"
            }

            // Hiển thị chi tiết từng sản phẩm trong đơn hàng
            binding.layoutOrderItemsContainer.removeAllViews()
            val inflater = LayoutInflater.from(binding.root.context)
            order.orderItems.forEachIndexed { index, item ->
                val subBinding = OrderSubItemBinding.inflate(inflater, binding.layoutOrderItemsContainer, false)
                val productName = item.productName ?: "Sản phẩm (${item.productId.take(6)})"

                val itemKey = "${order._id}_${item.productId}"
                val rawItemStatus = itemStatusList[index]

                val isItemConfirmed = rawItemStatus.equals("Đã xác nhận", ignoreCase = true) ||
                                      rawItemStatus.equals("approved", ignoreCase = true) ||
                                      rawItemStatus.equals("Hoàn thành", ignoreCase = true) ||
                                      rawItemStatus.equals("completed", ignoreCase = true)

                val isItemRejected = rawItemStatus.equals("Từ chối", ignoreCase = true) ||
                                     rawItemStatus.equals("Đã từ chối", ignoreCase = true) ||
                                     rawItemStatus.equals("rejected", ignoreCase = true) ||
                                     rawItemStatus.equals("Đã hủy", ignoreCase = true) ||
                                     rawItemStatus.equals("cancelled", ignoreCase = true)

                subBinding.tvSubProductName.text = productName
                subBinding.tvSubProductDetails.text = "SL: ${item.quantity} | ${formatter.format(item.price)} đ | ${item.local}"
                
                if (rawItemStatus.isNotEmpty()) {
                    subBinding.tvSubProductStatus.visibility = View.VISIBLE
                    subBinding.tvSubProductStatus.text = "Trạng thái: $rawItemStatus"
                    val statusColor = when {
                        isItemConfirmed -> "#16A34A"
                        isItemRejected -> "#DC2626"
                        else -> "#D97706"
                    }
                    subBinding.tvSubProductStatus.setTextColor(Color.parseColor(statusColor))
                } else {
                    subBinding.tvSubProductStatus.visibility = View.GONE
                }

                subBinding.imgSubProduct.loadImageWithGlide(item.productImg, maxWidth = 150, maxHeight = 150)
                
                val hideMenu = isFinalState || isItemConfirmed || isItemRejected
                if (hideMenu) {
                    subBinding.ivSubItemMenu.visibility = View.GONE
                } else {
                    subBinding.ivSubItemMenu.visibility = View.VISIBLE
                    subBinding.ivSubItemMenu.setOnClickListener { view ->
                        val popup = PopupMenu(view.context, view)
                        popup.menu.add(0, 1, 0, "Xác nhận")
                        popup.menu.add(0, 2, 1, "Từ chối")
                        popup.setOnMenuItemClickListener { menuItem ->
                            when (menuItem.itemId) {
                                1 -> {
                                    itemStatusMap[itemKey] = "Đã xác nhận"
                                    onSubItemAction(order, item, "Xác nhận")
                                    val pos = bindingAdapterPosition
                                    if (pos != RecyclerView.NO_POSITION) {
                                        notifyItemChanged(pos)
                                    }
                                    true
                                }
                                2 -> {
                                    itemStatusMap[itemKey] = "Đã từ chối"
                                    onSubItemAction(order, item, "Từ chối")
                                    val pos = bindingAdapterPosition
                                    if (pos != RecyclerView.NO_POSITION) {
                                        notifyItemChanged(pos)
                                    }
                                    true
                                }
                                else -> false
                            }
                        }
                        popup.show()
                    }
                }

                binding.layoutOrderItemsContainer.addView(subBinding.root)
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
