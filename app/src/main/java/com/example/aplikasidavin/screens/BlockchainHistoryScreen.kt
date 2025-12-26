package com.example.aplikasidavin.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Divider
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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

    // ===============================
    // STATE
    // ===============================

    val records = vm.records.value

    // ===============================
    // FETCH DATA
    // ===============================

    LaunchedEffect(Unit) {
        vm.fetchUserRecords(userId)
    }

    // ===============================
    // UI
    // ===============================

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                horizontal = 20.dp,
                vertical = 10.dp
            )
    ) {

        // ===============================
        // HEADER
        // ===============================

        Text(
            text = "Riwayat Blockchain",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            modifier = Modifier.padding(vertical = 12.dp)
        )

        // ===============================
        // CONTENT
        // ===============================

        if (records.isEmpty()) {

            Text(
                text = "Belum ada catatan blockchain",
                color = Color.Gray,
                fontSize = 14.sp
            )

        } else {

            records.forEachIndexed { index, item ->

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {

                    // ===============================
                    // ACTION & STOCK
                    // ===============================

                    Text(
                        text = "${item.action} • ${item.stock}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // ===============================
                    // AMOUNT
                    // ===============================

                    Text(
                        text = "Amount: ${item.amount}",
                        fontSize = 14.sp,
                        color = Color(0xFF444444)
                    )

                    // ===============================
                    // DATE
                    // ===============================

                    Text(
                        text = "Tanggal: ${item.created_at}",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // ===============================
                    // VERIFY BUTTON
                    // ===============================

                    Text(
                        text = "Verifikasi",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF5E35B1),
                        modifier = Modifier.clickable {
                            vm.verifyRecord(item.id) { result ->
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(result)
                                }
                            }
                        }
                    )
                }

                // ===============================
                // DIVIDER
                // ===============================

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
