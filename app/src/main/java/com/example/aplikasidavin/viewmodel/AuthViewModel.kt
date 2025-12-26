package com.example.aplikasidavin.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aplikasidavin.data.api.RetrofitInstance
import com.example.aplikasidavin.data.local.UserPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import retrofit2.HttpException

class AuthViewModel(
    private val prefs: UserPreferences
) : ViewModel() {

    // ===============================
    // STATE
    // ===============================

    private val _message = MutableStateFlow("")
    val message: StateFlow<String> = _message

    // ===============================
    // REGISTER
    // ===============================

    fun register(
        username: String,
        email: String,
        password: String,
        onSuccess: (Int) -> Unit
    ) {
        viewModelScope.launch {
            try {

                val body = mapOf(
                    "username" to username,
                    "email" to email,
                    "password" to password
                )

                val response =
                    RetrofitInstance.api.register(body)

                // 🔥 Backend selalu mengirim "user"
                val user = response.user

                if (user != null) {

                    // Simpan userId
                    prefs.saveUserId(user.id)

                    // Callback ke UI
                    onSuccess(user.id)

                    _message.value = "Registrasi berhasil"

                } else {
                    // Kasus langka
                    _message.value =
                        "⚠ Terdaftar, tapi data pengguna tidak lengkap"
                }

            } catch (e: HttpException) {

                val errorBody =
                    e.response()?.errorBody()?.string()

                val serverMsg =
                    parseErrorMessage(errorBody)

                _message.value =
                    serverMsg ?: when (e.code()) {
                        400 -> "❌ Data tidak lengkap"
                        409 -> "⚠ Akun sudah terdaftar"
                        else -> "❌ Gagal registrasi (${e.code()})"
                    }

            } catch (e: Exception) {

                _message.value =
                    "⚠️ Gagal konek ke server: ${e.localizedMessage}"
            }
        }
    }

    // ===============================
    // LOGIN
    // ===============================

    fun login(
        username: String,
        password: String,
        onSuccess: (Int) -> Unit
    ) {
        viewModelScope.launch {
            try {

                val body = mapOf(
                    "username" to username,
                    "password" to password
                )

                val response =
                    RetrofitInstance.api.login(body)

                val user = response.user

                if (user != null) {

                    prefs.saveUserId(user.id)

                    _message.value = "✅ Login berhasil!"

                    onSuccess(user.id)

                } else {
                    _message.value =
                        "❌ Username atau password salah"
                }

            } catch (e: HttpException) {

                val errorBody =
                    e.response()?.errorBody()?.string()

                val errorMsg =
                    parseErrorMessage(errorBody)

                _message.value =
                    errorMsg ?: when (e.code()) {
                        401 -> "❌ Password salah"
                        404 -> "❌ User tidak ditemukan"
                        else -> "❌ Gagal login (${e.code()})"
                    }

            } catch (e: Exception) {

                _message.value =
                    "⚠️ Gagal konek ke server: ${e.localizedMessage}"
            }
        }
    }

    // ===============================
    // UTIL: PARSE ERROR MESSAGE
    // ===============================

    private fun parseErrorMessage(
        errorBody: String?
    ): String? {
        return try {
            if (errorBody == null) return null

            val json = JSONObject(errorBody)
            json.optString("message", null)

        } catch (_: Exception) {
            null
        }
    }
}
