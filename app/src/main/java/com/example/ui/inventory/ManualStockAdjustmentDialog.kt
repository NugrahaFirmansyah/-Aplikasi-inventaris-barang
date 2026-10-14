package com.example.ui.inventory

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.Product

/**
 * Dialog input untuk memperbarui jumlah stok barang (masuk/keluar/opname) secara manual
 * jika barcode tidak terbaca atau untuk penyesuaian inventaris fisik.
 */
@Composable
fun ManualStockAdjustmentDialog(
    products: List<Product>,
    initialProduct: Product? = null,
    onDismiss: () -> Unit,
    onSubmitAdjustment: (product: Product, type: String, quantity: Long, reason: String) -> Unit
) {
    var selectedProduct by remember { mutableStateOf(initialProduct) }
    var productSearchQuery by remember { mutableStateOf("") }
    var operationType by remember { mutableStateOf("IN") } // "IN", "OUT", "ADJUSTMENT"
    var quantityText by remember { mutableStateOf("1") }
    var reasonText by remember { mutableStateOf("") }
    var isSelectingProduct by remember { mutableStateOf(initialProduct == null) }

    val presetReasons = listOf(
        "Barcode Rusak / Tidak Terbaca",
        "Stock Opname Rutin",
        "Restok Manual Gudang",
        "Barang Rusak / Kadaluarsa",
        "Penjualan Tanpa Scan",
        "Retur Pelanggan",
        "Koreksi Selisih Fisik"
    )

    val currentStock = selectedProduct?.quantity ?: 0L
    val qtyInput = quantityText.toLongOrNull() ?: 0L

    val projectedStock = when (operationType) {
        "IN" -> currentStock + qtyInput
        "OUT" -> maxOf(0L, currentStock - qtyInput)
        "ADJUSTMENT" -> qtyInput
        else -> currentStock
    }

    val filteredProducts = remember(products, productSearchQuery) {
        if (productSearchQuery.isBlank()) {
            products.take(15)
        } else {
            products.filter {
                it.name.contains(productSearchQuery, ignoreCase = true) ||
                it.barcode.contains(productSearchQuery, ignoreCase = true) ||
                it.category.contains(productSearchQuery, ignoreCase = true)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Penyesuaian Stok Manual",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Section 1: Product Selection / Search
                Text(
                    text = "1. Pilih Barang Inventaris:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )

                if (selectedProduct == null || isSelectingProduct) {
                    OutlinedTextField(
                        value = productSearchQuery,
                        onValueChange = { productSearchQuery = it },
                        placeholder = { Text("Ketik nama produk atau barcode...") },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                        },
                        trailingIcon = {
                            if (productSearchQuery.isNotEmpty()) {
                                IconButton(onClick = { productSearchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Hapus")
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("manual_adjust_product_search")
                    )

                    // Results dropdown list
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 180.dp)
                    ) {
                        if (filteredProducts.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (products.isEmpty()) "Belum ada produk di database." else "Produk tidak ditemukan.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        } else {
                            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                                items(filteredProducts, key = { it.id }) { prod ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                selectedProduct = prod
                                                isSelectingProduct = false
                                                productSearchQuery = ""
                                            }
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = prod.name,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "Barcode: ${prod.barcode} • ${prod.category}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                        }

                                        Surface(
                                            color = MaterialTheme.colorScheme.surface,
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = "${prod.quantity} ${prod.unit}",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                                }
                            }
                        }
                    }
                } else {
                    // Selected Product Banner
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = selectedProduct!!.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Barcode: ${selectedProduct!!.barcode} • Stok Saat Ini: ${selectedProduct!!.quantity} ${selectedProduct!!.unit}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            TextButton(
                                onClick = { isSelectingProduct = true },
                                modifier = Modifier.testTag("change_product_button")
                            ) {
                                Text("Ganti")
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Section 2: Choose Operation (Masuk / Keluar / Stock Opname)
                Text(
                    text = "2. Jenis Penyesuaian:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    OperationTabItem(
                        title = "Masuk (+)",
                        isSelected = operationType == "IN",
                        selectedColor = Color(0xFF10B981),
                        modifier = Modifier.weight(1f).testTag("op_tab_in"),
                        onClick = { operationType = "IN" }
                    )
                    OperationTabItem(
                        title = "Keluar (-)",
                        isSelected = operationType == "OUT",
                        selectedColor = Color(0xFFEF4444),
                        modifier = Modifier.weight(1f).testTag("op_tab_out"),
                        onClick = { operationType = "OUT" }
                    )
                    OperationTabItem(
                        title = "Opname (=)",
                        isSelected = operationType == "ADJUSTMENT",
                        selectedColor = Color(0xFF06B6D4),
                        modifier = Modifier.weight(1f).testTag("op_tab_adj"),
                        onClick = { operationType = "ADJUSTMENT" }
                    )
                }

                // Section 3: Quantity Input & Quick Increment Pills
                Text(
                    text = when (operationType) {
                        "IN" -> "3. Jumlah Barang Masuk:"
                        "OUT" -> "3. Jumlah Barang Keluar:"
                        else -> "3. Jumlah Total Hasil Hitung Fisik (Opname):"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { quantityText = it.filter { ch -> ch.isDigit() } },
                    placeholder = { Text("0") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    trailingIcon = {
                        selectedProduct?.let {
                            Text(
                                text = it.unit,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.padding(end = 12.dp)
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("manual_adjust_quantity_input")
                )

                // Quick Increment Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(1, 5, 10, 50, 100).forEach { inc ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    val cur = quantityText.toLongOrNull() ?: 0L
                                    quantityText = (cur + inc).toString()
                                }
                        ) {
                            Text(
                                text = "+$inc",
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 6.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                // Calculation Preview Box
                if (selectedProduct != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Stok Awal: $currentStock", style = MaterialTheme.typography.bodySmall)
                            Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(16.dp))
                            Text(
                                text = "Stok Akhir: $projectedStock ${selectedProduct?.unit ?: ""}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (projectedStock <= (selectedProduct?.minStock ?: 0L)) Color(0xFFF59E0B) else Color(0xFF10B981)
                            )
                        }
                    }
                }

                // Section 4: Reason / Keterangan
                Text(
                    text = "4. Alasan / Keterangan Penyesuaian:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )

                // Preset Reason Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presetReasons.forEach { preset ->
                        FilterChip(
                            selected = reasonText == preset,
                            onClick = { reasonText = preset },
                            label = { Text(preset) }
                        )
                    }
                }

                OutlinedTextField(
                    value = reasonText,
                    onValueChange = { reasonText = it },
                    placeholder = { Text("Contoh: Barcode rusak, penyesuaian opname...") },
                    maxLines = 2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("manual_adjust_reason_input")
                )
            }
        },
        confirmButton = {
            val isReady = selectedProduct != null && (qtyInput > 0L || operationType == "ADJUSTMENT")
            Button(
                onClick = {
                    selectedProduct?.let { prod ->
                        val finalReason = reasonText.ifBlank {
                            when (operationType) {
                                "IN" -> "Stok Masuk Manual"
                                "OUT" -> "Stok Keluar Manual"
                                else -> "Penyesuaian Fisik / Stock Opname"
                            }
                        }
                        onSubmitAdjustment(prod, operationType, qtyInput, finalReason)
                    }
                },
                enabled = isReady,
                colors = ButtonDefaults.buttonColors(
                    containerColor = when (operationType) {
                        "IN" -> Color(0xFF10B981)
                        "OUT" -> Color(0xFFEF4444)
                        else -> Color(0xFF06B6D4)
                    }
                ),
                modifier = Modifier.testTag("confirm_manual_adjustment_button")
            ) {
                Text("Simpan ke Cloud")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_manual_adjustment_button")
            ) {
                Text("Batal")
            }
        }
    )
}

@Composable
private fun OperationTabItem(
    title: String,
    isSelected: Boolean,
    selectedColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) selectedColor else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
