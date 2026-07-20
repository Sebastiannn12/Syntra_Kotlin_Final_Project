package com.example.androidappsample.users

import android.os.Bundle
import android.net.Uri
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import coil.load
import com.example.androidappsample.R
import com.example.androidappsample.data.SessionManager
import com.example.androidappsample.data.User
import com.example.androidappsample.data.UserRepository
import com.example.androidappsample.data.UserRequest
import com.example.androidappsample.databinding.ActivityUserFormBinding
import com.example.androidappsample.util.AppNotifier
import kotlinx.coroutines.launch
import java.io.File

class UserFormActivity : AppCompatActivity() {
    private lateinit var binding: ActivityUserFormBinding
    private val repository = UserRepository()
    private val session by lazy { SessionManager(this) }
    private var userId: Int? = null
    private val photoPicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { uploadPhoto(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserFormBinding.inflate(layoutInflater)
        setContentView(binding.root)
        userId = intent.getIntExtra(EXTRA_USER_ID, -1).takeIf { it > 0 }
        binding.toolbar.title = getString(if (userId == null) R.string.add_user else R.string.edit_user)
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.buttonSave.setOnClickListener { saveUser() }
        binding.buttonChoosePhoto.setOnClickListener { photoPicker.launch("image/*") }
        userId?.let { loadUser(it) }
    }

    private fun loadUser(id: Int) {
        setLoading(true)
        lifecycleScope.launch {
            runCatching { repository.getUser(id) }
                .onSuccess { showUser(it) }
                .onFailure {
                    Toast.makeText(this@UserFormActivity, it.message, Toast.LENGTH_LONG).show()
                    finish()
                }
            setLoading(false)
        }
    }

    private fun showUser(user: User) = with(binding) {
        inputUsername.setText(user.username)
        inputFirstName.setText(user.firstName)
        inputMiddleName.setText(user.middleName.orEmpty())
        inputLastName.setText(user.lastName)
        inputEmail.setText(user.email)
        inputPhoto.setText(user.photo.orEmpty())
        imagePreview.load(user.photo) {
            crossfade(true)
            placeholder(R.drawable.user)
            error(R.drawable.user)
        }
        layoutPassword.hint = getString(R.string.leave_password_blank)
    }

    private fun saveUser() {
        val username = binding.inputUsername.text.toString().trim()
        val first = binding.inputFirstName.text.toString().trim()
        val middle = binding.inputMiddleName.text.toString().trim()
        val last = binding.inputLastName.text.toString().trim()
        val email = binding.inputEmail.text.toString().trim()
        val password = binding.inputPassword.text.toString()
        val photo = binding.inputPhoto.text.toString().trim()

        listOf(binding.layoutUsername, binding.layoutFirstName, binding.layoutLastName, binding.layoutEmail, binding.layoutPassword)
            .forEach { it.error = null }
        var valid = true
        if (username.isBlank()) { binding.layoutUsername.error = getString(R.string.required_field); valid = false }
        if (first.isBlank()) { binding.layoutFirstName.error = getString(R.string.required_field); valid = false }
        if (last.isBlank()) { binding.layoutLastName.error = getString(R.string.required_field); valid = false }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) { binding.layoutEmail.error = getString(R.string.invalid_email); valid = false }
        val strongPassword = password.length >= 8 && password.any(Char::isUpperCase) && password.any(Char::isLowerCase) &&
            password.any(Char::isDigit) && password.any { !it.isLetterOrDigit() }
        if (userId == null && !strongPassword) { binding.layoutPassword.error = getString(R.string.strong_password_rule); valid = false }
        if (password.isNotBlank() && !strongPassword) { binding.layoutPassword.error = getString(R.string.strong_password_rule); valid = false }
        if (!valid) return

        val request = UserRequest(username, last, first, middle.ifBlank { null }, email, password.ifBlank { null }, photo.ifBlank { null })
        setLoading(true)
        lifecycleScope.launch {
            runCatching {
                userId?.let { repository.updateUser(it, request) } ?: repository.createUser(request)
                userId?.takeIf { it == session.userId }?.let { session.updateUser(repository.getUser(it)) }
            }.onSuccess {
                Toast.makeText(this@UserFormActivity, R.string.user_saved, Toast.LENGTH_SHORT).show()
                AppNotifier.show(this@UserFormActivity, "User saved", "The account details were updated successfully.")
                setResult(RESULT_OK)
                finish()
            }.onFailure {
                Toast.makeText(this@UserFormActivity, it.message ?: getString(R.string.connection_error), Toast.LENGTH_LONG).show()
            }
            setLoading(false)
        }
    }

    private fun uploadPhoto(uri: Uri) {
        val mime = contentResolver.getType(uri) ?: "image/jpeg"
        val extension = when (mime) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            else -> "jpg"
        }
        val file = File(cacheDir, "profile_${System.currentTimeMillis()}.$extension")
        contentResolver.openInputStream(uri)?.use { input -> file.outputStream().use(input::copyTo) }
            ?: return
        binding.textPhotoStatus.setText(R.string.photo_uploading)
        binding.buttonChoosePhoto.isEnabled = false
        lifecycleScope.launch {
            runCatching { repository.uploadPhoto(file, mime) }
                .onSuccess { url ->
                    binding.inputPhoto.setText(url)
                    binding.imagePreview.load(url) { crossfade(true); error(R.drawable.user) }
                    binding.textPhotoStatus.text = "Photo ready"
                }
                .onFailure { binding.textPhotoStatus.text = it.message }
            binding.buttonChoosePhoto.isEnabled = true
            file.delete()
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.buttonSave.isEnabled = !loading
    }

    companion object { const val EXTRA_USER_ID = "extra_user_id" }
}
