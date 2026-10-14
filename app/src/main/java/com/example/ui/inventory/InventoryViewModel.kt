package com.example.ui.inventory

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Product
import com.example.data.model.StockTransaction
import com.example.data.repository.InventoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

enum class StockFilter {
    ALL,
    LOW_STOCK,
    OUT_OF_STOCK
}

data class StockReport(
    val totalSKUs: Int = 0,
    val totalUnits: Long = 0L,
    val totalAssetValue: Double = 0.0,
    val totalRetailValue: Double = 0.0,
    val lowStockCount: Int = 0,
    val outOfStockCount: Int = 0,
    val inboundToday: Long = 0L,
    val outboundToday: Long = 0L,
    val topMovingProducts: List<Pair<String, Long>> = emptyList()
)

data class InventoryUiState(
    val products: List<Product> = emptyList(),
    val filteredProducts: List<Product> = emptyList(),
    val transactions: List<StockTransaction> = emptyList(),
    val categories: List<String> = emptyList(),
    val searchQuery: String = "",
    val selectedCategory: String? = null,
    val stockFilter: StockFilter = StockFilter.ALL,
    val report: StockReport = StockReport(),
    val isLoading: Boolean = true,
    val userMessage: String? = null
)

class InventoryViewModel(
    application: Application,
    private val repository: InventoryRepository = InventoryRepository(application)
) : AndroidViewModel(application) {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory = _selectedCategory.asStateFlow()

    private val _stockFilter = MutableStateFlow(StockFilter.ALL)
    val stockFilter = _stockFilter.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage = _userMessage.asStateFlow()

    private val productsFlow = repository.observeProducts()
        .catch { e ->
            _userMessage.value = "Gagal memuat produk dari Cloud: ${e.message}"
            emit(emptyList())
        }

    private val transactionsFlow = repository.observeTransactions()
        .catch { e ->
            _userMessage.value = "Gagal memuat riwayat transaksi: ${e.message}"
            emit(emptyList())
        }

    val uiState: StateFlow<InventoryUiState> = combine(
        productsFlow,
        transactionsFlow,
        _searchQuery,
        _selectedCategory,
        _stockFilter
    ) { products, transactions, query, category, filter ->
        val categories = products.map { it.category.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()

        val filtered = products.filter { product ->
            val matchesQuery = query.isBlank() ||
                product.name.contains(query, ignoreCase = true) ||
                product.barcode.contains(query, ignoreCase = true) ||
                product.category.contains(query, ignoreCase = true)

            val matchesCategory = category == null || product.category.equals(category, ignoreCase = true)

            val matchesStock = when (filter) {
                StockFilter.ALL -> true
                StockFilter.LOW_STOCK -> product.isLowStock
                StockFilter.OUT_OF_STOCK -> product.isOutOfStock
            }

            matchesQuery && matchesCategory && matchesStock
        }

        val report = computeStockReport(products, transactions)

        InventoryUiState(
            products = products,
            filteredProducts = filtered,
            transactions = transactions,
            categories = categories,
            searchQuery = query,
            selectedCategory = category,
            stockFilter = filter,
            report = report,
            isLoading = false,
            userMessage = _userMessage.value
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = InventoryUiState(isLoading = true)
    )

    private fun computeStockReport(products: List<Product>, transactions: List<StockTransaction>): StockReport {
        var totalUnits = 0L
        var totalAssetValue = 0.0
        var totalRetailValue = 0.0
        var lowStockCount = 0
        var outOfStockCount = 0

        for (product in products) {
            totalUnits += product.quantity
            totalAssetValue += product.totalAssetValue
            totalRetailValue += product.totalRetailValue
            if (product.isOutOfStock) {
                outOfStockCount++
            } else if (product.isLowStock) {
                lowStockCount++
            }
        }

        // Transactions today
        val startOfDay = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        var inToday = 0L
        var outToday = 0L
        val productOutMap = mutableMapOf<String, Long>()

        for (tx in transactions) {
            val txTime = tx.createdAt?.toDate()?.time ?: 0L
            if (txTime >= startOfDay) {
                if (tx.type == "IN") inToday += tx.quantity
                if (tx.type == "OUT") outToday += tx.quantity
            }
            if (tx.type == "OUT") {
                productOutMap[tx.productName] = (productOutMap[tx.productName] ?: 0L) + tx.quantity
            }
        }

        val topMoving = productOutMap.toList()
            .sortedByDescending { it.second }
            .take(5)

        return StockReport(
            totalSKUs = products.size,
            totalUnits = totalUnits,
            totalAssetValue = totalAssetValue,
            totalRetailValue = totalRetailValue,
            lowStockCount = lowStockCount,
            outOfStockCount = outOfStockCount,
            inboundToday = inToday,
            outboundToday = outToday,
            topMovingProducts = topMoving
        )
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedCategory(category: String?) {
        _selectedCategory.value = category
    }

    fun setStockFilter(filter: StockFilter) {
        _stockFilter.value = filter
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    fun findProductByBarcode(barcode: String): Product? {
        val clean = barcode.trim()
        return uiState.value.products.firstOrNull { it.barcode.equals(clean, ignoreCase = true) }
    }

    fun saveProduct(product: Product, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val result = repository.saveProduct(product = product)
            if (result.isSuccess) {
                _userMessage.value = "Produk berhasil disimpan ke Cloud!"
                onComplete(true)
            } else {
                _userMessage.value = "Gagal menyimpan produk: ${result.exceptionOrNull()?.message}"
                onComplete(false)
            }
        }
    }

    fun recordMutation(
        product: Product,
        type: String,
        quantity: Long,
        reason: String,
        onComplete: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            val result = repository.recordStockMutation(
                product = product,
                type = type,
                deltaQty = quantity,
                reason = reason
            )
            if (result.isSuccess) {
                _userMessage.value = "Mutasi stok $type ($quantity ${product.unit}) berhasil dicatat!"
                onComplete(true)
            } else {
                _userMessage.value = "Gagal mencatat mutasi: ${result.exceptionOrNull()?.message}"
                onComplete(false)
            }
        }
    }

    fun deleteProduct(productId: String, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val result = repository.deleteProduct(productId = productId)
            if (result.isSuccess) {
                _userMessage.value = "Produk berhasil dihapus!"
                onComplete(true)
            } else {
                _userMessage.value = "Gagal menghapus produk: ${result.exceptionOrNull()?.message}"
                onComplete(false)
            }
        }
    }
}
