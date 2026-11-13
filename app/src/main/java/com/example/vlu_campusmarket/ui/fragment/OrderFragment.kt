package com.example.vlu_campusmarket.ui.fragment

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
import com.example.vlu_campusmarket.data.repository.OrderRepository
import com.example.vlu_campusmarket.databinding.FragmentOrderBinding
import com.example.vlu_campusmarket.ui.adapter.OrderAdapter
import com.example.vlu_campusmarket.ui.orders.OrderViewModel
import kotlinx.coroutines.launch

class OrderFragment : Fragment() {

    private var _binding: FragmentOrderBinding? = null
    private val binding get() = _binding!!

    private val viewModel: OrderViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val context = requireContext()
                val db = AppDatabase.getDatabase(context)
                val apiService = RetrofitClient.getInstance(context)
                val repository = OrderRepository(db.orderDao(), apiService, db.userDao())

                @Suppress("UNCHECKED_CAST")
                return OrderViewModel(repository) as T
            }
        }
    }

    private lateinit var orderAdapter: OrderAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOrderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupListeners()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        orderAdapter = OrderAdapter(
            onCancelOrder = { orderId ->
                showCancelConfirmDialog(orderId)
            }
        )

        binding.rvOrders.layoutManager = LinearLayoutManager(requireContext())
        binding.rvOrders.adapter = orderAdapter
    }

    private fun showCancelConfirmDialog(orderId: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Hủy đơn hàng")
            .setMessage("Bạn có chắc chắn muốn hủy đơn hàng này không?")
            .setPositiveButton("Hủy đơn") { dialog, _ ->
                viewModel.cancelOrder(orderId) { success, msg ->
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Quay lại") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun setupListeners() {
        binding.swipeRefreshOrders.setOnRefreshListener {
            viewModel.refreshOrders()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                launch {
                    viewModel.orders.collect { list ->
                        orderAdapter.submitList(list)
                        binding.tvEmptyOrders.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                        binding.rvOrders.visibility = if (list.isEmpty()) View.GONE else View.VISIBLE
                    }
                }

                launch {
                    viewModel.isRefreshing.collect { refreshing ->
                        binding.swipeRefreshOrders.isRefreshing = refreshing
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
