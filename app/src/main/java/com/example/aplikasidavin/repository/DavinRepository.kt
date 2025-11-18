package com.example.aplikasidavin.repository

import com.example.aplikasidavin.data.api.RetrofitInstance
import com.example.aplikasidavin.data.model.AuthResponse
import com.example.aplikasidavin.data.model.BuyRequest
import com.example.aplikasidavin.data.model.SellRequest
import com.example.aplikasidavin.data.model.Transaction

class DavinRepository {
    suspend fun getUsers() = RetrofitInstance.api.getUsers()
    suspend fun getInvestments() = RetrofitInstance.api.getInvestments()

    suspend fun register(username: String, email: String, password: String) : AuthResponse {
        val body = mapOf("username" to username, "email" to email, "password" to password)
        return RetrofitInstance.api.register(body)
    }

    suspend fun login(username: String, password: String): AuthResponse {
        val body = mapOf("username" to username, "password" to password)
        return RetrofitInstance.api.login(body)
    }

    suspend fun topUp(id : Int, balance : Double) : Map<String, Any> {
        return RetrofitInstance.api.topUp(id,mapOf("balance" to balance))
    }

    suspend fun getCryptoPrices() : Map<String, Map<String, Double>>{
        return RetrofitInstance.api.getCryptoPrices()
    }

    suspend fun buyCrypto(
        userId: Int,
        investmentId: Int,
        amount: Double,
        price: Double
    ): Map<String, Any> {
        val req = BuyRequest(userId, investmentId, amount, price)
        return RetrofitInstance.api.buyCrypto(req)
    }

    suspend fun sellCrypto(
        userId: Int,
        investmentId: Int,
        amount: Double,
        price: Double
    ): Map<String, Any> {
        val body = SellRequest(
            user_id = userId,
            investment_id = investmentId,
            amount = amount,
            price = price
        )
        return RetrofitInstance.api.sellCrypto(body)
    }

    suspend fun getTransactions(userId: Int): List<Transaction> {
        return RetrofitInstance.api.getTransactions(userId)
    }



}