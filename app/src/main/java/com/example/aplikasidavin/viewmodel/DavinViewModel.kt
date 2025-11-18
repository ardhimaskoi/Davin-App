package com.example.aplikasidavin.viewmodel

import android.util.Log
import retrofit2.HttpException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aplikasidavin.data.api.RetrofitInstance
import com.example.aplikasidavin.data.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import org.json.JSONObject
import java.lang.Exception

class DavinViewModel : ViewModel() {

    // ================================
    // 🛡 RATE LIMIT PROTECTION
    // ================================
    private val _rateLimited = MutableStateFlow(false)
    val rateLimited: StateFlow<Boolean> = _rateLimited

    private val _cooldown = MutableStateFlow(0)
    val cooldown: StateFlow<Int> = _cooldown

    private fun startCooldown() {
        if (_cooldown.value > 0) return

        _rateLimited.value = true
        _cooldown.value = 60

        viewModelScope.launch {
            while (_cooldown.value > 0) {
                delay(1000)
                _cooldown.value -= 1
            }
            _rateLimited.value = false
        }
    }

    private fun safeDouble(any: Any?): Double {
        return try {
            any.toString().toDouble()
        } catch (_: Exception) {
            Double.NaN
        }
    }

    // =================================
    // STATE
    // =================================
    private val _prices = MutableStateFlow<Map<String, Map<String, Double>>>(emptyMap())
    val prices: StateFlow<Map<String, Map<String, Double>>> = _prices

    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions: StateFlow<List<Transaction>> = _transactions

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser

    private val _portfolio = MutableStateFlow<List<Portfolio>>(emptyList())
    val portfolio: StateFlow<List<Portfolio>> = _portfolio

    private val _chartData = MutableStateFlow<List<Pair<Double, Double>>>(emptyList())
    val chartData: StateFlow<List<Pair<Double, Double>>> = _chartData


    // =================================
    // 🔹 Fetch Prices + Rate Limit Handling
    // =================================
    fun fetchPrices(onError: (String) -> Unit = {}) {
        if (_rateLimited.value) {
            onError("⚠️ Tunggu ${_cooldown.value} detik sebelum refresh harga.")
            return
        }

        viewModelScope.launch {
            try {
                val res = RetrofitInstance.api.getCryptoPrices()

                val status = res["status"] as? Map<*, *>
                if (status != null && status["error_code"] == 429) {

                    _prices.value = emptyMap()
                    _prices.value = _prices.value.toMap()   // 🔥 force recomposition

                    startCooldown()
                    onError("⚠️ Rate Limit CoinGecko — coba lagi dalam 1 menit.")
                    return@launch
                }

                _prices.value = res

                val btc = safeDouble(res["bitcoin"]?.get("usd"))
                if (btc.isNaN()) {

                    _prices.value = emptyMap()
                    _prices.value = _prices.value.toMap()   // 🔥 force recomposition

                    startCooldown()
                    onError("⚠️ CoinGecko Rate Limit — menunggu 1 menit.")
                    return@launch
                }

            } catch (e: Exception) {

                _prices.value = emptyMap()
                _prices.value = _prices.value.toMap()       // 🔥 force recomposition

                onError("⚠️ Tidak bisa ambil harga crypto.")
            }
        }
    }


    // =================================
    // 🔹 Ambil transaksi
    // =================================
    fun fetchTransactions(userId: Int) {
        viewModelScope.launch {
            _transactions.value = RetrofitInstance.api.getTransactions(userId)
        }
    }

