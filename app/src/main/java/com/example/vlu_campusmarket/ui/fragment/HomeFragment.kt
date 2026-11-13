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
import androidx.recyclerview.widget.RecyclerView
import com.example.vlu_campusmarket.R
import com.example.vlu_campusmarket.data.local.AppDatabase
import com.example.vlu_campusmarket.data.remote.RetrofitClient
import com.example.vlu_campusmarket.data.repository.ProductRepository
import com.example.vlu_campusmarket.databinding.FragmentHomeBinding
import com.example.vlu_campusmarket.ui.adapter.ProductAdapter
import com.example.vlu_campusmarket.ui.product.ProductDetailFragment
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

    override fun onResume() {
        super.onResume()
        viewModel.refreshData()
    }

    private fun setupRecyclerView() {
        productAdapter = ProductAdapter(
<<<<<<< HEAD
            userNamesProvider = { viewModel.userNames.value },
=======
            userNamesMap = viewModel.userNames.value,
            userAvatarsMap = viewModel.userAvatars.value,
>>>>>>> features/optimize-leakCanary
            onRequestFetchUser = { userId ->
                viewModel.fetchUserNameIfNeeded(userId)
            },
            onAuthorClick = { userId ->
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragmentOutlet, ProfileFragment.newInstance(userId))
                    .addToBackStack(null)
                    .commit()
            },
            onProductClick = { product ->
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragmentOutlet, ProductDetailFragment.newInstance(product))
                    .addToBackStack(null)
                    .commit()
            }
        )

        binding.recyclerViewProducts.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.recyclerViewProducts.adapter = productAdapter

        binding.recyclerViewProducts.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = recyclerView.layoutManager as? GridLayoutManager ?: return
                val totalItemCount = layoutManager.itemCount
                val lastVisibleItem = layoutManager.findLastVisibleItemPosition()
                if (totalItemCount > 0 && totalItemCount <= lastVisibleItem + 4) {
                    viewModel.loadNextPage()
                }
            }
        })
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
                    R.id.chipPhone -> "Mobile"
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

                launch {
                    viewModel.filteredProducts.collect { products ->
                        productAdapter.submitList(products)
                        binding.tvTitle.text = "Danh sách bài đăng bán (${products.size})"
                    }
                }

                launch {
                    viewModel.isRefreshing.collect { refreshing ->
                        binding.swipeRefreshLayout.isRefreshing = refreshing
                    }
                }

<<<<<<< HEAD
                // Hiển thị lỗi đồng bộ (mất kết nối server...) thay vì im lặng
                launch {
                    viewModel.messages.collect { msg ->
                        Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
                    }
                }

                // Cập nhật lại adapter khi có tên người dùng mới tải về từ API
=======
>>>>>>> features/optimize-leakCanary
                launch {
                    viewModel.userNames.collect { names ->
                        productAdapter.updateUserNames(names)
                    }
                }

                launch {
                    viewModel.userAvatars.collect { avatars ->
                        productAdapter.updateUserAvatars(avatars)
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
