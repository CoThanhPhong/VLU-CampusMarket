package com.example.vlu_campusmarket.ui.fragment

import android.R
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.vlu_campusmarket.data.local.AppDatabase
import com.example.vlu_campusmarket.data.remote.RetrofitClient
import com.example.vlu_campusmarket.databinding.FragmentCreatepostBinding
import com.example.vlu_campusmarket.ui.product.CreatePostViewModel
import com.example.vlu_campusmarket.utils.loadImageWithGlide
import kotlinx.coroutines.launch

class CreatePostFragment : Fragment() {

    private var _binding: FragmentCreatepostBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CreatePostViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val context = requireContext()
                val db = AppDatabase.getDatabase(context)
                val apiService = RetrofitClient.getInstance(context)

                @Suppress("UNCHECKED_CAST")
                return CreatePostViewModel(apiService, db.productDao(), db.userDao()) as T
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreatepostBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupDropdowns()
        setupListeners()
        observeViewModel()
    }

    private fun setupDropdowns() {
        val deviceTypes = arrayOf("Điện thoại", "Tablet", "Mac", "Laptop")
        val deviceAdapter = ArrayAdapter(requireContext(), R.layout.simple_dropdown_item_1line, deviceTypes)
        binding.actDeviceType.setAdapter(deviceAdapter)
        binding.actDeviceType.setText(deviceTypes[0], false)

        val locals = arrayOf("Cơ sở 1", "Cơ sở 2", "Cơ sở 3")
        val localAdapter = ArrayAdapter(requireContext(), R.layout.simple_dropdown_item_1line, locals)
        binding.actLocal.setAdapter(localAdapter)
        binding.actLocal.setText(locals[0], false)
    }

    private fun setupListeners() {
        binding.etImgUrl.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val url = s?.toString()?.trim()
                binding.imgPreview.loadImageWithGlide(url)
            }
        })

        binding.btnSubmitPost.setOnClickListener {
            val name = binding.etProductName.text.toString().trim()
            val priceStr = binding.etProductPrice.text.toString().trim()
            val deviceType = binding.actDeviceType.text.toString().trim()
            val local = binding.actLocal.text.toString().trim()
            val amountStr = binding.etAmount.text.toString().trim()
            val description = binding.etDescription.text.toString().trim()
            val imgUrl = binding.etImgUrl.text.toString().trim()

            if (name.isEmpty() || priceStr.isEmpty() || amountStr.isEmpty()) {
                Toast.makeText(requireContext(), "Vui lòng nhập đầy đủ tên, giá và số lượng", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val price = priceStr.toDoubleOrNull() ?: 0.0
            val amount = amountStr.toIntOrNull() ?: 1

            binding.btnSubmitPost.isEnabled = false
            binding.btnSubmitPost.text = "Đang tải bài..."

            viewModel.createProduct(
                name = name,
                price = price,
                deviceType = deviceType,
                local = local,
                amount = amount,
                description = description,
                img = imgUrl
            ) { success, msg ->
                binding.btnSubmitPost.isEnabled = true
                binding.btnSubmitPost.text = "ĐĂNG BÀI NGAY"
                Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()

                if (success) {
                    binding.etProductName.text?.clear()
                    binding.etProductPrice.text?.clear()
                    binding.etDescription.text?.clear()
                    binding.etImgUrl.text?.clear()
                }
            }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isLoading.collect { loading ->
                    binding.btnSubmitPost.isEnabled = !loading
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
