package com.example.ui.inventory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.Product
import kotlin.random.Random

@Composable
fun ProductFormDialog(
    initialProduct: Product? = null,
    prefilledBarcode: String? = null,
    onDismiss: () -> Unit,
    onScanRequested: () -> Unit,
    onSave: (Product) -> Unit
) {
    var barcode by remember { mutableStateOf(initialProduct?.barcode ?: prefilledBarcode ?: "") }
    var name by remember { mutableStateOf(initialProduct?.name ?: "") }
    var category by remember { mutableStateOf(initialProduct?.category ?: "Umum") }
    var quantityText by remember { mutableStateOf((initialProduct?.quantity ?: 10L).toString()) }
    var minStockText by remember { mutableStateOf((initialProduct?.minStock ?: 5L).toString()) }
    var unit by remember { mutableStateOf(initialProduct?.unit ?: "pcs") }
    var buyPriceText by remember { mutableStateOf((initialProduct?.buyPrice ?: 0.0).toLong().toString()) }
    var sellPriceText by remember { mutableStateOf((initialProduct?.sellPrice ?: 0.0).toLong().toString()) }
    var notes by remember { mutableStateOf(initialProduct?.notes ?: "") }

    val isEditing = initialProduct != null && initialProduct.id.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEditing) "Edit Barang Inventaris" else "Tambah Barang Baru",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Barcode input with scan & generate buttons
                OutlinedTextField(
                    value = barcode,
                    onValueChange = { barcode = it },
                    label = { Text("Kode Barcode / SKU *") },
                    placeholder = { Text("Contoh: 8992753215881") },
                    singleLine = true,
                    trailingIcon = {
                        Row {
                            IconButton(
                                onClick = onScanRequested,
                                modifier = Modifier.testTag("form_scan_barcode_icon_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QrCodeScanner,
                                    contentDescription = "Pindai Barcode",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(
                                onClick = {
                                    barcode = (8990000000000L + Random.nextLong(1000000000L)).toString()
                                },
                                modifier = Modifier.testTag("form_generate_barcode_icon_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Autorenew,
                                    contentDescription = "Buat Barcode Acak",
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("product_form_barcode_input")
                )

                // Product Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Barang *") },
                    placeholder = { Text("Contoh: Minyak Goreng 2L") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("product_form_name_input")
                )

                // Category & Unit Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Kategori *") },
                        placeholder = { Text("Sembako / Snack") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1.3f)
                            .testTag("product_form_category_input")
                    )

                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("Satuan *") },
                        placeholder = { Text("pcs/box/kg") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(0.9f)
                            .testTag("product_form_unit_input")
                    )
                }

                // Initial Stock & Min Stock Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = quantityText,
                        onValueChange = { quantityText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Stok Sekarang *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("product_form_quantity_input")
                    )

                    OutlinedTextField(
                        value = minStockText,
                        onValueChange = { minStockText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Batas Minim *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("product_form_min_stock_input")
                    )
                }

                // Buy Price & Sell Price Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = buyPriceText,
                        onValueChange = { buyPriceText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Harga Beli (Rp) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("product_form_buy_price_input")
                    )

                    OutlinedTextField(
                        value = sellPriceText,
                        onValueChange = { sellPriceText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Harga Jual (Rp) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("product_form_sell_price_input")
                    )
                }

                // Notes / Rack location
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Catatan / Lokasi Rak (Opsional)") },
                    placeholder = { Text("Contoh: Gudang B, Rak 04") },
                    maxLines = 2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("product_form_notes_input")
                )
            }
        },
        confirmButton = {
            val isValid = barcode.isNotBlank() && name.isNotBlank() && category.isNotBlank() && unit.isNotBlank()
            Button(
                onClick = {
                    val finalProduct = (initialProduct ?: Product()).copy(
                        barcode = barcode.trim(),
                        name = name.trim(),
                        category = category.trim(),
                        quantity = quantityText.toLongOrNull() ?: 0L,
                        minStock = minStockText.toLongOrNull() ?: 5L,
                        unit = unit.trim(),
                        buyPrice = buyPriceText.toDoubleOrNull() ?: 0.0,
                        sellPrice = sellPriceText.toDoubleOrNull() ?: 0.0,
                        notes = notes.trim()
                    )
                    onSave(finalProduct)
                },
                enabled = isValid,
                modifier = Modifier.testTag("product_form_save_button")
            ) {
                Text(if (isEditing) "Simpan Perubahan" else "Tambah ke Cloud")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("product_form_cancel_button")
            ) {
                Text("Batal")
            }
        }
    )
}
