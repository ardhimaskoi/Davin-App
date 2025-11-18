package com.example.aplikasidavin.data.api

import com.example.aplikasidavin.data.model.*
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    @POST("auth/register")
    suspend fun register(@Body body: Map<String, String>): AuthResponse

    @POST("auth/login")
    suspend fun login(@Body body: Map<String, String>): AuthResponse

    @GET("users")
    suspend fun getUsers(): List<User>

    @PUT("users/{id}/topup")
    suspend fun topUp(@Path("id") id: Int, @Body body: Map<String, Double>): Map<String, Any>

    @GET("investments")
    suspend fun getInvestments(): List<Investment>

    @GET("prices")
    suspend fun getCryptoPrices(): Map<String, Map<String, Double>>

    @POST("transactions/buy")
    suspend fun buyCrypto(@Body body: BuyRequest): Map<String, Any>

    @POST("transactions/sell")
    suspend fun sellCrypto(@Body body: SellRequest): Map<String, Any>

    @GET("transactions/{user_id}")
    suspend fun getTransactions(@Path("user_id") userId: Int): List<Transaction>

    @POST("blockchain/record")
    suspend fun recordBlockchain(@Body body: BlockchainRequest): Response<BlockchainResponse>


    @GET("blockchain/records")
    suspend fun getBlockchainRecords(): Response<List<BlockchainRecord>>

    @GET("blockchain/verify/{id}")
    suspend fun verifyRecord(@Path("id") id: Int): Response<VerifyResponse>


    @GET("/portfolio/{user_id}")
    suspend fun getPortfolio(@Path("user_id") userId: Int): List<Portfolio>

    @GET("prices/{symbol}/chart")
    suspend fun getChartData(@Path("symbol") symbol: String): List<List<Double>>

    @POST("payment/create")
    suspend fun createPayment(@Body body: PaymentRequest): PaymentResponse

    @PUT("transactions/{id}/status")
    suspend fun updateTransactionStatus(
        @Path("id") id: Int,
        @Body body: Map<String, String>
    ): Map<String, Any>

    @DELETE("transactions/{id}")
    suspend fun deleteTransaction(@Path("id") id: Int): Map<String, Any>

    @DELETE("portfolio/{userId}/{investmentId}")
    suspend fun deletePortfolio(
        @Path("userId") userId: Int,
        @Path("investmentId") investmentId: Int
    ): Map<String, String>


    @DELETE("users/{id}")
    suspend fun deleteUser(@Path("id") id: Int): Map<String, String>


}
