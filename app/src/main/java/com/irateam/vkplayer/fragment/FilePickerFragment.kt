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
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.irateam.vkplayer.R
import com.irateam.vkplayer.adapter.FilePickerRecyclerAdapter
import com.irateam.vkplayer.util.extension.getViewById
import com.irateam.vkplayer.util.filepicker.PickedStateProvider
import java.io.File
import java.util.*

class FilePickerFragment : Fragment(), PickedStateProvider {

	private lateinit var recyclerView: RecyclerView
	private lateinit var adapter: FilePickerRecyclerAdapter

	private val pickedFiles: HashSet<File> = HashSet()
	private val excludedFiles: HashSet<File> = HashSet()

	override fun onCreateView(inflater: LayoutInflater,
							  container: ViewGroup?,
							  savedInstanceState: Bundle?): View {

		return inflater.inflate(R.layout.fragment_file_picker, container, false)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		recyclerView = view.getViewById(R.id.recycler_view)
		configureRecyclerView()
	}

	private fun configureRecyclerView() {
		adapter = FilePickerRecyclerAdapter.Builder()
				.setDirectory(File("/"))
				.setPickedStateProvider(this)
				.showDirectories(false)
				.build()

		recyclerView.adapter = adapter
		recyclerView.layoutManager = LinearLayoutManager(context)
	}

	override fun getPickedFiles(): Collection<File> {
		return pickedFiles
	}

	override fun getExcludedFiles(): Collection<File> {
		return excludedFiles
	}

	companion object {

		@JvmStatic
		fun newInstance() = FilePickerFragment()
	}
}