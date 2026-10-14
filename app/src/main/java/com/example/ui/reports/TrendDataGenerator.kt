package com.example.ui.reports

import com.example.data.model.Product
import com.example.data.model.StockTransaction
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class ReportTimeRange(val displayName: String, val days: Int) {
    LAST_7_DAYS("7 Hari", 7),
    LAST_14_DAYS("14 Hari", 14),
    LAST_30_DAYS("30 Hari", 30),
    ALL_TIME("Semua", 90)
}

enum class ChartViewType(val displayName: String) {
    INVENTORY_VALUE("Nilai Aset"),
    TOP_MOVING("Barang Terlaris"),
    STOCK_FLOW("Arus Stok"),
    CATEGORY_SHARE("Kategori")
}

data class InventoryValuePoint(
    val date: String,
    val timestamp: Long,
    val assetValue: Double,
    val retailValue: Double,
    val units: Long
)

data class TopMovingItem(
    val name: String,
    val barcode: String,
    val outboundUnits: Long,
    val inboundUnits: Long,
    val currentStock: Long,
    val turnoverValue: Double
)

data class StockFlowPoint(
    val date: String,
    val timestamp: Long,
    val inbound: Long,
    val outbound: Long,
    val net: Long
)

data class CategoryValuationPoint(
    val category: String,
    val assetValue: Double,
    val totalUnits: Long,
    val productCount: Int
)

data class ComprehensiveStockTrends(
    val valueOverTime: List<InventoryValuePoint>,
    val topMovingItems: List<TopMovingItem>,
    val stockFlowPoints: List<StockFlowPoint>,
    val categoryShares: List<CategoryValuationPoint>,
    val totalOutboundPeriod: Long,
    val totalInboundPeriod: Long,
    val highestMovingItem: String,
    val growthRatePct: Double
)

object TrendDataGenerator {

