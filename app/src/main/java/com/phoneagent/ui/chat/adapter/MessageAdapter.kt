package com.phoneagent.ui.chat.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.phoneagent.databinding.ItemMessageUserBinding
import com.phoneagent.databinding.ItemMessageAiBinding
import com.phoneagent.databinding.ItemMessageActionBinding
import com.phoneagent.ui.chat.Message
import com.phoneagent.ui.chat.Role
import java.text.SimpleDateFormat
import java.util.*

class MessageAdapter : ListAdapter<Message, RecyclerView.ViewHolder>(MessageDiffCallback()) {

    companion object {
        private const val TYPE_USER = 1
        private const val TYPE_AI = 2
        private const val TYPE_ACTION = 3
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position).role) {
            Role.USER -> TYPE_USER
            Role.AI -> TYPE_AI
            Role.ACTION -> TYPE_ACTION
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_USER -> UserMessageViewHolder(ItemMessageUserBinding.inflate(inflater, parent, false))
            TYPE_AI -> AiMessageViewHolder(ItemMessageAiBinding.inflate(inflater, parent, false))
            TYPE_ACTION -> ActionMessageViewHolder(ItemMessageActionBinding.inflate(inflater, parent, false))
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = getItem(position)
        when (holder) {
            is UserMessageViewHolder -> holder.bind(message)
            is AiMessageViewHolder -> holder.bind(message)
            is ActionMessageViewHolder -> holder.bind(message)
        }
    }

    inner class UserMessageViewHolder(private val binding: ItemMessageUserBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(message: Message) {
            binding.tvMessage.text = message.content
            binding.tvTimestamp.text = formatTime(message.timestamp)
            animateView(binding.root)
        }
    }

    inner class AiMessageViewHolder(private val binding: ItemMessageAiBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(message: Message) {
            binding.tvMessage.text = message.content
            binding.tvTimestamp.text = formatTime(message.timestamp)
            if (message.actionSteps.isNotEmpty()) {
                binding.tvActionSteps.visibility = View.VISIBLE
                binding.tvActionSteps.text = message.actionSteps.joinToString(" → ")
            } else {
                binding.tvActionSteps.visibility = View.GONE
            }
            animateView(binding.root)
        }
    }

    inner class ActionMessageViewHolder(private val binding: ItemMessageActionBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(message: Message) {
            binding.tvActionText.text = message.content
            animateView(binding.root)
        }
    }

    private fun formatTime(timestamp: Long): String {
        return SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(timestamp))
    }

    private fun animateView(view: View) {
        view.alpha = 0f
        view.translationY = 50f
        view.animate().alpha(1f).translationY(0f).setDuration(300).start()
    }

    class MessageDiffCallback : DiffUtil.ItemCallback<Message>() {
        override fun areItemsTheSame(oldItem: Message, newItem: Message): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Message, newItem: Message): Boolean {
            return oldItem == newItem
        }
    }
}
