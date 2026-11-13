package com.example.vlu_campusmarket.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.vlu_campusmarket.R
import com.example.vlu_campusmarket.data.local.AppDatabase
import com.example.vlu_campusmarket.data.remote.RetrofitClient
import com.example.vlu_campusmarket.databinding.FragmentAdminDashboardBinding
import kotlinx.coroutines.launch
import java.text.DecimalFormat

class AdminDashboardFragment : Fragment() {

    private var _binding: FragmentAdminDashboardBinding? = null
    private val binding get() = _binding!!

    private val formatter = DecimalFormat("#,###")

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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnManageProducts.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentOutlet, AdminProductFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.btnManageOrders.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentOutlet, AdminOrderFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.btnManageUsers.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentOutlet, AdminUserFragment())
                .addToBackStack(null)
                .commit()
        }

        observeViewModel()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                launch {
                    viewModel.allProducts.collect { products ->
                        binding.tvAdminProductCount.text = products.size.toString()
                    }
                }

                launch {
                    viewModel.allOrders.collect { orders ->
                        binding.tvAdminOrderCount.text = orders.size.toString()
                        val revenue = orders.sumOf { it.totalPrice }
                        binding.tvAdminRevenue.text = "${formatter.format(revenue)} \u20ab"
                    }
                }

                launch {
                    viewModel.allUsers.collect { users ->
                        binding.tvAdminUserCount.text = users.size.toString()
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
