package com.example.lokkala.data.remote


import com.example.lokkala.data.remote.dto.AdDto
import com.example.lokkala.data.remote.dto.ApiResponse
import com.example.lokkala.data.remote.dto.RequestOtpBody
import com.example.lokkala.data.remote.dto.VerifyOtpBody
import com.example.lokkala.data.remote.dto.VerifyOtpData
import com.example.lokkala.data.remote.dto.VerifyOtpResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiService {
    @POST("auth/request-otp")
    suspend fun requestOtp(@Body body: RequestOtpBody): ApiResponse<Unit>

    @POST("auth/verify-otp")
    suspend fun verifyOtp(@Body body: VerifyOtpBody): ApiResponse<VerifyOtpData>

    @GET("ads")
    suspend fun getAds(
        @Query("lat") lat: Double? = null,
        @Query("lng") lng: Double? = null,
        @Query("radiusKm") radiusKm: Int? = null
    ): ApiResponse<List<AdDto>>

    @GET("skills")
    suspend fun getSkills(): ApiResponse<List<String>>

}