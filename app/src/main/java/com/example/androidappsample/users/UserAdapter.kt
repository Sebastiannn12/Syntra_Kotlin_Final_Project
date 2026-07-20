package com.example.androidappsample.users

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.appcompat.widget.PopupMenu
import coil.load
import com.example.androidappsample.R
import com.example.androidappsample.data.User
import com.example.androidappsample.databinding.ItemUserBinding

class UserAdapter(
    private val onView: (User) -> Unit,
    private val onEdit: (User) -> Unit,
    private val onDelete: (User) -> Unit,
    private val onRestore: (User) -> Unit,
    private val onResetPassword: (User) -> Unit
) : ListAdapter<User, UserAdapter.UserViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        return UserViewHolder(ItemUserBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) = holder.bind(getItem(position))

    inner class UserViewHolder(private val binding: ItemUserBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(user: User) = with(binding) {
            imageAvatar.load(user.photo) {
                crossfade(true)
                placeholder(R.drawable.user)
                error(R.drawable.user)
            }
            textName.text = user.displayName
            textUsername.text = "@${user.username}"
            textEmail.text = user.email
            textStatus.text = root.context.getString(if (user.isActive) R.string.active_account else R.string.disabled_account)
            textStatus.setTextColor(root.context.getColor(if (user.isActive) R.color.success else R.color.danger))
            root.alpha = if (user.isActive) 1f else 0.68f
            root.setOnClickListener { onView(user) }
            buttonView.setOnClickListener { onView(user) }
            buttonEdit.setOnClickListener { onEdit(user) }
            buttonMore.setOnClickListener { anchor ->
                PopupMenu(anchor.context, anchor).apply {
                    menu.add(root.context.getString(R.string.reset_password))
                    menu.add(root.context.getString(if (user.isActive) R.string.delete_user else R.string.restore_user))
                    setOnMenuItemClickListener { item ->
                        when (item.title.toString()) {
                            root.context.getString(R.string.reset_password) -> onResetPassword(user)
                            else -> if (user.isActive) onDelete(user) else onRestore(user)
                        }
                        true
                    }
                    show()
                }
            }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: User, newItem: User) = oldItem == newItem
    }
}
