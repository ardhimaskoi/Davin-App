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
fun SellScreen(
    userId: Int,
    symbol: String,
    snackbarHostState: SnackbarHostState,
    coroutineScope: CoroutineScope,
    navController: NavController,
    vm: DavinViewModel = viewModel(),
    blockchainVM: BlockchainViewModel = viewModel()
) {

    // ===============================
    // STATE
    // ===============================

    var amount by remember { mutableStateOf("") }
    var shouldFocus by remember { mutableStateOf(false) }

    val prices by vm.prices.collectAsState()

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
    // CALCULATION
    // ===============================

    val short = getShortSymbol(symbol)
    val priceUsd = prices[symbol.lowercase()]?.get("usd") ?: 0.0

    val coinAmount = amount.toDoubleOrNull() ?: 0.0
    val totalUsd = coinAmount * priceUsd

    val totalDisplay = "%,.2f".format(totalUsd)
    val amountDisplay = if (amount.isEmpty()) "0" else amount.trimEnd('.')

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
                text = "Jual ${short.uppercase()}",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFB00020)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Harga 1 ${short.uppercase()}: $ ${"%,.2f".format(priceUsd)}",
                color = Color.Gray,
                fontSize = 13.sp
            )
        }

        // ===============================
        // DISPLAY BESAR
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
                text = "$amountDisplay ${short.uppercase()}",
                fontSize = 42.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "≈ $ $totalDisplay",
                color = Color.Gray,
                fontSize = 14.sp
            )
        }

        // ===============================
        // HIDDEN INPUT
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
        // SELL BUTTON
        // ===============================

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Button(
                onClick = {

                    val sellAmount = amount.toDoubleOrNull()

                    if (sellAmount == null || sellAmount <= 0) {
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
                    // 1️⃣ SELL PROCESS
                    // ===============================

                    vm.sellCrypto(
                        userId = userId,
                        investmentId = getInvestmentId(symbol),
                        amount = sellAmount,
                        price = priceUsd
                    ) { msg ->

                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(msg)
                        }

                        if (msg.contains("❌")) return@sellCrypto

                        // ===============================
                        // 2️⃣ BLOCKCHAIN RECORD
                        // ===============================

                        blockchainVM.recordActivity(
                            userId = userId,
                            action = "SELL",
                            stock = symbol.uppercase(),
                            amount = sellAmount
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
                    text = "Jual Sekarang",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
