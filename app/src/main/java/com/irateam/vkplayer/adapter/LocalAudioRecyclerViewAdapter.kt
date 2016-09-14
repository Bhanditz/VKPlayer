/*
 * Copyright (C) 2016 IRA-Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.irateam.vkplayer.adapter

import android.support.v4.view.MotionEventCompat
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import com.irateam.vkplayer.R
import com.irateam.vkplayer.event.DownloadFinishedEvent
import com.irateam.vkplayer.event.Event
import com.irateam.vkplayer.event.ItemUncheckedEvent
import com.irateam.vkplayer.models.Audio
import com.irateam.vkplayer.models.LocalAudio
import com.irateam.vkplayer.player.*
import com.irateam.vkplayer.ui.ItemTouchHelperAdapter
import com.irateam.vkplayer.ui.SimpleItemTouchHelperCallback
import com.irateam.vkplayer.ui.viewholder.AudioViewHolder
import com.irateam.vkplayer.ui.viewholder.AudioViewHolder.State.*
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.*

/**
 * @author Artem Glugovsky
 */
class LocalAudioRecyclerViewAdapter : RecyclerView.Adapter<AudioViewHolder>(),
        ItemTouchHelperAdapter {

    private val itemTouchHelper: ItemTouchHelper

    private var sortMode = false
    private var audios: ArrayList<LocalAudio> = ArrayList()
    private var searchQuery: String? = null

    var checkedAudios: HashSet<LocalAudio> = LinkedHashSet()
    var checkedListener: CheckedListener? = null

    init {
        val callback = SimpleItemTouchHelperCallback(this)
        itemTouchHelper = ItemTouchHelper(callback)
    }


    override fun onCreateViewHolder(parent: ViewGroup, position: Int): AudioViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val v = inflater.inflate(R.layout.item_audio, parent, false)
        return AudioViewHolder(v)
    }

    override fun onBindViewHolder(holder: AudioViewHolder?, position: Int) {
        throw UnsupportedOperationException("not implemented")
    }

    override fun onBindViewHolder(holder: AudioViewHolder,
                                  position: Int,
                                  payload: MutableList<Any>?) {

        if (payload?.isEmpty() ?: true) {
            val audio = audios[position]
            configureAudio(holder, audio)
            configurePlayingState(holder, audio)

            if (sortMode) {
                configureSortMode(holder, audio)

            } else {
                configureCheckedState(holder, audio)
            }
        } else {
            payload?.let {
                val events = it.filterIsInstance<Event>()
                dispatchEvents(holder, position, events)
            }
        }
    }

    override fun getItemCount(): Int {
        return audios.size
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView?) {
        itemTouchHelper.attachToRecyclerView(recyclerView)
    }

    override fun onItemMove(fromPosition: Int, toPosition: Int): Boolean {
        Collections.swap(audios, fromPosition, toPosition)
        notifyItemMoved(fromPosition, toPosition)
        return true
    }

    override fun onItemDismiss(position: Int) {
        audios.removeAt(position)
        notifyItemRemoved(position)
    }

    private fun dispatchEvents(holder: AudioViewHolder,
                               position: Int,
                               events: Collection<Event>) {
        events.forEach {
            when (it) {
                is ItemUncheckedEvent -> {
                    holder.setChecked(checked = false, shouldAnimate = true)
                }
            }
        }
    }

    private fun configureAudio(holder: AudioViewHolder, audio: Audio) {
        holder.setAudio(audio)
        val searchQuery = searchQuery
        if (searchQuery != null) holder.setQuery(searchQuery)
        holder.contentHolder.setOnClickListener {
            Player.play(audios, audio)
        }
    }

    private fun configurePlayingState(holder: AudioViewHolder, audio: Audio) {
        val playingAudio = Player.audio
        if (audio.id == playingAudio?.id) {
            val state = when {
                !Player.isReady -> PREPARE
                Player.isReady && Player.isPlaying -> PLAY
                Player.isReady && !Player.isPlaying -> PAUSE
                else -> NONE
            }
            holder.setPlayingState(state)
        }
    }

    private fun configureCheckedState(holder: AudioViewHolder, audio: LocalAudio) {
        holder.setChecked(audio in checkedAudios)
        holder.coverHolder.setOnClickListener {
            holder.toggleChecked(true)
            if (holder.isChecked()) checkedAudios.add(audio) else checkedAudios.remove(audio)
            checkedListener?.onChanged(audio, checkedAudios)
        }
    }

    private fun configureSortMode(holder: AudioViewHolder, audio: Audio) {
        holder.setSorting(sortMode)
        if (sortMode) {
            holder.coverHolder.setOnTouchListener { v, e ->
                if (MotionEventCompat.getActionMasked(e) == MotionEvent.ACTION_DOWN) {
                    itemTouchHelper.startDrag(holder)
                }
                false
            }
        }
    }

    fun clearChecked() {
        checkedAudios.forEach {
            notifyItemChanged(audios.indexOf(it), ItemUncheckedEvent())
        }
        checkedAudios.clear()
    }

    fun removeAll(audios: Collection<Audio>) {
        audios.forEach {
            val index = audios.indexOf(it)
            this.audios.removeAt(index)
        }
    }

    fun removeChecked() {
        val forIterate = ArrayList(checkedAudios)
        forIterate.forEach {
            val index = audios.indexOf(it)
            audios.removeAt(index)
            checkedAudios.remove(it)
            notifyItemRemoved(index)
        }
    }

    fun setSortMode(enabled: Boolean) {
        sortMode = enabled
        notifyDataSetChanged()
    }

    fun isSortMode(): Boolean {
        return sortMode
    }

    fun setAudios(audios: Collection<LocalAudio>) {
        this.audios = ArrayList(audios)
        notifyDataSetChanged()
    }

    fun setSearchQuery(query: String) {
        val lowerQuery = query.toLowerCase()
        searchQuery = query

        val filtered = audios.filter {
            lowerQuery in it.title.toLowerCase() || lowerQuery in it.artist.toLowerCase()
        }

        audios.clear()
        audios.addAll(filtered)
        notifyDataSetChanged()
    }

    fun clear() {
        audios = ArrayList()
        clearSearchQuery()
        clearChecked()
        notifyDataSetChanged()
    }

    fun addAudio(audio: LocalAudio) {
        audios.add(audio)
        notifyItemInserted(audios.indexOf(audio))
    }

    fun clearSearchQuery() {
        searchQuery = null
        notifyDataSetChanged()
    }

    @Subscribe
    fun onStartEvent(e: PlayerStartEvent) {
        notifyEvent(e)
    }

    @Subscribe
    fun onPlayEvent(e: PlayerPlayEvent) {
        notifyEvent(e)
    }

    @Subscribe
    fun onResumeEvent(e: PlayerResumeEvent) {
        notifyEvent(e)
    }

    @Subscribe
    fun onPauseEvent(e: PlayerPauseEvent) {
        notifyEvent(e)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onDownloadFinished(e: DownloadFinishedEvent) {
        val audio = e.audio
        audios.filter { it.id == audio.id }
                .forEach {
                    val index = audios.indexOf(it)
                    notifyItemChanged(index, e)
                }
    }

    private fun notifyEvent(e: PlayerEvent) {
        notifyDataSetChanged()
    }

    interface CheckedListener {

        fun onChanged(audio: Audio, checked: HashSet<LocalAudio>)
    }

    companion object {

        private val TYPE_HEADER = 1
        private val TYPE_AUDIO = 2
    }
}
