package com.example.androidappsample.network

import com.example.androidappsample.data.ApiResponse
import com.example.androidappsample.data.LoginRequest
import com.example.androidappsample.data.ChangePasswordRequest
import okhttp3.MultipartBody
import com.example.androidappsample.data.UserRequest
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Multipart
import retrofit2.http.Part
import retrofit2.http.Query

interface UserApiService {
    @POST("auth.php?action=login")
    suspend fun login(@Body request: LoginRequest): ApiResponse

    @POST("auth.php?action=register")
    suspend fun register(@Body request: UserRequest): ApiResponse

    @POST("auth.php?action=logout")
    suspend fun logout(): ApiResponse

    @GET("auth.php?action=me")
    suspend fun me(): ApiResponse

    @POST("password.php?action=change")
    suspend fun changePassword(@Body request: ChangePasswordRequest): ApiResponse

    @POST("password.php?action=reset")
    suspend fun resetPassword(@Query("id") id: Int): ApiResponse

    @POST("users.php?action=restore")
    suspend fun restoreUser(@Query("id") id: Int): ApiResponse

    @GET("stats.php")
    suspend fun getStats(): ApiResponse

    @Multipart
    @POST("upload.php")
    suspend fun uploadPhoto(@Part photo: MultipartBody.Part): ApiResponse

    @GET("users.php")
    suspend fun getUsers(
        @Query("search") search: String? = null,
        @Query("sort") sort: String = "newest"
    ): ApiResponse

    @GET("users.php")
    suspend fun getUser(@Query("id") id: Int): ApiResponse

    @POST("users.php")
    suspend fun createUser(@Body request: UserRequest): ApiResponse

    @PUT("users.php")
    suspend fun updateUser(@Query("id") id: Int, @Body request: UserRequest): ApiResponse

    @DELETE("users.php")
    suspend fun deleteUser(@Query("id") id: Int): ApiResponse
}
