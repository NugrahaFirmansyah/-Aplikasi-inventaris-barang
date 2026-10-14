package com.example

import com.example.data.model.Product
import com.example.data.model.StockTransaction
import com.example.ui.reports.ChartViewType
import com.example.ui.reports.ReportTimeRange
import com.example.ui.reports.TrendDataGenerator
import com.example.ui.scanner.getBarcodeFormatName
import com.google.firebase.Timestamp
import com.google.mlkit.vision.barcode.common.Barcode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

class ExampleUnitTest {

    @Test
    fun product_stockStatusCalculations() {
        val outOfStockItem = Product(
            id = "1",
            barcode = "8991234567890",
            name = "Kopi Bubuk",
            quantity = 0L,
            minStock = 5L
        )
        assertTrue(outOfStockItem.isOutOfStock)
        assertTrue(outOfStockItem.isLowStock)

        val lowStockItem = Product(
            id = "2",
            barcode = "8991234567891",
            name = "Gula Pasir",
            quantity = 3L,
            minStock = 5L
        )
        assertFalse(lowStockItem.isOutOfStock)
        assertTrue(lowStockItem.isLowStock)

        val normalStockItem = Product(
            id = "3",
            barcode = "8991234567892",
            name = "Beras Premium",
            quantity = 25L,
            minStock = 10L
        )
        assertFalse(normalStockItem.isOutOfStock)
        assertFalse(normalStockItem.isLowStock)
    }

    @Test
    fun product_valuationCalculations() {
        val product = Product(
            id = "item_1",
            name = "Minyak Goreng 2L",
            quantity = 20L,
            buyPrice = 28000.0,
            sellPrice = 34000.0
        )
        assertEquals(560000.0, product.totalAssetValue, 0.01)
        assertEquals(680000.0, product.totalRetailValue, 0.01)
    }

    @Test
    fun barcode_formatMapping() {
        assertEquals("QR Code", getBarcodeFormatName(Barcode.FORMAT_QR_CODE))
        assertEquals("EAN-13", getBarcodeFormatName(Barcode.FORMAT_EAN_13))
        assertEquals("Code 128", getBarcodeFormatName(Barcode.FORMAT_CODE_128))
        assertEquals("UPC-A", getBarcodeFormatName(Barcode.FORMAT_UPC_A))
    }

    @Test
    fun trendDataGenerator_calculationsAndJson() {
        val now = Timestamp(Date())
        val products = listOf(
            Product(id = "p1", name = "Beras 5kg", category = "Sembako", quantity = 50L, buyPrice = 60000.0, sellPrice = 75000.0),
            Product(id = "p2", name = "Minyak Goreng", category = "Sembako", quantity = 30L, buyPrice = 25000.0, sellPrice = 30000.0)
        )
        val transactions = listOf(
            StockTransaction(id = "t1", productId = "p1", productName = "Beras 5kg", type = "OUT", quantity = 15L, createdAt = now),
            StockTransaction(id = "t2", productId = "p2", productName = "Minyak Goreng", type = "OUT", quantity = 5L, createdAt = now),
            StockTransaction(id = "t3", productId = "p1", productName = "Beras 5kg", type = "IN", quantity = 20L, createdAt = now)
        )

        val trends = TrendDataGenerator.generateTrends(products, transactions, ReportTimeRange.LAST_7_DAYS)

        assertEquals("Beras 5kg", trends.highestMovingItem)
        assertEquals(20L, trends.totalOutboundPeriod)
        assertEquals(20L, trends.totalInboundPeriod)
        assertEquals(7, trends.valueOverTime.size)

        val jsonValue = TrendDataGenerator.buildRechartsDataJson(trends, ChartViewType.INVENTORY_VALUE)
        assertTrue(jsonValue.startsWith("["))
        assertTrue(jsonValue.contains("assetValue"))
        assertTrue(jsonValue.contains("retailValue"))

        val jsonTopMoving = TrendDataGenerator.buildRechartsDataJson(trends, ChartViewType.TOP_MOVING)
        assertTrue(jsonTopMoving.startsWith("["))
        assertTrue(jsonTopMoving.contains("Beras 5kg"))
        assertTrue(jsonTopMoving.contains("outbound"))
    }
}
