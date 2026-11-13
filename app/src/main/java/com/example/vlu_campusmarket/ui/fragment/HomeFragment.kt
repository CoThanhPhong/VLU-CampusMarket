package com.example.vlu_campusmarket.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import com.example.vlu_campusmarket.R
import com.example.vlu_campusmarket.data.local.AppDatabase
import com.example.vlu_campusmarket.data.remote.RetrofitClient
import com.example.vlu_campusmarket.data.repository.ProductRepository
import com.example.vlu_campusmarket.databinding.FragmentHomeBinding
import com.example.vlu_campusmarket.ui.adapter.ProductAdapter
import com.example.vlu_campusmarket.ui.product.ProductViewModel
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProductViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val database = AppDatabase.getDatabase(requireContext())
                val apiService = RetrofitClient.getInstance(requireContext())
                val repository = ProductRepository(database.productDao(), apiService)

                @Suppress("UNCHECKED_CAST")
                return ProductViewModel(repository) as T
            }
        }
    }

    private lateinit var productAdapter: ProductAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupListeners()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        productAdapter = ProductAdapter(
            userNamesProvider = { viewModel.userNames.value },
            onRequestFetchUser = { userId ->
                viewModel.fetchUserNameIfNeeded(userId)
            },
            onAuthorClick = { userId ->
                // Xử lý chuyển hướng sang trang Profile cá nhân tại đây nếu cần
            }
        )

        binding.recyclerViewProducts.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.recyclerViewProducts.adapter = productAdapter
    }

    private fun setupListeners() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.refreshData()
        }

        binding.btnSearch.setOnClickListener {
            val query = binding.edtSearch.text.toString().trim()
            viewModel.setSearchQuery(query)
        }

        binding.chipAll.isChecked = true

        binding.chipGroupFilter.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                val category = when (checkedIds.first()) {
                    R.id.chipAll -> "ALL"
                    R.id.chipPhone -> "Điện thoại"
                    R.id.chipTablet -> "Tablet"
                    R.id.chipMac -> "Mac"
                    R.id.chipLaptop -> "Laptop"
                    else -> "ALL"
                }
                viewModel.setCategory(category)
            }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                // Lắng nghe danh sách sản phẩm, tự động cập nhật số lượng (count) vào tvTitle
                launch {
                    viewModel.filteredProducts.collect { products ->
                        productAdapter.submitList(products)
                        binding.tvTitle.text = "Danh sách bài đăng bán (${products.size})"
                    }
                }

                // Lắng nghe trạng thái làm mới (Pull-to-Refresh)
                launch {
                    viewModel.isRefreshing.collect { refreshing ->
                        binding.swipeRefreshLayout.isRefreshing = refreshing
                    }
                }

                // Hiển thị lỗi đồng bộ (mất kết nối server...) thay vì im lặng
                launch {
                    viewModel.messages.collect { msg ->
                        Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
                    }
                }

                // Cập nhật lại adapter khi có tên người dùng mới tải về từ API
                launch {
                    viewModel.userNames.collect { _ ->
                        productAdapter.notifyDataSetChanged()
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