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

package com.irateam.vkplayer.api.service

import com.irateam.vkplayer.api.Query
import com.irateam.vkplayer.api.VKAudioQuery
import com.irateam.vkplayer.model.VKAudio
import com.vk.sdk.api.VKApi
import com.vk.sdk.api.VKApiConst
import com.vk.sdk.api.VKParameters

class VKSearchService {

    fun search(query: String): Query<List<VKAudio>> {
        val request = VKApi.audio().search(VKParameters.from(VKApiConst.Q, query))
        return VKAudioQuery(request)
    }
}