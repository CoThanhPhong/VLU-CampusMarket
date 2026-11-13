package com.example.vlu_campusmarket.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
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
import com.example.vlu_campusmarket.R
import com.example.vlu_campusmarket.data.local.AppDatabase
import com.example.vlu_campusmarket.data.models.ShippingInfoModel
import com.example.vlu_campusmarket.data.remote.RetrofitClient
import com.example.vlu_campusmarket.data.repository.CartRepository
import com.example.vlu_campusmarket.data.repository.OrderRepository
import com.example.vlu_campusmarket.databinding.FragmentCartBinding
import com.example.vlu_campusmarket.ui.adapter.CartAdapter
import com.example.vlu_campusmarket.ui.cart.CartViewModel
import com.example.vlu_campusmarket.utils.SessionManager
import kotlinx.coroutines.launch
import java.text.DecimalFormat

class CartFragment : Fragment() {

    private var _binding: FragmentCartBinding? = null
    private val binding get() = _binding!!

    private val formatter = DecimalFormat("#,###")

    private val viewModel: CartViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val context = requireContext()
                val db = AppDatabase.getDatabase(context)
                val apiService = RetrofitClient.getInstance(context)
                val cartRepository = CartRepository(db.cartDao(), apiService, db.userDao())
                val orderRepository = OrderRepository(db.orderDao(), apiService, db.userDao())

                @Suppress("UNCHECKED_CAST")
                return CartViewModel(cartRepository, orderRepository, db.productDao()) as T
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

        setupRecyclerView()
        setupListeners()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        cartAdapter = CartAdapter(
            onQuantityChanged = { productId, newQty ->
                viewModel.updateQuantity(productId, newQty)
            },
            onDeleteItem = { productId ->
                viewModel.updateQuantity(productId, 0)
            }
        )

        binding.rvCartItems.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCartItems.adapter = cartAdapter
    }

    private fun setupListeners() {
        binding.swipeRefreshCart.setOnRefreshListener {
            viewModel.refreshCart()
        }

        binding.btnCheckout.setOnClickListener {
            if (cartAdapter.itemCount == 0) {
                Toast.makeText(requireContext(), "Giỏ hàng đang trống!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            showCheckoutDialog()
        }
    }

    private fun showCheckoutDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_checkout_shipping, null)
        val etFullName = dialogView.findViewById<EditText>(R.id.etFullName)
        val etPhone = dialogView.findViewById<EditText>(R.id.etPhone)
        val etCampus = dialogView.findViewById<EditText>(R.id.etCampus)

        // Tự động load thông tin tên và SĐT của user trong session
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
                val name = etFullName.text.toString().trim()
                val phone = etPhone.text.toString().trim()
                val campus = etCampus.text.toString().trim().ifEmpty { "Cơ sở 1" }

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
                    viewModel.cartItems.collect { items ->
                        cartAdapter.submitList(items)
                        binding.tvEmptyCart.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
                        binding.rvCartItems.visibility = if (items.isEmpty()) View.GONE else View.VISIBLE
                    }
                }

                launch {
                    viewModel.totalPrice.collect { total ->
                        binding.tvTotalPrice.text = "${formatter.format(total)} \u20ab"
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
