package com.example.androidappsample.users

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.example.androidappsample.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class ConfirmDeleteDialogFragment : DialogFragment() {
    interface Listener { fun onDeleteConfirmed(userId: Int) }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val id = requireArguments().getInt(ARG_ID)
        val name = requireArguments().getString(ARG_NAME).orEmpty()
        return MaterialAlertDialogBuilder(requireContext())
            .setIcon(R.drawable.delete_user)
            .setTitle("Disable account")
            .setMessage("Disable $name? Their data stays in the database, but they cannot sign in until restored.")
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton("Disable") { _, _ ->
                (requireActivity() as Listener).onDeleteConfirmed(id)
            }
            .create()
    }

    companion object {
        private const val ARG_ID = "id"
        private const val ARG_NAME = "name"
        fun newInstance(user: com.example.androidappsample.data.User) = ConfirmDeleteDialogFragment().apply {
            arguments = Bundle().apply {
                putInt(ARG_ID, user.id)
                putString(ARG_NAME, user.displayName)
            }
        }
    }
}
