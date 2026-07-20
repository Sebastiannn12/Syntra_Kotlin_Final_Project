package com.example.androidappsample

import com.example.androidappsample.data.User
import com.example.androidappsample.users.UsersUiState
import org.junit.Assert.assertEquals
import org.junit.Test

class UserModelTest {
    private val user = User(
        id = 7,
        username = "ada",
        firstName = "Ada",
        middleName = "M",
        lastName = "Lovelace",
        email = "ada@example.com",
        photo = null,
        dateCreated = "2026-07-20"
    )

    @Test
    fun displayNameAndInitials_areDerivedFromAccountFields() {
        assertEquals("Ada M Lovelace", user.displayName)
        assertEquals("AL", user.initials)
    }

    @Test
    fun visibleUsers_searchesNameUsernameAndEmailIgnoringCase() {
        assertEquals(listOf(user), UsersUiState(users = listOf(user), query = "LOVE").visibleUsers)
        assertEquals(listOf(user), UsersUiState(users = listOf(user), query = "ada@").visibleUsers)
        assertEquals(emptyList<User>(), UsersUiState(users = listOf(user), query = "grace").visibleUsers)
    }
}
