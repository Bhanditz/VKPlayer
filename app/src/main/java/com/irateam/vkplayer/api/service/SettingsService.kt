package com.irateam.vkplayer.api.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.SharedPreferences
import android.os.Environment
import android.preference.PreferenceManager
import com.irateam.vkplayer.player.Player.RepeatState
import com.irateam.vkplayer.service.DownloadService
import com.irateam.vkplayer.util.SharedPreferencesProvider
import com.irateam.vkplayer.util.extension.SharedPreferencesDelegates.boolean
import com.irateam.vkplayer.util.extension.SharedPreferencesDelegates.custom
import com.irateam.vkplayer.util.extension.SharedPreferencesDelegates.int
import com.irateam.vkplayer.util.extension.SharedPreferencesDelegates.string
import com.irateam.vkplayer.util.extension.SharedPreferencesDelegates.time
import java.io.File
import java.util.*

class SettingsService : SharedPreferencesProvider {

    private val context: Context
    override val sharedPreferences: SharedPreferences

    constructor(context: Context) {
        this.context = context
        this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    }

    var language: String by string(getCurrentLanguageCode())

    //Playback
    var repeatState: RepeatState by custom(RepeatState.NO_REPEAT, { RepeatState.valueOf(it) })
    var randomState: Boolean by boolean(false)

    //Sync
    var syncEnabled: Boolean by boolean(false)
    var syncWifiOnly: Boolean by boolean(false)
    var syncCount: Int by int(DEFAULT_SYNC_COUNT)
    var syncTime: Calendar by time(DEFAULT_SYNC_TIME)

    fun getAudioCacheDir(): File {
        val state = Environment.getExternalStorageState()
        return if (Environment.MEDIA_MOUNTED == state) {
            context.getExternalFilesDir(null)
        } else {
            context.filesDir
        }
    }

    fun setSyncAlarm() {
        val intent = DownloadService.startSyncIntent(context, false)
        val pendingIntent = PendingIntent.getService(context,
                SYNC_ALARM_ID,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT)

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP,
                syncTime.timeInMillis,
                AlarmManager.INTERVAL_DAY,
                pendingIntent)
    }

    fun cancelSyncAlarm() {
        val intent = DownloadService.startSyncIntent(context, false)
        val pendingIntent = PendingIntent.getService(context,
                SYNC_ALARM_ID,
                intent,
                0)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)
    }

    private fun getCurrentLanguageCode() : String {
        return Locale.getDefault().language
    }

    companion object {

        private val SYNC_ALARM_ID = 1
        private val DEFAULT_SYNC_COUNT = 10
        private val DEFAULT_SYNC_TIME: Long = 60 * 60 * 1000

        @JvmField val SYNC_TIME = "sync_time"
    }
}