package com.tsato.mobile.ekaki.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.tsato.mobile.ekaki.data.models.PlayerData
import com.tsato.mobile.ekaki.databinding.ItemPlayerBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

// empty inject constructor allows to inject this RoomAdapter without 'provide'
class PlayerAdapter @Inject constructor() : RecyclerView.Adapter<PlayerAdapter.PlayerViewHolder>() {

    class PlayerViewHolder(val binding: ItemPlayerBinding) : RecyclerView.ViewHolder(binding.root)

    /*
        this function suspends as long as this update operation lasts for the recyclerview

        uses default dispatcher because it is cpu-heavy algorithms
     */
    suspend fun updateDataSet(newDataSet: List<PlayerData>) = withContext(Dispatchers.Default) {
        val diff = DiffUtil.calculateDiff(object: DiffUtil.Callback() {
            override fun getOldListSize(): Int {
                return players.size
            }

            override fun getNewListSize(): Int {
                return newDataSet.size
            }

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return players[oldItemPosition] == newDataSet[newItemPosition]
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return players[oldItemPosition] == newDataSet[newItemPosition]
            }
        })
        /*
            switching to the main thread; updating UI stuffs has to be done on the main thread
         */
        withContext(Dispatchers.Main) {
            players = newDataSet
            diff.dispatchUpdatesTo(this@PlayerAdapter)
        }
    }

    var players = listOf<PlayerData>()
        private set

    override fun getItemCount(): Int {
        return players.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlayerViewHolder {
        return PlayerViewHolder(
            ItemPlayerBinding.inflate(LayoutInflater.from(parent.context),
            parent,
            false)
        )
    }

    override fun onBindViewHolder(holder: PlayerViewHolder, position: Int) {
        val player = players[position]
        holder.binding.apply {
            val playerRankText = "${player.rank}. "
            tvRank.text = playerRankText
            tvScore.text = player.score.toString()
            tvUsername.text = player.userName
            ivPencil.isVisible = player.isDrawing
        }
    }

}