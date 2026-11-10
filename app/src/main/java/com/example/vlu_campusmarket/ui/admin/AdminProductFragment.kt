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
import androidx.recyclerview.widget.GridLayoutManager
import com.example.vlu_campusmarket.data.local.AppDatabase
import com.example.vlu_campusmarket.data.remote.RetrofitClient
import com.example.vlu_campusmarket.databinding.FragmentAdminProductBinding
import kotlinx.coroutines.launch

class AdminProductFragment : Fragment() {

    private var _binding: FragmentAdminProductBinding? = null
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

    private lateinit var productAdapter: AdminProductAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminProductBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupListeners()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        productAdapter = AdminProductAdapter { productId ->
            showDeleteConfirmDialog(productId)
        }
        binding.rvAdminProducts.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.rvAdminProducts.adapter = productAdapter
    }

    private fun showDeleteConfirmDialog(productId: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Xóa bài đăng")
            .setMessage("Bạn có chắc chắn muốn xóa bài đăng này khỏi hệ thống không?")
            .setPositiveButton("Xóa") { dialog, _ ->
                viewModel.deleteProduct(productId) { success, msg ->
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
        binding.swipeRefreshAdminProducts.setOnRefreshListener {
            viewModel.refreshAllData()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.allProducts.collect { products ->
                        productAdapter.submitList(products)
                        binding.tvEmptyAdminProducts.visibility = if (products.isEmpty()) View.VISIBLE else View.GONE
                        binding.rvAdminProducts.visibility = if (products.isEmpty()) View.GONE else View.VISIBLE
                    }
                }

                launch {
                    viewModel.isLoading.collect { loading ->
                        binding.swipeRefreshAdminProducts.isRefreshing = loading
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
