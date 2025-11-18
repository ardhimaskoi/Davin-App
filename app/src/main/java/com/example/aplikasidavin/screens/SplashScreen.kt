package com.example.aplikasidavin.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.aplikasidavin.data.local.UserPreferences
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(navController: NavController) {
    // animation state
    var visible by remember { mutableStateOf(false) }
    val scale = remember { Animatable(0.8f) }

    val context = LocalContext.current


    val prefs = remember { UserPreferences(context) }

    // launch animation + navigate
    LaunchedEffect(Unit) {
        delay(300)
        visible = true
        scale.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800)
        )
        delay(1800)
        val userId = prefs.getUserIdOnce() // ✅ cek apakah user sudah login

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

    // UI with gradient & animations
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF512DA8), Color(0xFF9575CD))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = rememberAsyncImagePainter("https://i.ibb.co/2NRsTXN/davin-logo.png"),
                contentDescription = "Davin Logo",
                modifier = Modifier
                    .size(110.dp)
                    .scale(scale.value)
            )

            Spacer(Modifier.height(20.dp))

            AnimatedVisibility(visible = visible) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "DAVIN",
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Decentralized Investment App",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 14.sp,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            Spacer(Modifier.height(100.dp))
        }

        // Footer text kecil di bawah
        Text(
            text = "© 2025 Davin Labs",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 12.sp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
        )
    }
}
