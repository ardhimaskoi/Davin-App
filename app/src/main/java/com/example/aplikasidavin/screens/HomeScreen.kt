package com.example.aplikasidavin.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.example.aplikasidavin.R
import com.example.aplikasidavin.viewmodel.DavinViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    userId: Int,
    navController: NavController,
    vm: DavinViewModel = viewModel()
) {
    val currentUser by vm.currentUser.collectAsState()
    val prices by vm.prices.collectAsState()
    val portfolio by vm.portfolio.collectAsState()

    // === Snackbar + CoroutineScope ===
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // === FETCH DATA (Dengan Error Handler) ===
    LaunchedEffect(Unit) {
        vm.fetchUserProfile(userId)
        vm.fetchPortfolio(userId)

        vm.fetchPrices { msg ->
            coroutineScope.launch {
                snackbarHostState.showSnackbar(msg)
            }
        }
    }

    val username = currentUser?.username ?: "User"
    val balance = currentUser?.balance ?: 0.0

    // === HITUNG TOTAL PORTOFOLIO ===
    val totalPortfolioValue = portfolio.sumOf { item ->
        val price = prices[item.asset.lowercase()]?.get("usd") ?: 0.0
        item.amount * price
    }
    val totalWealth = balance + totalPortfolioValue

    // === GAMBAR CAROUSEL ===
    val promoImages = listOf(R.drawable.promo, R.drawable.promo2, R.drawable.promo3)
    val pagerState = rememberPagerState(pageCount = { promoImages.size })

    // Auto-slide
    LaunchedEffect(pagerState) {
        while (true) {
            delay(3000)
            pagerState.animateScrollToPage((pagerState.currentPage + 1) % promoImages.size)
        }
    }

    // === MAIN UI ===
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {

        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {

            Spacer(Modifier.height(20.dp))

            Text(
                "Halo, ${username.uppercase()} 👋",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(8.dp))

            // === CARD TOTAL KEKAYAAN ===
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(14.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Total Kekayaan",
                            color = Color(0xFF8E8E93),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(Modifier.height(8.dp))

                        Text(
                            text = "$ ${"%,.2f".format(totalWealth)}",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1C1C1E)
                        )
                    }

                    OutlinedButton(
                        onClick = { navController.navigate("profile/$userId") },
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF5E35B1)),
                        border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 2.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text("Isi Saldo", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // === CAROUSEL PROMO ===
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.height(160.dp)
            ) { page ->
                Card(
                    shape = RoundedCornerShape(18.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp)
                ) {
                    Image(
                        painter = painterResource(id = promoImages[page]),
                        contentDescription = "Promo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Text(
                "🔥 Aset Kripto Kamu",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(10.dp))

            if (prices.isEmpty()) {
                Text("⏳ Memuat data harga...", color = Color.Gray)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(prices.entries.toList()) { (coin, data) ->
                        val price = data["usd"] ?: 0.0
                        CryptoListCard(
                            name = coin.uppercase(),
                            symbol = coin.lowercase(),
                            price = price,
                            onClick = {
                                navController.navigate("crypto_detail/$userId/${coin.lowercase()}")
                            }
                        )
                    }

                    item {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "＋ Tambahkan Aset",
                            color = Color(0xFF6200EE),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        // === SNACKBAR HOST (WAJIB) ===
        // === SNACKBAR HOST (WAJIB) ===
        SnackbarHost(
            hostState = snackbarHostState,
            snackbar = { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = Color.White,        // 🔥 Background putih
                    contentColor = Color(0xFF4A148C),    // 🔥 Teks ungu elegan (bisa diubah)
                    actionContentColor = Color(0xFF4A148C),
                    shape = RoundedCornerShape(12.dp),   // Rounded biar modern
                    modifier = Modifier.padding(16.dp)
                )
            }
        )
    }
}

@Composable
fun CryptoListCard(name: String, symbol: String, price: Double, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(70.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(14.dp),
        onClick = onClick
    ) {
        Row(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {

                val iconRes = when (symbol.lowercase()) {
                    "btc" -> R.drawable.bitcoin
                    "ethereum" -> R.drawable.ethereum
                    "solana" -> R.drawable.solana
                    else -> R.drawable.bitcoin
                }

                Image(
                    painter = painterResource(id = iconRes),
                    contentDescription = name,
                    modifier = Modifier.size(36.dp)
                )

                Spacer(Modifier.width(12.dp))

                Column {
                    Text(name, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Text(symbol.uppercase(), color = Color.Gray, fontSize = 12.sp)
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "$${"%,.2f".format(price)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Text(" ", fontSize = 11.sp)
            }
        }
    }
}
