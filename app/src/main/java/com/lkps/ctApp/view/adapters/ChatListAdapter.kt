package com.lkps.ctApp.view.adapters

import android.view.LayoutInflater.from
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.lkps.ct.databinding.ItemChatBinding
import com.lkps.ctApp.data.model.Chat

class ChatListAdapter(private val clickListener: OnChatClickListener) : ListAdapter<Chat,
    RecyclerView.ViewHolder>(GridViewDiffCallback) {

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val product = getItem(position)
        (holder as MsgViewHolder).bind(clickListener, product)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MsgViewHolder {
        return MsgViewHolder(ItemChatBinding.inflate(from(parent.context), parent, false))
    }

    class MsgViewHolder constructor(val binding: ItemChatBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(clickListener: OnChatClickListener, item: Chat) {
            binding.chat = item
            binding.clickListener = clickListener
            binding.executePendingBindings()
        }
    }

    companion object GridViewDiffCallback : DiffUtil.ItemCallback<Chat>() {
        override fun areItemsTheSame(oldItem: Chat, newItem: Chat): Boolean {
            return oldItem.chatId == newItem.chatId
        }

        override fun areContentsTheSame(oldItem: Chat, newItem: Chat): Boolean {
            return oldItem == newItem
        }
    }

    class OnChatClickListener(val clickListener: (productId: Chat) -> Unit) {
        fun onClick(v: View, product: Chat) = clickListener(product)
    }
}