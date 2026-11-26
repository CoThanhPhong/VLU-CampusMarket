package com.example.vlu_campusmarket.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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
import com.example.vlu_campusmarket.data.models.ProductModel
import com.example.vlu_campusmarket.data.remote.RetrofitClient
import com.example.vlu_campusmarket.databinding.FragmentAdminOrderBinding
import com.example.vlu_campusmarket.ui.product.ProductDetailFragment
import com.example.vlu_campusmarket.utils.NotificationHelper
import com.example.vlu_campusmarket.utils.SessionManager
import com.google.gson.Gson
import kotlinx.coroutines.launch
import java.text.DecimalFormat

class AdminOrderFragment : Fragment() {

    private var _binding: FragmentAdminOrderBinding? = null
    private val binding get() = _binding!!

    private val formatter = DecimalFormat("#,###")

    private val viewModel: AdminViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val context = requireContext()
                val db = AppDatabase.getDatabase(context)
                val apiService = RetrofitClient.getInstance(context)
                val sessionManager = SessionManager(context)

                @Suppress("UNCHECKED_CAST")
                return AdminViewModel(apiService, db.productDao(), db.userDao(), sessionManager) as T
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
        (activity as? AppCompatActivity)?.supportActionBar?.title = "Quản lý đơn hàng"

        setupRecyclerView()
        setupListeners()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        orderAdapter = AdminOrderAdapter(
            onConfirmOrder = { orderId ->
                viewModel.updateOrderStatus(orderId, "Đã xác nhận") { _, msg ->
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                }
            },
            onCancelOrder = { orderId ->
                viewModel.updateOrderStatus(orderId, "Đã hủy") { _, msg ->
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                }
            },
            onSubItemAction = { order, item, action ->
                if (action.equals("Xác nhận", ignoreCase = true)) {
                    viewModel.confirmOrderItem(order._id, item.productId) { success, msg ->
                        Toast.makeText(requireContext(), if (success) "Đã xác nhận sản phẩm" else msg, Toast.LENGTH_SHORT).show()
                    }
                } else {
                    viewModel.rejectOrderItem(order._id, item.productId) { success, msg ->
                        Toast.makeText(requireContext(), if (success) "Đã từ chối sản phẩm" else msg, Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onProductClick = { product ->
                if (product.safeId.isEmpty()) {
                    Toast.makeText(requireContext(), "Sản phẩm không có mã hợp lệ!", Toast.LENGTH_SHORT).show()
                    return@AdminOrderAdapter
                }
                lifecycleScope.launch {
                    var prodToOpen: ProductModel? = null

                    try {
                        val apiService = RetrofitClient.getInstance(requireContext())
                        val res = apiService.getProductById(product.safeId)
                        if (res.isSuccessful && res.body()?.data != null) {
                            prodToOpen = res.body()?.data
                        }
                    } catch (_: Exception) {}

                    if (prodToOpen == null) {
                        try {
                            val db = AppDatabase.getDatabase(requireContext())
                            val localEntity = db.productDao().getProductById(product.safeId)
                            if (localEntity != null) {
                                val rawImg: Any = if (localEntity.img.startsWith("[")) {
                                    try {
                                        Gson().fromJson(localEntity.img, Array<String>::class.java).toList()
                                    } catch (_: Exception) { localEntity.img }
                                } else localEntity.img

                                prodToOpen = ProductModel(
                                    _id = localEntity._id,
                                    name = localEntity.name,
                                    description = localEntity.description,
                                    price = localEntity.price,
                                    amount = localEntity.amount,
                                    local = localEntity.local,
                                    img = rawImg,
                                    status = localEntity.status,
                                    deviceType = localEntity.deviceType,
                                    author = localEntity.author
                                )
                            }
                        } catch (_: Exception) {}
                    }

                    if (prodToOpen == null) {
                        prodToOpen = product
                    }

                    requireActivity().supportFragmentManager.beginTransaction()
                        .replace(R.id.fragmentOutlet, ProductDetailFragment.newInstance(prodToOpen))
                        .addToBackStack(null)
                        .commit()
                }
            }
        )
        binding.rvAdminOrders.layoutManager = LinearLayoutManager(requireContext())
        binding.rvAdminOrders.adapter = orderAdapter
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
                        binding.tvAdminOrderCount.text = orders.size.toString()
                        val revenue = orders.sumOf { it.totalPrice }
                        binding.tvAdminRevenue.text = "${formatter.format(revenue)} \u20ab"

                        binding.tvEmptyAdminOrders.visibility = if (orders.isEmpty()) View.VISIBLE else View.GONE
                        binding.rvAdminOrders.visibility = if (orders.isEmpty()) View.GONE else View.VISIBLE
                    }
                }

                launch {
                    viewModel.notificationEvent.collect { message ->
                        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
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