    private val dayFormat = SimpleDateFormat("dd MMM", Locale("id", "ID"))
    private val fullDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    fun generateTrends(
        products: List<Product>,
        transactions: List<StockTransaction>,
        range: ReportTimeRange
    ): ComprehensiveStockTrends {
        val daysCount = range.days
        val nowCal = Calendar.getInstance()

        // Generate date buckets for the selected range
        val dateBuckets = mutableListOf<Calendar>()
        for (i in (daysCount - 1) downTo 0) {
            val c = Calendar.getInstance()
            c.add(Calendar.DAY_OF_YEAR, -i)
            c.set(Calendar.HOUR_OF_DAY, 0)
            c.set(Calendar.MINUTE, 0)
            c.set(Calendar.SECOND, 0)
            c.set(Calendar.MILLISECOND, 0)
            dateBuckets.add(c)
        }

        val rangeStartMillis = dateBuckets.first().timeInMillis

        // 1. Calculate Inflow / Outflow per day
        val dailyInboundMap = mutableMapOf<String, Long>()
        val dailyOutboundMap = mutableMapOf<String, Long>()
        val productOutboundMap = mutableMapOf<String, Long>()
        val productInboundMap = mutableMapOf<String, Long>()

        var totalPeriodInbound = 0L
        var totalPeriodOutbound = 0L

        for (tx in transactions) {
            val txDate = tx.createdAt?.toDate() ?: Date()
            val txTime = txDate.time

            val dateKey = fullDateFormat.format(txDate)
            if (txTime >= rangeStartMillis) {
                if (tx.type == "IN") {
                    dailyInboundMap[dateKey] = (dailyInboundMap[dateKey] ?: 0L) + tx.quantity
                    totalPeriodInbound += tx.quantity
                    val prodKey = tx.productName.ifBlank { "Barang #${tx.barcode}" }
                    productInboundMap[prodKey] = (productInboundMap[prodKey] ?: 0L) + tx.quantity
                } else if (tx.type == "OUT") {
                    dailyOutboundMap[dateKey] = (dailyOutboundMap[dateKey] ?: 0L) + tx.quantity
                    totalPeriodOutbound += tx.quantity
                    val prodKey = tx.productName.ifBlank { "Barang #${tx.barcode}" }
                    productOutboundMap[prodKey] = (productOutboundMap[prodKey] ?: 0L) + tx.quantity
                }
            }
        }

        // Build Stock Flow Points
        val stockFlowPoints = dateBuckets.map { cal ->
            val dateKey = fullDateFormat.format(cal.time)
            val inQty = dailyInboundMap[dateKey] ?: 0L
            val outQty = dailyOutboundMap[dateKey] ?: 0L
            StockFlowPoint(
                date = dayFormat.format(cal.time),
                timestamp = cal.timeInMillis,
                inbound = inQty,
                outbound = outQty,
                net = inQty - outQty
            )
        }

        // 2. Inventory Value Over Time
        // Current values
        val currentAssetValue = products.sumOf { it.totalAssetValue }
        val currentRetailValue = products.sumOf { it.totalRetailValue }
        val currentUnits = products.sumOf { it.quantity }

        // Compute valuation changes step-by-step
        // We know the current valuation at today. We can walk backward using net transactions,
        // or interpolate valuation points for a smooth, realistic timeline.
        val valueOverTime = mutableListOf<InventoryValuePoint>()
        var rollingAssetVal = currentAssetValue
        var rollingRetailVal = currentRetailValue
        var rollingUnits = currentUnits

        val avgUnitBuyPrice = if (currentUnits > 0) currentAssetValue / currentUnits else 15000.0
        val avgUnitRetailPrice = if (currentUnits > 0) currentRetailValue / currentUnits else 20000.0

        // Create points from earliest to today
        val reversePoints = mutableListOf<InventoryValuePoint>()
        for (i in dateBuckets.indices.reversed()) {
            val cal = dateBuckets[i]
            val dateKey = fullDateFormat.format(cal.time)
            val inQty = dailyInboundMap[dateKey] ?: 0L
            val outQty = dailyOutboundMap[dateKey] ?: 0L
            val netDelta = inQty - outQty

            reversePoints.add(
                InventoryValuePoint(
                    date = dayFormat.format(cal.time),
                    timestamp = cal.timeInMillis,
                    assetValue = maxOf(0.0, rollingAssetVal),
                    retailValue = maxOf(0.0, rollingRetailVal),
                    units = maxOf(0L, rollingUnits)
                )
            )

            // Step backward in time: subtract inbound, add outbound
            rollingUnits -= netDelta
            rollingAssetVal -= (netDelta * avgUnitBuyPrice)
            rollingRetailVal -= (netDelta * avgUnitRetailPrice)
        }

        valueOverTime.addAll(reversePoints.reversed())

        // 3. Top-Moving Items (by outbound sales/movements)
        val productLookup = products.associateBy { it.name.trim() }
        val topMovingItems = mutableListOf<TopMovingItem>()

        if (productOutboundMap.isNotEmpty()) {
            val sortedOutbound = productOutboundMap.entries
                .sortedByDescending { it.value }
                .take(6)

            for ((prodName, outQty) in sortedOutbound) {
                val matched = productLookup[prodName]
                val inQty = productInboundMap[prodName] ?: 0L
                val sellPrice = matched?.sellPrice ?: 20000.0
                val stock = matched?.quantity ?: 0L
                val barcode = matched?.barcode ?: ""

                topMovingItems.add(
                    TopMovingItem(
                        name = prodName,
                        barcode = barcode,
                        outboundUnits = outQty,
                        inboundUnits = inQty,
                        currentStock = stock,
                        turnoverValue = outQty * sellPrice
                    )
                )
            }
        } else {
            // Fallback if no outbound transactions yet: rank products by current turnover or quantity
            products.sortedByDescending { it.quantity }.take(5).forEach { prod ->
                topMovingItems.add(
                    TopMovingItem(
                        name = prod.name,
                        barcode = prod.barcode,
                        outboundUnits = 0L,
                        inboundUnits = prod.quantity,
                        currentStock = prod.quantity,
                        turnoverValue = prod.totalRetailValue
                    )
                )
            }
        }

        // 4. Category Shares
        val categoryGroups = products.groupBy { it.category.ifBlank { "Umum" } }
        val categoryShares = categoryGroups.map { (cat, list) ->
            CategoryValuationPoint(
                category = cat,
                assetValue = list.sumOf { it.totalAssetValue },
                totalUnits = list.sumOf { it.quantity },
                productCount = list.size
            )
        }.sortedByDescending { it.assetValue }

        // Growth rate: compare first point of period with last point
        val firstVal = valueOverTime.firstOrNull()?.assetValue ?: currentAssetValue
        val lastVal = valueOverTime.lastOrNull()?.assetValue ?: currentAssetValue
        val growthRate = if (firstVal > 0.0) ((lastVal - firstVal) / firstVal) * 100.0 else 0.0

        val highestItem = topMovingItems.firstOrNull()?.name ?: "Belum ada transaksi"

        return ComprehensiveStockTrends(
            valueOverTime = valueOverTime,
            topMovingItems = topMovingItems,
            stockFlowPoints = stockFlowPoints,
            categoryShares = categoryShares,
            totalOutboundPeriod = totalPeriodOutbound,
            totalInboundPeriod = totalPeriodInbound,
            highestMovingItem = highestItem,
            growthRatePct = growthRate
        )
    }

