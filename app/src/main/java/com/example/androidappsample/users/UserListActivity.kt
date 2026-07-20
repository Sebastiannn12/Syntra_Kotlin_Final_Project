package com.example.androidappsample.users

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.androidappsample.R
import com.example.androidappsample.data.SessionManager
import com.example.androidappsample.databinding.ActivityUserListBinding
import kotlinx.coroutines.launch
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.example.androidappsample.util.AppNotifier

class UserListActivity : AppCompatActivity(), ConfirmDeleteDialogFragment.Listener {
    private lateinit var binding: ActivityUserListBinding
    private val viewModel: UserViewModel by viewModels()
    private val session by lazy { SessionManager(this) }

    private val formLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) viewModel.loadUsers()
    }

    private val adapter = UserAdapter(
        onView = { UserDetailsDialogFragment.newInstance(it).show(supportFragmentManager, "details") },
        onEdit = { openForm(it.id) },
        onDelete = {
            if (it.id == session.userId) {
                Toast.makeText(this, "You cannot delete the account currently signed in.", Toast.LENGTH_LONG).show()
            } else ConfirmDeleteDialogFragment.newInstance(it).show(supportFragmentManager, "delete")
        },
        onRestore = { viewModel.restoreUser(it.id) },
        onResetPassword = { user ->
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.reset_password)
                .setMessage("Create a temporary password for ${user.displayName}?")
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.reset_password) { _, _ -> viewModel.resetPassword(user.id) }
                .show()
        }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.recyclerUsers.adapter = adapter
        binding.fabAdd.setOnClickListener { openForm() }
        binding.buttonRetry.setOnClickListener { viewModel.loadUsers() }
        binding.inputSearch.doAfterTextChanged { viewModel.search(it?.toString().orEmpty()) }
        binding.swipeUsers.setOnRefreshListener { viewModel.loadUsers() }
        binding.buttonSort.setOnClickListener { anchor ->
            PopupMenu(this, anchor).apply {
                menu.add(getString(R.string.sort_newest))
                menu.add(getString(R.string.sort_oldest))
                menu.add(getString(R.string.sort_name))
                setOnMenuItemClickListener { item ->
                    val sort = when (item.title) {
                        getString(R.string.sort_oldest) -> "oldest"
                        getString(R.string.sort_name) -> "name"
                        else -> "newest"
                    }
                    binding.buttonSort.text = item.title
                    viewModel.sort(sort)
                    true
                }
                show()
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.progressBar.visibility = if (state.loading) View.VISIBLE else View.GONE
                    binding.swipeUsers.isRefreshing = false
                    binding.groupError.visibility = if (state.error != null) View.VISIBLE else View.GONE
                    binding.textError.text = state.error
                    val visible = state.visibleUsers
                    adapter.submitList(visible)
                    binding.groupEmpty.visibility = if (!state.loading && state.error == null && visible.isEmpty()) View.VISIBLE else View.GONE
                    binding.textCount.text = "${visible.size} ${if (visible.size == 1) "person" else "people"}"
                    state.temporaryPassword?.let { password ->
                        MaterialAlertDialogBuilder(this@UserListActivity)
                            .setTitle("Temporary password")
                            .setMessage("Share this securely with the user:\n\n$password\n\nIt is shown only once.")
                            .setPositiveButton(android.R.string.ok, null)
                            .show()
                        viewModel.consumeTemporaryPassword()
                    }
                    state.successMessage?.let { message ->
                        Toast.makeText(this@UserListActivity, message, Toast.LENGTH_SHORT).show()
                        AppNotifier.show(this@UserListActivity, "User management", message)
                        viewModel.consumeSuccess()
                    }
                }
            }
        }
        viewModel.loadUsers()
    }

    override fun onDeleteConfirmed(userId: Int) = viewModel.deleteUser(userId)

    private fun openForm(userId: Int? = null) {
        formLauncher.launch(Intent(this, UserFormActivity::class.java).apply {
            userId?.let { putExtra(UserFormActivity.EXTRA_USER_ID, it) }
        })
    }
}
