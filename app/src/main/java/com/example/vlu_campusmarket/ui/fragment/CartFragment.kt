package com.example.vlu_campusmarket.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
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
import com.example.vlu_campusmarket.data.models.ShippingInfoModel
import com.example.vlu_campusmarket.data.remote.RetrofitClient
import com.example.vlu_campusmarket.data.repository.CartRepository
import com.example.vlu_campusmarket.data.repository.OrderRepository
import com.example.vlu_campusmarket.databinding.FragmentCartBinding
import com.example.vlu_campusmarket.ui.adapter.CartAdapter
import com.example.vlu_campusmarket.ui.cart.CartViewModel
import com.example.vlu_campusmarket.ui.product.ProductDetailFragment
import com.example.vlu_campusmarket.utils.SessionManager
import kotlinx.coroutines.launch

class CartFragment : Fragment() {

    private var _binding: FragmentCartBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CartViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val context = requireContext()
                val database = AppDatabase.getDatabase(context)
                val apiService = RetrofitClient.getInstance(context)

                val cartRepository = CartRepository(database.cartDao(), apiService, database.userDao())
                val orderRepository = OrderRepository(database.orderDao(), apiService, database.userDao(), database.productDao())

                @Suppress("UNCHECKED_CAST")
                return CartViewModel(cartRepository, orderRepository, database.productDao()) as T
            }
        }
    }

    private lateinit var cartAdapter: CartAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCartBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as? AppCompatActivity)?.supportActionBar?.title = "Giỏ hàng"

        setupRecyclerView()
        setupListeners()
        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        (activity as? AppCompatActivity)?.supportActionBar?.title = "Giỏ hàng"
        viewModel.refreshCart()
    }

    private fun setupRecyclerView() {
        cartAdapter = CartAdapter(
            onItemChecked = { productId, isChecked ->
                viewModel.toggleItemSelection(productId, isChecked)
            },
            onAuthorChecked = { authorName, isChecked ->
                viewModel.toggleAuthorSelection(authorName, isChecked)
            },
            onQuantityChanged = { productId, newQuantity ->
                viewModel.updateQuantity(productId, newQuantity)
            },
            onDeleteItem = { productId ->
                viewModel.removeItem(productId) { _, msg ->
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                }
            },
            onItemClick = { product ->
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragmentOutlet, ProductDetailFragment.newInstance(product))
                    .addToBackStack(null)
                    .commit()
            }
        )

        binding.rvCartItems.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = cartAdapter
        }
    }

    private fun setupListeners() {
        binding.swipeRefreshCart.setOnRefreshListener {
            viewModel.refreshCart()
        }

        binding.cbSelectAllCart.setOnCheckedChangeListener { _, isChecked ->
            viewModel.toggleSelectAll(isChecked)
        }

        binding.btnDeleteSelected.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Xóa sản phẩm")
                .setMessage("Bạn có chắc chắn muốn xóa các sản phẩm đã chọn khỏi giỏ hàng không?")
                .setPositiveButton("Xóa") { dialog, _ ->
                    viewModel.deleteSelectedItems { _, msg ->
                        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                    }
                    dialog.dismiss()
                }
                .setNegativeButton("Hủy", null)
                .show()
        }

        binding.btnCheckout.setOnClickListener {
            showCheckoutDialog()
        }
    }

    private fun showCheckoutDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_checkout_shipping, null)
        val etFullName = dialogView.findViewById<EditText>(R.id.etFullName)
        val etPhone = dialogView.findViewById<EditText>(R.id.etPhone)
        val actCampus = dialogView.findViewById<AutoCompleteTextView>(R.id.actCampus)

        val campuses = arrayOf("Cơ sở 1", "Cơ sở 2", "Cơ sở 3")
        val campusAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, campuses)
        actCampus?.setAdapter(campusAdapter)
        actCampus?.setText(campuses[0], false)

        val sessionUser = SessionManager(requireContext()).fetchUser()
        if (sessionUser != null) {
            if (etFullName != null && etFullName.text.isEmpty()) {
                etFullName.setText(sessionUser.profile?.name ?: "")
            }
            if (etPhone != null && etPhone.text.isEmpty()) {
                etPhone.setText(sessionUser.profile?.phone ?: "")
            }
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Xác nhận Đặt hàng")
            .setView(dialogView)
            .setPositiveButton("Thanh toán COD", null)
            .setNegativeButton("Hủy", null)
            .create()

        dialog.setOnShowListener {
            val btnPositive = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            btnPositive.setOnClickListener {
                val name = etFullName?.text?.toString()?.trim() ?: ""
                val phone = etPhone?.text?.toString()?.trim() ?: ""
                val campus = actCampus?.text?.toString()?.trim()?.ifEmpty { "Cơ sở 1" } ?: "Cơ sở 1"

                if (name.isEmpty() || phone.isEmpty()) {
                    Toast.makeText(requireContext(), "Vui lòng điền đầy đủ thông tin giao hàng", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val shippingInfo = ShippingInfoModel(
                    fullName = name,
                    phone = phone,
                    local = campus
                )

                viewModel.checkout(shippingInfo, "COD") { _, message ->
                    Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
                }
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                launch {
                    viewModel.cartUiRows.collect { rows ->
                        cartAdapter.submitList(rows)
                        val isEmpty = rows.isEmpty()
                        binding.tvEmptyCart.visibility = if (isEmpty) View.VISIBLE else View.GONE
                        binding.rvCartItems.visibility = if (isEmpty) View.GONE else View.VISIBLE
                    }
                }

                launch {
                    viewModel.totalItemCount.collect { count ->
                        binding.tvCartTitleCount.text = "Giỏ Hàng Của Tôi ($count)"
                    }
                }

                launch {
                    viewModel.selectedCount.collect { count ->
                        binding.tvSelectedCount.text = "Đã chọn: $count sản phẩm"
                        binding.btnCheckout.text = "Mua Hàng ($count)"
                        binding.btnDeleteSelected.visibility = if (count > 0) View.VISIBLE else View.GONE
                    }
                }

                launch {
                    viewModel.isAllSelected.collect { allSelected ->
                        binding.cbSelectAllCart.isChecked = allSelected
                    }
                }

                launch {
                    viewModel.totalPrice.collect { price ->
                        val formattedPrice = String.format("%,d", price.toLong()).replace(',', '.')
                        binding.tvTotalPrice.text = "$formattedPrice \u20ab"
                    }
                }

                launch {
                    viewModel.isLoading.collect { loading ->
                        binding.swipeRefreshCart.isRefreshing = loading
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
