package com.example.androidappsample.users

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.androidappsample.data.User
import com.example.androidappsample.data.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UsersUiState(
    val loading: Boolean = false,
    val users: List<User> = emptyList(),
    val query: String = "",
    val sort: String = "newest",
    val temporaryPassword: String? = null,
    val successMessage: String? = null,
    val error: String? = null
) {
    val visibleUsers: List<User>
        get() {
            val term = query.trim().lowercase()
            return if (term.isBlank()) users else users.filter {
                it.displayName.lowercase().contains(term) ||
                    it.username.lowercase().contains(term) ||
                    it.email.lowercase().contains(term)
            }
        }
}

class UserViewModel : ViewModel() {
    private val repository = UserRepository()
    private val _uiState = MutableStateFlow(UsersUiState())
    val uiState: StateFlow<UsersUiState> = _uiState.asStateFlow()

    fun loadUsers() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            runCatching { repository.getUsers(sort = _uiState.value.sort) }
                .onSuccess { users -> _uiState.update { it.copy(loading = false, users = users) } }
                .onFailure { error -> _uiState.update { it.copy(loading = false, error = error.message) } }
        }
    }

    fun search(query: String) = _uiState.update { it.copy(query = query) }

    fun sort(sort: String) {
        _uiState.update { it.copy(sort = sort) }
        loadUsers()
    }

    fun deleteUser(id: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            runCatching { repository.deleteUser(id) }
                .onSuccess {
                    _uiState.update { it.copy(successMessage = "User account disabled") }
                    loadUsers()
                }
                .onFailure { error -> _uiState.update { it.copy(loading = false, error = error.message) } }
        }
    }

    fun consumeError() = _uiState.update { it.copy(error = null) }

    fun consumeSuccess() = _uiState.update { it.copy(successMessage = null) }

    fun restoreUser(id: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            runCatching { repository.restoreUser(id) }
                .onSuccess {
                    _uiState.update { it.copy(successMessage = "User account restored") }
                    loadUsers()
                }
                .onFailure { error -> _uiState.update { it.copy(loading = false, error = error.message) } }
        }
    }

    fun resetPassword(id: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            runCatching { repository.resetPassword(id) }
                .onSuccess { password -> _uiState.update { it.copy(loading = false, temporaryPassword = password) } }
                .onFailure { error -> _uiState.update { it.copy(loading = false, error = error.message) } }
        }
    }

    fun consumeTemporaryPassword() = _uiState.update { it.copy(temporaryPassword = null) }
}
