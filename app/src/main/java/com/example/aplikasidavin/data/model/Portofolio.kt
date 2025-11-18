package com.example.aplikasidavin.data.model

import com.google.gson.annotations.SerializedName

data class Portfolio(
    val id: Int,
    val investment_id: Int,
    val asset: String,
    val amount: Double,
    @SerializedName("total_invested")
    val totalInvested: Double = 0.0
)
