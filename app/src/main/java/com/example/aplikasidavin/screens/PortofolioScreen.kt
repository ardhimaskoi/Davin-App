package com.example.aplikasidavin.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.aplikasidavin.viewmodel.DavinViewModel

@Composable
fun PortfolioScreen(userId: Int, vm: DavinViewModel = viewModel()) {
    val portfolio by vm.portfolio.collectAsState()
    val prices by vm.prices.collectAsState()

    LaunchedEffect(Unit) {
        vm.fetchPortfolio(userId)
        vm.fetchPrices()
    }

    val configuration = LocalConfiguration.current
    val isCompact = configuration.screenWidthDp < 600

    Column(
        Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Text("📊 Portofolio Kamu", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(20.dp))

        if (portfolio.isEmpty()) {
            Text("Belum ada aset 😅")
        } else {
            val values = portfolio.map {
                val price = prices[it.asset.lowercase()]?.get("usd") ?: 0.0
                price * it.amount
            }
            val totalValue = values.sum()
            val portions = values.map { it / totalValue }

            val baseColors = listOf(
                Color(0xFF7E57C2),
                Color(0xFFE91E63),
                Color(0xFFBA68C8),
                Color(0xFF4FC3F7),
                Color(0xFFFFB300),
                Color(0xFF26A69A)
            )
            val colors = List(portfolio.size) { index -> baseColors[index % baseColors.size] }

            // ✅ Responsive layout
            if (isCompact) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    DonutChart(portions, colors, totalValue)
                    Spacer(Modifier.height(20.dp))
                    PortfolioLegend(portfolio, prices, portions, colors)
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Box(
                        Modifier
                            .weight(1.2f)
                            .aspectRatio(1f)
                    ) {
                        DonutChart(portions, colors, totalValue)
                    }
                    Column(
                        Modifier
                            .weight(1f)
                            .padding(start = 12.dp)
                    ) {
                        PortfolioLegend(portfolio, prices, portions, colors)
                    }
                }
            }
        }
    }
}

@Composable
fun DonutChart(portions: List<Double>, colors: List<Color>, total: Double) {
    Box(
        modifier = Modifier.size(230.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(230.dp)) {
            val totalAngle = 360f
            var startAngle = -90f
            val strokeWidth = size.width * 0.12f // Donut tebal proporsional
            portions.forEachIndexed { index, portion ->
                val sweep = (portion * totalAngle).toFloat()
                drawArc(
                    color = colors[index],
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    style = Stroke(width = strokeWidth),
                    size = Size(size.width, size.height)
                )
                startAngle += sweep
            }
        }

        // 💰 Teks tengah
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$ ${formatShort(total)}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Nilai Portofolio",
                color = Color.Gray,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
fun PortfolioLegend(
    portfolio: List<com.example.aplikasidavin.data.model.Portfolio>,
    prices: Map<String, Map<String, Double>>,
    portions: List<Double>,
    colors: List<Color>
) {
    Column {
        portfolio.forEachIndexed { index, item ->
            val price = prices[item.asset.lowercase()]?.get("usd") ?: 0.0
            val totalAssetValue = price * item.amount
            val percentage = portions[index] * 100

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(colors[index], CircleShape)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(item.asset.uppercase(), style = MaterialTheme.typography.bodyMedium)
                }
                Text(
                    "${String.format("%.2f", percentage)}%",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

fun formatShort(value: Double): String {
    return when {
        value >= 1_000_000_000 -> String.format("%.1fM", value / 1_000_000_000)
        value >= 1_000_000 -> String.format("%.1fjt", value / 1_000_000)
        value >= 1_000 -> String.format("%.1fK", value / 1_000)
        else -> String.format("%.0f", value)
    }
}
