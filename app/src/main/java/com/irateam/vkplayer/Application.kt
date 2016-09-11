/*
 * Copyright (C) 2015 IRA-Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.irateam.vkplayer

import com.irateam.vkplayer.api.service.LocalAudioService
import com.irateam.vkplayer.util.extension.success
import com.nostra13.universalimageloader.core.DisplayImageOptions
import com.nostra13.universalimageloader.core.ImageLoader
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration
import com.vk.sdk.VKSdk

class Application : android.app.Application() {

    override fun onCreate() {
        super.onCreate()
        VKSdk.initialize(this)

        val options = DisplayImageOptions.Builder()
                .cacheInMemory(true)
                .cacheOnDisk(true)
                .build()

        val config = ImageLoaderConfiguration.Builder(this)
                .defaultDisplayImageOptions(options)
                .build()

        /*  vkDir.walk()
                  .filter { !it.isDirectory }
                  .map {
                      try {
                          Mp3File(it.path)
                      } catch (e: Exception) {
                          null
                      }
                  }
                  .filterNotNull()
                  .map { it.id3v2Tag }
                  .filterNotNull()
                  .forEach { Log.e("MP3ScanVK", it.artist + " - " + it.title) }*/

        LocalAudioService(this).getAllIndexed().execute(success {
            println(it)
        })

        ImageLoader.getInstance().init(config)
    }
}
