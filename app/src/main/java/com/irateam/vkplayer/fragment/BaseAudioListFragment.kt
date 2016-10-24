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

package com.irateam.vkplayer.fragment

import android.os.Bundle
import android.support.annotation.MenuRes
import android.support.v4.app.Fragment
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.SearchView
import android.view.*
import com.irateam.vkplayer.R
import com.irateam.vkplayer.adapter.BaseAudioRecyclerAdapter
import com.irateam.vkplayer.controller.PlayerController
import com.irateam.vkplayer.model.Audio
import com.irateam.vkplayer.player.Player
import com.irateam.vkplayer.util.EventBus
import com.irateam.vkplayer.util.extension.getViewById
import com.irateam.vkplayer.util.extension.slideInUp
import com.irateam.vkplayer.util.extension.slideOutDown
import java.util.*

abstract class BaseAudioListFragment : Fragment(),
        ActionMode.Callback,
        SearchView.OnQueryTextListener,
        BackPressedListener,
        BaseAudioRecyclerAdapter.CheckedListener {


    protected abstract val adapter: BaseAudioRecyclerAdapter<out Audio, out RecyclerView.ViewHolder>

    /**
     * Views
     */
    protected lateinit var recyclerView: RecyclerView
    protected lateinit var refreshLayout: SwipeRefreshLayout
    protected lateinit var emptyView: View
    protected lateinit var sortModeHolder: View

    /**
     * Menus
     */
    protected lateinit var menu: Menu
    protected lateinit var searchView: SearchView

    /**
     * Action Mode
     */
    protected var actionMode: ActionMode? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    /**
     * Configure view components
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        recyclerView = view.getViewById(R.id.recycler_view)
        configureRecyclerView()

        refreshLayout = view.getViewById(R.id.refresh_layout)
        configureRefreshLayout()

        sortModeHolder = view.getViewById(R.id.sort_mode_holder)
        configureSortModeHolder()

        emptyView = view.getViewById(R.id.empty_view)
        configureEmptyView()
    }

	override fun onStart() {
		super.onStart()
		EventBus.register(adapter)
	}

	override fun onStop() {
		EventBus.unregister(adapter)
		super.onStop()
	}

    /**
     * Initialize menu variable and configure SearchView.
     */
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        this.menu = menu
        activity.menuInflater.inflate(getMenuResource(), menu)
        val itemSearch = menu.findItem(R.id.action_search)

        searchView = itemSearch.actionView as SearchView
        searchView.setIconifiedByDefault(false)
        searchView.setOnQueryTextListener(this)
    }

    /**
     * Must return resource of menu that would be inflated
     */
    @MenuRes
    protected abstract fun getMenuResource(): Int

    /**
     * Dispatches select event of menu items that are common for any subclass
     */
    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.action_sort -> {
            startSortMode()
            true
        }

        R.id.action_sort_done -> {
            commitSortMode()
            true
        }

        else -> false
    }


    /**
     * Callback that notify about switching checked state of audio.
     * Start ActionMode if set of checked audios not empty.
     * If set of checked audios becomes empty ActionMode would be closed.
     */
    override fun onChanged(audio: Audio, checked: HashSet<out Audio>) {
        if (actionMode == null && checked.size > 0) {
            actionMode = activity.startActionMode(this)
        }

        actionMode?.apply {
            if (checked.isEmpty()) {
                finish()
                return
            }

            title = checked.size.toString()
        }

    }

    /**
     * Do nothing. Search process invokes by onQueryTextChange
     */
    override fun onQueryTextSubmit(query: String) = false

    /**
     * Notify adapter about search query.
     * All search logic should be provided by adapter.
     */
    override fun onQueryTextChange(query: String): Boolean {
        adapter.setSearchQuery(query)
        return true
    }

    @MenuRes
    protected abstract fun getActionModeMenuResource(): Int

    /**
     * Creates ActionMode and assign it to variable
     */
    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        actionMode = mode
        mode.menuInflater.inflate(getActionModeMenuResource(), menu)
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
        return false
    }

    /**
     * Dispatches select event of ActionMode menu items that are common for any subclasses.
     * In the end finish ActionMode.
     */
    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_play -> {
                val audios = adapter.checkedAudios.toList()
                Player.play(audios, audios[0])
            }

            R.id.action_play_next -> {
                val audios = adapter.checkedAudios.toList()
                Player.addToPlayNext(audios)
            }

            R.id.action_delete -> {
                adapter.removeChecked()
            }

            R.id.action_add_to_queue -> {
                Player.addToQueue(adapter.checkedAudios)
            }
        }
        mode.finish()
        return true
    }

    override fun onDestroyActionMode(mode: ActionMode) {
        adapter.clearChecked()
        actionMode = null
    }

    override fun onBackPressed(): Boolean {
        if (adapter.isSortMode()) {
            revertSortMode()
            return true
        } else {
            return false
        }
    }

    private fun configureRecyclerView() {
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.itemAnimator = DefaultItemAnimator()
    }

    private fun configureRefreshLayout() {
        refreshLayout.setColorSchemeResources(R.color.accent, R.color.primary)
        refreshLayout.setOnRefreshListener {
            actionMode?.finish()
            if (adapter.isSortMode()) {
                commitSortMode()
            }
            onRefresh()
        }
    }

    open protected fun onRefresh() {

    }

    //TODO:
    private fun configureSortModeHolder() {
        sortModeHolder.apply {
            findViewById(R.id.sort_by_title).setOnClickListener {
                //adapter.sort(Comparators.TITLE_COMPARATOR)
            }

            findViewById(R.id.sort_by_artist).setOnClickListener {
                //adapter.sort(Comparators.ARTIST_COMPARATOR)
            }

            findViewById(R.id.sort_by_length).setOnClickListener {
                //adapter.sort(Comparators.LENGTH_COMPARATOR)
            }
        }
    }

    open protected fun configureEmptyView() {

    }

    private fun startSortMode() {
        adapter.startSortMode()
        configureStartSortMode()
    }

    private fun commitSortMode() {
        adapter.commitSortMode()
        configureFinishSortMode()
    }

    private fun revertSortMode() {
        adapter.revertSortMode()
        configureFinishSortMode()
    }

    private fun configureStartSortMode() {
        activity.apply {
            if (this is PlayerController.VisibilityController) {
                hidePlayerController()
            }
        }

        sortModeHolder.slideInUp()
        menu.findItem(R.id.action_sort).isVisible = false
        menu.findItem(R.id.action_sort_done).isVisible = true
    }

    private fun configureFinishSortMode() {
        activity.apply {
            if (this is PlayerController.VisibilityController) {
                showPlayerController()
            }
        }

        sortModeHolder.slideOutDown()
        menu.findItem(R.id.action_sort).isVisible = true
        menu.findItem(R.id.action_sort_done).isVisible = false
    }
}