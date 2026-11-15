package com.example.vlu_campusmarket.ui.admin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.vlu_campusmarket.data.models.UserModel
import com.example.vlu_campusmarket.databinding.AdminUserItemBinding

class AdminUserAdapter(
    private val onSelectionChanged: (Int) -> Unit,
    private val onDeleteUser: (String) -> Unit,
    private val onChangeRole: (UserModel) -> Unit,
    private val onItemClick: (UserModel) -> Unit
) : ListAdapter<UserModel, AdminUserAdapter.AdminUserViewHolder>(UserDiffCallback()) {

    private val selectedIds = mutableSetOf<String>()
    private var currentUserId: String? = null

    fun setCurrentUserId(id: String?) {
        if (this.currentUserId == id) return
        this.currentUserId = id
        notifyDataSetChanged()
    }

    fun getSelectedIds(): List<String> = selectedIds.toList()

    fun selectAll(allUserIds: List<String>) {
        selectedIds.clear()
        selectedIds.addAll(allUserIds.filter { it != currentUserId })
        notifyDataSetChanged()
        onSelectionChanged(selectedIds.size)
    }

    fun clearSelection() {
        selectedIds.clear()
        notifyDataSetChanged()
        onSelectionChanged(0)
    }

    inner class AdminUserViewHolder(val binding: AdminUserItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(user: UserModel) {
            val isSelf = !currentUserId.isNullOrEmpty() && user.id.equals(currentUserId, ignoreCase = true)

            binding.tvUserName.text = if (isSelf) "${user.profile?.name ?: "Sinh viên VLU"} (Tài khoản của bạn)" else (user.profile?.name ?: "Sinh viên VLU")
            binding.tvUserMssv.text = "MSSV: ${user.mssv}"
            binding.tvUserRole.text = "Role: ${user.role.uppercase()}"

            if (isSelf) {
                binding.cbSelect.visibility = View.INVISIBLE
                binding.cbSelect.isChecked = false
                selectedIds.remove(user.id)
                binding.btnChangeRole.visibility = View.GONE
                binding.btnDeleteUser.visibility = View.GONE
            } else {
                binding.cbSelect.visibility = View.VISIBLE
                binding.btnChangeRole.visibility = View.VISIBLE
                binding.btnDeleteUser.visibility = View.VISIBLE

                binding.cbSelect.setOnCheckedChangeListener(null)
                binding.cbSelect.isChecked = selectedIds.contains(user.id)

                binding.cbSelect.setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        selectedIds.add(user.id)
                    } else {
                        selectedIds.remove(user.id)
                    }
                    onSelectionChanged(selectedIds.size)
                }

                binding.btnDeleteUser.setOnClickListener {
                    onDeleteUser(user.id)
                }

                binding.btnChangeRole.setOnClickListener {
                    onChangeRole(user)
                }
            }

            // Click vào item vẫn mở trang thông tin chi tiết
            binding.root.setOnClickListener {
                onItemClick(user)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdminUserViewHolder {
        val binding = AdminUserItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AdminUserViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AdminUserViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class UserDiffCallback : DiffUtil.ItemCallback<UserModel>() {
        override fun areItemsTheSame(oldItem: UserModel, newItem: UserModel): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: UserModel, newItem: UserModel): Boolean {
            return oldItem == newItem
        }
    }
}
