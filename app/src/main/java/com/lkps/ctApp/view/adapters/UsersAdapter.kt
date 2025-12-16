package com.lkps.ctApp.view.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.lkps.ct.databinding.ItemUsersBinding
import com.lkps.ctApp.data.model.Message
import com.lkps.ctApp.data.model.User

class UsersAdapter(private val clickListener: OnUsersClickListener) : ListAdapter<User,
        RecyclerView.ViewHolder>(GridViewDiffCallback) {

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val user = getItem(position)
        (holder as UserViewHolder).bind(clickListener, user)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        return UserViewHolder(
            ItemUsersBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    class UserViewHolder constructor(val binding: ItemUsersBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(clickListener: OnUsersClickListener, item: User) {
            binding.user = item
            binding.clickListener = clickListener
            binding.executePendingBindings()
        }
    }

    companion object GridViewDiffCallback : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem.userId == newItem.userId
        }

        override fun areContentsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem == newItem
        }
    }

    interface OnUsersClickListener {
        fun onClick(view: View, user: User)
    }
}