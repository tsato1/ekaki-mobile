package com.tsato.mobile.ekaki.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.tsato.mobile.ekaki.data.remote.ws.Room
import com.tsato.mobile.ekaki.databinding.ItemRoomBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

// empty inject constructor allows to inject this RoomAdapter without 'provide'
class RoomAdapter @Inject constructor() : RecyclerView.Adapter<RoomAdapter.RoomViewHolder>() {

    class RoomViewHolder(val binding: ItemRoomBinding) : RecyclerView.ViewHolder(binding.root)

    /*
        this function suspends as long as this update operation lasts for the recyclerview

        uses default dispatcher because it is cpu-heavy algorithms
     */
    suspend fun updateDataSet(newDataSet: List<Room>) = withContext(Dispatchers.Default) {
        val diff = DiffUtil.calculateDiff(object: DiffUtil.Callback() {
            override fun getOldListSize(): Int {
                return rooms.size
            }

            override fun getNewListSize(): Int {
                return newDataSet.size
            }

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return rooms[oldItemPosition] == newDataSet[newItemPosition]
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return rooms[oldItemPosition] == newDataSet[newItemPosition]
            }
        })
        /*
            switching to the main thread; updating UI stuffs has to be done on the main thread
         */
        withContext(Dispatchers.Main) {
            rooms = newDataSet
            diff.dispatchUpdatesTo(this@RoomAdapter)
        }
    }

    var rooms = listOf<Room>()
        private set

    override fun getItemCount(): Int {
        return rooms.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoomViewHolder {
        return RoomViewHolder(
            ItemRoomBinding.inflate(LayoutInflater.from(parent.context),
            parent,
            false)
        )
    }

    override fun onBindViewHolder(holder: RoomViewHolder, position: Int) {
        val room = rooms[position]
        holder.binding.apply {
            tvRoomName.text = room.name
            val playerCountText = "${room.playerCount}/ ${room.maxPlayers}"
            tvRoomPersonCount.text = playerCountText

            root.setOnClickListener {
                onRoomClickListener?.let { click ->
                    click(room)
                }
            }
        }
    }

    private var onRoomClickListener : ((Room) -> Unit)? = null

    fun setOnRoomClicklistener(listener: (Room) -> Unit) {
        onRoomClickListener = listener
    }
}