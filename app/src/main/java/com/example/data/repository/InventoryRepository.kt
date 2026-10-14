package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.Product
import com.example.data.model.StockTransaction
import com.example.data.util.OperationType
import com.example.data.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class InventoryRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun observeProducts(userId: String = requireUserId()): Flow<List<Product>> = flow {
        val path = "users/$userId/products"
        emitAll(
            db.collection("users").document(userId).collection("products")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(Product::class.java)
                }
                .catch { error ->
                    if (error is Exception) {
                        handleFirestoreError(error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeTransactions(userId: String = requireUserId()): Flow<List<StockTransaction>> = flow {
        val path = "users/$userId/transactions"
        emitAll(
            db.collection("users").document(userId).collection("transactions")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(StockTransaction::class.java)
                }
                .catch { error ->
                    if (error is Exception) {
                        handleFirestoreError(error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun saveProduct(userId: String = requireUserId(), product: Product): Result<String> {
        val colRef = db.collection("users").document(userId).collection("products")
        return try {
            if (product.id.isBlank()) {
                val newDoc = colRef.document()
                val payload = buildMap<String, Any> {
                    put("userId", userId)
                    put("barcode", product.barcode.trim())
                    put("name", product.name.trim())
                    put("category", product.category.trim())
                    put("quantity", product.quantity)
                    put("minStock", product.minStock)
                    put("unit", product.unit.trim())
                    put("buyPrice", product.buyPrice)
                    put("sellPrice", product.sellPrice)
                    if (product.notes.isNotBlank()) {
                        put("notes", product.notes.trim())
                    }
                    put("createdAt", FieldValue.serverTimestamp())
                    put("updatedAt", FieldValue.serverTimestamp())
                }
                newDoc.set(payload).await()
                Result.success(newDoc.id)
            } else {
                val docRef = colRef.document(product.id)
                val payload = buildMap<String, Any> {
                    put("barcode", product.barcode.trim())
                    put("name", product.name.trim())
                    put("category", product.category.trim())
                    put("quantity", product.quantity)
                    put("minStock", product.minStock)
                    put("unit", product.unit.trim())
                    put("buyPrice", product.buyPrice)
                    put("sellPrice", product.sellPrice)
                    if (product.notes.isNotBlank()) {
                        put("notes", product.notes.trim())
                    }
                    put("updatedAt", FieldValue.serverTimestamp())
                }
                docRef.update(payload).await()
                Result.success(product.id)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, if (product.id.isBlank()) OperationType.CREATE else OperationType.UPDATE, colRef.path)
            Result.failure(e)
        }
    }

    suspend fun recordStockMutation(
        userId: String = requireUserId(),
        product: Product,
        type: String, // "IN", "OUT", "ADJUSTMENT"
        deltaQty: Long,
        reason: String
    ): Result<Unit> {
        val productsCol = db.collection("users").document(userId).collection("products")
        val txCol = db.collection("users").document(userId).collection("transactions")
        val productDoc = productsCol.document(product.id)
        val txDoc = txCol.document()

        val previousStock = product.quantity
        val newStock = when (type) {
            "IN" -> previousStock + deltaQty
            "OUT" -> maxOf(0L, previousStock - deltaQty)
            "ADJUSTMENT" -> deltaQty
            else -> previousStock
        }

        return try {
            val batch = db.batch()

            // 1. Update product quantity
            batch.update(
                productDoc,
                mapOf(
                    "quantity" to newStock,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            // 2. Add immutable transaction record
            val txPayload = buildMap<String, Any> {
                put("userId", userId)
                put("productId", product.id)
                put("productName", product.name)
                put("barcode", product.barcode)
                put("type", type)
                put("quantity", deltaQty)
                put("previousStock", previousStock)
                put("newStock", newStock)
                if (reason.isNotBlank()) {
                    put("reason", reason.trim())
                }
                put("createdAt", FieldValue.serverTimestamp())
            }
            batch.set(txDoc, txPayload)

            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "users/$userId")
            Result.failure(e)
        }
    }

    suspend fun deleteProduct(userId: String = requireUserId(), productId: String): Result<Unit> {
        val docRef = db.collection("users").document(userId).collection("products").document(productId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }
}
