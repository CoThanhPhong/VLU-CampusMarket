package com.example.vlu_campusmarket.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
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
import com.example.vlu_campusmarket.data.remote.RetrofitClient
import com.example.vlu_campusmarket.data.repository.OrderRepository
import com.example.vlu_campusmarket.databinding.FragmentOrderBinding
import com.example.vlu_campusmarket.ui.adapter.OrderAdapter
import com.example.vlu_campusmarket.ui.orders.OrderViewModel
import com.example.vlu_campusmarket.ui.product.ProductDetailFragment
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
                val repository = OrderRepository(db.orderDao(), apiService, db.userDao(), db.productDao())

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

    override fun onResume() {
        super.onResume()
        (activity as? AppCompatActivity)?.supportActionBar?.title = "Hóa đơn của tôi"
    }

    private fun setupRecyclerView() {
        orderAdapter = OrderAdapter(
            userNamesMap = emptyMap(),
            onRequestFetchUser = { userId ->
                viewModel.fetchUserNameIfNeeded(userId)
            },
            onCancelOrder = { orderId ->
                showCancelConfirmDialog(orderId)
            },
            onProductClick = { product ->
                if (product.safeId.isEmpty() || (product.price <= 0 && product.name.startsWith("Sản phẩm ("))) {
                    Toast.makeText(requireContext(), "Sản phẩm không tồn tại hoặc đã bị xóa!", Toast.LENGTH_SHORT).show()
                    return@OrderAdapter
                }
                lifecycleScope.launch {
                    try {
                        val db = AppDatabase.getDatabase(requireContext())
                        val apiService = RetrofitClient.getInstance(requireContext())
                        val localProd = db.productDao().getProductById(product.safeId)
                        val res = apiService.getProductById(product.safeId)

                        if (localProd == null && (!res.isSuccessful || res.body()?.data == null)) {
                            Toast.makeText(requireContext(), "Sản phẩm không tồn tại hoặc đã bị xóa!", Toast.LENGTH_SHORT).show()
                        } else {
                            val prodToOpen = res.body()?.data ?: product
                            requireActivity().supportFragmentManager.beginTransaction()
                                .replace(R.id.fragmentOutlet, ProductDetailFragment.newInstance(prodToOpen))
                                .addToBackStack(null)
                                .commit()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(requireContext(), "Sản phẩm không tồn tại hoặc đã bị xóa!", Toast.LENGTH_SHORT).show()
                    }
                }
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
                    viewModel.notificationEvent.collect { message ->
                        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
                    }
                }

                launch {
                    viewModel.userNames.collect { names ->
                        orderAdapter.updateUserNames(names)
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
