package com.irateam.vkplayer.services;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Binder;
import android.os.IBinder;

import com.irateam.vkplayer.models.Audio;
import com.irateam.vkplayer.models.Settings;
import com.irateam.vkplayer.notifications.PlayerNotification;
import com.irateam.vkplayer.player.Player;
import com.irateam.vkplayer.receivers.DownloadFinishedReceiver;

import java.util.List;

public class PlayerService extends Service implements Player.PlayerEventListener, AudioManager.OnAudioFocusChangeListener {

    public static final String PREVIOUS = "playerService.PREVIOUS";
    public static final String PAUSE = "playerService.PAUSE";
    public static final String RESUME = "playerService.RESUME";
    public static final String NEXT = "playerService.NEXT";
    public static final String STOP = "playerService.STOP";

    private Player player;
    private Binder binder = new PlayerBinder();
    private BroadcastReceiver headsetReceiver;
    private AudioManager audioManager;

    private Settings settings;
    private boolean removeNotification = false;
    private boolean wasPlaying = false;
    private DownloadFinishedReceiver downloadFinishedReceiver;

    @Override
    public void onCreate() {
        super.onCreate();
        player = new Player();
        player.addPlayerEventListener(this);
        settings = Settings.getInstance(this);
        player.setRepeatState(settings.getPlayerRepeat());
        player.setRandomState(settings.getRandomState());

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);

        headsetReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(Intent.ACTION_HEADSET_PLUG)) {
                    if (intent.getIntExtra("state", -1) == 0) {
                        if (isPlaying()) {
                            pause(false);
                        }
                    }
                }
            }
        };

        downloadFinishedReceiver = new DownloadFinishedReceiver() {
            @Override
            public void onDownloadFinished(Audio downloaded) {
                for (Audio audio : getPlaylist()) {
                    if (audio.equalsId(downloaded)) {
                        audio.setCacheFile(downloaded.getCacheFile());
                    }
                }
            }
        };

        registerReceiver(headsetReceiver, new IntentFilter(Intent.ACTION_HEADSET_PLUG));
        registerReceiver(downloadFinishedReceiver, new IntentFilter(DownloadService.DOWNLOAD_FINISHED));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();
        if (action != null) {
            switch (action) {
                case PREVIOUS:
                    previous();
                    break;
                case PAUSE:
                    pause(false);
                    break;
                case RESUME:
                    resume();
                    break;
                case NEXT:
                    next();
                    break;
                case STOP:
                    stop();
                    stopForeground(true);
                    break;
            }
        }
        return START_NOT_STICKY;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        player.removePlayerEventListener(this);
        unregisterReceiver(headsetReceiver);
        unregisterReceiver(downloadFinishedReceiver);
        audioManager.abandonAudioFocus(this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public class PlayerBinder extends Binder {
        public PlayerService getPlayerService() {
            return PlayerService.this;
        }
    }

    //Player methods
    public void setPlaylist(List<Audio> list) {
        player.setList(list);
    }

    public List<Audio> getPlaylist() {
        return player.getList();
    }

    public void play(int index) {
        player.play(index);
    }

    public void resume() {
        player.resume();
    }

    public void pause() {
        pause(true);
    }

    public void pause(boolean removeNotification) {
        this.removeNotification = removeNotification;
        player.pause();
    }

    public void stop() {
        player.stop();
    }

    public void next() {
        player.next();
    }

    public void previous() {
        player.previous();
    }

    public boolean isPlaying() {
        return player.isPlaying();
    }

    public boolean isReady() {
        return player.isReady();
    }

    public int getPauseTime() {
        return player.getPauseTime();
    }

    public Audio getPlayingAudio() {
        return player.getPlayingAudio();
    }

    public Integer getPlayingAudioIndex() {
        return player.getPlayingAudioIndex();
    }

    public void setRepeatState(Player.RepeatState state) {
        settings.setPlayerRepeat(state);
        player.setRepeatState(state);
    }

    public Player.RepeatState switchRepeatState() {
        Player.RepeatState state = player.switchRepeatState();
        settings.setPlayerRepeat(state);
        return state;
    }

    public Player.RepeatState getRepeatState() {
        return player.getRepeatState();
    }

    public boolean switchRandomState() {
        boolean state = player.switchRandomState();
        settings.setRandomState(state);
        return state;
    }

    public boolean getRandomState() {
        return player.getRandomState();
    }

    public void addPlayerEventListener(Player.PlayerEventListener listener) {
        player.addPlayerEventListener(listener);
    }

    public void removePlayerEventListener(Player.PlayerEventListener listener) {
        player.removePlayerEventListener(listener);
    }

    public void addPlayerProgressListener(Player.PlayerProgressListener listener) {
        player.addPlayerProgressListener(listener);
    }

    public void removePlayerProgressListener(Player.PlayerProgressListener listener) {
        player.removePlayerProgressListener(listener);
    }

    public void seekTo(int milliseconds) {
        player.seekTo(milliseconds);
    }

    //Player callbacks
    @Override
    public void onEvent(int position, Audio audio, Player.PlayerEvent event) {
        switch (event) {
            case START:
                startForeground(PlayerNotification.ID, PlayerNotification.create(this, position, audio, event));
                break;
            case PAUSE:
                if (removeNotification) {
                    stopForeground(true);
                } else {
                    PlayerNotification.update(this, position, audio, Player.PlayerEvent.PAUSE);
                }
                break;
            case RESUME:
                startForeground(PlayerNotification.ID, PlayerNotification.create(this, position, audio, event));
                break;
            case STOP:
                stopForeground(true);
                break;
        }
    }


    @Override
    public void onAudioFocusChange(int focusChange) {
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_LOSS:
                wasPlaying = isPlaying();
                pause(false);
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                wasPlaying = isPlaying();
                pause(false);
                break;
            case AudioManager.AUDIOFOCUS_GAIN:
                if (wasPlaying) {
                    resume();
                }
                break;
        }
    }

}