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
import android.os.Environment
import android.util.Log
import com.irateam.vkplayer.api.AbstractQuery
import com.irateam.vkplayer.api.ProgressableAbstractQuery
import com.irateam.vkplayer.api.ProgressableQuery
import com.irateam.vkplayer.api.Query
import com.irateam.vkplayer.database.AudioLocalIndexedDatabase
import com.irateam.vkplayer.event.AudioScannedEvent
import com.irateam.vkplayer.models.LocalAudio
import com.mpatric.mp3agic.Mp3File
import java.io.File

class LocalAudioService {

    val database: AudioLocalIndexedDatabase
    val nameDiscover: LocalAudioNameDiscover

    constructor(context: Context) {
        database = AudioLocalIndexedDatabase(context)
        nameDiscover = LocalAudioNameDiscover()
    }

    fun scan(): ProgressableQuery<List<LocalAudio>, AudioScannedEvent> {
        val root = Environment.getExternalStorageDirectory()
        return ScanAndIndexAudioQuery(root)
    }

    fun getAllIndexed(): Query<List<LocalAudio>> {
        return IndexedAudioQuery()
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
                    .filter { !it.isDirectory }
                    .filter { it.name.endsWith(".mp3") }
                    .map { Mp3File(it.path) }

            /**
             * This looks like kotlin's bug but audios.count() locks thread.
             * .toList(), ArrayList(..) locks too.
             */
            val total = 1

            return audios
                    .map { createLocalAudioFromMp3(it) }
                    .mapIndexed { i, audio ->
                        try {
                            database.index(audio)
                            Log.e(TAG, "Stored $audio")
                        } catch (ignore: Exception) {
                        }
                        notifyProgress(AudioScannedEvent(audio, i + 1, total))
                        audio
                    }
                    .toList()
        }
    }

    private fun createLocalAudioFromMp3(mp3: Mp3File): LocalAudio = if (mp3.hasId3v2Tag()) {
        LocalAudio(mp3.id3v2Tag.artist,
                mp3.id3v2Tag.title,
                mp3.lengthInSeconds.toInt(),
                mp3.filename)
    } else {
        val name = File(mp3.filename).nameWithoutExtension
        val titleArtist = nameDiscover.getTitleAndArtist(name)

        LocalAudio(titleArtist.artist,
                titleArtist.title,
                mp3.lengthInSeconds.toInt(),
                mp3.filename)
    }

    companion object {
        val TAG = LocalAudioService::class.java.name
    }
}