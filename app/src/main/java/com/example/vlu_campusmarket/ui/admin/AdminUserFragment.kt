package com.example.vlu_campusmarket.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.vlu_campusmarket.data.local.AppDatabase
import com.example.vlu_campusmarket.data.remote.RetrofitClient
import com.example.vlu_campusmarket.databinding.FragmentAdminUserBinding
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

        setupRecyclerView()
        setupListeners()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        userAdapter = AdminUserAdapter { userId ->
            showDeleteConfirmDialog(userId)
        }
        binding.rvAdminUsers.layoutManager = LinearLayoutManager(requireContext())
        binding.rvAdminUsers.adapter = userAdapter
    }

    private fun showDeleteConfirmDialog(userId: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Xóa tài khoản")
            .setMessage("Bạn có chắc chắn muốn xóa tài khoản sinh viên này khỏi hệ thống không?")
            .setPositiveButton("Xóa") { dialog, _ ->
                viewModel.deleteUser(userId) { success, msg ->
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
            viewModel.refreshAllData()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.allUsers.collect { users ->
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
