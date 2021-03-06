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

package com.irateam.vkplayer.api

abstract class ProgressableAbstractQuery<T, P> : AbstractQuery<T>(), ProgressableQuery<T, P> {

	private var progressableCallback: ProgressableCallback<T, P>? = null

	override fun execute(callback: ProgressableCallback<T, P>) {
		this.progressableCallback = callback
		execute(callback as Callback<T>)
	}

	protected fun notifyProgress(progress: P) {
		progressableCallback?.let { UI_HANDLER.post { it.onProgress(progress) } }
	}
}
