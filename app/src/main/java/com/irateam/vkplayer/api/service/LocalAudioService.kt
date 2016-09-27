/*
 * Copyright (C)r 2016 IRA-Team
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

package com.irateam.vkplayer.api.service

import android.content.Context
import android.util.Log
import com.irateam.vkplayer.R
import com.irateam.vkplayer.api.AbstractQuery
import com.irateam.vkplayer.api.ProgressableAbstractQuery
import com.irateam.vkplayer.api.ProgressableQuery
import com.irateam.vkplayer.api.Query
import com.irateam.vkplayer.database.AudioLocalIndexedDatabase
import com.irateam.vkplayer.event.AudioScannedEvent
import com.irateam.vkplayer.model.LocalAudio
import com.irateam.vkplayer.util.extension.e
import com.irateam.vkplayer.util.extension.process
import com.mpatric.mp3agic.Mp3File
import java.io.File
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

class LocalAudioService {

    private val context: Context
    private val database: AudioLocalIndexedDatabase
    private val nameDiscover: LocalAudioNameDiscover

    private val unknownArtist: String
    private val unknownTitle: String

    constructor(context: Context) {
        this.context = context
        this.database = AudioLocalIndexedDatabase(context)
        this.nameDiscover = LocalAudioNameDiscover()

        this.unknownArtist = context.getString(R.string.unknown_artist)
        this.unknownTitle = context.getString(R.string.unknown_title)
    }

    fun scan(): ProgressableQuery<List<LocalAudio>, AudioScannedEvent> {
        val root = File("/storage")
        return ScanAndIndexAudioQuery(root)
    }

    fun getAllIndexed(): Query<List<LocalAudio>> {
        return IndexedAudioQuery()
    }

    fun removeFromFilesystem(audios: Collection<LocalAudio>): Query<Collection<LocalAudio>> {
        return RemoveFromFilesystemQuery(audios)
    }

    private fun createLocalAudioFromMp3(mp3: Mp3File): LocalAudio = if (mp3.hasId3v2Tag()) {
        val artist = mp3.id3v2Tag.artist ?: unknownArtist
        val title = mp3.id3v2Tag.title ?: unknownTitle
        mp3.id3v2Tag.albumImage

        LocalAudio(artist,
                title,
                mp3.lengthInSeconds.toInt(),
                mp3.filename)
    } else {
        val name = File(mp3.filename).nameWithoutExtension
        val titleArtist = nameDiscover.getTitleAndArtist(name)

        val artist = titleArtist.artist ?: unknownArtist
        val title = titleArtist.title ?: unknownTitle

        LocalAudio(artist,
                title,
                mp3.lengthInSeconds.toInt(),
                mp3.filename)
    }

    private inner class IndexedAudioQuery : AbstractQuery<List<LocalAudio>>() {

        override fun query() = database.getAll()
    }

    private inner class ScanAndIndexAudioQuery : ProgressableAbstractQuery<List<LocalAudio>, AudioScannedEvent> {

        val root: File

        constructor(root: File) : super() {
            this.root = root
        }

        override fun query(): List<LocalAudio> {
            val audios = root.walk()
                    .toList()
                    .map { e(it); it }
                    .filter { !it.isDirectory }
                    .filter { it.name.endsWith(".mp3") }
                    .map {
                        try {
                            Mp3File(it.path)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            e(it.path)
                            null
                        }
                    }
                    .filterNotNull()

            val total = audios.size

            val elementsCount = Math.ceil(total.toDouble() / CORE_COUNT).toInt()
            val slices = ArrayList<List<Mp3File>>()
            for (i in 0..CORE_COUNT - 1) {
                val probMax = (i + 1) * elementsCount
                val max = if (probMax > total) {
                    total
                } else {
                    probMax
                }

                val range = IntRange(i * elementsCount, max - 1)
                slices.add(audios.toList().slice(range))
            }

            val i = AtomicInteger()
            val tasks = slices.map {
                Callable {
                    it.map { createLocalAudioFromMp3(it) }
                            .process {
                                try {
                                    database.index(it)
                                    Log.i(TAG, "Stored $it")
                                } catch (ignore: Exception) {
                                }

                                notifyProgress(AudioScannedEvent(
                                        it,
                                        i.incrementAndGet(),
                                        audios.count()))
                            }
                            .toList()
                }
            }

            return CONVERTER_EXECUTOR.invokeAll(tasks)
                    .map { it.get() }
                    .flatMap { it }
        }
    }

    private inner class RemoveFromFilesystemQuery : AbstractQuery<Collection<LocalAudio>> {

        val audios: Collection<LocalAudio>

        /**
         * Always make copy
         */
        constructor(audios: Collection<LocalAudio>) : super() {
            this.audios = audios.toList()
        }

        override fun query(): Collection<LocalAudio> {
            val removed = ArrayList<LocalAudio>()
            audios.forEach {
                val file = File(it.path)
                if (!file.exists() || file.delete()) {
                    database.delete(it)
                    removed.add(it)
                } else {
                    throw AccessDeniedException(
                            file = file,
                            reason = "The error occurred during deleting file.")
                }
            }
            return removed
        }
    }

    companion object {
        val TAG: String = LocalAudioService::class.java.name

        val CORE_COUNT = Runtime.getRuntime().availableProcessors() * 2
        val CONVERTER_EXECUTOR: ExecutorService = Executors.newFixedThreadPool(CORE_COUNT)
    }
}