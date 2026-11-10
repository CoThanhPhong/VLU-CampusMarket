package com.example.vlu_campusmarket.ui.product

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vlu_campusmarket.data.models.ProductModel
import com.example.vlu_campusmarket.data.repository.ProductRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ProductViewModel(private val repository: ProductRepository) : ViewModel() {

    private val _allProducts = MutableStateFlow<List<ProductModel>>(emptyList())
    private val _selectedCategory = MutableStateFlow("ALL")
    private val _searchQuery = MutableStateFlow("")

    // Bộ nhớ cache tạm thời ánh xạ từ userId sang tên người dùng
    private val userCache = mutableMapOf<String, String>()

    private val _userNames = MutableStateFlow(mapOf<String, String>())
    val userNames: StateFlow<Map<String, String>> = _userNames.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    // Danh sách sản phẩm sau khi lọc danh mục & tìm kiếm
    val filteredProducts: StateFlow<List<ProductModel>> = combine(
        _allProducts,
        _selectedCategory,
        _searchQuery
    ) { products, category, query ->
        products.filter { product ->
            val matchCategory = if (category == "ALL") true else product.deviceType.equals(category, ignoreCase = true)
            val matchQuery = product.name.contains(query, ignoreCase = true)
            matchCategory && matchQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        observeLocalDatabase()
    }

    private fun observeLocalDatabase() {
        viewModelScope.launch {
            repository.localProducts.collect { list ->
                _allProducts.value = list
                if (list.isEmpty()) {
                    refreshData()
                }
            }
        }
    }

    fun refreshData() {
        viewModelScope.launch {
            _isRefreshing.value = true
            repository.fetchFromRemoteAndSave()
            _isRefreshing.value = false
        }
    }

    // Lấy tên người dùng dựa theo cấu trúc profile?.name của UserModel
    fun fetchUserNameIfNeeded(userId: String) {
        if (userId.isBlank() || userCache.containsKey(userId)) return

        viewModelScope.launch {
            val user = repository.getUserDetail(userId)
            val authorName = user?.profile?.name ?: "Người dùng"
            userCache[userId] = authorName
            _userNames.value = userCache.toMap()
        }
    }

    fun setCategory(category: String) {
        _selectedCategory.value = category
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }
}