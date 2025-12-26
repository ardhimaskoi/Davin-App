package com.example.aplikasidavin.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.aplikasidavin.viewmodel.DavinViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun HistoryScreen(
    userId: Int,
    snackbarHostState: SnackbarHostState,
    coroutineScope: CoroutineScope,
    vm: DavinViewModel = viewModel()
) {

    // ===============================
    // STATE
    // ===============================

    val transactions by vm.transactions.collectAsState()

    LaunchedEffect(Unit) {
        vm.fetchTransactions(userId)
    }

    // ===============================
    // UI
    // ===============================

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(
                horizontal = 20.dp,
                vertical = 16.dp
            )
    ) {

        // ===============================
        // HEADER
        // ===============================

        Text(
            text = "📜 Riwayat Transaksi",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1C1C1E)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Pantau semua aktivitas beli dan jual aset kamu di sini.",
            fontSize = 13.sp,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ===============================
        // CONTENT
        // ===============================

        if (transactions.isEmpty()) {

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Belum ada transaksi",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }

        } else {

            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(transactions) { tx ->

                    TransactionRow(
                        type = tx.type,
                        amount = tx.amount,
                        totalPrice = tx.total_price,
                        date = tx.date,
                        status = tx.status,

                        onMarkRead = {
                            vm.updateTransactionStatus(tx.id, "read") {
                                vm.fetchTransactions(userId)
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(
                                        "✔ Transaksi ditandai sudah dibaca"
                                    )
                                }
                            }
                        },

                        onDelete = {
                            vm.deleteTransaction(tx.id) {
                                vm.fetchTransactions(userId)
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(
                                        "🗑 Transaksi berhasil dihapus!"
                                    )
                                }
                            }
                        }
                    )

                    Divider(
                        color = Color(0xFFE0E0E0),
                        thickness = 1.dp
                    )
                }
            }
        }
    }
}

// ==================================================
// TRANSACTION ROW
// ==================================================

@Composable
fun TransactionRow(
    type: String,
    amount: Double,
    totalPrice: Double,
    date: String,
    status: String,
    onMarkRead: () -> Unit,
    onDelete: () -> Unit
) {

    val isBuy = type.equals("BUY", ignoreCase = true)
    val accentColor =
        if (isBuy) Color(0xFF4CAF50) else Color(0xFFD32F2F)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {

        // ===============================
        // LEFT INFO
        // ===============================

        Column {

            Text(
                text = if (isBuy) "Pembelian Aset" else "Penjualan Aset",
                fontWeight = FontWeight.SemiBold,
                color = accentColor,
                fontSize = 15.sp
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = "Jumlah: ${"%,.4f".format(amount)}",
                color = Color.Gray,
                fontSize = 13.sp
            )

            Text(
                text = date,
                color = Color(0xFF9E9E9E),
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            // ===============================
            // STATUS + DELETE
            // ===============================

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                if (status == "unread") {
                    Text(
                        text = "Tandai dibaca",
                        color = Color(0xFF1976D2),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.clickable {
                            onMarkRead()
                        }
                    )
                } else {
                    Text(
                        text = "Sudah dibaca",
                        color = Color.Gray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Hapus",
                    tint = Color(0xFFD32F2F),
                    modifier = Modifier
                        .size(17.dp)
                        .clickable {
                            onDelete()
                        }
                )
            }
        }

        // ===============================
        // RIGHT PRICE
        // ===============================

        Column(
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text =
                    (if (isBuy) "+" else "-") +
                            " Rp${"%,.0f".format(totalPrice)}",
                color = accentColor,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }
    }
}
