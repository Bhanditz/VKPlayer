package com.irateam.vkplayer.controllers;

import android.content.Context;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.view.View;
import android.widget.TextView;

import com.irateam.vkplayer.R;
import com.irateam.vkplayer.models.Audio;
import com.irateam.vkplayer.models.AudioInfo;
import com.irateam.vkplayer.player.Player;
import com.irateam.vkplayer.services.PlayerService;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class ActivityPlayerController extends PlayerController implements Player.PlayerEventListener {

    public TextView currentTime;
    public TextView timeToFinish;
    public TextView numberAudio;
    public TextView sizeAudio;

    private Resources resources;

    public ActivityPlayerController(Context context, View view) {
        super(context, view);

        resources = context.getResources();

        currentTime = (TextView) view.findViewById(R.id.player_panel_current_time);
        timeToFinish = (TextView) view.findViewById(R.id.player_panel_time_remaining);
        numberAudio = (TextView) view.findViewById(R.id.player_panel_count_audio);
        sizeAudio = (TextView) view.findViewById(R.id.player_panel_audio_size);
    }

    @SuppressWarnings("deprecation")
    public void setPlayerService(final PlayerService playerService) {
        super.setPlayerService(playerService);
        playPause.setOnClickListener((v) -> {
            if (playerService.isPlaying()) {
                playerService.pause();
            } else {
                playerService.resume();
            }
        });
    }

    @Override
    public void onEvent(int position, Audio audio, Player.PlayerEvent event) {
        super.onEvent(position, audio, event);
        switch (event) {
            case START:
                setAudio(position, audio);
                break;
            case PAUSE:
                setPlayPause(false);
                break;
            case RESUME:
                setPlayPause(true);
                break;
        }
    }

    public void setPlayPause(boolean play) {
        super.setPlayPause(play);
        if (play)
            playPause.setImageDrawable(resources.getDrawable(R.drawable.ic_player_pause_grey_24dp));
        else
            playPause.setImageDrawable(resources.getDrawable(R.drawable.ic_player_play_grey_24dp));
    }

    public void setAudio(int position, Audio audio) {
        super.setAudio(position, audio);
        songName.setText(audio.getTitle());
        numberAudio.setText("#" + (position + 1) + "/" + playerService.getPlaylist().size());
        AudioInfo.load(context, audio, info -> {
            sizeAudio.setText(String.format("%.1f", info.size / (double) 1024 / (double) 1024) + "Mb");
            sizeAudio.setText(sizeAudio.getText() + " " + info.bitrate);
        });
    }

    @Override
    public void onProgressChanged(int milliseconds) {
        super.onProgressChanged(milliseconds);
        currentTime.setText(String.format("%02d:%02d",
                TimeUnit.MILLISECONDS.toMinutes(progress.getProgress()),
                TimeUnit.MILLISECONDS.toSeconds(progress.getProgress()) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(progress.getProgress()))
        ));
        int timeRemaining = progress.getMax() - progress.getProgress();
        timeToFinish.setText("-" + String.format("%02d:%02d",
                TimeUnit.MILLISECONDS.toMinutes(timeRemaining),
                TimeUnit.MILLISECONDS.toSeconds(timeRemaining) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(timeRemaining))
        ));
    }
}
