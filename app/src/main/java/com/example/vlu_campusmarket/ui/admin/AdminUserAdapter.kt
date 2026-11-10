package com.example.vlu_campusmarket.ui.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.vlu_campusmarket.data.models.UserModel
import com.example.vlu_campusmarket.databinding.AdminUserItemBinding

class AdminUserAdapter(
    private val onDeleteUser: (String) -> Unit
) : ListAdapter<UserModel, AdminUserAdapter.AdminUserViewHolder>(UserDiffCallback()) {

    inner class AdminUserViewHolder(val binding: AdminUserItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(user: UserModel) {
            binding.tvUserName.text = user.profile?.name ?: "Sinh viên VLU"
            binding.tvUserMssv.text = "MSSV: ${user.mssv} (${user.role.uppercase()})"
            binding.tvUserEmail.text = user.email

            binding.btnDeleteUser.setOnClickListener {
                onDeleteUser(user.id)
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
