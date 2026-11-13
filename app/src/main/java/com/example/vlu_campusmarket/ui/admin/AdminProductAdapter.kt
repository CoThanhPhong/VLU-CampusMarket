package com.example.vlu_campusmarket.ui.admin

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.vlu_campusmarket.data.models.ProductModel
import com.example.vlu_campusmarket.databinding.AdminProductItemBinding
import com.example.vlu_campusmarket.utils.loadImageWithGlide
import java.text.DecimalFormat

class AdminProductAdapter(
    private val onSelectionChanged: (Int) -> Unit,
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
            binding.tvDeviceType.text = product.deviceType
            binding.tvProductLocal.text = if (product.local.startsWith("Cơ sở")) product.local else "Cơ sở: ${product.local}"

            val statusText = when (product.status?.uppercase()) {
                "AVAILABLE", "CÒN HÀNG", "APPROVED" -> "Đã duyệt / Còn hàng"
                "REJECTED", "TỪ CHỐI" -> "Từ chối"
                else -> "Chờ duyệt"
            }
            binding.tvProductStatus.text = statusText
            val statusColor = when (product.status?.uppercase()) {
                "AVAILABLE", "CÒN HÀNG", "APPROVED" -> "#16A34A"
                "REJECTED", "TỪ CHỐI" -> "#EF4444"
                else -> "#D97706"
            }
            binding.tvProductStatus.setTextColor(Color.parseColor(statusColor))
            binding.tvProductStatus.setBackgroundColor(Color.parseColor(if (statusColor == "#16A34A") "#DCFCE7" else if (statusColor == "#EF4444") "#FEE2E2" else "#FEF3C7"))

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
