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

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.*
import android.widget.TextView
import android.widget.Toast
import com.irateam.vkplayer.R
import com.irateam.vkplayer.adapter.LocalAudioRecyclerAdapter
import com.irateam.vkplayer.api.SimpleProgressableCallback
import com.irateam.vkplayer.api.service.LocalAudioService
import com.irateam.vkplayer.dialog.LocalAudioRemoveAlertDialog
import com.irateam.vkplayer.event.AudioScannedEvent
import com.irateam.vkplayer.models.LocalAudio
import com.irateam.vkplayer.util.extension.*

class LocalAudioListFragment : BaseAudioListFragment() {

    override val adapter = LocalAudioRecyclerAdapter()

    /**
     * Services
     */
    private lateinit var localAudioService: LocalAudioService

    /**
     * Views
     */
    private lateinit var scanProgressHolder: View
    private lateinit var scanProgress: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        localAudioService = LocalAudioService(context)
    }

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View {

        return inflater.inflate(R.layout.fragment_local_audio_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        scanProgressHolder = view.getViewById(R.id.scan_progress_holder)
        scanProgress = view.getViewById(R.id.scan_progress)

        adapter.checkedListener = this

        loadLocalAudios()
    }

    override fun onRefresh() {
        loadLocalAudios()
    }

    override fun configureEmptyView() {
        emptyView.findViewById(R.id.empty_list_scan_audio).setOnClickListener {
            scanLocalAudios()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.action_scan -> {
            scanLocalAudios()
            true
        }

        else -> super.onOptionsItemSelected(item)
    }

    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.action_remove_from_filesystem -> {
                val dialog = LocalAudioRemoveAlertDialog()
                dialog.positiveButtonClickListener = {
                    removeCheckedLocalAudios()
                    actionMode?.finish()
                }
                dialog.show(fragmentManager, LocalAudioRemoveAlertDialog.TAG)
                return false
            }
        }

        return super.onActionItemClicked(mode, item)
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<out String>,
                                            grantResults: IntArray) {

        when (requestCode) {
            SCAN_LOCAL_AUDIO_REQUEST_CODE -> if (grantResults.isNotEmpty()
                    && grantResults.first() == PackageManager.PERMISSION_GRANTED) {

                startScanLocalAudios()
            } else {
                Toast.makeText(context, "We Need permission Storage", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadLocalAudios() {
        refreshLayout.post { refreshLayout.isRefreshing = true }
        localAudioService.getAllIndexed().execute(success<List<LocalAudio>> {
            adapter.setAudios(it)
            emptyView.isVisible = it.isEmpty()
        } finish {
            refreshLayout.post { refreshLayout.isRefreshing = false }
        })
    }

    private fun removeCheckedLocalAudios() {
        localAudioService.removeFromFilesystem(adapter.checkedAudios).execute(
                success<Collection<LocalAudio>> {
                    adapter.removeAll(it)
                })
    }

    private fun scanLocalAudios() {
        requestPermissions(
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                SCAN_LOCAL_AUDIO_REQUEST_CODE)
    }

    private fun startScanLocalAudios() {
        scanProgress.isVisible = false
        scanProgressHolder.slideInDown()
        adapter.setAudios(emptyList())

        localAudioService.scan().execute(
                SimpleProgressableCallback<List<LocalAudio>, AudioScannedEvent> {
                    adapter.setAudios(it)
                } progress {
                    if (!scanProgress.isVisible) {
                        scanProgress.isVisible = true
                    }

                    if (emptyView.isVisible) {
                        emptyView.isVisible = false
                    }

                    adapter.addAudio(it.audio)
                    scanProgress.text = "${it.current}/${it.total}"
                } finish {
                    scanProgressHolder.slideOutUp()
                })
    }


    companion object {

        val TAG = LocalAudioListFragment::class.java.name
        val SCAN_LOCAL_AUDIO_REQUEST_CODE = 1

        @JvmStatic
        fun newInstance(): LocalAudioListFragment {
            return LocalAudioListFragment()
        }
    }
}