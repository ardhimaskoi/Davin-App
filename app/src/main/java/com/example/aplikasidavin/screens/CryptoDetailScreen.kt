package com.example.aplikasidavin.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.aplikasidavin.R
import com.example.aplikasidavin.viewmodel.DavinViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun CryptoDetailScreen(
    userId: Int,
    symbol: String,
    navController: NavController,
    snackbarHostState: SnackbarHostState,
    coroutineScope: CoroutineScope,
    vm: DavinViewModel = viewModel()
) {

    // ===============================
    // STATE
    // ===============================

    val chartData by vm.chartData.collectAsState()
    val prices by vm.prices.collectAsState()
    val portfolio by vm.portfolio.collectAsState()

    // ===============================
    // FETCH DATA
    // ===============================

    LaunchedEffect(symbol) {
        vm.fetchPrices { msg ->
            coroutineScope.launch {
                snackbarHostState.showSnackbar(msg)
            }
        }

        vm.fetchChart(symbol)
        vm.fetchPortfolio(userId)
    }

    val price =
        prices[symbol.lowercase()]?.get("usd") ?: 0.0

    val ownedAmount =
        portfolio.firstOrNull {
            it.asset.equals(symbol, true)
        }?.amount ?: 0.0

    // ===============================
    // UI
    // ===============================

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = 20.dp,
                    vertical = 16.dp
                )
                .padding(bottom = 100.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // ===============================
            // HEADER
            // ===============================

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(
                        id = getCryptoIcon(symbol)
                    ),
                    contentDescription = symbol,
                    modifier = Modifier.size(42.dp)
                )

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = symbol.uppercase(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                    Text(
                        text = getCryptoFullName(symbol),
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
            }

            // ===============================
            // PRICE
            // ===============================

            Text(
                text = "$${"%,.2f".format(price)}",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold
                )
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "-$225.07 (-1.28%)",
                color = Color(0xFFD32F2F),
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ===============================
            // CHART
            // ===============================

            if (chartData.isEmpty()) {
                Text("⏳ Memuat grafik harga...")
            } else {
                CryptoChart(
                    prices = chartData.map { it.second }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ===============================
            // OWNERSHIP
            // ===============================

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 2.dp
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 16.dp,
                            vertical = 14.dp
                        )
                ) {
                    Text(
                        text = "Kepemilikan ${symbol.uppercase()}",
                        fontWeight = FontWeight.Medium,
                        color = Color.Gray,
                        fontSize = 14.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = String.format(
                            "%.8f %s",
                            ownedAmount,
                            symbol.uppercase()
                        ),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ===============================
            // DESCRIPTION
            // ===============================

            Text(
                text = getCryptoDescription(symbol),
                color = Color.Gray,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        // ===============================
        // BOTTOM ACTION BAR
        // ===============================

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.White)
                .padding(
                    horizontal = 16.dp,
                    vertical = 12.dp
                ),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            OutlinedButton(
                onClick = {
                    navController.navigate(
                        "sell/$userId/$symbol"
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFF5E35B1)
                )
            ) {
                Text("Jual")
            }

            Button(
                onClick = {
                    navController.navigate(
                        "buy/$userId/$symbol"
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF5E35B1)
                )
            ) {
                Text(
                    text = "Beli",
                    color = Color.White
                )
            }
        }
    }
}

// ===============================
// CHART
// ===============================

@Composable
fun CryptoChart(
    prices: List<Double>
) {
    if (prices.isEmpty()) return

    val gradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF5E35B1).copy(alpha = 0.4f),
            Color.Transparent
        )
    )

    val minY = prices.minOrNull() ?: 0.0
    val maxY = prices.maxOrNull() ?: 0.0

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
    ) {

        val stepX = size.width / (prices.size - 1)
        val stepY =
            if (maxY - minY != 0.0)
                size.height / (maxY - minY).toFloat()
            else 1f

        for (i in 1 until prices.size) {

            val x1 = (i - 1) * stepX
            val y1 =
                size.height -
                        (prices[i - 1] - minY).toFloat() * stepY

            val x2 = i * stepX
            val y2 =
                size.height -
                        (prices[i] - minY).toFloat() * stepY

            drawLine(
                color = Color(0xFF5E35B1),
                start = Offset(x1, y1),
                end = Offset(x2, y2),
                strokeWidth = 3f
            )
        }

        drawRect(
            brush = gradient,
            topLeft = Offset(
                0f,
                size.height * 0.3f
            ),
            size = size.copy(
                height = size.height * 0.7f
            )
        )
    }
}

// ===============================
// HELPERS
// ===============================

fun getCryptoIcon(
    symbol: String
): Int =
    when (symbol.lowercase()) {
        "btc", "bitcoin" -> R.drawable.bitcoin
        "eth", "ethereum" -> R.drawable.ethereum
        "sol", "solana" -> R.drawable.solana
        else -> R.drawable.bitcoin
    }

fun getCryptoFullName(
    symbol: String
): String =
    when (symbol.lowercase()) {
        "bitcoin" -> "Bitcoin"
        "ethereum" -> "Ethereum"
        "solana" -> "Solana"
        "bnb" -> "BNB Chain"
        "xrp" -> "Ripple"
        else -> symbol.uppercase()
    }

fun getCryptoDescription(
    symbol: String
): String =
    when (symbol.lowercase()) {
        "bitcoin" ->
            "Bitcoin adalah mata uang kripto pertama di dunia yang berjalan tanpa otoritas pusat dan menggunakan sistem blockchain untuk menjaga transparansi transaksi."
        "ethereum" ->
            "Ethereum adalah platform blockchain yang mendukung smart contract dan aplikasi terdesentralisasi (dApps)."
        "solana" ->
            "Solana adalah blockchain berkecepatan tinggi dengan biaya transaksi rendah untuk mendukung berbagai aplikasi Web3."
        "bnb" ->
            "BNB Chain adalah blockchain yang dikembangkan oleh Binance, digunakan untuk transaksi cepat dan efisien."
        "xrp" ->
            "Ripple (XRP) berfokus pada sistem pembayaran lintas negara dengan biaya rendah dan kecepatan tinggi."
        else ->
            "Aset kripto ini merupakan bagian dari inovasi blockchain global yang berkembang pesat."
    }