    // =================================
    // 🔹 BUY
    // =================================
    fun buyCrypto(userId: Int, investmentId: Int, amount: Double, price: Double, onResult: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val body = BuyRequest(user_id = userId, investment_id = investmentId, amount = amount, price = price)
                val res = RetrofitInstance.api.buyCrypto(body)
                onResult(res["message"].toString())

            } catch (e: HttpException) {
                val msg = parseError(e.response()?.errorBody()?.string())
                onResult("❌ $msg")

            } catch (e: Exception) {
                onResult("❌ ${e.localizedMessage}")
            }
        }
    }

    // =================================
    // 🔹 SELL
    // =================================
    fun sellCrypto(userId: Int, investmentId: Int, amount: Double, price: Double, onResult: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val body = SellRequest(user_id = userId, investment_id = investmentId, amount = amount, price = price)
                val res = RetrofitInstance.api.sellCrypto(body)
                onResult(res["message"].toString())

            } catch (e: HttpException) {
                val msg = parseError(e.response()?.errorBody()?.string())
                onResult("❌ $msg")

            } catch (e: Exception) {
                onResult("❌ ${e.localizedMessage}")
            }
        }
    }

    private fun parseError(errorBody: String?): String {
        return try {
            if (errorBody == null) return "Terjadi kesalahan"
            val json = JSONObject(errorBody)
            json.optString("message", "Terjadi kesalahan")
        } catch (_: Exception) {
            "Terjadi kesalahan"
        }
    }

    // =================================
    // 🔹 TOP UP
    // =================================
    fun topUp(userId: Int, amount: Double, onResult: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val res = RetrofitInstance.api.topUp(userId, mapOf("balance" to amount))
                onResult(res["message"].toString())
            } catch (e: Exception) {
                onResult("❌ Gagal: ${e.localizedMessage}")
            }
        }
    }

    // =================================
    // 🔹 Portfolio
    // =================================
    fun fetchPortfolio(userId: Int) {
        viewModelScope.launch {
            try {
                _portfolio.value = RetrofitInstance.api.getPortfolio(userId)
            } catch (_: Exception) { }
        }
    }

    // =================================
    // 🔹 User Profile
    // =================================
    fun fetchUserProfile(userId: Int) {
        viewModelScope.launch {
            try {
                val user = RetrofitInstance.api.getUsers().firstOrNull { it.id == userId }
                _currentUser.value = user
            } catch (_: Exception) {
                _currentUser.value = null
            }
        }
    }

    // =================================
    // 🔹 Chart
    // =================================
    fun fetchChart(symbol: String) {
        viewModelScope.launch {
            _chartData.value = emptyList()
            try {
                val res = RetrofitInstance.api.getChartData(symbol)
                _chartData.value = res.map { Pair(it[0], it[1]) }
            } catch (e: Exception) {
                _chartData.value = emptyList()
            }
        }
    }

    // =================================
    // 🔹 Create Payment
    // =================================
    fun createPayment(userId: Int, amount: Double, onResult: (PaymentResponse?) -> Unit) {
        viewModelScope.launch {
            try {
                val request = PaymentRequest(user_id = userId, amount = amount)
                val res = RetrofitInstance.api.createPayment(request)
                onResult(res)
            } catch (e: Exception) {
                Log.e("PAYMENT", "ERROR: ${e.message}")
                onResult(null)
            }
        }
    }


    // =================================
    // 🔹 Edit
    // =================================
    fun updateTransactionStatus(id: Int, status: String, onResult: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val body = mapOf("status" to status)
                val res = RetrofitInstance.api.updateTransactionStatus(id, body)
                onResult(res["message"].toString())
            } catch (e: Exception) {
                onResult("❌ Gagal: ${e.localizedMessage}")
            }
        }
    }

    // =================================
    // 🔹 Delete trankasasi
    // =================================
    fun deleteTransaction(id: Int, onResult: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val res = RetrofitInstance.api.deleteTransaction(id)
                onResult(res["message"].toString())
            } catch (e: Exception) {
                onResult("❌ Gagal: ${e.localizedMessage}")
            }
        }
    }

    // =================================
    // 🔹 Delete portofolio
    // =================================

    fun deletePortfolio(userId: Int, investmentId: Int, onResult: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val res = RetrofitInstance.api.deletePortfolio(userId, investmentId)
                fetchPortfolio(userId)
                onResult(res["message"].toString())   // sukses
            } catch (e: retrofit2.HttpException) {
                val msg = try {
                    val json = JSONObject(e.response()?.errorBody()?.string() ?: "")
                    json.optString("message", "Gagal menghapus")
                } catch (_: Exception) {
                    "Gagal menghapus"
                }
                onResult("❌ $msg")                    // error khusus amount > 0
            } catch (e: Exception) {
                onResult("❌ ${e.localizedMessage}")
            }
        }
    }

    fun deleteUser(userId: Int, onResult: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val res = RetrofitInstance.api.deleteUser(userId)
                onResult(res["message"] ?: "Akun berhasil dihapus")
            } catch (e: Exception) {
                onResult("❌ Gagal hapus akun: ${e.localizedMessage}")
            }
        }
    }



}
