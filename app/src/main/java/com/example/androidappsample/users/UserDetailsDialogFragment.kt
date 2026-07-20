package com.example.androidappsample.users

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.example.androidappsample.R
import com.example.androidappsample.data.User
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class UserDetailsDialogFragment : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val args = requireArguments()
        val message = buildString {
            append("Username\n@${args.getString(ARG_USERNAME).orEmpty()}\n\n")
            append("Email\n${args.getString(ARG_EMAIL).orEmpty()}\n\n")
            append("Member since\n${args.getString(ARG_DATE).orEmpty().ifBlank { "Not available" }}")
        }
        return MaterialAlertDialogBuilder(requireContext())
            .setIcon(R.drawable.user)
            .setTitle(args.getString(ARG_NAME).orEmpty())
            .setMessage(message)
            .setPositiveButton(android.R.string.ok, null)
            .create()
    }

    companion object {
        private const val ARG_NAME = "name"
        private const val ARG_USERNAME = "username"
        private const val ARG_EMAIL = "email"
        private const val ARG_DATE = "date"
        fun newInstance(user: User) = UserDetailsDialogFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_NAME, user.displayName)
                putString(ARG_USERNAME, user.username)
                putString(ARG_EMAIL, user.email)
                putString(ARG_DATE, user.dateCreated)
            }
        }
    }
}
