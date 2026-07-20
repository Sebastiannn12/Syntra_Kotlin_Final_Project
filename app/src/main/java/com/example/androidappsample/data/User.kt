package com.example.androidappsample.data

import com.google.gson.annotations.SerializedName

data class User(
    val id: Int,
    val username: String,
    @SerializedName("last_name") val lastName: String,
    @SerializedName("first_name") val firstName: String,
    @SerializedName("middle_name") val middleName: String?,
    val email: String,
    val photo: String?,
    @SerializedName("is_active") val isActive: Boolean = true,
    @SerializedName("last_login") val lastLogin: String? = null,
    @SerializedName("date_created") val dateCreated: String?
) {
    val displayName: String
        get() = listOf(firstName, middleName.orEmpty(), lastName)
            .filter { it.isNotBlank() }
            .joinToString(" ")

    val initials: String
        get() = listOf(firstName, lastName)
            .mapNotNull { it.firstOrNull()?.uppercase() }
            .joinToString("")
            .ifBlank { "U" }
}

data class UserRequest(
    val username: String,
    @SerializedName("last_name") val lastName: String,
    @SerializedName("first_name") val firstName: String,
    @SerializedName("middle_name") val middleName: String?,
    val email: String,
    val password: String?,
    val photo: String?
)

data class LoginRequest(val identity: String, val password: String)

data class ChangePasswordRequest(
    @SerializedName("current_password") val currentPassword: String,
    @SerializedName("new_password") val newPassword: String
)

data class DashboardStats(val total: Int, val active: Int, val disabled: Int)

data class ApiResponse(
    val success: Boolean,
    val message: String?,
    val id: Int?,
    val token: String?,
    @SerializedName("temporary_password") val temporaryPassword: String?,
    val photo: String?,
    val stats: DashboardStats?,
    val user: User?,
    val users: List<User>?
)
