package com.example.aplikasidavin.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.aplikasidavin.viewmodel.BlockchainViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun BlockchainHistoryScreen(
    userId: Int,
    snackbarHostState: SnackbarHostState,
    coroutineScope: CoroutineScope,
    vm: BlockchainViewModel = viewModel()
) {
    val records = vm.records.value

    LaunchedEffect(Unit) {
        vm.fetchUserRecords(userId)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        Text(
            "Riwayat Blockchain",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            modifier = Modifier.padding(vertical = 12.dp)
        )

        if (records.isEmpty()) {
            Text(
                "Belum ada catatan blockchain",
                color = Color.Gray,
                fontSize = 14.sp
            )
        } else {
            records.forEachIndexed { index, item ->

                Column(modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                ) {

                    // ACTION + STOCK
                    Text(
                        "${item.action} • ${item.stock}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black
                    )

                    Spacer(Modifier.height(4.dp))

                    // AMOUNT
                    Text(
                        "Amount: ${item.amount}",
                        fontSize = 14.sp,
                        color = Color(0xFF444444)
                    )

                    // DATE
                    Text(
                        "Tanggal: ${item.created_at}",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )

                    Spacer(Modifier.height(6.dp))

                    // TOMBOL VERIFIKASI
                    Text(
                        text = "Verifikasi",
                        fontSize = 14.sp,
                        color = Color(0xFF5E35B1),
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable {
                            vm.verifyRecord(item.id) { result ->
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(result)
                                }
                            }
                        }
                    )
                }

                // Divider antar item
                if (index != records.lastIndex) {
                    Divider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        thickness = 1.dp,
                        color = Color(0xFFE0E0E0)
                    )
                }
            }
        }
    }
}
