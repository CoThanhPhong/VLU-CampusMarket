package com.example.vlu_campusmarket.ui.fragment

import android.content.Intent
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
import com.example.vlu_campusmarket.databinding.FragmentProfileBinding
import com.example.vlu_campusmarket.ui.adapter.ProductAdapter
import com.example.vlu_campusmarket.ui.login.LoginActivity
import com.example.vlu_campusmarket.ui.product.ProductDetailFragment
import com.example.vlu_campusmarket.ui.profile.ProfileViewModel
import com.example.vlu_campusmarket.utils.SessionManager
import com.example.vlu_campusmarket.utils.loadImageWithGlide
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var productAdapter: ProductAdapter
    private lateinit var sessionManager: SessionManager

    private val viewModel: ProfileViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val context = requireContext()
                val db = AppDatabase.getDatabase(context)
                val apiService = RetrofitClient.getInstance(context)

                @Suppress("UNCHECKED_CAST")
                return ProfileViewModel(db.userDao(), apiService) as T
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        sessionManager = SessionManager(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()

        // 1. Load nhanh thông tin từ Session đã lưu ngay khi mở fragment
        loadUserDataFromSession()

        // 2. Xử lý sự kiện Đăng xuất
        binding.btnLogout.setOnClickListener {
            sessionManager.logout()
            viewModel.logout {
                Toast.makeText(requireContext(), "Đã đăng xuất", Toast.LENGTH_SHORT).show()
                val intent = Intent(requireContext(), LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            }
        }

        observeViewModel()
    }

    private fun setupRecyclerView() {
        productAdapter = ProductAdapter(
            onRequestFetchUser = { _ -> },
            onAuthorClick = { _ -> },
            onProductClick = { product ->
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragmentOutlet, ProductDetailFragment.newInstance(product))
                    .addToBackStack(null)
                    .commit()
            }
        )
        binding.rvUserProducts.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = productAdapter
        }
    }

    private fun loadUserDataFromSession() {
        val userSession = sessionManager.fetchUser()

        // Kiểm tra an toàn chống crash
        if (userSession != null) {
            // Gán thông tin tài khoản cơ bản từ UserModel
            binding.tvMssv.text = "MSSV: ${userSession.mssv.ifEmpty { "---" }}"
            binding.tvEmail.text = "Email: ${userSession.email.ifEmpty { "---" }}"
            binding.tvUserRole.text = userSession.role.uppercase()

            // Gán thông tin chi tiết từ UserProfile bên trong
            userSession.profile?.let { profile ->
                binding.tvUserName.text = profile.name.ifEmpty { "Sinh viên VLU" }
                binding.tvPhone.text = "Số điện thoại: ${profile.phone.ifEmpty { "Chưa cập nhật" }}"
                binding.tvBio.text = "Tiểu sử: ${profile.bio ?: "Chưa có tiểu sử"}"

                // Chuẩn hóa URL avatar tránh lỗi thiếu domain từ server Node.js
                val fixedAvatarUrl = getAbsoluteUrl(profile.avatar)
                binding.imgAvatar.loadImageWithGlide(fixedAvatarUrl)
            }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                launch {
                    viewModel.currentUser.collect { user ->
                        if (user != null) {
                            binding.tvMssv.text = "MSSV: ${user.mssv}"
                            binding.tvEmail.text = "Email: ${user.email}"
                            binding.tvUserRole.text = user.role.uppercase()
                        }
                    }
                }

                launch {
                    viewModel.userDetail.collect { detail ->
                        if (detail != null) {
                            val profile = detail.profile
                            binding.tvUserName.text = profile?.name ?: "Sinh viên VLU"
                            binding.tvPhone.text = "Số điện thoại: ${profile?.phone ?: "Chưa cập nhật"}"
                            binding.tvBio.text = "Tiểu sử: ${profile?.bio ?: "Chưa có tiểu sử"}"

                            // Chuẩn hóa và load ảnh đại diện qua Glide
                            val fixedAvatarUrl = getAbsoluteUrl(profile?.avatar)
                            binding.imgAvatar.loadImageWithGlide(fixedAvatarUrl)
                        }
                    }
                }

                launch {
                    viewModel.userProducts.collect { products ->
                        productAdapter.submitList(products)
                    }
                }
            }
        }
    }

    /**
     * Chuyển đổi đường dẫn ảnh tương đối (VD: /uploads/avatar.jpg) thành tuyệt đối (VD: http://10.0.2.2:5000/uploads/avatar.jpg)
     */
    private fun getAbsoluteUrl(path: String?): String? {
        if (path.isNullOrEmpty()) return null

        var fixedPath = path

        // 1. Nếu là URL từ DiceBear dạng SVG, chuyển sang PNG để Glide đọc được trực tiếp
        if (fixedPath.contains("api.dicebear.com") && fixedPath.contains(".svg")) {
            fixedPath = fixedPath.replace(".svg", ".png")
        }

        // 2. Thay thế khoảng trắng bằng %20 để tránh lỗi URL malformed
        fixedPath = fixedPath.replace(" ", "%20")

        // 3. Xử lý đường dẫn tuyệt đối hoặc tương đối với server Node.js
        if (fixedPath.startsWith("http://") || fixedPath.startsWith("https://")) {
            return fixedPath
        }

        val baseUrl = "http://10.0.2.2:5000/"
        return if (fixedPath.startsWith("/")) {
            baseUrl.removeSuffix("/") + fixedPath
        } else {
            "$baseUrl/$fixedPath"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}