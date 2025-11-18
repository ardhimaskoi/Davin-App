package com.example.aplikasidavin.data.model

data class BlockchainRequest(
    val user_id: Int,
    val action: String,
    val stock: String,
    val amount: Double
)
