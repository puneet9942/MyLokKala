package com.example.museapp.data.remote


import com.example.museapp.data.auth.dto.OtpPayload
import com.example.museapp.data.auth.dto.SendOtpData
import com.example.museapp.data.auth.dto.VerifyOtpData
import com.example.museapp.data.auth.dto.VerifyOtpPayload
import com.example.museapp.data.remote.dto.AdDto
import com.example.museapp.data.remote.dto.ApiResponse
import com.example.museapp.data.remote.dto.CacheUserDto
import com.example.museapp.data.remote.dto.CommonRequest
import com.example.museapp.data.remote.dto.EmptyData
import com.example.museapp.data.remote.dto.FavoriteUserAddRequestDto
import com.example.museapp.data.remote.dto.FavoriteUserAddResponseDto
import com.example.museapp.data.remote.dto.FavoriteUserDto
import com.example.museapp.data.remote.dto.FeedbackDataDto
import com.example.museapp.data.remote.dto.FeedbackRequestDto
import com.example.museapp.data.remote.dto.FeedbackResponseDto
import com.example.museapp.data.remote.dto.InterestsDataDto
import com.example.museapp.data.remote.dto.ProfileDataDto
import com.example.museapp.data.remote.dto.UserDto
import com.example.museapp.data.remote.dto.UsersDataDto
import okhttp3.MultipartBody
import okhttp3.RequestBody

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {
    @POST("api/auth/send-otp")
    suspend fun sendOtp(@Body body: CommonRequest<OtpPayload>): ApiResponse<SendOtpData>

    @POST("api/auth/resend-otp")
    suspend fun resendOtp(@Body body: CommonRequest<OtpPayload>): ApiResponse<SendOtpData>

    @POST("api/auth/verify-otp")
    suspend fun verifyOtp(@Body body: CommonRequest<VerifyOtpPayload>): ApiResponse<VerifyOtpData>

    @GET("api/interests")
    suspend fun getInterests(@Query("page") page: Int, @Query("limit") limit: Int): ApiResponse<InterestsDataDto>

    @GET("ads")
    suspend fun getAds(
        @Query("lat") lat: Double? = null,
        @Query("lng") lng: Double? = null,
        @Query("radiusKm") radiusKm: Int? = null
    ): ApiResponse<List<AdDto>>

    @GET("api/user/all")
    suspend fun getAllUsers(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 10
    ): ApiResponse<UsersDataDto>

    @GET("skills")
    suspend fun getSkills(): ApiResponse<List<String>>

    // ---- Favourites endpoints (new) ----
    @GET("favorites")
    suspend fun getFavorites(): ApiResponse<List<AdDto>>

    @POST("favorites/{adId}")
    suspend fun addFavorite(@Path("adId") adId: String): ApiResponse<Unit>

    @DELETE("favorites/{adId}")
    suspend fun removeFavorite(@Path("adId") adId: String): ApiResponse<Unit>

    // ---- User favorites (new) ----
    /**
     * POST /api/favorites
     * Body: CommonRequest<FavoriteUserAddRequestDto>
     * Response: FavoriteUserAddResponseDto wrapped in ApiResponse
     */
    @POST("api/favorites")
    suspend fun addUserFavorite(@Body body: CommonRequest<FavoriteUserAddRequestDto>): ApiResponse<FavoriteUserAddResponseDto>

    /**
     * DELETE /api/favorites/{favoriteId}
     */
    @DELETE("api/favorites/{favoriteId}")
    suspend fun removeUserFavorite(@Path("favoriteId") favoriteId: String): ApiResponse<EmptyData>

    /**
     * GET /api/favorites/my-favorites
     */
    @GET("api/favorites/my-favorites")
    suspend fun getMyUserFavorites(): ApiResponse<List<FavoriteUserDto>>

//    // in your existing ApiService (do NOT create a new interface file)
//    @POST("/api/user/feedback")
//    suspend fun sendFeedback(@Body request: FeedbackRequestDto): Response<FeedbackResponseDto>
    /**
     * POST /api/user/feedback
     */
    @POST("api/user/feedback")
    suspend fun sendFeedback(@Body request: FeedbackRequestDto): ApiResponse<FeedbackDataDto>

    /**
     * POST v1/profile/update
     * - payload : application/json as a multipart part named "payload"
     * - files   : MultipartBody.Part list containing "photo" (single), repeated "photos", repeated "videos"
     */
    @Multipart
    @PUT("api/user/update")
    suspend fun updateProfile(
        @Part("payload") payload: RequestBody,
        @Part photo: MultipartBody.Part? = null,              // single profile photo (optional)
        @Part photos: List<MultipartBody.Part>? = null,      // repeated photos
        @Part videos: List<MultipartBody.Part>? = null
    ): ApiResponse<Any>

    @GET("/api/user/profile")
    suspend fun getProfile(): ApiResponse<CacheUserDto>

}