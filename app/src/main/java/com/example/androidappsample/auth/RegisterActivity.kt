package com.example.androidappsample.auth

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.core.widget.doAfterTextChanged
import com.example.androidappsample.MainActivity
import com.example.androidappsample.R
import com.example.androidappsample.data.SessionManager
import com.example.androidappsample.data.UserRepository
import com.example.androidappsample.data.UserRequest
import com.example.androidappsample.databinding.ActivityRegisterBinding
import com.example.androidappsample.util.AppNotifier
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegisterBinding
    private val repository = UserRepository()
    private val session by lazy { SessionManager(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.buttonRegister.setOnClickListener { register() }
        binding.inputPassword.doAfterTextChanged { updateStrength(it?.toString().orEmpty()) }
    }

    private fun register() {
        val username = binding.inputUsername.text.toString().trim()
        val firstName = binding.inputFirstName.text.toString().trim()
        val middleName = binding.inputMiddleName.text.toString().trim()
        val lastName = binding.inputLastName.text.toString().trim()
        val email = binding.inputEmail.text.toString().trim()
        val password = binding.inputPassword.text.toString()
        val confirm = binding.inputConfirmPassword.text.toString()

        listOf(binding.layoutUsername, binding.layoutFirstName, binding.layoutLastName,
            binding.layoutEmail, binding.layoutPassword, binding.layoutConfirmPassword)
            .forEach { it.error = null }

        var valid = true
        if (username.isBlank()) { binding.layoutUsername.error = getString(R.string.required_field); valid = false }
        if (firstName.isBlank()) { binding.layoutFirstName.error = getString(R.string.required_field); valid = false }
        if (lastName.isBlank()) { binding.layoutLastName.error = getString(R.string.required_field); valid = false }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) { binding.layoutEmail.error = getString(R.string.invalid_email); valid = false }
        if (!isStrong(password)) { binding.layoutPassword.error = getString(R.string.strong_password_rule); valid = false }
        if (confirm != password) { binding.layoutConfirmPassword.error = getString(R.string.password_mismatch); valid = false }
        if (!valid) return

        val request = UserRequest(username, lastName, firstName, middleName.ifBlank { null }, email, password, null)
        setLoading(true)
        lifecycleScope.launch {
            runCatching { repository.register(request) }
                .onSuccess { (token, user) ->
                    session.save(token, user)
                    Toast.makeText(this@RegisterActivity, R.string.account_created, Toast.LENGTH_SHORT).show()
                    AppNotifier.show(this@RegisterActivity, "Registration complete", "Your account is ready to use.")
                    startActivity(Intent(this@RegisterActivity, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    })
                }
                .onFailure {
                    Toast.makeText(this@RegisterActivity, it.message ?: getString(R.string.connection_error), Toast.LENGTH_LONG).show()
                }
            setLoading(false)
        }
    }

    private fun isStrong(password: String): Boolean = password.length >= 8 &&
        password.any(Char::isUpperCase) && password.any(Char::isLowerCase) &&
        password.any(Char::isDigit) && password.any { !it.isLetterOrDigit() }

    private fun updateStrength(password: String) {
        val score = listOf(
            password.length >= 8,
            password.any(Char::isUpperCase),
            password.any(Char::isLowerCase),
            password.any(Char::isDigit),
            password.any { !it.isLetterOrDigit() }
        ).count { it }
        binding.progressStrength.progress = score
        binding.textStrength.setText(when {
            score >= 5 -> R.string.password_strong
            score >= 3 -> R.string.password_medium
            else -> R.string.password_weak
        })
    }

    private fun setLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.buttonRegister.isEnabled = !loading
    }
}
