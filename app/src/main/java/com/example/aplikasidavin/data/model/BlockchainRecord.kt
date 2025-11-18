package com.example.aplikasidavin.data.model

data class BlockchainRecord(
    val id: Int,
    val user_id: Int,
    val action: String,
    val stock: String,
    val amount: Double,
    val created_at: String
)
