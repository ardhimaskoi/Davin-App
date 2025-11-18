package com.example.aplikasidavin.data.model


data class VerifyResponse(
    val valid: Boolean,
    val localHash: String,
    val blockchainHashExists: Boolean,
    val message: String
)
