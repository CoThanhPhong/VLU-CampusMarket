package com.example.vlu_campusmarket.ui.admin

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.vlu_campusmarket.R
import com.example.vlu_campusmarket.data.local.AppDatabase
import com.example.vlu_campusmarket.data.models.UserModel
import com.example.vlu_campusmarket.data.remote.RetrofitClient
import com.example.vlu_campusmarket.databinding.FragmentAdminUserBinding
import com.example.vlu_campusmarket.ui.fragment.ProfileFragment
import com.example.vlu_campusmarket.utils.SessionManager
import kotlinx.coroutines.launch

class AdminUserFragment : Fragment() {

    private var _binding: FragmentAdminUserBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AdminViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val context = requireContext()
                val db = AppDatabase.getDatabase(context)
                val apiService = RetrofitClient.getInstance(context)

                @Suppress("UNCHECKED_CAST")
                return AdminViewModel(apiService, db.productDao()) as T
            }
        }
    }

    private lateinit var userAdapter: AdminUserAdapter
    private var currentFilteredUsers: List<UserModel> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminUserBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as? AppCompatActivity)?.supportActionBar?.title = "Quản lý tài khoản"

        setupRecyclerView()
        setupListeners()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        userAdapter = AdminUserAdapter(
            onSelectionChanged = { count ->
                binding.tvSelectedUsersCount.text = "Đã chọn: $count"
                binding.cbSelectAllUsers.isChecked = count > 0 && count == userAdapter.itemCount
            },
            onDeleteUser = { userId ->
                showDeleteConfirmDialog(userId)
            },
            onChangeRole = { user ->
                showChangeRoleDialog(user)
            },
            onItemClick = { user ->
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragmentOutlet, ProfileFragment.newInstance(user.id))
                    .addToBackStack(null)
                    .commit()
            }
        )
        binding.rvAdminUsers.layoutManager = LinearLayoutManager(requireContext())
        binding.rvAdminUsers.adapter = userAdapter
    }

    private fun showChangeRoleDialog(user: UserModel) {
        val currentUser = SessionManager(requireContext()).fetchUser()
        val currentLoggedInUserId = currentUser?.id
        if (!currentLoggedInUserId.isNullOrEmpty() && user.id.equals(currentLoggedInUserId, ignoreCase = true)) {
            Toast.makeText(requireContext(), "Không thể tự thay đổi role của chính tài khoản đang đăng nhập", Toast.LENGTH_SHORT).show()
            return
        }

        val roles = arrayOf("user", "admin")
        val currentRoleIndex = if (user.role.equals("admin", ignoreCase = true)) 1 else 0
        var selectedRole = user.role

        AlertDialog.Builder(requireContext())
            .setTitle("Đổi Role cho ${user.profile?.name ?: user.mssv}")
            .setSingleChoiceItems(roles, currentRoleIndex) { _, which ->
                selectedRole = roles[which]
            }
            .setPositiveButton("Lưu") { dialog, _ ->
                viewModel.updateUserRole(user.id, selectedRole) { _, msg ->
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun showBatchChangeRoleDialog(selectedIds: List<String>) {
        if (selectedIds.isEmpty()) {
            Toast.makeText(requireContext(), "Vui lòng chọn tài khoản cần đổi role", Toast.LENGTH_SHORT).show()
            return
        }

        val currentUser = SessionManager(requireContext()).fetchUser()
        val currentLoggedInUserId = currentUser?.id
        if (currentLoggedInUserId != null && selectedIds.contains(currentLoggedInUserId)) {
            Toast.makeText(requireContext(), "Không thể thay đổi role của chính tài khoản đang đăng nhập trong danh sách chọn", Toast.LENGTH_LONG).show()
            return
        }

        val roles = arrayOf("user", "admin")
        var selectedRole = "user"

        AlertDialog.Builder(requireContext())
            .setTitle("Đổi Role hàng loạt (${selectedIds.size} tài khoản)")
            .setSingleChoiceItems(roles, 0) { _, which ->
                selectedRole = roles[which]
            }
            .setPositiveButton("Lưu") { dialog, _ ->
                viewModel.updateUserRoleBatch(selectedIds, selectedRole) { _, msg ->
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                    userAdapter.clearSelection()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun showDeleteConfirmDialog(userId: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Xóa tài khoản")
            .setMessage("Bạn có chắc chắn muốn xóa tài khoản sinh viên này khỏi hệ thống không?")
            .setPositiveButton("Xóa") { dialog, _ ->
                viewModel.deleteUser(userId) { _, msg ->
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Hủy") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun setupListeners() {
        binding.swipeRefreshAdminUsers.setOnRefreshListener {
            userAdapter.clearSelection()
            viewModel.refreshAllData()
        }

        binding.etSearchAdminUsers.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.setUserSearchQuery(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.cbSelectAllUsers.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                userAdapter.selectAll(currentFilteredUsers.map { it.id })
            } else if (userAdapter.getSelectedIds().size == userAdapter.itemCount) {
                userAdapter.clearSelection()
            }
        }

        binding.btnBatchChangeRole.setOnClickListener {
            val selected = userAdapter.getSelectedIds()
            showBatchChangeRoleDialog(selected)
        }

        binding.btnBatchDeleteUsers.setOnClickListener {
            val selected = userAdapter.getSelectedIds()
            if (selected.isEmpty()) {
                Toast.makeText(requireContext(), "Vui lòng chọn tài khoản cần xóa", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val currentUser = SessionManager(requireContext()).fetchUser()
            val currentLoggedInUserId = currentUser?.id
            if (currentLoggedInUserId != null && selected.contains(currentLoggedInUserId)) {
                Toast.makeText(requireContext(), "Không thể xóa chính tài khoản đang đăng nhập", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            AlertDialog.Builder(requireContext())
                .setTitle("Xóa hàng loạt")
                .setMessage("Bạn có chắc chắn muốn xóa ${selected.size} tài khoản đã chọn?")
                .setPositiveButton("Xóa") { dialog, _ ->
                    viewModel.deleteUsersBatch(selected) { _, msg ->
                        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                        userAdapter.clearSelection()
                    }
                    dialog.dismiss()
                }
                .setNegativeButton("Hủy") { dialog, _ -> dialog.dismiss() }
                .show()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.filteredAdminUsers.collect { users ->
                        val currentUser = SessionManager(requireContext()).fetchUser()
                        val currentId = currentUser?.id

                        userAdapter.setCurrentUserId(currentId)
                        currentFilteredUsers = users
                        userAdapter.submitList(users)
                        binding.tvEmptyAdminUsers.visibility = if (users.isEmpty()) View.VISIBLE else View.GONE
                        binding.rvAdminUsers.visibility = if (users.isEmpty()) View.GONE else View.VISIBLE
                    }
                }

                launch {
                    viewModel.isLoading.collect { loading ->
                        binding.swipeRefreshAdminUsers.isRefreshing = loading
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
