package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class StockTransaction(
    @DocumentId val id: String = "",
    val userId: String = "",
    val productId: String = "",
    val productName: String = "",
    val barcode: String = "",
    val type: String = "IN", // IN, OUT, ADJUSTMENT
    val quantity: Long = 0L,
    val previousStock: Long = 0L,
    val newStock: Long = 0L,
    val reason: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)
