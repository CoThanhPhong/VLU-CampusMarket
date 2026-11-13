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
import com.example.vlu_campusmarket.databinding.FragmentAdminOrderBinding
import kotlinx.coroutines.launch

class AdminOrderFragment : Fragment() {

    private var _binding: FragmentAdminOrderBinding? = null
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

    private lateinit var orderAdapter: AdminOrderAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminOrderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupListeners()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        orderAdapter = AdminOrderAdapter { orderId, currentStatus ->
            showChangeStatusDialog(orderId, currentStatus)
        }
        binding.rvAdminOrders.layoutManager = LinearLayoutManager(requireContext())
        binding.rvAdminOrders.adapter = orderAdapter
    }

    private fun showChangeStatusDialog(orderId: String, currentStatus: String) {
        val statuses = arrayOf("Chờ xử lý", "Đã xác nhận", "Đang giao", "Hoàn thành", "Đã hủy")
        AlertDialog.Builder(requireContext())
            .setTitle("Cập nhật trạng thái đơn hàng")
            .setItems(statuses) { dialog, which ->
                val newStatus = statuses[which]
                viewModel.updateOrderStatus(orderId, newStatus) { success, msg ->
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun setupListeners() {
        binding.swipeRefreshAdminOrders.setOnRefreshListener {
            viewModel.refreshAllData()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.allOrders.collect { orders ->
                        orderAdapter.submitList(orders)
                        binding.tvEmptyAdminOrders.visibility = if (orders.isEmpty()) View.VISIBLE else View.GONE
                        binding.rvAdminOrders.visibility = if (orders.isEmpty()) View.GONE else View.VISIBLE
                    }
                }

                launch {
                    viewModel.isLoading.collect { loading ->
                        binding.swipeRefreshAdminOrders.isRefreshing = loading
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
