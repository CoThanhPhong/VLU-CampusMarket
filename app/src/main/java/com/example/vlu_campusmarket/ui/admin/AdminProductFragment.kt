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
import android.widget.EditText
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.vlu_campusmarket.R
import com.example.vlu_campusmarket.data.local.AppDatabase
import com.example.vlu_campusmarket.data.models.ProductModel
import com.example.vlu_campusmarket.data.remote.RetrofitClient
import com.example.vlu_campusmarket.databinding.FragmentAdminProductBinding
import kotlinx.coroutines.launch
import java.math.BigDecimal

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
        (activity as? AppCompatActivity)?.supportActionBar?.title = "Quản lý sản phẩm"

        setupRecyclerView()
        setupListeners()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        productAdapter = AdminProductAdapter(
            onSelectionChanged = { count ->
                binding.tvSelectedCount.text = "Đã chọn: $count"
                binding.cbSelectAll.isChecked = count > 0 && count == productAdapter.itemCount
            },
            onEditProduct = { product ->
                showEditProductDialog(product)
            },
            onApproveProduct = { product ->
                viewModel.updateProductStatusBatch(listOf(product.safeId), "Còn hàng") { _, msg ->
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                }
            },
            onRejectProduct = { product ->
                viewModel.updateProductStatusBatch(listOf(product.safeId), "Bị từ chối") { _, msg ->
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                }
            },
            onDeleteProduct = { product ->
                AlertDialog.Builder(requireContext())
                    .setTitle("Xóa sản phẩm")
                    .setMessage("Bạn có chắc chắn muốn xóa sản phẩm này không?")
                    .setPositiveButton("Xóa") { dialog, _ ->
                        viewModel.deleteProduct(product.safeId) { _, msg ->
                            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                        }
                        dialog.dismiss()
                    }
                    .setNegativeButton("Hủy", null)
                    .show()
            }
        )
        binding.rvAdminProducts.layoutManager = LinearLayoutManager(requireContext())
        binding.rvAdminProducts.adapter = productAdapter
    }

    private fun showEditProductDialog(product: ProductModel) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_product, null)
        val etName = dialogView.findViewById<EditText>(R.id.etEditProductName)
        val etPrice = dialogView.findViewById<EditText>(R.id.etEditProductPrice)
        val etDeviceType = dialogView.findViewById<EditText>(R.id.etEditProductDeviceType)
        val etStatus = dialogView.findViewById<EditText>(R.id.etEditProductStatus)
        val etDescription = dialogView.findViewById<EditText>(R.id.etEditProductDescription)

        etName.setText(product.name)
        etPrice.setText(BigDecimal.valueOf(product.price).toPlainString())
        etDeviceType.setText(product.deviceType)
        etStatus.setText(product.status ?: "AVAILABLE")
        etDescription.setText(product.description ?: "")

        AlertDialog.Builder(requireContext())
            .setTitle("Chỉnh sửa sản phẩm")
            .setView(dialogView)
            .setPositiveButton("Lưu") { dialog, _ ->
                val name = etName.text.toString().trim()
                val price = etPrice.text.toString().toDoubleOrNull() ?: product.price
                val deviceType = etDeviceType.text.toString().trim()
                val status = etStatus.text.toString().trim()
                val description = etDescription.text.toString().trim()

                val updatedProduct = product.copy(
                    name = name,
                    price = price,
                    deviceType = if (deviceType.isNotEmpty()) deviceType else product.deviceType,
                    status = if (status.isNotEmpty()) status else product.status,
                    description = description
                )

                viewModel.updateProductDetails(updatedProduct) { success, msg ->
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun setupListeners() {
        binding.swipeRefreshAdminProducts.setOnRefreshListener {
            productAdapter.clearSelection()
            viewModel.refreshAllData()
        }

        binding.etSearchAdminProducts.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.setProductSearchQuery(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })

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
            viewModel.updateProductStatusBatch(selected, "Còn hàng") { success, msg ->
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
            viewModel.updateProductStatusBatch(selected, "Bị từ chối") { success, msg ->
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
                    viewModel.filteredAdminProducts.collect { products ->
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
