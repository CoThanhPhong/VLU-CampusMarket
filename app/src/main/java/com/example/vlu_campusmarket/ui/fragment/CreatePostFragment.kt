package com.example.vlu_campusmarket.ui.fragment

import android.R
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.vlu_campusmarket.data.local.AppDatabase
import com.example.vlu_campusmarket.data.models.ProductModel
import com.example.vlu_campusmarket.data.remote.RetrofitClient
import com.example.vlu_campusmarket.databinding.FragmentCreatepostBinding
import com.example.vlu_campusmarket.databinding.ImageThumbnailItemBinding
import com.example.vlu_campusmarket.ui.product.CreatePostViewModel
import com.example.vlu_campusmarket.utils.loadImageWithGlide
import com.google.gson.Gson
import kotlinx.coroutines.launch

class CreatePostFragment : Fragment() {

    private var _binding: FragmentCreatepostBinding? = null
    private val binding get() = _binding!!

    private var editingProduct: ProductModel? = null
    private val selectedImageUrls = mutableListOf<String>()
    private lateinit var thumbnailAdapter: ImageThumbnailAdapter

    companion object {
        private const val ARG_EDIT_PRODUCT = "arg_edit_product"

        fun newInstance(product: ProductModel): CreatePostFragment {
            val f = CreatePostFragment()
            val b = Bundle()
            b.putString(ARG_EDIT_PRODUCT, Gson().toJson(product))
            f.arguments = b
            return f
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.getString(ARG_EDIT_PRODUCT)?.let { json ->
            editingProduct = Gson().fromJson(json, ProductModel::class.java)
        }
    }

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

    private val pickImagesLauncher = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) {
            val remainingSlots = 5 - selectedImageUrls.size
            if (remainingSlots <= 0) {
                Toast.makeText(requireContext(), "Tối đa chỉ được chọn 5 hình ảnh", Toast.LENGTH_SHORT).show()
                return@registerForActivityResult
            }
            val selectedUris = uris.take(remainingSlots)
            for (uri in selectedUris) {
                uriToBase64(requireContext(), uri)?.let { base64 ->
                    if (selectedImageUrls.size < 5) {
                        selectedImageUrls.add(base64)
                    }
                }
            }
            thumbnailAdapter.notifyDataSetChanged()
        }
    }

    private fun uriToBase64(context: Context, uri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val bytes = inputStream?.readBytes()
            inputStream?.close()
            if (bytes != null) {
                val base64String = Base64.encodeToString(bytes, Base64.NO_WRAP)
                "data:image/jpeg;base64,$base64String"
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
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
        setupRecyclerView()
        setupListeners()
        observeViewModel()

        editingProduct?.let { product ->
            binding.etProductName.setText(product.name)
            binding.etProductPrice.setText(product.price.toInt().toString())
            binding.etAmount.setText(product.amount.toString())
            binding.etDescription.setText(product.description ?: "")
            
            selectedImageUrls.clear()
            selectedImageUrls.addAll(product.imageList)
            thumbnailAdapter.notifyDataSetChanged()

            binding.btnSubmitPost.text = "CẬP NHẬT BÀI ĐĂNG"
        }
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

        editingProduct?.let { product ->
            val mappedDeviceType = when (product.deviceType.lowercase()) {
                "mobile", "phone", "điện thoại" -> "Điện thoại"
                "tablet" -> "Tablet"
                "mac" -> "Mac"
                "laptop" -> "Laptop"
                else -> "Điện thoại"
            }
            if (deviceTypes.contains(mappedDeviceType)) {
                binding.actDeviceType.setText(mappedDeviceType, false)
            }
            if (locals.contains(product.local)) {
                binding.actLocal.setText(product.local, false)
            }
        }
    }

    private fun setupRecyclerView() {
        thumbnailAdapter = ImageThumbnailAdapter(selectedImageUrls) { position ->
            if (position in selectedImageUrls.indices) {
                selectedImageUrls.removeAt(position)
                thumbnailAdapter.notifyDataSetChanged()
            }
        }
        binding.rvImageThumbnails.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = thumbnailAdapter
        }
    }

    private fun setupListeners() {
        binding.btnPickImages.setOnClickListener {
            if (selectedImageUrls.size >= 5) {
                Toast.makeText(requireContext(), "Đã đạt tối đa 5 hình ảnh", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            pickImagesLauncher.launch("image/*")
        }

        binding.btnAddUrl.setOnClickListener {
            val url = binding.etImgUrl.text.toString().trim()
            if (url.isEmpty()) {
                Toast.makeText(requireContext(), "Vui lòng nhập link URL hình ảnh", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (selectedImageUrls.size >= 5) {
                Toast.makeText(requireContext(), "Tối đa chỉ được chọn 5 hình ảnh", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            selectedImageUrls.add(url)
            thumbnailAdapter.notifyDataSetChanged()
            binding.etImgUrl.text?.clear()
        }

        binding.btnSubmitPost.setOnClickListener {
            val name = binding.etProductName.text.toString().trim()
            val priceStr = binding.etProductPrice.text.toString().trim()
            val deviceType = binding.actDeviceType.text.toString().trim()
            val local = binding.actLocal.text.toString().trim()
            val amountStr = binding.etAmount.text.toString().trim()
            val description = binding.etDescription.text.toString().trim()

            if (name.isEmpty() || priceStr.isEmpty() || amountStr.isEmpty()) {
                Toast.makeText(requireContext(), "Vui lòng nhập đầy đủ tên, giá và số lượng", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (selectedImageUrls.isEmpty()) {
                Toast.makeText(requireContext(), "Vui lòng thêm ít nhất 1 hình ảnh cho sản phẩm", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val price = priceStr.toDoubleOrNull() ?: 0.0
            val amount = amountStr.toIntOrNull() ?: 1

            binding.btnSubmitPost.isEnabled = false
            binding.btnSubmitPost.text = "Đang xử lý..."

            if (editingProduct != null) {
                viewModel.updateProduct(
                    id = editingProduct!!.safeId,
                    name = name,
                    price = price,
                    deviceType = deviceType,
                    local = local,
                    amount = amount,
                    description = description,
                    img = selectedImageUrls.toList()
                ) { success, msg ->
                    binding.btnSubmitPost.isEnabled = true
                    binding.btnSubmitPost.text = "CẬP NHẬT BÀI ĐĂNG"
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
                    if (success) {
                        parentFragmentManager.popBackStack()
                    }
                }
            } else {
                viewModel.createProduct(
                    name = name,
                    price = price,
                    deviceType = deviceType,
                    local = local,
                    amount = amount,
                    description = description,
                    img = selectedImageUrls.toList()
                ) { success, msg ->
                    binding.btnSubmitPost.isEnabled = true
                    binding.btnSubmitPost.text = "ĐĂNG BÀI NGAY"
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()

                    if (success) {
                        binding.etProductName.text?.clear()
                        binding.etProductPrice.text?.clear()
                        binding.etDescription.text?.clear()
                        binding.etImgUrl.text?.clear()
                        selectedImageUrls.clear()
                        thumbnailAdapter.notifyDataSetChanged()
                    }
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

    inner class ImageThumbnailAdapter(
        private val images: MutableList<String>,
        private val onRemove: (Int) -> Unit
    ) : RecyclerView.Adapter<ImageThumbnailAdapter.ViewHolder>() {

        inner class ViewHolder(val bindingItem: ImageThumbnailItemBinding) : RecyclerView.ViewHolder(bindingItem.root) {
            fun bind(url: String, position: Int) {
                bindingItem.ivThumbnail.loadImageWithGlide(url)
                bindingItem.btnRemoveImage.setOnClickListener {
                    onRemove(position)
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val itemBinding = ImageThumbnailItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(itemBinding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(images[position], position)
        }

        override fun getItemCount(): Int = images.size
    }
}
