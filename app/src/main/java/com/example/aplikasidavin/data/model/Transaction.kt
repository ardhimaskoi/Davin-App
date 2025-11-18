package com.example.aplikasidavin.data.model

data class Transaction(
    val id: Int,
    val user_id: Int,
    val investment_id: Int,
    val type: String,
    val amount: Double,
    val total_price: Double,
    val date: String,
    val status: String
)
