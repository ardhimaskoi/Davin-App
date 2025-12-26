package com.example.aplikasidavin.screens.auth

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.aplikasidavin.data.local.UserPreferences
import com.example.aplikasidavin.viewmodel.AuthViewModel
import com.example.aplikasidavin.viewmodel.AuthViewModelFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

// ===============================
// COLOR
// ===============================

private val DavinBlue = Color(0xFF1B2C67)
private val BorderGray = Color(0xFFE0E0E0)

@Composable
fun LoginScreen(
    navController: NavController,
    snackbarHostState: SnackbarHostState,
    coroutineScope: CoroutineScope
) {

    // ===============================
    // INIT
    // ===============================

    val context = LocalContext.current
    val prefs = remember { UserPreferences(context) }

    val authVM: AuthViewModel =
        viewModel(factory = AuthViewModelFactory(prefs))

    val message by authVM.message.collectAsState()

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // ===============================
    // ANIMATION (OPTIONAL)
    // ===============================

    LaunchedEffect(Unit) {
        Animatable(0f).animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 500)
        )
    }

    // ===============================
    // SNACKBAR HANDLER
    // ===============================

    if (message.isNotEmpty()) {
        LaunchedEffect(message) {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(message)
        }
    }

    // ===============================
    // UI
    // ===============================

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 28.dp),
        verticalArrangement = Arrangement.Center
    ) {

        // ===============================
        // HEADER TEXT
        // ===============================

        Text(
            text = "Masuk",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Akses akun investasi Anda",
            fontSize = 14.sp,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(32.dp))

        // ===============================
        // USERNAME FIELD
        // ===============================

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = DavinBlue,
                unfocusedBorderColor = BorderGray,
                focusedLabelColor = DavinBlue,
                cursorColor = DavinBlue,
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black
            )
        )

        Spacer(modifier = Modifier.height(14.dp))

        // ===============================
        // PASSWORD FIELD
        // ===============================

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions.Default.copy(
                keyboardType = KeyboardType.Password
            ),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = DavinBlue,
                unfocusedBorderColor = BorderGray,
                focusedLabelColor = DavinBlue,
                cursorColor = DavinBlue,
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        // ===============================
        // LOGIN BUTTON
        // ===============================

        Button(
            onClick = {
                if (username.isBlank() || password.isBlank()) {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(
                            "❌ Semua field wajib diisi!"
                        )
                    }
                } else {
                    authVM.login(username, password) { userId ->
                        coroutineScope.launch {
                            prefs.saveUserId(userId)
                        }

                        navController.navigate("home/$userId") {
                            popUpTo("login") { inclusive = true }
                        }
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = DavinBlue
            )
        ) {
            Text(
                text = "Masuk",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // ===============================
        // REGISTER LINK
        // ===============================

        TextButton(
            onClick = {
                navController.navigate("register")
            },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text(
                text = "Daftar akun baru",
                color = DavinBlue,
                fontSize = 14.sp
            )
        }
    }
}
