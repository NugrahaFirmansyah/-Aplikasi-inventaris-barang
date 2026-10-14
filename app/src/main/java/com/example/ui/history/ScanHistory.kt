package com.example.ui.history

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.model.Product
import com.example.data.model.StockTransaction
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class ScanHistoryFilter {
    ALL,
    TODAY,
    INBOUND,
    OUTBOUND
}

/**
 * Komponen UI 'ScanHistory' yang menampilkan daftar barang yang baru saja dipindai
 * menggunakan LazyColumn dengan data real-time dari Firestore.
 */
@Composable
fun ScanHistory(
    transactions: List<StockTransaction>,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    products: List<Product> = emptyList(),
    onItemClick: ((StockTransaction) -> Unit)? = null,
    onQuickInbound: ((Product) -> Unit)? = null,
    onQuickOutbound: ((Product) -> Unit)? = null,
    onScanNewClicked: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID")) }

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(ScanHistoryFilter.ALL) }

    // Start of today in millis for filtering
    val startOfToday = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    // Filter transactions
    val filteredTransactions = remember(transactions, searchQuery, selectedFilter) {
        transactions.filter { tx ->
            val matchesQuery = searchQuery.isBlank() ||
                tx.productName.contains(searchQuery, ignoreCase = true) ||
                tx.barcode.contains(searchQuery, ignoreCase = true) ||
                tx.reason.contains(searchQuery, ignoreCase = true)

            val txMillis = tx.createdAt?.toDate()?.time ?: 0L
            val matchesFilter = when (selectedFilter) {
                ScanHistoryFilter.ALL -> true
                ScanHistoryFilter.TODAY -> txMillis >= startOfToday
                ScanHistoryFilter.INBOUND -> tx.type == "IN"
                ScanHistoryFilter.OUTBOUND -> tx.type == "OUT"
            }

            matchesQuery && matchesFilter
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("scan_history_component")
    ) {
        // Search & Filter Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Cari riwayat barcode / produk...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Hapus")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("scan_history_search_input")
            )

            // Filter Chips Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedFilter == ScanHistoryFilter.ALL,
                    onClick = { selectedFilter = ScanHistoryFilter.ALL },
                    label = { Text("Semua (${transactions.size})") },
                    modifier = Modifier.testTag("filter_scan_all")
                )
                FilterChip(
                    selected = selectedFilter == ScanHistoryFilter.TODAY,
                    onClick = { selectedFilter = ScanHistoryFilter.TODAY },
                    label = { Text("Hari Ini") },
                    modifier = Modifier.testTag("filter_scan_today")
                )
                FilterChip(
                    selected = selectedFilter == ScanHistoryFilter.INBOUND,
                    onClick = { selectedFilter = ScanHistoryFilter.INBOUND },
                    label = { Text("Masuk (+)") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF10B981).copy(alpha = 0.2f),
                        selectedLabelColor = Color(0xFF10B981)
                    ),
                    modifier = Modifier.testTag("filter_scan_in")
                )
                FilterChip(
                    selected = selectedFilter == ScanHistoryFilter.OUTBOUND,
                    onClick = { selectedFilter = ScanHistoryFilter.OUTBOUND },
                    label = { Text("Keluar (-)") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFEF4444).copy(alpha = 0.2f),
                        selectedLabelColor = Color(0xFFEF4444)
                    ),
                    modifier = Modifier.testTag("filter_scan_out")
                )
            }
        }

        // List Content with LazyColumn
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else if (filteredTransactions.isEmpty()) {
            EmptyScanHistoryView(
                hasFilters = searchQuery.isNotBlank() || selectedFilter != ScanHistoryFilter.ALL,
                onScanClicked = onScanNewClicked
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("scan_history_lazy_column"),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(
                    items = filteredTransactions,
                    key = { it.id.ifBlank { it.hashCode().toString() } }
                ) { tx ->
                    val matchingProduct = products.firstOrNull { it.id == tx.productId || it.barcode == tx.barcode }

                    ScanHistoryItemCard(
                        transaction = tx,
                        matchingProduct = matchingProduct,
                        dateFormat = dateFormat,
                        onCopyBarcode = {
                            clipboardManager.setText(AnnotatedString(tx.barcode))
                            Toast.makeText(context, "Barcode ${tx.barcode} disalin!", Toast.LENGTH_SHORT).show()
                        },
                        onClick = { onItemClick?.invoke(tx) },
                        onQuickInbound = {
                            matchingProduct?.let { prod -> onQuickInbound?.invoke(prod) }
                        },
                        onQuickOutbound = {
                            matchingProduct?.let { prod -> onQuickOutbound?.invoke(prod) }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ScanHistoryItemCard(
    transaction: StockTransaction,
    matchingProduct: Product?,
    dateFormat: SimpleDateFormat,
    onCopyBarcode: () -> Unit,
    onClick: () -> Unit,
    onQuickInbound: () -> Unit,
    onQuickOutbound: () -> Unit
) {
    val isIncoming = transaction.type == "IN"
    val isOutgoing = transaction.type == "OUT"

    val typeColor = when {
        isIncoming -> Color(0xFF10B981)
        isOutgoing -> Color(0xFFEF4444)
        else -> Color(0xFF06B6D4)
    }

    val typeLabel = when {
        isIncoming -> "Stok Masuk"
        isOutgoing -> "Stok Keluar"
        else -> "Penyesuaian"
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag("scan_history_item_${transaction.id}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header Row: Type pill + Timestamp
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(typeColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when {
                                isIncoming -> Icons.Default.ArrowDownward
                                isOutgoing -> Icons.Default.ArrowUpward
                                else -> Icons.Default.Tune
                            },
                            contentDescription = null,
                            tint = typeColor,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    Surface(
                        color = typeColor.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = typeLabel,
                            color = typeColor,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                val timeStr = transaction.createdAt?.toDate()?.let { dateFormat.format(it) } ?: "Baru saja"
                Text(
                    text = timeStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            // Product Name and Quantity Delta Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = transaction.productName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = "${if (isIncoming) "+" else if (isOutgoing) "-" else ""}${transaction.quantity} unit",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = typeColor
                )
            }

            // Barcode Chip + Stock Mutation Preview (Prev -> New)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onCopyBarcode)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCode,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = transaction.barcode,
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Salin Barcode",
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }

                Text(
                    text = "Stok: ${transaction.previousStock} → ${transaction.newStock}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    fontWeight = FontWeight.Medium
                )
            }

            // Reason / Notes if present
            if (transaction.reason.isNotBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Catatan: ${transaction.reason}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Quick Re-action Buttons if matching product is available
            if (matchingProduct != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Aksi Cepat:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(end = 8.dp)
                    )

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF10B981).copy(alpha = 0.15f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable(onClick = onQuickInbound)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(12.dp))
                            Text("+ Masuk", style = MaterialTheme.typography.labelSmall, color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.size(6.dp))

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFEF4444).copy(alpha = 0.15f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable(onClick = onQuickOutbound)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(12.dp))
                            Text("- Keluar", style = MaterialTheme.typography.labelSmall, color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyScanHistoryView(
    hasFilters: Boolean,
    onScanClicked: (() -> Unit)?
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.History,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(54.dp)
            )

            Text(
                text = if (hasFilters) "Tidak Ada Riwayat Sesuai Filter" else "Belum Ada Riwayat Pindaian",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = if (hasFilters)
                    "Coba ubah kata kunci pencarian atau ganti filter kategori pindaian."
                else
                    "Setiap kali Anda memindai barcode untuk stok masuk atau keluar, riwayatnya akan tersimpan di cloud dan muncul di sini secara otomatis.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center
            )

            if (onScanClicked != null) {
                Spacer(modifier = Modifier.height(6.dp))
                androidx.compose.material3.Button(onClick = onScanClicked) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Text("Pindai Barcode Sekarang")
                }
            }
        }
    }
}
