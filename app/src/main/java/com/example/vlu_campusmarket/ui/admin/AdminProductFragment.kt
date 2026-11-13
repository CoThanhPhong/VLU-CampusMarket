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
        productAdapter = AdminProductAdapter(
            onSelectionChanged = { count ->
                binding.tvSelectedCount.text = "Đã chọn: $count"
                binding.cbSelectAll.isChecked = count > 0 && count == productAdapter.itemCount
            }
        )
        binding.rvAdminProducts.layoutManager = LinearLayoutManager(requireContext())
        binding.rvAdminProducts.adapter = productAdapter
    }

    private fun setupListeners() {
        binding.swipeRefreshAdminProducts.setOnRefreshListener {
            productAdapter.clearSelection()
            viewModel.refreshAllData()
        }

        binding.cbSelectAll.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                productAdapter.selectAll()
            } else if (productAdapter.getSelectedIds().size == productAdapter.itemCount) {
                productAdapter.clearSelection()
            }
        }

        binding.btnBatchApprove.setOnClickListener {
            val selected = productAdapter.getSelectedIds()
            if (selected.isEmpty()) {
                Toast.makeText(requireContext(), "Vui lòng chọn sản phẩm cần duyệt", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewModel.updateProductStatusBatch(selected, "APPROVED") { success, msg ->
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                if (success) productAdapter.clearSelection()
            }
        }

        binding.btnBatchReject.setOnClickListener {
            val selected = productAdapter.getSelectedIds()
            if (selected.isEmpty()) {
                Toast.makeText(requireContext(), "Vui lòng chọn sản phẩm cần từ chối", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewModel.updateProductStatusBatch(selected, "REJECTED") { success, msg ->
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                if (success) productAdapter.clearSelection()
            }
        }

        binding.btnBatchDelete.setOnClickListener {
            val selected = productAdapter.getSelectedIds()
            if (selected.isEmpty()) {
                Toast.makeText(requireContext(), "Vui lòng chọn sản phẩm cần xóa", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            AlertDialog.Builder(requireContext())
                .setTitle("Xóa hàng loạt")
                .setMessage("Bạn có chắc chắn muốn xóa ${selected.size} sản phẩm đã chọn?")
                .setPositiveButton("Xóa") { dialog, _ ->
                    viewModel.deleteProductsBatch(selected) { success, msg ->
                        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                        if (success) productAdapter.clearSelection()
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
