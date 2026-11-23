package com.example.vlu_campusmarket.ui.product

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vlu_campusmarket.data.models.ProductModel
import com.example.vlu_campusmarket.data.repository.ProductRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ProductViewModel(private val repository: ProductRepository) : ViewModel() {

    private val _allProducts = MutableStateFlow<List<ProductModel>>(emptyList())
    private val _selectedCategory = MutableStateFlow("ALL")
    private val _searchQuery = MutableStateFlow("".trim())

    private val userCache = mutableMapOf<String, String>()
    private val avatarCache = mutableMapOf<String, String>()

    private val _userNames = MutableStateFlow(mapOf<String, String>())
    val userNames: StateFlow<Map<String, String>> = _userNames.asStateFlow()

    private val _userAvatars = MutableStateFlow(mapOf<String, String>())
    val userAvatars: StateFlow<Map<String, String>> = _userAvatars.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private var currentPage = 1
    private var totalPages = 1
    private var isLoadingMore = false

    val filteredProducts: StateFlow<List<ProductModel>> = combine(
        _allProducts,
        _selectedCategory,
        _searchQuery
    ) { products, category, query ->
        products.filter { product ->
            val dt = product.formattedDeviceType
            val matchCategory = if (category == "ALL") true else {
                if (category.equals("Macbook", ignoreCase = true) || category.equals("Mac", ignoreCase = true)) {
                    dt.contains("mac", ignoreCase = true)
                } else {
                    dt.equals(category, ignoreCase = true)
                }
            }
            val matchQuery = (product.name ?: "").contains(query, ignoreCase = true)
            val statusLower = product.status?.trim()?.lowercase() ?: ""
            val isRejected = statusLower in listOf("bị từ chối", "rejected", "disabled", "hủy", "cancelled", "bị hủy")
            val isApproved = !isRejected
            matchCategory && matchQuery && isApproved
        }.sortedByDescending { it.safeId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        observeLocalDatabase()
        refreshData()
        startRealtimeProductSync()
    }

    private fun observeLocalDatabase() {
        viewModelScope.launch {
            repository.localProducts.collect { list ->
                _allProducts.value = list
            }
        }
    }

    private fun startRealtimeProductSync() {
        viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                delay(10000) // Tự động đồng bộ sản phẩm mới từ server MongoDB mỗi 10s
                try {
                    val res = repository.fetchFromRemoteAndSave(page = 1)
                    if (res != null && res.totalPages > 1) {
                        for (p in 2..minOf(res.totalPages, 10)) {
                            repository.fetchFromRemoteAndSave(page = p)
                        }
                    }
                } catch (_: Exception) {
                    // Ignored
                }
            }
        }
    }

    fun refreshData() {
        viewModelScope.launch {
            _isRefreshing.value = true
            currentPage = 1
            val result = repository.fetchFromRemoteAndSave(page = 1)
            if (result != null) {
                currentPage = result.currentPage
                totalPages = result.totalPages
                var p = 2
                while (p <= totalPages && p <= 20) {
                    val pageRes = repository.fetchFromRemoteAndSave(page = p)
                    if (pageRes == null || pageRes.data.isEmpty()) {
                        break
                    }
                    totalPages = maxOf(totalPages, pageRes.totalPages)
                    p++
                }
            }
            _isRefreshing.value = false
        }
    }

    fun loadNextPage() {
        if (isLoadingMore || currentPage >= totalPages) return
        isLoadingMore = true

        viewModelScope.launch {
            val nextPage = currentPage + 1
            val result = repository.fetchFromRemoteAndSave(page = nextPage)
            if (result != null) {
                currentPage = result.currentPage
                totalPages = result.totalPages
            }
            isLoadingMore = false
        }
    }

    fun fetchUserNameIfNeeded(userId: String) {
        if (userId.isBlank() || (userCache.containsKey(userId) && avatarCache.containsKey(userId))) return
        userCache[userId] = "Sinh viên VLU"

        viewModelScope.launch {
            try {
                val user = repository.getUserDetail(userId)
                if (user != null) {
                    val authorName = user.profile?.name ?: "Sinh viên VLU"
                    val authorAvatar = user.profile?.avatar ?: ""
                    userCache[userId] = authorName
                    avatarCache[userId] = authorAvatar
                }
            } catch (_: Exception) {
                userCache[userId] = "Sinh viên VLU"
                avatarCache[userId] = ""
            }
            _userNames.value = userCache.toMap()
            _userAvatars.value = avatarCache.toMap()
        }
    }

    fun setCategory(category: String) {
        _selectedCategory.value = category
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }
}
