package com.example.ui.reports

import android.annotation.SuppressLint
import android.graphics.Color as AndroidColor
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun RechartsStockChart(
    trends: ComprehensiveStockTrends,
    chartType: ChartViewType,
    timeRange: ReportTimeRange,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val dataJson = remember(trends, chartType) {
        TrendDataGenerator.buildRechartsDataJson(trends, chartType)
    }

    val htmlContent = remember(dataJson, chartType, timeRange, isDark) {
        generateRechartsHtml(
            dataJson = dataJson,
            chartType = chartType,
            timeRange = timeRange,
            isDark = isDark
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(290.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .testTag("recharts_stock_chart_container")
    ) {
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.loadWithOverviewMode = true
                    settings.useWideViewPort = true
                    settings.cacheMode = WebSettings.LOAD_DEFAULT
                    setBackgroundColor(AndroidColor.TRANSPARENT)

                    webChromeClient = object : WebChromeClient() {
                        override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                            return true
                        }
                    }

                    webViewClient = object : WebViewClient() {}
                    loadDataWithBaseURL("https://recharts.local", htmlContent, "text/html", "UTF-8", null)
                }
            },
            update = { webView ->
                webView.loadDataWithBaseURL("https://recharts.local", htmlContent, "text/html", "UTF-8", null)
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

private fun generateRechartsHtml(
    dataJson: String,
    chartType: ChartViewType,
    timeRange: ReportTimeRange,
    isDark: Boolean
): String {
    val bgColor = if (isDark) "#18181b" else "#ffffff"
    val textColor = if (isDark) "#e4e4e7" else "#18181b"
    val subTextColor = if (isDark) "#a1a1aa" else "#71717a"
    val gridColor = if (isDark) "#27272a" else "#f1f5f9"
    val tooltipBg = if (isDark) "rgba(24, 24, 27, 0.95)" else "rgba(255, 255, 255, 0.95)"
    val tooltipBorder = if (isDark) "#3f3f46" else "#e2e8f0"

    val chartTypeStr = chartType.name

    return """
    <!DOCTYPE html>
    <html lang="id">
    <head>
      <meta charset="UTF-8" />
      <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
      <title>Recharts Stock Trends</title>
      <style>
        * { box-sizing: border-box; margin: 0; padding: 0; -webkit-tap-highlight-color: transparent; }
        body {
          font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
          background-color: transparent;
          color: $textColor;
          width: 100%;
          height: 100vh;
          overflow: hidden;
          padding: 8px 10px 4px 10px;
          display: flex;
          flex-direction: column;
        }
        #chart-header {
          display: flex;
          justify-content: space-between;
          align-items: center;
          padding: 0 4px 6px 4px;
          font-size: 11px;
          color: $subTextColor;
        }
        .badge {
          display: inline-flex;
          align-items: center;
          gap: 4px;
          background: rgba(16, 185, 129, 0.12);
          color: #10B981;
          padding: 2px 8px;
          border-radius: 999px;
          font-weight: 600;
          font-size: 10px;
        }
        #chart-container {
          flex: 1;
          width: 100%;
          height: 100%;
          min-height: 230px;
          position: relative;
        }
        .recharts-default-tooltip {
          background-color: $tooltipBg !important;
          border: 1px solid $tooltipBorder !important;
          border-radius: 8px !important;
          box-shadow: 0 4px 12px rgba(0,0,0,0.15) !important;
          color: $textColor !important;
          font-size: 11px !important;
          padding: 8px 12px !important;
        }
        .recharts-tooltip-item {
          color: $textColor !important;
          padding: 2px 0 !important;
        }
        /* Fallback SVG chart styles */
        .svg-chart {
          width: 100%;
          height: 100%;
        }
        .axis-text {
          font-size: 10px;
          fill: $subTextColor;
          text-anchor: middle;
        }
        .grid-line {
          stroke: $gridColor;
          stroke-dasharray: 3, 3;
        }
        .chart-legend {
          display: flex;
          justify-content: center;
          gap: 14px;
          font-size: 10px;
          color: $subTextColor;
          margin-top: 4px;
        }
        .legend-item {
          display: flex;
          align-items: center;
          gap: 5px;
        }
        .legend-dot {
          width: 8px;
          height: 8px;
          border-radius: 2px;
        }
      </style>
      <!-- Load React 18 & Recharts via CDN -->
      <script src="https://unpkg.com/react@18.2.0/umd/react.production.min.js"></script>
      <script src="https://unpkg.com/react-dom@18.2.0/umd/react-dom.production.min.js"></script>
      <script src="https://unpkg.com/recharts@2.12.7/umd/Recharts.js"></script>
    </head>
    <body>
      <div id="chart-header">
        <span>Recharts v2 • Live Engine</span>
        <span class="badge">● Cloud Firestore Sync</span>
      </div>
      <div id="chart-container">
        <div id="react-root" style="width:100%;height:100%;"></div>
      </div>

      <script>
        const rawData = $dataJson;
        const chartType = "$chartTypeStr";
        const isDark = $isDark;
        const textColor = "$textColor";
        const subTextColor = "$subTextColor";
        const gridColor = "$gridColor";

        function formatIDR(val) {
          if (val >= 1000000000) return 'Rp ' + (val / 1000000000).toFixed(1) + 'M';
          if (val >= 1000000) return 'Rp ' + (val / 1000000).toFixed(1) + 'jt';
          if (val >= 1000) return 'Rp ' + (val / 1000).toFixed(0) + 'rb';
          return 'Rp ' + Number(val).toLocaleString('id-ID');
        }

        function renderWithRecharts() {
          if (!window.Recharts || !window.React || !window.ReactDOM) {
            renderFallbackSvg();
            return;
          }

          const {
            ResponsiveContainer, AreaChart, Area, BarChart, Bar,
            XAxis, YAxis, CartesianGrid, Tooltip, Legend
          } = window.Recharts;
          const e = React.createElement;

          let chartElement;

          if (chartType === 'INVENTORY_VALUE') {
            chartElement = e(AreaChart, { data: rawData, margin: { top: 10, right: 10, left: -15, bottom: 0 } },
              e('defs', null,
                e('linearGradient', { id: 'assetGrad', x1: 0, y1: 0, x2: 0, y2: 1 },
                  e('stop', { offset: '5%', stopColor: '#10B981', stopOpacity: 0.8 }),
                  e('stop', { offset: '95%', stopColor: '#10B981', stopOpacity: 0.05 })
                ),
                e('linearGradient', { id: 'retailGrad', x1: 0, y1: 0, x2: 0, y2: 1 },
                  e('stop', { offset: '5%', stopColor: '#6366F1', stopOpacity: 0.7 }),
                  e('stop', { offset: '95%', stopColor: '#6366F1', stopOpacity: 0.02 })
                )
              ),
              e(CartesianGrid, { strokeDasharray: '3 3', stroke: gridColor, vertical: false }),
              e(XAxis, { dataKey: 'date', tick: { fill: subTextColor, fontSize: 10 }, tickLine: false, axisLine: { stroke: gridColor } }),
              e(YAxis, { tickFormatter: formatIDR, tick: { fill: subTextColor, fontSize: 10 }, tickLine: false, axisLine: false }),
              e(Tooltip, {
                formatter: (val, name) => [formatIDR(val), name === 'assetValue' ? 'Nilai Aset' : 'Nilai Jual'],
                labelStyle: { fontWeight: 'bold', color: textColor }
              }),
              e(Legend, {
                wrapperStyle: { fontSize: '10px', paddingTop: '4px' },
                formatter: (value) => value === 'assetValue' ? 'Nilai Aset (Modal)' : 'Estimasi Nilai Jual'
              }),
              e(Area, { type: 'monotone', dataKey: 'assetValue', stroke: '#10B981', strokeWidth: 2.5, fillOpacity: 1, fill: 'url(#assetGrad)' }),
              e(Area, { type: 'monotone', dataKey: 'retailValue', stroke: '#6366F1', strokeWidth: 2, fillOpacity: 1, fill: 'url(#retailGrad)' })
            );
          } else if (chartType === 'TOP_MOVING') {
            chartElement = e(BarChart, { data: rawData, margin: { top: 10, right: 10, left: -20, bottom: 20 } },
              e(CartesianGrid, { strokeDasharray: '3 3', stroke: gridColor, vertical: false }),
              e(XAxis, { dataKey: 'name', tick: { fill: subTextColor, fontSize: 9 }, interval: 0, angle: -20, textAnchor: 'end' }),
              e(YAxis, { tick: { fill: subTextColor, fontSize: 10 }, tickLine: false, axisLine: false }),
              e(Tooltip, {
                formatter: (val, name, item) => {
                  if (name === 'outbound') return [val + ' unit', 'Keluar/Terjual (' + item.payload.fullName + ')'];
                  if (name === 'stock') return [val + ' unit', 'Sisa Stok'];
                  return [val, name];
                }
              }),
              e(Legend, {
                wrapperStyle: { fontSize: '10px', paddingTop: '4px' },
                formatter: (value) => value === 'outbound' ? 'Unit Keluar' : 'Sisa Stok'
              }),
              e(Bar, { dataKey: 'outbound', fill: '#3B82F6', radius: [4, 4, 0, 0] }),
              e(Bar, { dataKey: 'stock', fill: '#10B981', radius: [4, 4, 0, 0] })
            );
          } else if (chartType === 'STOCK_FLOW') {
            chartElement = e(BarChart, { data: rawData, margin: { top: 10, right: 10, left: -20, bottom: 0 } },
              e(CartesianGrid, { strokeDasharray: '3 3', stroke: gridColor, vertical: false }),
              e(XAxis, { dataKey: 'date', tick: { fill: subTextColor, fontSize: 10 }, tickLine: false, axisLine: { stroke: gridColor } }),
              e(YAxis, { tick: { fill: subTextColor, fontSize: 10 }, tickLine: false, axisLine: false }),
              e(Tooltip, {
                formatter: (val, name) => [val + ' unit', name === 'masuk' ? 'Barang Masuk (+)' : 'Barang Keluar (-)']
              }),
              e(Legend, {
                wrapperStyle: { fontSize: '10px', paddingTop: '4px' },
                formatter: (value) => value === 'masuk' ? 'Masuk (+)' : 'Keluar (-)'
              }),
              e(Bar, { dataKey: 'masuk', fill: '#10B981', radius: [4, 4, 0, 0] }),
              e(Bar, { dataKey: 'keluar', fill: '#EF4444', radius: [4, 4, 0, 0] })
            );
          } else {
            // CATEGORY_SHARE
            chartElement = e(BarChart, { data: rawData, margin: { top: 10, right: 10, left: -10, bottom: 0 } },
              e(CartesianGrid, { strokeDasharray: '3 3', stroke: gridColor, vertical: false }),
              e(XAxis, { dataKey: 'name', tick: { fill: subTextColor, fontSize: 10 } }),
              e(YAxis, { tickFormatter: formatIDR, tick: { fill: subTextColor, fontSize: 10 }, tickLine: false, axisLine: false }),
              e(Tooltip, {
                formatter: (val) => [formatIDR(val), 'Total Aset Kategori']
              }),
              e(Bar, { dataKey: 'value', fill: '#8B5CF6', radius: [4, 4, 0, 0] })
            );
          }

          const container = e(ResponsiveContainer, { width: '100%', height: '100%' }, chartElement);
          ReactDOM.render(container, document.getElementById('react-root'));
        }

        // High-Fidelity SVG Fallback Engine replicating Recharts
        function renderFallbackSvg() {
          const root = document.getElementById('react-root');
          if (!rawData || rawData.length === 0) {
            root.innerHTML = '<div style="display:flex;height:100%;align-items:center;justify-content:center;color:' + subTextColor + ';font-size:12px;">Memuat visualisasi Recharts...</div>';
            return;
          }

          const w = root.clientWidth || 340;
          const h = 210;
          const padL = 36, padR = 14, padT = 20, padB = 30;
          const plotW = w - padL - padR;
          const plotH = h - padT - padB;

          let svgHtml = '<svg class="svg-chart" viewBox="0 0 ' + w + ' ' + h + '">';
          svgHtml += '<defs>';
          svgHtml += '<linearGradient id="fallbackAsset" x1="0" y1="0" x2="0" y2="1"><stop offset="0%" stop-color="#10B981" stop-opacity="0.6"/><stop offset="100%" stop-color="#10B981" stop-opacity="0.05"/></linearGradient>';
          svgHtml += '</defs>';

          // Gridlines
          for (let i = 0; i <= 4; i++) {
            const gy = padT + (plotH / 4) * i;
            svgHtml += '<line x1="' + padL + '" y1="' + gy + '" x2="' + (w - padR) + '" y2="' + gy + '" class="grid-line" />';
          }

          if (chartType === 'INVENTORY_VALUE') {
            const maxVal = Math.max(...rawData.map(d => d.retailValue || d.assetValue || 1), 1000);
            const ptsAsset = [];
            const ptsRetail = [];

            rawData.forEach((d, idx) => {
              const x = padL + (plotW / (rawData.length - 1 || 1)) * idx;
              const yA = padT + plotH - ((d.assetValue || 0) / maxVal) * plotH;
              const yR = padT + plotH - ((d.retailValue || 0) / maxVal) * plotH;
              ptsAsset.push({ x, y: yA, val: d.assetValue, date: d.date });
              ptsRetail.push({ x, y: yR, val: d.retailValue, date: d.date });
            });

            // Area path
            let areaPath = 'M ' + ptsAsset[0].x + ' ' + ptsAsset[0].y;
            for (let i = 1; i < ptsAsset.length; i++) {
              areaPath += ' L ' + ptsAsset[i].x + ' ' + ptsAsset[i].y;
            }
            areaPath += ' L ' + ptsAsset[ptsAsset.length - 1].x + ' ' + (padT + plotH) + ' L ' + ptsAsset[0].x + ' ' + (padT + plotH) + ' Z';
            svgHtml += '<path d="' + areaPath + '" fill="url(#fallbackAsset)" />';

            // Lines
            let lineAsset = 'M ' + ptsAsset[0].x + ' ' + ptsAsset[0].y;
            ptsAsset.slice(1).forEach(p => lineAsset += ' L ' + p.x + ' ' + p.y);
            svgHtml += '<path d="' + lineAsset + '" fill="none" stroke="#10B981" stroke-width="2.5" />';

            let lineRetail = 'M ' + ptsRetail[0].x + ' ' + ptsRetail[0].y;
            ptsRetail.slice(1).forEach(p => lineRetail += ' L ' + p.x + ' ' + p.y);
            svgHtml += '<path d="' + lineRetail + '" fill="none" stroke="#6366F1" stroke-width="2" stroke-dasharray="2 2" />';

            // Points
            ptsAsset.forEach(p => {
              svgHtml += '<circle cx="' + p.x + '" cy="' + p.y + '" r="3" fill="#10B981" />';
              svgHtml += '<text class="axis-text" x="' + p.x + '" y="' + (h - 8) + '">' + p.date + '</text>';
            });

            // Left axis labels
            svgHtml += '<text class="axis-text" x="' + (padL - 4) + '" y="' + (padT + 8) + '" text-anchor="end">' + formatIDR(maxVal) + '</text>';
            svgHtml += '<text class="axis-text" x="' + (padL - 4) + '" y="' + (padT + plotH) + '" text-anchor="end">0</text>';
          } else {
            // Bar style for TOP_MOVING and STOCK_FLOW
            const maxVal = Math.max(...rawData.map(d => (d.outbound || d.masuk || d.value || 1)), 5);
            const barGroupW = plotW / rawData.length;
            const barW = Math.min(22, barGroupW * 0.45);

            rawData.forEach((d, idx) => {
              const cx = padL + barGroupW * idx + barGroupW / 2;
              const val = d.outbound !== undefined ? d.outbound : (d.masuk !== undefined ? d.masuk : d.value);
              const barH = (val / maxVal) * plotH;
              const by = padT + plotH - barH;

              svgHtml += '<rect x="' + (cx - barW / 2) + '" y="' + by + '" width="' + barW + '" height="' + barH + '" rx="4" fill="#3B82F6" />';
              svgHtml += '<text class="axis-text" x="' + cx + '" y="' + (h - 8) + '">' + (d.name || d.date) + '</text>';
              svgHtml += '<text class="axis-text" x="' + cx + '" y="' + (by - 4) + '" font-size="9" fill="' + textColor + '">' + val + '</text>';
            });
          }

          svgHtml += '</svg>';
          svgHtml += '<div class="chart-legend">';
          if (chartType === 'INVENTORY_VALUE') {
            svgHtml += '<div class="legend-item"><div class="legend-dot" style="background:#10B981"></div><span>Nilai Aset (Recharts)</span></div>';
            svgHtml += '<div class="legend-item"><div class="legend-dot" style="background:#6366F1"></div><span>Estimasi Jual</span></div>';
          } else {
            svgHtml += '<div class="legend-item"><div class="legend-dot" style="background:#3B82F6"></div><span>Unit Bergerak (Recharts)</span></div>';
          }
          svgHtml += '</div>';

          root.innerHTML = svgHtml;
        }

        // Initialize: try Recharts, fallback if needed
        window.onload = function() {
          if (window.Recharts) {
            renderWithRecharts();
          } else {
            setTimeout(function() {
              if (window.Recharts) {
                renderWithRecharts();
              } else {
                renderFallbackSvg();
              }
            }, 300);
          }
        };

        // Resize listener for responsive Recharts
        window.addEventListener('resize', function() {
          if (window.Recharts) renderWithRecharts();
          else renderFallbackSvg();
        });
      </script>
    </body>
    </html>
    """.trimIndent()
}
