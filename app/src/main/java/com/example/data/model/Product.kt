package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class Product(
    @DocumentId val id: String = "",
    val userId: String = "",
    val barcode: String = "",
    val name: String = "",
    val category: String = "Umum",
    val quantity: Long = 0L,
    val minStock: Long = 5L,
    val unit: String = "pcs",
    val buyPrice: Double = 0.0,
    val sellPrice: Double = 0.0,
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val isLowStock: Boolean
        get() = quantity <= minStock

    val isOutOfStock: Boolean
        get() = quantity <= 0L

    val totalAssetValue: Double
        get() = quantity * buyPrice

    val totalRetailValue: Double
        get() = quantity * sellPrice
}
