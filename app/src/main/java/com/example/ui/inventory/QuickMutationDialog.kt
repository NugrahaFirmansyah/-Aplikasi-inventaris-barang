package com.example.ui.inventory

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.Product

@Composable
fun QuickMutationDialog(
    product: Product,
    defaultType: String = "IN", // "IN", "OUT", "ADJUSTMENT"
    onDismiss: () -> Unit,
    onSubmit: (type: String, quantity: Long, reason: String) -> Unit
) {
    var type by remember { mutableStateOf(defaultType) }
    var quantityText by remember { mutableStateOf("1") }
    var reason by remember { mutableStateOf("") }

    val qty = quantityText.toLongOrNull() ?: 0L
    val projectedStock = when (type) {
        "IN" -> product.quantity + qty
        "OUT" -> maxOf(0L, product.quantity - qty)
        "ADJUSTMENT" -> qty
        else -> product.quantity
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Mutasi Stok Cepat",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Type Switcher Row (Masuk / Keluar / Penyesuaian)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    TypeTabButton(
                        title = "Masuk (+)",
                        isSelected = type == "IN",
                        selectedColor = Color(0xFF10B981),
                        modifier = Modifier.weight(1f).testTag("tab_mutation_in"),
                        onClick = { type = "IN" }
                    )
                    TypeTabButton(
                        title = "Keluar (-)",
                        isSelected = type == "OUT",
                        selectedColor = Color(0xFFEF4444),
                        modifier = Modifier.weight(1f).testTag("tab_mutation_out"),
                        onClick = { type = "OUT" }
                    )
                    TypeTabButton(
                        title = "Koreksi",
                        isSelected = type == "ADJUSTMENT",
                        selectedColor = Color(0xFF06B6D4),
                        modifier = Modifier.weight(1f).testTag("tab_mutation_adj"),
                        onClick = { type = "ADJUSTMENT" }
                    )
                }

                // Current vs Projected Stock Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Stok Saat Ini", style = MaterialTheme.typography.labelSmall)
                            Text(
                                "${product.quantity} ${product.unit}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline
                        )

                        Column(horizontalAlignment = Alignment.End) {
                            Text("Stok Baru Nanti", style = MaterialTheme.typography.labelSmall)
                            Text(
                                "$projectedStock ${product.unit}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (projectedStock <= product.minStock) Color(0xFFF59E0B) else Color(0xFF10B981)
                            )
                        }
                    }
                }

                // Quantity Input
                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { quantityText = it.filter { ch -> ch.isDigit() } },
                    label = {
                        Text(if (type == "ADJUSTMENT") "Jumlah Stok Baru" else "Jumlah Perubahan (${product.unit})")
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("mutation_quantity_input")
                )

                // Quick Increment Pills
                if (type != "ADJUSTMENT") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(1, 5, 10, 50).forEach { inc ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        val cur = quantityText.toLongOrNull() ?: 0L
                                        quantityText = (cur + inc).toString()
                                    }
                                    .testTag("inc_button_$inc")
                            ) {
                                Text(
                                    text = "+$inc",
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(vertical = 6.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }

                // Reason / Keterangan
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Keterangan / Alasan") },
                    placeholder = {
                        Text(when (type) {
                            "IN" -> "Restok dari Supplier / Retur Pelanggan"
                            "OUT" -> "Penjualan / Barang Rusak / Kadaluarsa"
                            else -> "Stock Opname Gudang"
                        })
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("mutation_reason_input")
                )
            }
        },
        confirmButton = {
            val valid = qty > 0L || type == "ADJUSTMENT"
            Button(
                onClick = {
                    onSubmit(type, qty, reason.trim())
                },
                enabled = valid,
                colors = ButtonDefaults.buttonColors(
                    containerColor = when (type) {
                        "IN" -> Color(0xFF10B981)
                        "OUT" -> Color(0xFFEF4444)
                        else -> Color(0xFF06B6D4)
                    }
                ),
                modifier = Modifier.testTag("mutation_confirm_button")
            ) {
                Text("Simpan Mutasi")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("mutation_cancel_button")
            ) {
                Text("Batal")
            }
        }
    )
}

@Composable
private fun TypeTabButton(
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
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
