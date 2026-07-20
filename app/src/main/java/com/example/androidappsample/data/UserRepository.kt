package com.example.androidappsample.data

import com.example.androidappsample.network.RetrofitClient
import com.google.gson.JsonParser
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.HttpException
import java.io.File
import java.io.IOException

class UserRepository {
    private val api = RetrofitClient.userApi

    suspend fun login(identity: String, password: String): Pair<String, User> {
        val response = execute { api.login(LoginRequest(identity, password)) }
        if (!response.success) error(response.message ?: "Unable to sign in")
        return (response.token ?: error("Missing session token")) to
            (response.user ?: error("Missing account details"))
    }

    suspend fun register(request: UserRequest): Pair<String, User> {
        val response = execute { api.register(request) }
        if (!response.success) error(response.message ?: "Unable to create account")
        return (response.token ?: error("Missing session token")) to
            (response.user ?: error("Missing account details"))
    }

    suspend fun logout() = execute { api.logout() }

    suspend fun me(): User {
        val response = execute { api.me() }
        if (!response.success) error(response.message ?: "Unable to load profile")
        return response.user ?: error("Profile not found")
    }

    suspend fun getUsers(search: String? = null, sort: String = "newest"): List<User> {
        val response = execute { api.getUsers(search, sort) }
        if (!response.success) error(response.message ?: "Unable to load users")
        return response.users.orEmpty()
    }

    suspend fun getUser(id: Int): User {
        val response = execute { api.getUser(id) }
        if (!response.success) error(response.message ?: "Unable to load user")
        return response.user ?: error("User not found")
    }

    suspend fun createUser(request: UserRequest) {
        val response = execute { api.createUser(request) }
        if (!response.success) error(response.message ?: "Unable to create user")
    }

    suspend fun updateUser(id: Int, request: UserRequest) {
        val response = execute { api.updateUser(id, request) }
        if (!response.success) error(response.message ?: "Unable to update user")
    }

    suspend fun deleteUser(id: Int) {
        val response = execute { api.deleteUser(id) }
        if (!response.success) error(response.message ?: "Unable to delete user")
    }

    suspend fun restoreUser(id: Int) {
        val response = execute { api.restoreUser(id) }
        if (!response.success) error(response.message ?: "Unable to restore user")
    }

    suspend fun resetPassword(id: Int): String {
        val response = execute { api.resetPassword(id) }
        if (!response.success) error(response.message ?: "Unable to reset password")
        return response.temporaryPassword ?: error("Temporary password missing")
    }

    suspend fun changePassword(current: String, new: String) {
        val response = execute { api.changePassword(ChangePasswordRequest(current, new)) }
        if (!response.success) error(response.message ?: "Unable to change password")
    }

    suspend fun getStats(): Pair<DashboardStats, List<User>> {
        val response = execute { api.getStats() }
        if (!response.success) error(response.message ?: "Unable to load dashboard")
        return (response.stats ?: error("Statistics unavailable")) to response.users.orEmpty()
    }

    suspend fun uploadPhoto(file: File, mimeType: String): String {
        val body = file.asRequestBody(mimeType.toMediaTypeOrNull())
        val part = MultipartBody.Part.createFormData("photo", file.name, body)
        val response = execute { api.uploadPhoto(part) }
        if (!response.success) error(response.message ?: "Unable to upload photo")
        return response.photo ?: error("Photo URL missing")
    }

    private suspend fun <T> execute(block: suspend () -> T): T {
        try {
            return block()
        } catch (error: HttpException) {
            val message = runCatching {
                JsonParser.parseString(error.response()?.errorBody()?.string())
                    .asJsonObject.get("message")?.asString
            }.getOrNull()
            throw IllegalStateException(message ?: "Server error ${error.code()}")
        } catch (_: IOException) {
            throw IllegalStateException("No Internet connection or the hosted server is unavailable")
        }
    }
}
