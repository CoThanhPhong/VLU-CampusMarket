package com.example.vlu_campusmarket.ui.product

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.vlu_campusmarket.R
import com.example.vlu_campusmarket.data.local.AppDatabase
import com.example.vlu_campusmarket.data.models.ProductModel
import com.example.vlu_campusmarket.data.remote.RetrofitClient
import com.example.vlu_campusmarket.data.repository.CartRepository
import com.example.vlu_campusmarket.data.repository.ProductRepository
import com.example.vlu_campusmarket.databinding.FragmentProductDetailBinding
import com.example.vlu_campusmarket.ui.fragment.CartFragment
import com.example.vlu_campusmarket.utils.loadImageWithGlide
import com.google.gson.Gson
import kotlinx.coroutines.launch
import java.text.DecimalFormat

class ProductDetailFragment : Fragment() {

    private var _binding: FragmentProductDetailBinding? = null
    private val binding get() = _binding!!

    private var product: ProductModel? = null
    private val formatter = DecimalFormat("#,###")

    companion object {
        private const val ARG_PRODUCT_JSON = "arg_product_json"

        fun newInstance(product: ProductModel): ProductDetailFragment {
            val fragment = ProductDetailFragment()
            val args = Bundle()
            args.putString(ARG_PRODUCT_JSON, Gson().toJson(product))
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.getString(ARG_PRODUCT_JSON)?.let { json ->
            product = Gson().fromJson(json, ProductModel::class.java)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProductDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val item = product ?: return
        bindProductData(item)

        binding.btnAddToCart.setOnClickListener {
            addToCartAndNotify(item._id, false)
        }

        binding.btnBuyNow.setOnClickListener {
            addToCartAndNotify(item._id, true)
        }
    }

    private fun bindProductData(item: ProductModel) {
        binding.tvDetailName.text = item.name
        binding.tvDetailPrice.text = "${formatter.format(item.price)} \u20ab"
        binding.tvDetailDeviceType.text = item.deviceType
        binding.tvDetailLocal.text = if (item.local.startsWith("Cơ sở")) item.local else "Cơ sở: ${item.local}"
        binding.tvDetailStock.text = "Kho: ${item.amount} máy"
        binding.tvDetailDescription.text = item.description?.ifEmpty { "Không có mô tả chi tiết." } ?: "Không có mô tả chi tiết."

        // Load ảnh sản phẩm chính
        binding.imgDetailProduct.loadImageWithGlide(item.imageUrl)

        // Lấy thông tin chi tiết người bán (Tên và Avatar)
        lifecycleScope.launch {
            try {
                val db = AppDatabase.getDatabase(requireContext())
                val apiService = RetrofitClient.getInstance(requireContext())
                val repository = ProductRepository(db.productDao(), apiService)

                val user = repository.getUserDetail(item.authorId)
                if (user != null) {
                    val authorName = user.profile?.name ?: "Sinh viên VLU"
                    binding.tvDetailAuthorName.text = "Đăng bởi: $authorName"

                    // Chuẩn hóa đường dẫn avatar và load qua Glide
                    val avatarUrl = getAbsoluteUrl(user.profile?.avatar)
                    binding.imgDetailAuthorAvatar.loadImageWithGlide(avatarUrl)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Hàm chuẩn hóa URL ảnh từ server (xử lý SVG sang PNG nếu là DiceBear và thêm Domain Node.js)
     */
    private fun getAbsoluteUrl(path: String?): String? {
        if (path.isNullOrEmpty()) return null
        var fixedPath = path
        if (fixedPath.contains("api.dicebear.com") && fixedPath.contains(".svg")) {
            fixedPath = fixedPath.replace(".svg", ".png")
        }
        fixedPath = fixedPath.replace(" ", "%20")
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

    private fun addToCartAndNotify(productId: String, redirectToCart: Boolean) {
        lifecycleScope.launch {
            try {
                val db = AppDatabase.getDatabase(requireContext())
                val apiService = RetrofitClient.getInstance(requireContext())
                val cartRepository = CartRepository(db.cartDao(), apiService, db.userDao())
                val success = cartRepository.addToCart(productId, 1)

                if (success) {
                    Toast.makeText(requireContext(), "Đã thêm sản phẩm vào giỏ hàng!", Toast.LENGTH_SHORT).show()
                    if (redirectToCart) {
                        parentFragmentManager.beginTransaction()
                            .replace(R.id.fragmentOutlet, CartFragment())
                            .addToBackStack(null)
                            .commit()
                    }
                } else {
                    Toast.makeText(requireContext(), "Không thể thêm vào giỏ hàng. Vui lòng đăng nhập.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}