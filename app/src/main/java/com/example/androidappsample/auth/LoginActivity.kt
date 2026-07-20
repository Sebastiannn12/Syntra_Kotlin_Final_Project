package com.example.androidappsample.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.androidappsample.BuildConfig
import com.example.androidappsample.MainActivity
import com.example.androidappsample.R
import com.example.androidappsample.data.SessionManager
import com.example.androidappsample.data.UserRepository
import com.example.androidappsample.databinding.ActivityLoginBinding
import com.example.androidappsample.util.AppNotifier
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private val repository = UserRepository()
    private val session by lazy { SessionManager(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.textServer.text = getString(R.string.connected_to, BuildConfig.API_BASE_URL)
        binding.buttonLogin.setOnClickListener { login() }
        binding.buttonCreateAccount.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun login() {
        clearErrors()
        val identity = binding.inputIdentity.text.toString().trim()
        val password = binding.inputPassword.text.toString()
        if (identity.isBlank()) binding.layoutIdentity.error = getString(R.string.required_field)
        if (password.isBlank()) binding.layoutPassword.error = getString(R.string.required_field)
        if (identity.isBlank() || password.isBlank()) return

        setLoading(true)
        lifecycleScope.launch {
            runCatching { repository.login(identity, password) }
                .onSuccess { (token, user) ->
                    session.save(token, user, binding.checkRemember.isChecked)
                    AppNotifier.show(this@LoginActivity, "Signed in", "Welcome back, ${user.firstName}.")
                    startActivity(Intent(this@LoginActivity, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    })
                }
                .onFailure { showError(it) }
            setLoading(false)
        }
    }

    private fun clearErrors() {
        binding.layoutIdentity.error = null
        binding.layoutPassword.error = null
    }

    private fun setLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.buttonLogin.isEnabled = !loading
        binding.buttonCreateAccount.isEnabled = !loading
    }

    private fun showError(error: Throwable) {
        Toast.makeText(this, error.message ?: getString(R.string.connection_error), Toast.LENGTH_LONG).show()
    }
}
