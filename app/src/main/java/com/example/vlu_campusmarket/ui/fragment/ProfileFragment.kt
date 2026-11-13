package com.example.vlu_campusmarket.ui.fragment

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
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

    private var editAvatarBase64: String? = null
    private var editAvatarImageView: ImageView? = null

    companion object {
        private const val ARG_USER_ID = "arg_user_id"

        fun newInstance(userId: String): ProfileFragment {
            val fragment = ProfileFragment()
            val args = Bundle()
            args.putString(ARG_USER_ID, userId)
            fragment.arguments = args
            return fragment
        }
    }

    private val pickAvatarLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            editAvatarBase64 = uriToBase64(requireContext(), uri)
            editAvatarImageView?.loadImageWithGlide(editAvatarBase64, placeholderRes = R.drawable.ic_user_profile, maxWidth = 200, maxHeight = 200)
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

    private val viewModel: ProfileViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val context = requireContext()
                val db = AppDatabase.getDatabase(context)
                val apiService = RetrofitClient.getInstance(context)
                val targetUserId = arguments?.getString(ARG_USER_ID)

                @Suppress("UNCHECKED_CAST")
                return ProfileViewModel(db.userDao(), apiService, targetUserId, db.productDao()) as T
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

        val targetUserId = arguments?.getString(ARG_USER_ID)
        if (targetUserId.isNullOrEmpty()) {
            loadUserDataFromSession()
            binding.btnEditProfile.visibility = View.VISIBLE
            binding.btnEditProfile.setOnClickListener {
                showEditProfileDialog()
            }
        } else {
            binding.btnEditProfile.visibility = View.GONE
        }

        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshProfileData()
    }

    private fun showEditProfileDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_profile, null)
        val etName = dialogView.findViewById<EditText>(R.id.etEditName)
        val etPhone = dialogView.findViewById<EditText>(R.id.etEditPhone)
        val etBio = dialogView.findViewById<EditText>(R.id.etEditBio)
        val ivAvatar = dialogView.findViewById<ImageView>(R.id.ivEditAvatar)

        editAvatarImageView = ivAvatar
        editAvatarBase64 = null

        val currentUserDetail = viewModel.userDetail.value
        val currentProfile = currentUserDetail?.profile
        etName.setText(currentProfile?.name ?: "")
        etPhone.setText(currentProfile?.phone ?: "")
        etBio.setText(currentProfile?.bio ?: "")
        ivAvatar.loadImageWithGlide(currentProfile?.avatar, placeholderRes = R.drawable.ic_user_profile, maxWidth = 200, maxHeight = 200)

        ivAvatar.setOnClickListener {
            pickAvatarLauncher.launch("image/*")
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Chỉnh sửa hồ sơ cá nhân")
            .setView(dialogView)
            .setPositiveButton("Lưu") { _, _ ->
                val name = etName.text.toString().trim()
                val phone = etPhone.text.toString().trim()
                val bio = etBio.text.toString().trim()

                viewModel.updateProfile(name, phone, bio, editAvatarBase64) { success, msg ->
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                    if (success) {
                        loadUserDataFromSession()
                    }
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
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

        if (userSession != null) {
            binding.tvMssv.text = "MSSV: ${userSession.mssv.ifEmpty { "---" }}"
            binding.tvEmail.text = "Email: ${userSession.email.ifEmpty { "---" }}"
            binding.tvUserRole.text = userSession.role.uppercase()

            userSession.profile?.let { profile ->
                binding.tvUserName.text = profile.name.ifEmpty { "Sinh viên VLU" }
                binding.tvPhone.text = "Số điện thoại: ${profile.phone.ifEmpty { "Chưa cập nhật" }}"
                binding.tvBio.text = "Tiểu sử: ${profile.bio ?: "Chưa có tiểu sử"}"

                val fixedAvatarUrl = RetrofitClient.getFullImageUrl(profile.avatar)
                binding.imgAvatar.loadImageWithGlide(fixedAvatarUrl, placeholderRes = R.drawable.ic_user_profile)
            }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                launch {
                    viewModel.currentUser.collect { user ->
                        val targetUserId = arguments?.getString(ARG_USER_ID)
                        if (user != null && targetUserId.isNullOrEmpty()) {
                            binding.tvMssv.text = "MSSV: ${user.mssv}"
                            binding.tvEmail.text = "Email: ${user.email}"
                            binding.tvUserRole.text = user.role.uppercase()
                        }
                    }
                }

                launch {
                    viewModel.userDetail.collect { detail ->
                        if (detail != null) {
                            binding.tvMssv.text = "MSSV: ${detail.mssv}"
                            binding.tvEmail.text = "Email: ${detail.email}"
                            binding.tvUserRole.text = detail.role.uppercase()

                            val profile = detail.profile
                            binding.tvUserName.text = profile?.name ?: "Sinh viên VLU"
                            binding.tvPhone.text = "Số điện thoại: ${profile?.phone ?: "Chưa cập nhật"}"
                            binding.tvBio.text = "Tiểu sử: ${profile?.bio ?: "Chưa có tiểu sử"}"

                            val fixedAvatarUrl = RetrofitClient.getFullImageUrl(profile?.avatar)
                            binding.imgAvatar.loadImageWithGlide(fixedAvatarUrl, placeholderRes = R.drawable.ic_user_profile)
                        }
                    }
                }

                launch {
                    viewModel.userProducts.collect { products ->
                        productAdapter.submitList(products)
                        binding.tvMyProductsTitle.text = "Sản phẩm của tôi (${products.size})"
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
