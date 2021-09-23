package com.tsato.mobile.ekaki.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.tsato.mobile.ekaki.data.models.Announcement
import com.tsato.mobile.ekaki.data.models.BaseModel
import com.tsato.mobile.ekaki.data.models.ChatMessage
import com.tsato.mobile.ekaki.data.remote.ws.Room
import com.tsato.mobile.ekaki.databinding.ItemAnnouncementBinding
import com.tsato.mobile.ekaki.databinding.ItemChatMessageIncomingBinding
import com.tsato.mobile.ekaki.databinding.ItemChatMessageOutgoingBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.lang.IllegalStateException
import java.text.SimpleDateFormat
import java.util.*

private const val VIEW_TYPE_INCOMING_MESSAGE = 0
private const val VIEW_TYPE_OUTGOING_MESSAGE = 1
private const val VIEW_TYPE_ANNOUNCEMENT = 2

class ChatMessageAdapter(
    private val userName: String // userName of self. userName of other players are
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    class IncomingChatMessageViewHolder(
        val binding: ItemChatMessageIncomingBinding) :RecyclerView.ViewHolder(binding.root)

    class OutgoingChatMessageViewHolder(
        val binding: ItemChatMessageOutgoingBinding) :RecyclerView.ViewHolder(binding.root)

    class AnnouncementViewHolder(
        val binding: ItemAnnouncementBinding) :RecyclerView.ViewHolder(binding.root)

    var chatObjects = listOf<BaseModel>() // BaseModel has to be ChatMessages or Announcements

    /*
        updates the list of chat messages

        we want to suspend the listenToSocketEvents() coroutine in DrawingActivity.kt as long as
        we update the recycler view to avoid concurrent issues on chat message stack
     */
    suspend fun updateDataSet(newDataSet: List<BaseModel>) = withContext(Dispatchers.Default) {
        val diff = DiffUtil.calculateDiff(object: DiffUtil.Callback() {
            override fun getOldListSize(): Int {
                return chatObjects.size
            }

            override fun getNewListSize(): Int {
                return newDataSet.size
            }

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return chatObjects[oldItemPosition] == newDataSet[newItemPosition]
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return chatObjects[oldItemPosition] == newDataSet[newItemPosition]
            }
        })
        /*
            switching to the main thread; updating UI stuffs has to be done on the main thread
         */
        withContext(Dispatchers.Main) {
            chatObjects = newDataSet
            diff.dispatchUpdatesTo(this@ChatMessageAdapter)
        }
    }

    override fun getItemCount(): Int {
        return chatObjects.size
    }

    override fun getItemViewType(position: Int): Int {
        return when (val obj = chatObjects[position]) {
            is Announcement -> VIEW_TYPE_ANNOUNCEMENT
            is ChatMessage -> if (userName == obj.from) // person sent that message
                VIEW_TYPE_OUTGOING_MESSAGE
            else
                VIEW_TYPE_INCOMING_MESSAGE
            else -> throw IllegalStateException("getItemViewType(): Unknown view type")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_INCOMING_MESSAGE -> IncomingChatMessageViewHolder(
                ItemChatMessageIncomingBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )
            VIEW_TYPE_OUTGOING_MESSAGE -> OutgoingChatMessageViewHolder(
                ItemChatMessageOutgoingBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )
            VIEW_TYPE_ANNOUNCEMENT -> AnnouncementViewHolder(
                ItemAnnouncementBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )
            else -> throw IllegalStateException("onCreateViewHolder(): Unknown view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is AnnouncementViewHolder -> {
                val announcement = chatObjects[position] as Announcement

                holder.binding.apply {
                    tvAnnouncement.text = announcement.message

                    val dateFormat = SimpleDateFormat("kk:mm:ss", Locale.getDefault())
                    val date = dateFormat.format(announcement.timestamp)
                    tvTime.text = date

                    when (announcement.announcementType) {
                        Announcement.TYPE_EVERYBODY_GUESSED_IT -> {
                            root.setBackgroundColor(Color.LTGRAY)
                            tvAnnouncement.setTextColor(Color.BLACK)
                            tvTime.setTextColor(Color.BLACK)
                        }
                        Announcement.TYPE_PLAYER_GUESSED_WORD -> {
                            root.setBackgroundColor(Color.YELLOW)
                            tvAnnouncement.setTextColor(Color.BLACK)
                            tvTime.setTextColor(Color.BLACK)
                        }
                        Announcement.TYPE_PLAYER_JOINED -> {
                            root.setBackgroundColor(Color.GREEN)
                            tvAnnouncement.setTextColor(Color.BLACK)
                            tvTime.setTextColor(Color.BLACK)
                        }
                        Announcement.TYPE_PLAYER_LEFT -> {
                            root.setBackgroundColor(Color.RED)
                            tvAnnouncement.setTextColor(Color.WHITE)
                            tvTime.setTextColor(Color.WHITE)
                        }
                    }
                }
            }
            is IncomingChatMessageViewHolder -> {
                val message = chatObjects[position] as ChatMessage

                holder.binding.apply {
                    tvMessage.text = message.message
                    tvUsername.text = message.from

                    val dateFormat = SimpleDateFormat("kk:mm:ss", Locale.getDefault())
                    val date = dateFormat.format(message.timeStamp)
                    tvTime.text = date
                }
            }
            is OutgoingChatMessageViewHolder -> {
                val message = chatObjects[position] as ChatMessage

                holder.binding.apply {
                    tvMessage.text = message.message
                    tvUsername.text = message.from

                    val dateFormat = SimpleDateFormat("kk:mm:ss", Locale.getDefault())
                    val date = dateFormat.format(message.timeStamp)
                    tvTime.text = date
                }
            }
        }
    }

}