package com.example.vlu_campusmarket.ui.product

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.example.vlu_campusmarket.R
import com.example.vlu_campusmarket.data.local.AppDatabase
import com.example.vlu_campusmarket.data.local.entity.ProductEntity
import com.example.vlu_campusmarket.data.models.ProductModel
import com.example.vlu_campusmarket.data.remote.RetrofitClient
import com.example.vlu_campusmarket.data.remote.response.ProductIdRequest
import com.example.vlu_campusmarket.data.repository.CartRepository
import com.example.vlu_campusmarket.data.repository.ProductRepository
import com.example.vlu_campusmarket.databinding.FragmentProductDetailBinding
import com.example.vlu_campusmarket.ui.fragment.CartFragment
import com.example.vlu_campusmarket.ui.fragment.CreatePostFragment
import com.example.vlu_campusmarket.ui.fragment.ProfileFragment
import com.example.vlu_campusmarket.utils.loadImageWithGlide
import com.google.gson.Gson
import kotlinx.coroutines.flow.firstOrNull
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

        binding.cardAuthor.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentOutlet, ProfileFragment.newInstance(item.authorId))
                .addToBackStack(null)
                .commit()
        }

        lifecycleScope.launch {
            try {
                val db = AppDatabase.getDatabase(requireContext())
                val userDao = db.userDao()
                val loggedInUser = userDao.getLoggedInUser().firstOrNull()
                val currentUserId = loggedInUser?.id?.trim() ?: ""
                val currentMssv = loggedInUser?.mssv?.trim() ?: ""
                val authorId = item.authorId.trim()

                val isAuthor = currentUserId.isNotEmpty() && (
                    currentUserId.equals(authorId, ignoreCase = true) ||
                    currentMssv.equals(authorId, ignoreCase = true) ||
                    item.author.toString().contains(currentUserId)
                )

                if (isAuthor) {
                    binding.btnAddToCart.text = "Chỉnh sửa"
                    binding.btnAddToCart.setBackgroundColor(Color.parseColor("#2563EB"))
                    binding.btnAddToCart.setTextColor(Color.parseColor("#FFFFFF"))
                    binding.btnAddToCart.strokeWidth = 0
                    binding.btnAddToCart.setOnClickListener {
                        parentFragmentManager.beginTransaction()
                            .replace(R.id.fragmentOutlet, CreatePostFragment.newInstance(item))
                            .addToBackStack(null)
                            .commit()
                    }

                    binding.btnBuyNow.text = "Xóa bài đăng"
                    binding.btnBuyNow.setBackgroundColor(Color.parseColor("#EF4444"))
                    binding.btnBuyNow.setTextColor(Color.parseColor("#FFFFFF"))
                    binding.btnBuyNow.strokeWidth = 0
                    binding.btnBuyNow.setOnClickListener {
                        AlertDialog.Builder(requireContext())
                            .setTitle("Xóa bài đăng")
                            .setMessage("Bạn có chắc chắn muốn xóa sản phẩm này không?")
                            .setPositiveButton("Xóa") { _, _ ->
                                lifecycleScope.launch {
                                    try {
                                        val apiService = RetrofitClient.getInstance(requireContext())
                                        val response = apiService.deleteProduct(ProductIdRequest(item.safeId))
                                        val success = response.isSuccessful && response.body()?.success == true
                                        if (success) {
                                            db.productDao().deleteProductById(item.safeId)
                                            Toast.makeText(requireContext(), "Đã xóa sản phẩm", Toast.LENGTH_SHORT).show()
                                            parentFragmentManager.popBackStack()
                                        } else {
                                            Toast.makeText(requireContext(), "Không thể xóa sản phẩm", Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (e: Exception) {
                                        Toast.makeText(requireContext(), "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                            .setNegativeButton("Hủy", null)
                            .show()
                    }
                } else {
                    binding.btnAddToCart.setOnClickListener {
                        addToCartAndNotify(item.safeId, false)
                    }
                    binding.btnBuyNow.setOnClickListener {
                        addToCartAndNotify(item.safeId, true)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        product?.let { refreshLatestProduct(it.safeId) }
    }

    private fun refreshLatestProduct(productId: String) {
        if (productId.isEmpty()) return
        lifecycleScope.launch {
            try {
                val db = AppDatabase.getDatabase(requireContext())
                val apiService = RetrofitClient.getInstance(requireContext())

                val localEntity = db.productDao().getProductById(productId)
                if (localEntity != null) {
                    val rawImg: Any = if (localEntity.img.startsWith("[")) {
                        try {
                            Gson().fromJson(localEntity.img, Array<String>::class.java).toList()
                        } catch (e: Exception) {
                            localEntity.img
                        }
                    } else {
                        localEntity.img
                    }

                    val updatedModel = ProductModel(
                        _id = localEntity._id,
                        name = localEntity.name,
                        description = localEntity.description,
                        price = localEntity.price,
                        amount = localEntity.amount,
                        local = localEntity.local,
                        img = rawImg,
                        status = localEntity.status,
                        deviceType = localEntity.deviceType,
                        author = localEntity.author
                    )
                    product = updatedModel
                    bindProductData(updatedModel)
                }

                val response = apiService.getProductById(productId)
                if (response.isSuccessful && response.body()?.success == true) {
                    val remoteModel = response.body()?.data
                    if (remoteModel != null) {
                        product = remoteModel
                        bindProductData(remoteModel)
                        val entity = ProductEntity(
                            _id = remoteModel.safeId,
                            name = remoteModel.name,
                            description = remoteModel.description,
                            price = remoteModel.price,
                            amount = remoteModel.amount,
                            local = remoteModel.local,
                            img = if (remoteModel.imageList.isNotEmpty()) Gson().toJson(remoteModel.imageList) else remoteModel.imageUrl,
                            status = remoteModel.status,
                            deviceType = remoteModel.deviceType,
                            author = remoteModel.authorId
                        )
                        if (entity._id.isNotEmpty()) {
                            db.productDao().insertProducts(listOf(entity))
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun bindProductData(item: ProductModel) {
        binding.tvDetailName.text = item.name
        binding.tvDetailPrice.text = "${formatter.format(item.price)} \u20ab"
        binding.tvDetailDeviceType.text = item.deviceType
        binding.tvDetailLocal.text = if (item.local.startsWith("Cơ sở")) item.local else "Cơ sở: ${item.local}"
        binding.tvDetailStock.text = "Kho: ${item.amount} máy"
        binding.tvDetailDescription.text = item.description?.ifEmpty { "Không có mô tả chi tiết." } ?: "Không có mô tả chi tiết."

        // Load danh sách ảnh sản phẩm mới nhất (hỗ trợ tối đa 5 ảnh qua ViewPager2 slider)
        val imageList = item.imageList
        if (imageList.isNotEmpty()) {
            val adapter = ImageSliderAdapter(imageList)
            binding.viewPagerImages.adapter = adapter
            binding.tvImageCounter.text = "1/${imageList.size}"
            binding.viewPagerImages.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    binding.tvImageCounter.text = "${position + 1}/${imageList.size}"
                }
            })
        }

        // Hiển thị thông tin người bán trực tiếp từ ProductModel (đã được MongoDB populate)
        val authorName = item.authorName
        if (!authorName.isNullOrBlank()) {
            binding.tvDetailAuthorName.text = "Đăng bởi: $authorName"
        } else {
            binding.tvDetailAuthorName.text = "Đăng bởi: Sinh viên VLU"
        }

        val avatarUrl = RetrofitClient.getFullImageUrl(item.authorAvatar)
        binding.imgDetailAuthorAvatar.loadImageWithGlide(avatarUrl, placeholderRes = R.drawable.ic_user_profile)

        // Tải thông tin người bán từ Server nếu chưa có tên người bán đầy đủ trong ProductModel
        if (item.authorName.isNullOrEmpty() && item.authorId.isNotEmpty()) {
            lifecycleScope.launch {
                try {
                    val apiService = RetrofitClient.getInstance(requireContext())
                    val db = AppDatabase.getDatabase(requireContext())
                    val repository = ProductRepository(db.productDao(), apiService)
                    val user = repository.getUserDetail(item.authorId)
                    if (user != null) {
                        val name = user.profile?.name?.ifEmpty { "Sinh viên VLU" } ?: "Sinh viên VLU"
                        binding.tvDetailAuthorName.text = "Đăng bởi: $name"

                        val url = RetrofitClient.getFullImageUrl(user.profile?.avatar)
                        binding.imgDetailAuthorAvatar.loadImageWithGlide(url, placeholderRes = R.drawable.ic_user_profile)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
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

class ImageSliderAdapter(private val imageUrls: List<String>) :
    RecyclerView.Adapter<ImageSliderAdapter.SliderViewHolder>() {

    class SliderViewHolder(val imageView: ImageView) : RecyclerView.ViewHolder(imageView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SliderViewHolder {
        val imageView = ImageView(parent.context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            scaleType = ImageView.ScaleType.CENTER_CROP
        }
        return SliderViewHolder(imageView)
    }

    override fun onBindViewHolder(holder: SliderViewHolder, position: Int) {
        holder.imageView.loadImageWithGlide(imageUrls[position])
    }

    override fun getItemCount(): Int = imageUrls.size
}
