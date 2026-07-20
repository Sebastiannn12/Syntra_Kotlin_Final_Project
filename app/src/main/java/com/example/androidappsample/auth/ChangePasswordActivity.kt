package com.example.androidappsample.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.androidappsample.R
import com.example.androidappsample.data.SessionManager
import com.example.androidappsample.data.UserRepository
import com.example.androidappsample.databinding.ActivityChangePasswordBinding
import kotlinx.coroutines.launch

class ChangePasswordActivity : AppCompatActivity() {
    private lateinit var binding: ActivityChangePasswordBinding
    private val repository = UserRepository()
    private val session by lazy { SessionManager(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChangePasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.buttonChange.setOnClickListener { changePassword() }
    }

    private fun changePassword() {
        val current = binding.inputCurrent.text.toString()
        val next = binding.inputNew.text.toString()
        val confirm = binding.inputConfirm.text.toString()
        binding.layoutCurrent.error = null
        binding.layoutNew.error = null
        binding.layoutConfirm.error = null
        val strong = next.length >= 8 && next.any(Char::isUpperCase) && next.any(Char::isLowerCase) &&
            next.any(Char::isDigit) && next.any { !it.isLetterOrDigit() }
        if (current.isBlank()) binding.layoutCurrent.error = getString(R.string.required_field)
        if (!strong) binding.layoutNew.error = getString(R.string.strong_password_rule)
        if (confirm != next) binding.layoutConfirm.error = getString(R.string.password_mismatch)
        if (current.isBlank() || !strong || confirm != next) return

        setLoading(true)
        lifecycleScope.launch {
            runCatching { repository.changePassword(current, next) }
                .onSuccess {
                    session.clear()
                    Toast.makeText(this@ChangePasswordActivity, "Password changed. Sign in again.", Toast.LENGTH_LONG).show()
                    startActivity(Intent(this@ChangePasswordActivity, LoginActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    })
                }
                .onFailure { Toast.makeText(this@ChangePasswordActivity, it.message, Toast.LENGTH_LONG).show() }
            setLoading(false)
        }
    }

    private fun setLoading(value: Boolean) {
        binding.progressBar.visibility = if (value) View.VISIBLE else View.GONE
        binding.buttonChange.isEnabled = !value
    }
}