    // Helper to generate JSON for Recharts
    fun buildRechartsDataJson(
        trends: ComprehensiveStockTrends,
        chartType: ChartViewType
    ): String {
        val sb = StringBuilder()
        when (chartType) {
            ChartViewType.INVENTORY_VALUE -> {
                sb.append("[")
                trends.valueOverTime.forEachIndexed { idx, pt ->
                    if (idx > 0) sb.append(",")
                    sb.append("{\"date\":\"").append(escapeJson(pt.date))
                        .append("\",\"assetValue\":").append(String.format(Locale.US, "%.0f", pt.assetValue))
                        .append(",\"retailValue\":").append(String.format(Locale.US, "%.0f", pt.retailValue))
                        .append(",\"units\":").append(pt.units)
                        .append("}")
                }
                sb.append("]")
            }
            ChartViewType.TOP_MOVING -> {
                sb.append("[")
                trends.topMovingItems.forEachIndexed { idx, item ->
                    if (idx > 0) sb.append(",")
                    val shortName = if (item.name.length > 14) item.name.take(12) + ".." else item.name
                    sb.append("{\"name\":\"").append(escapeJson(shortName))
                        .append("\",\"fullName\":\"").append(escapeJson(item.name))
                        .append("\",\"outbound\":").append(item.outboundUnits)
                        .append(",\"inbound\":").append(item.inboundUnits)
                        .append(",\"stock\":").append(item.currentStock)
                        .append(",\"turnover\":").append(String.format(Locale.US, "%.0f", item.turnoverValue))
                        .append("}")
                }
                sb.append("]")
            }
            ChartViewType.STOCK_FLOW -> {
                sb.append("[")
                trends.stockFlowPoints.forEachIndexed { idx, pt ->
                    if (idx > 0) sb.append(",")
                    sb.append("{\"date\":\"").append(escapeJson(pt.date))
                        .append("\",\"masuk\":").append(pt.inbound)
                        .append(",\"keluar\":").append(pt.outbound)
                        .append(",\"net\":").append(pt.net)
                        .append("}")
                }
                sb.append("]")
            }
            ChartViewType.CATEGORY_SHARE -> {
                sb.append("[")
                trends.categoryShares.forEachIndexed { idx, cat ->
                    if (idx > 0) sb.append(",")
                    sb.append("{\"name\":\"").append(escapeJson(cat.category))
                        .append("\",\"value\":").append(String.format(Locale.US, "%.0f", cat.assetValue))
                        .append(",\"units\":").append(cat.totalUnits)
                        .append(",\"count\":").append(cat.productCount)
                        .append("}")
                }
                sb.append("]")
            }
        }
        return sb.toString()
    }

    private fun escapeJson(value: String): String {
        return value.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
    }
}
