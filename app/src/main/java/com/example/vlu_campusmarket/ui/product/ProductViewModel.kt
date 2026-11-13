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

    // Cache userId -> tên người dùng, và các userId đang được gọi API (tránh gọi trùng)
    private val userCache = mutableMapOf<String, String>()
    private val inFlightUsers = mutableSetOf<String>()

    private val _userNames = MutableStateFlow(mapOf<String, String>())
    val userNames: StateFlow<Map<String, String>> = _userNames.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    // Thông báo lỗi một lần (hiển thị Toast)
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    private var initialSyncRequested = false

    val filteredProducts: StateFlow<List<ProductModel>> = combine(
        _allProducts,
        _selectedCategory,
        _searchQuery
    ) { products, category, query ->
        products.filter { product ->
            val matchCategory = category == "ALL" || product.deviceType.equals(category, ignoreCase = true)
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
                // Chỉ tự đồng bộ 1 lần khi DB rỗng (tránh lặp vô hạn khi server lỗi)
                if (list.isEmpty() && !initialSyncRequested) {
                    initialSyncRequested = true
                    refreshData()
                }
            }
        }
    }

    fun refreshData() {
        if (_isRefreshing.value) return
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                repository.fetchFromRemoteAndSave()?.let { _messages.tryEmit(it) }
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun fetchUserNameIfNeeded(userId: String) {
        if (userId.isBlank() || userCache.containsKey(userId) || !inFlightUsers.add(userId)) return

        viewModelScope.launch {
            val user = repository.getUserDetail(userId)
            userCache[userId] = user?.profile?.name?.takeIf { it.isNotBlank() } ?: "Người dùng"
            inFlightUsers.remove(userId)
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
