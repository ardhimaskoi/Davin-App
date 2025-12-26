package com.example.aplikasidavin.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.aplikasidavin.R
import com.example.aplikasidavin.data.local.UserPreferences
import kotlinx.coroutines.delay

private val DavinBlue = Color(0xFF1B2C67)

@Composable
fun SplashScreen(
    navController: NavController
) {

    val scale = remember { Animatable(0.92f) }
    val context = LocalContext.current
    val prefs = remember { UserPreferences(context) }

    // ===============================
    // ANIMATION + NAVIGATION
    // ===============================
    LaunchedEffect(Unit) {
        delay(300)

        scale.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 700)
        )

        delay(1500)

        val userId = prefs.getUserIdOnce()
        if (userId != null) {
            navController.navigate("home/$userId") {
                popUpTo("splash") { inclusive = true }
            }
        } else {
            navController.navigate("login") {
                popUpTo("splash") { inclusive = true }
            }
        }
    }

    // ===============================
    // UI
    // ===============================
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {

        // SUBTLE TOP ACCENT (VERY LIGHT)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            DavinBlue.copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    )
                )
        )

        // LOGO ONLY
        androidx.compose.foundation.Image(
            painter = painterResource(id = R.drawable.ic_davin_logo),
            contentDescription = "Davin Logo",
            modifier = Modifier
                .size(120.dp) // ⬅️ agak lebih besar
                .scale(scale.value)
                .align(Alignment.Center)
        )
    }
}
