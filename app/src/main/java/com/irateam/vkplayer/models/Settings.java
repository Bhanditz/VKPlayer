package com.irateam.vkplayer.models;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.irateam.vkplayer.player.Player;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class Settings {

    public static final String REPEAT_STATE = "repeat_state";
    public static final String RANDOM_STATE = "random_state";

    public static final String SYNC_ENABLED = "sync_enabled";
    public static final String SYNC_TIME = "sync_time";
    public static final String SYNC_COUNT = "sync_count";

    private static Settings instance;

    private SharedPreferences preferences;

    public static synchronized Settings getInstance(Context context) {
        if (instance == null) {
            instance = new Settings(context);
        }
        return instance;
    }

    private Settings(Context context) {
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public void setPlayerRepeat(Player.RepeatState state) {
        preferences.edit()
                .putString(REPEAT_STATE, state.name())
                .apply();
    }

    public Player.RepeatState getPlayerRepeat() {
        return Player.RepeatState.valueOf(preferences.getString(REPEAT_STATE, Player.RepeatState.NO_REPEAT.name()));
    }

    public void setRandomState(boolean state) {
        preferences.edit()
                .putBoolean(RANDOM_STATE, state)
                .apply();
    }

    public boolean getRandomState() {
        return preferences.getBoolean(RANDOM_STATE, false);
    }

    public void setSyncEnabled(boolean enabled) {
        preferences.edit()
                .putBoolean(SYNC_ENABLED, enabled)
                .apply();
    }

    public boolean getSyncEnabled() {
        return preferences.getBoolean(SYNC_ENABLED, false);
    }

    public void setSyncTime(int hour, int minutes) {
        preferences.edit()
                .putString(SYNC_TIME, String.format("%02d", hour) + ":" + String.format("%02d", minutes))
                .apply();
    }

    public Calendar getSyncTime() {
        Calendar calendar = Calendar.getInstance();
        Date date = null;
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm");
        try {
            date = simpleDateFormat.parse(preferences.getString(SYNC_TIME, "18:30"));
        } catch (ParseException e) {
            e.printStackTrace();
        }

        calendar.set(Calendar.HOUR_OF_DAY, date.getHours());
        calendar.set(Calendar.MINUTE, date.getMinutes());
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
            calendar.set(Calendar.DAY_OF_YEAR, calendar.get(Calendar.DAY_OF_YEAR) + 1);
        }
        return calendar;
    }

    public void setSyncCount(int count) {
        preferences.edit()
                .putString(SYNC_COUNT, String.valueOf(count))
                .apply();
    }

    public int getSyncCount() {
        return Integer.valueOf(preferences.getString(SYNC_COUNT, "10"));
    }
}