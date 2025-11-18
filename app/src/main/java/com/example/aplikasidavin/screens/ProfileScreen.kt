package com.example.aplikasidavin.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.aplikasidavin.data.local.UserPreferences
import com.example.aplikasidavin.data.model.Portfolio
import com.example.aplikasidavin.data.model.Transaction
import com.example.aplikasidavin.viewmodel.DavinViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    userId: Int,
    navController: NavController,
    snackbarHostState: SnackbarHostState,
    coroutineScope: CoroutineScope,
    vm: DavinViewModel = viewModel(),
) {
    val user by vm.currentUser.collectAsState()
    val portfolio by vm.portfolio.collectAsState()
    val prices by vm.prices.collectAsState()
    val transactions by vm.transactions.collectAsState()

    var topUpAmount by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val prefs = remember { UserPreferences(context) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Fetch data awal
    LaunchedEffect(Unit) {
        vm.fetchUserProfile(userId)
        vm.fetchPortfolio(userId)
        vm.fetchPrices()
        vm.fetchTransactions(userId)
    }

    // Snackbar lokal (untuk top up)
    if (message.isNotEmpty()) {
        LaunchedEffect(message) {
            snackbarHostState.currentSnackbarData?.dismiss()
            coroutineScope.launch {
                snackbarHostState.showSnackbar(message)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF5E35B1), Color(0xFF9575CD))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // Avatar
            Image(
                painter = rememberAsyncImagePainter(
                    "https://api.dicebear.com/7.x/identicon/svg?seed=${user?.username ?: "User"}"
                ),
                contentDescription = "Avatar",
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f))
                    .padding(8.dp)
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = user?.username ?: "Memuat...",
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = Color.White
            )

            Text(
                text = user?.email ?: "",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.8f)
            )

            Spacer(Modifier.height(28.dp))

            // SALDO
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Saldo Tersedia", color = Color.Gray, fontSize = 14.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "$ ${"%,.2f".format(user?.balance ?: 0.0)}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 26.sp,
                        color = Color(0xFF4A148C)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // TOP UP
            Text(
                "💰 Tambah Saldo",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = topUpAmount,
                onValueChange = { topUpAmount = it.filter { c -> c.isDigit() } },
                label = { Text("Nominal Top Up ($)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.White,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                    focusedLabelColor = Color.White,
                    unfocusedLabelColor = Color.White.copy(alpha = 0.8f),
                    cursorColor = Color.White,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    val amount = topUpAmount.toDoubleOrNull()
                    if (amount == null || amount <= 0) {
                        message = "❌ Nominal tidak valid!"
                        return@Button
                    }

                    vm.createPayment(userId, amount) { res ->
                        if (res == null) {
                            message = "❌ Gagal membuat transaksi!"
                            return@createPayment
                        }

                        val uri = android.net.Uri.parse(res.redirect_url)
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
                        context.startActivity(intent)

                        topUpAmount = ""
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White)
            ) {
                Text("Tambahkan Saldo", color = Color(0xFF4A148C), fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(30.dp))

            // PORTOFOLIO
            Text(
                "📊 Portofolio Saya",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(Modifier.height(12.dp))

            if (portfolio.isEmpty()) {
                Text("Belum ada aset yang dimiliki.", color = Color.White.copy(alpha = 0.8f))
            } else {
                PortfolioSection(
                    portfolio = portfolio,
                    prices = prices,
                    transactions = transactions,

                    // 🔥 Inilah bagian pentingnya
                    onDeletePortfolio = { investmentId ->
                        vm.deletePortfolio(userId, investmentId) { msg ->
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(msg)
                            }
                        }
                    }
                )
            }

            Spacer(Modifier.height(40.dp))

            // Logout
            Button(
                onClick = {
                    coroutineScope.launch {
                        prefs.clearUser()
                        snackbarHostState.showSnackbar("✅ Logout berhasil!")
                        navController.navigate("login") {
                            popUpTo("home/$userId") { inclusive = true }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Logout", color = Color.White, fontWeight = FontWeight.SemiBold)
            }



            Spacer(Modifier.height(20.dp))

            // === HAPUS AKUN ===

            Button(
                onClick = { showDeleteDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB71C1C)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    "Hapus Akun",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
            }

            if (showDeleteDialog) {
                AlertDialog(
                    onDismissRequest = { showDeleteDialog = false },
                    title = {
                        Text("Konfirmasi Hapus Akun", fontWeight = FontWeight.Bold)
                    },
                    text = {
                        Text("Akun ini akan dihapus permanen beserta transaksi, portofolio dan activity log. Lanjutkan?")
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showDeleteDialog = false

                                vm.deleteUser(userId) { msg ->
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar(msg)
                                    }

                                    // Hapus sesi user
                                    coroutineScope.launch {
                                        prefs.clearUser()
                                    }

                                    // Arahkan ke login
                                    navController.navigate("login") {
                                        popUpTo("home/$userId") { inclusive = true }
                                    }
                                }
                            }
                        ) {
                            Text("Hapus", color = Color.Red, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteDialog = false }) {
                            Text("Batal", color = Color.Gray)
                        }
                    },
                    containerColor = Color.White,
                    shape = RoundedCornerShape(16.dp)
                )
            }

            Spacer(Modifier.height(20.dp))

        }
    }
}

@Composable
fun PortfolioSection(
    portfolio: List<Portfolio>,
    prices: Map<String, Map<String, Double>>,
    transactions: List<Transaction>,
    onDeletePortfolio: (Int) -> Unit
) {
    val totalMarketValue = portfolio.sumOf { item ->
        val price = prices[item.asset.lowercase()]?.get("usd") ?: 0.0
        item.amount * price
    }

    val totalInvested = transactions.sumOf { tx ->
        when (tx.type.uppercase()) {
            "BUY"  -> tx.total_price
            "SELL" -> -tx.total_price
            else   -> 0.0
        }
    }.coerceAtLeast(0.0)

    val totalReturnValue = totalMarketValue - totalInvested
    val totalReturnPercent =
        if (totalInvested > 0) (totalReturnValue / totalInvested) * 100 else 0.0

    val returnColor = when {
        totalReturnPercent > 0 -> Color(0xFF2E7D32)
        totalReturnPercent < 0 -> Color(0xFFC62828)
        else -> Color.Gray
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Text("Total Nilai Portofolio", color = Color.Gray, fontSize = 13.sp)
        Spacer(Modifier.height(4.dp))
        Text(
            "$ ${"%,.2f".format(totalMarketValue)}",
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = Color(0xFF4A148C)
        )

        Spacer(Modifier.height(8.dp))
        Text(
            "Total Return: ${"%.2f".format(totalReturnPercent)}%",
            color = returnColor,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp
        )

        Spacer(Modifier.height(12.dp))

        portfolio.forEach { item ->

            val price = prices[item.asset.lowercase()]?.get("usd") ?: 0.0
            val value = price * item.amount

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(item.asset.uppercase(), fontWeight = FontWeight.Medium)
                    Text(
                        "Total ${"%.6f".format(item.amount)}",
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {

                    Text(
                        "$ ${"%,.2f".format(value)}",
                        color = Color.Black,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(Modifier.width(12.dp))

                    // 🔥 Tombol HAPUS
                    Text(
                        "Hapus",
                        color = if (item.amount == 0.0) Color(0xFFD32F2F) else Color.LightGray,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .padding(4.dp)
                            .clickable(enabled = item.amount == 0.0) {
                                onDeletePortfolio(item.investment_id)
                            }
                    )
                }
            }
        }
    }
}
