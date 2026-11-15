package com.example.vlu_campusmarket.ui.admin

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.vlu_campusmarket.data.models.ProductModel
import com.example.vlu_campusmarket.databinding.AdminProductItemBinding
import com.example.vlu_campusmarket.utils.loadImageWithGlide
import java.text.DecimalFormat

class AdminProductAdapter(
    private val onSelectionChanged: (Int) -> Unit,
    private val onEditProduct: (ProductModel) -> Unit,
    private val onApproveProduct: (ProductModel) -> Unit,
    private val onRejectProduct: (ProductModel) -> Unit,
    private val onDeleteProduct: (ProductModel) -> Unit,
    private val onItemClick: ((ProductModel) -> Unit)? = null
) : ListAdapter<ProductModel, AdminProductAdapter.AdminProductViewHolder>(ProductDiffCallback()) {

    private val formatter = DecimalFormat("#,###")
    private val selectedIds = mutableSetOf<String>()

    fun getSelectedIds(): List<String> = selectedIds.toList()

    fun selectAll() {
        selectedIds.clear()
        selectedIds.addAll(currentList.map { it.safeId })
        notifyDataSetChanged()
        onSelectionChanged(selectedIds.size)
    }

    fun clearSelection() {
        selectedIds.clear()
        notifyDataSetChanged()
        onSelectionChanged(0)
    }

    inner class AdminProductViewHolder(val binding: AdminProductItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(product: ProductModel) {
            binding.tvProductName.text = product.name
            binding.tvProductPrice.text = "${formatter.format(product.price)} \u20ab"
            binding.tvDeviceType.text = product.formattedDeviceType
            binding.tvProductLocal.text = if (product.local.startsWith("Cơ sở")) product.local else "Cơ sở: ${product.local}"
            binding.tvProductAmount.text = "Kho: ${product.amount}"

            val rawStatus = product.status?.trim() ?: "Chờ duyệt"
            val statusText = when (rawStatus) {
                "Còn hàng", "AVAILABLE", "APPROVED", "Đã duyệt" -> "Còn hàng"
                "Hết hàng" -> "Hết hàng"
                "Chờ duyệt" -> "Chờ duyệt"
                "Bị từ chối", "REJECTED", "TỪ CHỐI", "Từ chối" -> "Bị từ chối"
                else -> rawStatus
            }
            binding.tvProductStatus.text = statusText
            val statusColor = when (statusText) {
                "Còn hàng" -> "#16A34A"
                "Hết hàng" -> "#D97706"
                "Bị từ chối" -> "#EF4444"
                else -> "#D97706"
            }
            binding.tvProductStatus.setTextColor(Color.parseColor(statusColor))
            binding.tvProductStatus.setBackgroundColor(Color.parseColor(
                when (statusColor) {
                    "#16A34A" -> "#DCFCE7"
                    "#EF4444" -> "#FEE2E2"
                    else -> "#FEF3C7"
                }
            ))

            binding.imgProduct.loadImageWithGlide(product.imageUrl)

            binding.cbSelect.setOnCheckedChangeListener(null)
            binding.cbSelect.isChecked = selectedIds.contains(product.safeId)

            binding.cbSelect.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    selectedIds.add(product.safeId)
                } else {
                    selectedIds.remove(product.safeId)
                }
                onSelectionChanged(selectedIds.size)
            }

            binding.root.setOnClickListener {
                val newState = !selectedIds.contains(product.safeId)
                binding.cbSelect.isChecked = newState
                onItemClick?.invoke(product)
            }

            binding.ivEditProduct.setOnClickListener { view ->
                val popup = PopupMenu(view.context, view)
                popup.menu.add(0, 1, 0, "Sửa")
                popup.menu.add(0, 2, 1, "Duyệt")
                popup.menu.add(0, 3, 2, "Từ chối")
                popup.menu.add(0, 4, 3, "Xóa")
                popup.setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        1 -> { onEditProduct(product); true }
                        2 -> { onApproveProduct(product); true }
                        3 -> { onRejectProduct(product); true }
                        4 -> { onDeleteProduct(product); true }
                        else -> false
                    }
                }
                popup.show()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdminProductViewHolder {
        val binding = AdminProductItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AdminProductViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AdminProductViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ProductDiffCallback : DiffUtil.ItemCallback<ProductModel>() {
        override fun areItemsTheSame(oldItem: ProductModel, newItem: ProductModel): Boolean {
            return oldItem.safeId == newItem.safeId
        }

        override fun areContentsTheSame(oldItem: ProductModel, newItem: ProductModel): Boolean {
            return oldItem == newItem
        }
    }
}
