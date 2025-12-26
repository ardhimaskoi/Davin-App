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

private val DavinBlue = Color(0xFF1B2C67)

@Composable
fun ProfileScreen(
    userId: Int,
    navController: NavController,
    snackbarHostState: SnackbarHostState,
    coroutineScope: CoroutineScope,
    vm: DavinViewModel = viewModel()
) {

    val user by vm.currentUser.collectAsState()
    val portfolio by vm.portfolio.collectAsState()
    val prices by vm.prices.collectAsState()
    val transactions by vm.transactions.collectAsState()

    var topUpAmount by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val prefs = remember { UserPreferences(context) }

    val avatarUrl =
        "https://randomuser.me/api/portraits/men/${user?.id?.rem(100) ?: 1}.jpg"


    LaunchedEffect(Unit) {
        vm.fetchUserProfile(userId)
        vm.fetchPortfolio(userId)
        vm.fetchPrices()
        vm.fetchTransactions(userId)
    }

    if (message.isNotEmpty()) {
        LaunchedEffect(message) {
            snackbarHostState.currentSnackbarData?.dismiss()
            coroutineScope.launch {
                snackbarHostState.showSnackbar(message)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // ===== PROFILE =====
        Image(
            painter = rememberAsyncImagePainter(avatarUrl),
            contentDescription = "Avatar",
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(Color(0xFFF1F3F6))
        )


        Spacer(Modifier.height(10.dp))

        Text(user?.username ?: "Memuat...", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(user?.email ?: "", fontSize = 13.sp, color = Color.Gray)

        Spacer(Modifier.height(24.dp))

        // ===== SALDO =====
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FB)),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(Modifier.padding(18.dp)) {
                Text("Saldo Tersedia", color = Color.Gray, fontSize = 13.sp)
                Spacer(Modifier.height(6.dp))
                Text(
                    "$ ${"%,.2f".format(user?.balance ?: 0.0)}",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = DavinBlue
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // ===== TOP UP =====
        Text("Tambah Saldo", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = topUpAmount,
            onValueChange = { topUpAmount = it.filter(Char::isDigit) },
            label = { Text("Nominal ($)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = {
                val amount = topUpAmount.toDoubleOrNull()
                if (amount == null || amount <= 0) {
                    message = "❌ Nominal tidak valid"
                    return@Button
                }

                vm.createPayment(userId, amount) { res ->
                    if (res == null) {
                        message = "❌ Gagal membuat transaksi"
                        return@createPayment
                    }

                    context.startActivity(
                        android.content.Intent(
                            android.content.Intent.ACTION_VIEW,
                            android.net.Uri.parse(res.redirect_url)
                        )
                    )
                    topUpAmount = ""
                }
            },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = DavinBlue)
        ) {
            Text("Tambahkan Saldo", color = Color.White)
        }

        Spacer(Modifier.height(28.dp))

        // ===== PORTFOLIO =====
        Text("Portofolio Saya", fontSize = 18.sp, fontWeight = FontWeight.Bold)

        Spacer(Modifier.height(12.dp))

        if (portfolio.isEmpty()) {
            Text("Belum ada aset", color = Color.Gray)
        } else {
            PortfolioSection(
                portfolio = portfolio,
                prices = prices,
                transactions = transactions,
                onDeletePortfolio = { id ->
                    vm.deletePortfolio(userId, id) {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(it)
                        }
                    }
                }
            )
        }

        Spacer(Modifier.height(30.dp))

        // ===== LOGOUT =====
        Button(
            onClick = {
                coroutineScope.launch {
                    prefs.clearUser()
                    snackbarHostState.showSnackbar("✅ Logout berhasil")
                    navController.navigate("login") {
                        popUpTo("home/$userId") { inclusive = true }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
        ) {
            Text("Logout", color = Color.White)
        }

        Spacer(Modifier.height(16.dp))

        // ===== DELETE ACCOUNT =====
        Button(
            onClick = { showDeleteDialog = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB71C1C))
        ) {
            Text("Hapus Akun", color = Color.White)
        }

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Konfirmasi Hapus Akun", fontWeight = FontWeight.Bold) },
                text = { Text("Akun akan dihapus permanen. Lanjutkan?") },
                confirmButton = {
                    TextButton(onClick = {
                        showDeleteDialog = false
                        vm.deleteUser(userId) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(it)
                                prefs.clearUser()
                                navController.navigate("login") {
                                    popUpTo("home/$userId") { inclusive = true }
                                }
                            }
                        }
                    }) {
                        Text("Hapus", color = Color.Red)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Batal")
                    }
                }
            )
        }
    }
}

// ================= PORTFOLIO SECTION =================
@Composable
fun PortfolioSection(
    portfolio: List<Portfolio>,
    prices: Map<String, Map<String, Double>>,
    transactions: List<Transaction>,
    onDeletePortfolio: (Int) -> Unit
) {

    val totalMarketValue = portfolio.sumOf {
        (prices[it.asset.lowercase()]?.get("usd") ?: 0.0) * it.amount
    }

    val totalInvested = portfolio.sumOf { item ->
        transactions.filter { it.investment_id == item.investment_id }
            .sumOf {
                when (it.type.uppercase()) {
                    "BUY" -> it.total_price
                    "SELL" -> -it.total_price
                    else -> 0.0
                }
            }
    }.coerceAtLeast(0.0)

    val returnPercent =
        if (totalInvested > 0)
            ((totalMarketValue - totalInvested) / totalInvested) * 100
        else 0.0

    val returnColor =
        when {
            returnPercent > 0 -> Color(0xFF2E7D32)
            returnPercent < 0 -> Color(0xFFC62828)
            else -> Color.Gray
        }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FB)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(Modifier.padding(16.dp)) {

            Text("Total Nilai Portofolio", color = Color.Gray, fontSize = 13.sp)
            Text(
                "$ ${"%,.2f".format(totalMarketValue)}",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = DavinBlue
            )

            Text(
                "Return ${"%.2f".format(returnPercent)}%",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = returnColor
            )

            Spacer(Modifier.height(12.dp))

            portfolio.forEach {
                val value =
                    (prices[it.asset.lowercase()]?.get("usd") ?: 0.0) * it.amount
                val canDelete = it.amount == 0.0

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(it.asset.uppercase(), fontWeight = FontWeight.Medium)
                        Text(
                            "Qty ${"%.6f".format(it.amount)}",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text("$ ${"%,.2f".format(value)}", fontWeight = FontWeight.SemiBold)
                        Text(
                            "Hapus",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (canDelete) Color(0xFFD32F2F) else Color.LightGray,
                            modifier = Modifier.clickable(enabled = canDelete) {
                                onDeletePortfolio(it.investment_id)
                            }
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))
            }
        }
    }
}
