package com.example.aplikasidavin.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.aplikasidavin.viewmodel.BlockchainViewModel
import com.example.aplikasidavin.viewmodel.DavinViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuyScreen(
    userId: Int,
    symbol: String,
    snackbarHostState: SnackbarHostState,
    coroutineScope: CoroutineScope,
    navController: NavController,
    vm: DavinViewModel = viewModel(),
    blockchainVM: BlockchainViewModel = viewModel(),
    onHideBottomNav: (Boolean) -> Unit
) {

    // ===============================
    // BOTTOM NAV VISIBILITY
    // ===============================

    LaunchedEffect(Unit) {
        onHideBottomNav(true)
    }

    DisposableEffect(Unit) {
        onDispose {
            onHideBottomNav(false)
        }
    }

    // ===============================
    // STATE
    // ===============================

    var amount by remember { mutableStateOf("") }
    val prices by vm.prices.collectAsState()

    var shouldFocus by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    // ===============================
    // FETCH PRICE
    // ===============================

    LaunchedEffect(Unit) {
        vm.fetchPrices { msg ->
            coroutineScope.launch {
                snackbarHostState.showSnackbar(msg)
            }
        }
    }

    // ===============================
    // CALCULATION
    // ===============================

    val short = getShortSymbol(symbol)
    val priceUsd = prices[symbol.lowercase()]?.get("usd") ?: 0.0

    val coinAmount = amount.toDoubleOrNull() ?: 0.0
    val totalUsd = coinAmount * priceUsd

    val hargaUsdDisplay = "%,.2f".format(totalUsd)
    val coinDisplay = if (amount.isEmpty()) "0" else amount.trimEnd('.')

    // ===============================
    // AUTO FOCUS INPUT
    // ===============================

    LaunchedEffect(shouldFocus) {
        if (shouldFocus) {
            delay(100)
            focusRequester.requestFocus()
            keyboardController?.show()
            shouldFocus = false
        }
    }

    // ===============================
    // UI
    // ===============================

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                horizontal = 24.dp,
                vertical = 32.dp
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {

        // ===============================
        // HEADER
        // ===============================

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "Beli ${short.uppercase()}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Harga 1 ${short.uppercase()}: $ ${"%,.2f".format(priceUsd)}",
                color = Color.Gray,
                fontSize = 13.sp
            )
        }

        // ===============================
        // DISPLAY INPUT (LARGE)
        // ===============================

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 40.dp)
                .noRippleClickable {
                    shouldFocus = true
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                text = "$coinDisplay ${short.uppercase()}",
                fontSize = 42.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1C1C1E)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "≈ $ $hargaUsdDisplay",
                color = Color.Gray,
                fontSize = 14.sp
            )
        }

        // ===============================
        // HIDDEN REAL INPUT
        // ===============================

        BasicTextField(
            value = amount,
            onValueChange = { new ->
                if (new.matches(Regex("^\\d*\\.?\\d*$"))) {
                    amount = new
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal
            ),
            modifier = Modifier
                .focusRequester(focusRequester)
                .width(1.dp)
                .height(1.dp)
        )

        // ===============================
        // BUY BUTTON
        // ===============================

        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            Button(
                onClick = {

                    // ===============================
                    // VALIDATION
                    // ===============================

                    val inputAmount = amount.toDoubleOrNull()
                    if (inputAmount == null || inputAmount <= 0) {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(
                                "❌ Jumlah coin tidak valid"
                            )
                        }
                        return@Button
                    }

                    if (priceUsd <= 0) {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(
                                "⚠️ Harga tidak tersedia"
                            )
                        }
                        return@Button
                    }

                    // ===============================
                    // 1️⃣ BUY PROCESS
                    // ===============================

                    vm.buyCrypto(
                        userId = userId,
                        investmentId = getInvestmentId(symbol),
                        amount = inputAmount,
                        price = priceUsd
                    ) { msg ->

                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(msg)
                        }

                        // ❌ Jika gagal → stop
                        if (msg.contains("❌")) return@buyCrypto

                        // ===============================
                        // 2️⃣ BLOCKCHAIN RECORD
                        // ===============================

                        blockchainVM.recordActivity(
                            userId = userId,
                            action = "BUY",
                            stock = symbol.uppercase(),
                            amount = inputAmount
                        ) { bcMsg ->
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(bcMsg)
                            }
                        }

                        // ===============================
                        // 3️⃣ NAVIGATE BACK
                        // ===============================

                        coroutineScope.launch {
                            delay(600)
                            navController.popBackStack()
                        }

                        amount = ""
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Beli Sekarang",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/* ======================================
   HELPER FUNCTIONS
   ====================================== */

fun getShortSymbol(symbol: String): String =
    when (symbol.lowercase()) {
        "bitcoin" -> "BTC"
        "ethereum" -> "ETH"
        "solana" -> "SOL"
        else -> symbol.uppercase()
    }

@Composable
fun Modifier.noRippleClickable(
    onClick: () -> Unit
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    return this.clickable(
        indication = null,
        interactionSource = interactionSource
    ) {
        onClick()
    }
}

fun getInvestmentId(symbol: String): Int =
    when (symbol.lowercase()) {
        "btc", "bitcoin" -> 1
        "eth", "ethereum" -> 2
        "sol", "solana" -> 3
        else -> 1
    }
