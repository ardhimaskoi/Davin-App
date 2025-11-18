package com.example.aplikasidavin.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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

    var amount by remember { mutableStateOf("") }
    val prices by vm.prices.collectAsState()
    var shouldFocus by remember { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    // === RATE LIMIT HANDLER ===
    LaunchedEffect(Unit) {
        vm.fetchPrices { msg ->
            coroutineScope.launch {
                snackbarHostState.showSnackbar(msg)
            }
        }
    }

    // Auto focus input
    LaunchedEffect(shouldFocus) {
        if (shouldFocus) {
            delay(100)
            focusRequester.requestFocus()
            keyboard?.show()
            shouldFocus = false
        }
    }

    val short = getShortSymbol(symbol)
    val priceUsd = prices[symbol.lowercase()]?.get("usd") ?: 0.0
    val coinAmount = amount.toDoubleOrNull() ?: 0.0
    val totalUsd = coinAmount * priceUsd

    val totalDisplay = "%,.2f".format(totalUsd)
    val amountDisplay = if (amount.isEmpty()) "0" else amount.trimEnd('.')

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {

        // HEADER
        Column(horizontalAlignment = Alignment.Start, modifier = Modifier.fillMaxWidth()) {
            Text(
                "Jual ${short.uppercase()}",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFB00020)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Harga 1 ${short.uppercase()}: $ ${"%,.2f".format(priceUsd)}",
                color = Color.Gray,
                fontSize = 13.sp
            )
        }

        // DISPLAY BESAR
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 40.dp)
                .noRippleClickable { shouldFocus = true }
        ) {
            Text(
                "$amountDisplay ${short.uppercase()}",
                fontSize = 42.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(6.dp))

            Text(
                "≈ $ $totalDisplay",
                color = Color.Gray,
                fontSize = 14.sp
            )
        }

        // INPUT
        BasicTextField(
            value = amount,
            onValueChange = { new ->
                if (new.matches(Regex("^\\d*\\.?\\d*\$"))) {
                    amount = new
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier
                .focusRequester(focusRequester)
                .width(1.dp)
                .height(1.dp)
        )

        // TOMBOL
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = {

                    val sellAmount = amount.toDoubleOrNull()

                    if (sellAmount == null || sellAmount <= 0) {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("❌ Jumlah coin tidak valid")
                        }
                        return@Button
                    }

                    if (priceUsd <= 0) {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("⚠️ Harga tidak tersedia")
                        }
                        return@Button
                    }

                    vm.sellCrypto(
                        userId = userId,
                        investmentId = getInvestmentId(symbol),
                        amount = sellAmount,
                        price = priceUsd
                    ) { msg ->

                        coroutineScope.launch { snackbarHostState.showSnackbar(msg) }

                        if (msg.contains("❌")) return@sellCrypto

                        blockchainVM.recordActivity(
                            userId, "SELL", symbol.uppercase(), sellAmount
                        ) { bcMsg ->
                            coroutineScope.launch { snackbarHostState.showSnackbar(bcMsg) }
                        }

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
                Text("Jual Sekarang", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

