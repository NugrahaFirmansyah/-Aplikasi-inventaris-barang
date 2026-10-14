package com.example.ui.inventory

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowOutward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.Product
import com.example.data.model.StockTransaction
import com.example.ui.reports.ChartViewType
import com.example.ui.reports.ComprehensiveStockTrends
import com.example.ui.reports.RechartsStockChart
import com.example.ui.reports.ReportTimeRange
import com.example.ui.reports.TopMovingItem
import com.example.ui.reports.TrendDataGenerator
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ReportsScreen(
    state: InventoryUiState,
    onRestockProduct: (Product) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedMainTab by remember { mutableIntStateOf(0) }
    var selectedChartType by remember { mutableStateOf(ChartViewType.INVENTORY_VALUE) }
    var selectedTimeRange by remember { mutableStateOf(ReportTimeRange.LAST_7_DAYS) }

    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("id", "ID")) }
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID")) }

    val report = state.report
    val profitEstimate = report.totalRetailValue - report.totalAssetValue
    val profitMarginPct = if (report.totalAssetValue > 0) (profitEstimate / report.totalAssetValue) * 100 else 0.0

    // Compute comprehensive trend data from Firestore products & transactions
    val trends: ComprehensiveStockTrends = remember(state.products, state.transactions, selectedTimeRange) {
        TrendDataGenerator.generateTrends(
            products = state.products,
            transactions = state.transactions,
            range = selectedTimeRange
        )
    }

    val onShareReport = {
        val summaryBuilder = StringBuilder().apply {
            appendLine("📊 LAPORAN INVENTARIS & TREN STOKKILAT")
            appendLine("Sinkronisasi Cloud Firestore Real-time: ${dateFormat.format(Date())}")
            appendLine("Periode Analitik: ${selectedTimeRange.displayName}")
            appendLine("----------------------------------------")
            appendLine("Total Nilai Aset (Modal): ${currencyFormat.format(report.totalAssetValue)}")
            appendLine("Estimasi Nilai Jual: ${currencyFormat.format(report.totalRetailValue)}")
            appendLine("Potensi Margin Laba: ${currencyFormat.format(profitEstimate)} (${String.format(Locale.US, "%.1f", profitMarginPct)}%)")
            appendLine("Total SKU: ${report.totalSKUs} | Total Unit Fisik: ${report.totalUnits}")
            appendLine("Kondisi: ${report.lowStockCount} Menipis | ${report.outOfStockCount} Habis")
            appendLine("----------------------------------------")
            appendLine("Tren Mutasi Periode:")
            appendLine("  Barang Terlaris (Top-Moving): ${trends.highestMovingItem}")
            appendLine("  Total Masuk: ${trends.totalInboundPeriod} unit")
            appendLine("  Total Keluar: ${trends.totalOutboundPeriod} unit")
            appendLine("  Pertumbuhan Nilai Aset: ${String.format(Locale.US, "%.1f", trends.growthRatePct)}%")
            appendLine("----------------------------------------")
            appendLine("Visualisasi grafik didukung Recharts Engine via StokKilat")
        }

        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, summaryBuilder.toString())
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Bagikan Laporan Stok & Tren")
        context.startActivity(shareIntent)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("reports_screen_root")
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Header Title + Firestore Real-time indicator + Export Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Laporan & Tren Stok",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudDone,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Data Langsung Firestore Cloud",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF10B981),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Button(
                onClick = onShareReport,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("share_report_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Bagikan",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.size(6.dp))
                Text("Ekspor")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Executive Metrics Cards Row
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TOTAL NILAI ASET INVENTARIS",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        fontWeight = FontWeight.SemiBold
                    )
                    // Growth Rate Badge
                    val isPositiveGrowth = trends.growthRatePct >= 0
                    Surface(
                        color = if (isPositiveGrowth) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFEF4444).copy(alpha = 0.15f),
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isPositiveGrowth) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                contentDescription = null,
                                tint = if (isPositiveGrowth) Color(0xFF10B981) else Color(0xFFEF4444),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "${String.format(Locale.US, "%.1f", trends.growthRatePct)}%",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isPositiveGrowth) Color(0xFF10B981) else Color(0xFFEF4444)
                            )
                        }
                    }
                }

                Text(
                    text = currencyFormat.format(report.totalAssetValue),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(6.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Est. Nilai Jual", style = MaterialTheme.typography.labelSmall)
                        Text(
                            currencyFormat.format(report.totalRetailValue),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text("Est. Potensi Margin", style = MaterialTheme.typography.labelSmall)
                        Text(
                            "+${currencyFormat.format(profitEstimate)} (${String.format(Locale.US, "%.1f", profitMarginPct)}%)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF10B981)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Main Navigation Tabs
        TabRow(
            selectedTabIndex = selectedMainTab,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Tab(
                selected = selectedMainTab == 0,
                onClick = { selectedMainTab = 0 },
                text = { Text("Grafik Recharts") },
                modifier = Modifier.testTag("tab_recharts_visualizer")
            )
            Tab(
                selected = selectedMainTab == 1,
                onClick = { selectedMainTab = 1 },
                text = { Text("Ringkasan") }
            )
            Tab(
                selected = selectedMainTab == 2,
                onClick = { selectedMainTab = 2 },
                text = {
                    Text(
                        if (report.lowStockCount + report.outOfStockCount > 0)
                            "Kritis (${report.lowStockCount + report.outOfStockCount})"
                        else "Kritis"
                    )
                }
            )
            Tab(
                selected = selectedMainTab == 3,
                onClick = { selectedMainTab = 3 },
                text = { Text("Mutasi (${state.transactions.size})") }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Tab Content
        when (selectedMainTab) {
            0 -> RechartsVisualizerTabContent(
                trends = trends,
                selectedChartType = selectedChartType,
                onSelectChartType = { selectedChartType = it },
                selectedTimeRange = selectedTimeRange,
                onSelectTimeRange = { selectedTimeRange = it },
                currencyFormat = currencyFormat
            )
            1 -> SummaryTabContent(report = report, products = state.products)
            2 -> CriticalStockTabContent(
                products = state.products.filter { it.isLowStock || it.isOutOfStock },
                onRestock = onRestockProduct
            )
            3 -> TransactionsTabContent(transactions = state.transactions)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RechartsVisualizerTabContent(
    trends: ComprehensiveStockTrends,
    selectedChartType: ChartViewType,
    onSelectChartType: (ChartViewType) -> Unit,
    selectedTimeRange: ReportTimeRange,
    onSelectTimeRange: (ReportTimeRange) -> Unit,
    currencyFormat: NumberFormat
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 80.dp),
        modifier = Modifier.testTag("recharts_tab_content")
    ) {
        // Chart View Type Selector Chips
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Pilih Visualisasi Recharts:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    // Time range chips
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        ReportTimeRange.values().forEach { range ->
                            FilterChip(
                                selected = selectedTimeRange == range,
                                onClick = { onSelectTimeRange(range) },
                                label = { Text(range.displayName, style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ChartViewType.values().forEach { chartType ->
                        FilterChip(
                            selected = selectedChartType == chartType,
                            onClick = { onSelectChartType(chartType) },
                            leadingIcon = {
                                Icon(
                                    imageVector = when (chartType) {
                                        ChartViewType.INVENTORY_VALUE -> Icons.Default.ShowChart
                                        ChartViewType.TOP_MOVING -> Icons.Default.TrendingUp
                                        ChartViewType.STOCK_FLOW -> Icons.Default.SyncAlt
                                        ChartViewType.CATEGORY_SHARE -> Icons.Default.PieChart
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = { Text(chartType.displayName) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }
            }
        }

        // The Live Recharts Chart Component
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoGraph,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = when (selectedChartType) {
                                    ChartViewType.INVENTORY_VALUE -> "Tren Nilai Aset Inventaris"
                                    ChartViewType.TOP_MOVING -> "Barang Paling Aktif Keluar (Top-Moving)"
                                    ChartViewType.STOCK_FLOW -> "Arus Stok Masuk vs Keluar Harian"
                                    ChartViewType.CATEGORY_SHARE -> "Distribusi Nilai Aset per Kategori"
                                },
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = selectedTimeRange.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Recharts embedded container
                    RechartsStockChart(
                        trends = trends,
                        chartType = selectedChartType,
                        timeRange = selectedTimeRange,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Trend Insights Summary Box
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Wawasan Tren Inventaris",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Barang Terlaris:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            Text(
                                text = trends.highestMovingItem,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(1f)) {
                            Text("Aktivitas Periode Ini:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            Text(
                                text = "+${trends.totalInboundPeriod} masuk / -${trends.totalOutboundPeriod} keluar",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        // Detailed Top-Moving Items List
        item {
            Text(
                text = "Peringkat Barang Terlaris (Top-Moving Items)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        if (trends.topMovingItems.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Belum ada transaksi mutasi keluar pada periode ini.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(trends.topMovingItems) { item ->
                TopMovingItemCard(item = item, currencyFormat = currencyFormat)
            }
        }
    }
}

@Composable
private fun TopMovingItemCard(item: TopMovingItem, currencyFormat: NumberFormat) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF3B82F6).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.TrendingUp,
                        contentDescription = null,
                        tint = Color(0xFF3B82F6),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Sisa Stok: ${item.currentStock} unit • Barcode: ${item.barcode.ifBlank { "-" }}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${item.outboundUnits} unit keluar",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF3B82F6)
                )
                if (item.turnoverValue > 0) {
                    Text(
                        text = currencyFormat.format(item.turnoverValue),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryTabContent(report: StockReport, products: List<Product>) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        item {
            // Units & SKU distribution cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricCard(
                    title = "Total Unit Barang",
                    value = "${report.totalUnits} unit",
                    subtitle = "${report.totalSKUs} variasi SKU",
                    modifier = Modifier.weight(1f)
                )

                MetricCard(
                    title = "Kondisi Stok",
                    value = if (report.lowStockCount == 0 && report.outOfStockCount == 0) "Optimal" else "${report.lowStockCount} Menipis",
                    subtitle = "${report.outOfStockCount} item habis",
                    highlightColor = if (report.outOfStockCount > 0) Color(0xFFEF4444) else if (report.lowStockCount > 0) Color(0xFFF59E0B) else Color(0xFF10B981),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            // Today's Activity Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Aktivitas Hari Ini",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF10B981).copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.size(8.dp))
                            Column {
                                Text("Masuk", style = MaterialTheme.typography.labelSmall)
                                Text("+${report.inboundToday} unit", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFEF4444).copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.size(8.dp))
                            Column {
                                Text("Keluar", style = MaterialTheme.typography.labelSmall)
                                Text("-${report.outboundToday} unit", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
                            }
                        }
                    }
                }
            }
        }

        if (report.topMovingProducts.isNotEmpty()) {
            item {
                Text(
                    text = "Ringkasan Produk Teraktif",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            items(report.topMovingProducts) { (prodName, count) ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = prodName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "$count unit keluar",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    subtitle: String,
    highlightColor: Color? = null,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = highlightColor ?: MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
private fun CriticalStockTabContent(
    products: List<Product>,
    onRestock: (Product) -> Unit
) {
    if (products.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.TrendingUp,
                    contentDescription = null,
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Semua Stok Aman!",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Tidak ada barang yang berada di bawah ambang batas minimum.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(top = 4.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
        return
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        items(products, key = { it.id }) { product ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (product.isOutOfStock) Color(0xFFEF4444) else Color(0xFFF59E0B),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.size(4.dp))
                            Text(
                                text = if (product.isOutOfStock) "STOK HABIS" else "STOK MENIPIS",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (product.isOutOfStock) Color(0xFFEF4444) else Color(0xFFF59E0B)
                            )
                        }
                        Text(
                            text = product.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Sisa: ${product.quantity} ${product.unit} (Min: ${product.minStock}) • Barcode: ${product.barcode}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }

                    Button(
                        onClick = { onRestock(product) },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("+ Restok")
                    }
                }
            }
        }
    }
}

@Composable
private fun TransactionsTabContent(transactions: List<StockTransaction>) {
    if (transactions.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text("Belum Ada Riwayat Mutasi", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Transaksi stok masuk dan keluar akan tercatat otomatis di sini.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
            }
        }
        return
    }

    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("id", "ID")) }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        items(transactions, key = { it.id.ifBlank { it.hashCode().toString() } }) { tx ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val badgeColor = when (tx.type) {
                            "IN" -> Color(0xFF10B981)
                            "OUT" -> Color(0xFFEF4444)
                            else -> Color(0xFF06B6D4)
                        }
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(badgeColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when (tx.type) {
                                    "IN" -> Icons.Default.ArrowDownward
                                    "OUT" -> Icons.Default.ArrowUpward
                                    else -> Icons.Default.TrendingUp
                                },
                                contentDescription = null,
                                tint = badgeColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Column {
                            Text(
                                text = tx.productName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            val timeStr = tx.createdAt?.toDate()?.let { dateFormat.format(it) } ?: "Baru saja"
                            Text(
                                text = "$timeStr • ${tx.reason.ifBlank { "Mutasi ${tx.type}" }}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        val sign = if (tx.type == "IN") "+" else if (tx.type == "OUT") "-" else ""
                        val color = if (tx.type == "IN") Color(0xFF10B981) else if (tx.type == "OUT") Color(0xFFEF4444) else Color(0xFF06B6D4)
                        Text(
                            text = "$sign${tx.quantity}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = color
                        )
                        Text(
                            text = "${tx.previousStock} → ${tx.newStock}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    }
}
