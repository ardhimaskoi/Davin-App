package com.example.aplikasidavin.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aplikasidavin.data.api.RetrofitInstance
import com.example.aplikasidavin.data.model.BlockchainRecord
import com.example.aplikasidavin.data.model.BlockchainRequest
import kotlinx.coroutines.launch

class BlockchainViewModel : ViewModel() {

    // ===============================
    // STATE: BLOCKCHAIN RECORDS (PER USER)
    // ===============================

    private val _records =
        mutableStateOf<List<BlockchainRecord>>(emptyList())

    val records: State<List<BlockchainRecord>> = _records

    // ===============================
    // FETCH RECORDS
    // ===============================

    fun fetchUserRecords(
        userId: Int
    ) {
        viewModelScope.launch {
            try {

                val response =
                    RetrofitInstance.api.getBlockchainRecords()

                if (response.isSuccessful && response.body() != null) {

                    val allRecords = response.body()!!

                    _records.value =
                        allRecords.filter {
                            it.user_id == userId
                        }

                } else {
                    _records.value = emptyList()
                }

            } catch (_: Exception) {
                _records.value = emptyList()
            }
        }
    }

    // ===============================
    // RECORD ACTIVITY TO BLOCKCHAIN
    // ===============================

    fun recordActivity(
        userId: Int,
        action: String,
        stock: String,
        amount: Double,
        onResult: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {

                val body =
                    BlockchainRequest(
                        userId,
                        action,
                        stock,
                        amount
                    )

                val response =
                    RetrofitInstance.api.recordBlockchain(body)

                if (response.isSuccessful && response.body() != null) {

                    val res = response.body()!!

                    val text = """
                        ✔ Blockchain Proof Tersimpan

                        TX Hash:
                        ${res.txHash}

                        Hash:
                        ${res.hash}
                    """.trimIndent()

                    onResult(text)

                } else {
                    onResult(
                        "❌ ${response.errorBody()?.string()}"
                    )
                }

            } catch (e: Exception) {
                onResult(
                    "❌ ${e.localizedMessage}"
                )
            }
        }
    }

    // ===============================
    // VERIFY BLOCKCHAIN RECORD
    // ===============================

    fun verifyRecord(
        id: Int,
        onResult: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {

                val response =
                    RetrofitInstance.api.verifyRecord(id)

                if (response.isSuccessful && response.body() != null) {

                    val res = response.body()!!
                    onResult(res.message)

                } else {
                    onResult(
                        "❌ Error: ${response.errorBody()?.string()}"
                    )
                }

            } catch (e: Exception) {
                onResult(
                    "❌ ${e.localizedMessage}"
                )
            }
        }
    }
}
