package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.Product
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InventoryRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun createProduct_authenticatedUser_success() = runBlocking {
    val uid = signInTestUser("tester@example.com")
    val repository = InventoryRepository(firestore)

    val sampleProduct = Product(
      userId = uid,
      barcode = "1234567890123",
      name = "Kopi Robusta 250g",
      category = "Minuman",
      quantity = 25L,
      minStock = 5L,
      unit = "bungkus",
      buyPrice = 20000.0,
      sellPrice = 30000.0,
      notes = "Rak B-1"
    )

    val result = withTimeout(10000L) {
      repository.saveProduct(userId = uid, product = sampleProduct)
    }

    assertTrue(result.isSuccess)
    val productId = result.getOrThrow()
    assertTrue(productId.isNotEmpty())

    val products = withTimeout(10000L) {
      repository.observeProducts(userId = uid).first { list -> list.any { it.id == productId } }
    }
    val fetched = products.first { it.id == productId }
    assertEquals("Kopi Robusta 250g", fetched.name)
    assertEquals(25L, fetched.quantity)
  }

  @Test
  fun recordStockMutation_authenticatedUser_updatesStockAndLogsTransaction() = runBlocking {
    val uid = signInTestUser("mutation_test@example.com")
    val repository = InventoryRepository(firestore)

    val initialProduct = Product(
      userId = uid,
      barcode = "9876543210987",
      name = "Teh Melati Celup",
      category = "Minuman",
      quantity = 10L,
      minStock = 3L,
      unit = "kotak",
      buyPrice = 8000.0,
      sellPrice = 12000.0
    )

    val createRes = withTimeout(10000L) {
      repository.saveProduct(userId = uid, product = initialProduct)
    }
    val productId = createRes.getOrThrow()

    // Mutate stock: IN 15 units
    val productWithId = initialProduct.copy(id = productId)
    val mutationRes = withTimeout(10000L) {
      repository.recordStockMutation(
        userId = uid,
        product = productWithId,
        type = "IN",
        deltaQty = 15L,
        reason = "Pembelian grosir"
      )
    }
    assertTrue(mutationRes.isSuccess)

    // Verify updated stock
    val products = withTimeout(10000L) {
      repository.observeProducts(userId = uid).first { list -> list.any { it.id == productId && it.quantity == 25L } }
    }
    assertNotNull(products)

    // Verify transaction log
    val transactions = withTimeout(10000L) {
      repository.observeTransactions(userId = uid).first { it.isNotEmpty() }
    }
    val latestTx = transactions.first()
    assertEquals("IN", latestTx.type)
    assertEquals(15L, latestTx.quantity)
    assertEquals(25L, latestTx.newStock)
  }
}
