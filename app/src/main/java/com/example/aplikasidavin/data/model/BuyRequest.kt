package com.example.aplikasidavin.data.model

data class BuyRequest(
    val user_id: Int,
    val investment_id: Int,
    val amount: Double,
    val price: Double
)
