package com.irateam.vkplayer.viewholders;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.irateam.vkplayer.R;
import com.irateam.vkplayer.activities.AudioActivity;
import com.irateam.vkplayer.activities.ListActivity;
import com.irateam.vkplayer.player.Player;
import com.irateam.vkplayer.services.PlayerService;
import com.vk.sdk.api.model.VKApiAudio;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.concurrent.ExecutionException;

public class PlayerPanel implements Player.PlayerEventListener, Player.PlayerProgressListener {

    public View rootView;
    public TextView songName;
    public TextView author;
    public TextView currentTime;
    public TextView timeToFinish;
    public TextView numberAudio;
    public TextView sizeAudio;

    public ImageView repeat;
    public ImageView previous;
    public ImageView playPause;
    public ImageView next;
    public ImageView random;

    public SeekBar progress;
    private boolean dragMode;
    public boolean audioActivity = false;

    private Context context;
    private Resources resources;

    private LinearLayout headerLayout;

    public PlayerPanel(Context context, View view) {
        this.context = context;
        resources = context.getResources();

        rootView = view;
        songName = (TextView) view.findViewById(R.id.player_panel_song_name);
        author = (TextView) view.findViewById(R.id.player_panel_author);
        currentTime = (TextView) view.findViewById(R.id.player_panel_current_time);
        timeToFinish = (TextView) view.findViewById(R.id.player_panel_time_remaining);
        numberAudio = (TextView) view.findViewById(R.id.player_panel_count_audio);
        sizeAudio = (TextView) view.findViewById(R.id.player_panel_audio_size);

        repeat = (ImageView) view.findViewById(R.id.player_panel_repeat);
        previous = (ImageView) view.findViewById(R.id.player_panel_previous);
        playPause = (ImageView) view.findViewById(R.id.player_panel_play_pause);
        next = (ImageView) view.findViewById(R.id.player_panel_next);
        random = (ImageView) view.findViewById(R.id.player_panel_random);

        progress = (SeekBar) view.findViewById(R.id.progress);

        headerLayout = (LinearLayout) view.findViewById(R.id.player_panel_header_layout);
    }

    @SuppressWarnings("deprecation")
    public void setPlayerService(final PlayerService playerService) {
        final Resources resources = context.getResources();
        configurePanel(playerService);

        playPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (playerService.isPlaying()) {
                    playerService.pause();
                    if (!audioActivity)
                        playPause.setImageDrawable(resources.getDrawable(R.drawable.ic_player_play_grey_18dp));
                    else
                        playPause.setImageDrawable(resources.getDrawable(R.drawable.ic_player_play_grey_24dp));
                } else {
                    playerService.resume();
                    if (!audioActivity)
                        playPause.setImageDrawable(resources.getDrawable(R.drawable.ic_player_pause_grey_18dp));
                    else
                        playPause.setImageDrawable(resources.getDrawable(R.drawable.ic_player_pause_grey_24dp));
                }
            }
        });

        previous.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playerService.previous();
            }
        });

        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playerService.next();
            }
        });

        repeat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setRepeatState(playerService.switchRepeatState());
            }
        });

        random.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setRandomState(playerService.switchRandomState());
            }
        });

        progress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                dragMode = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                dragMode = false;
                playerService.seekTo(progress.getProgress());
            }
        });

        headerLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!audioActivity) {
                    Intent intent = new Intent(context, AudioActivity.class);
                    context.startActivity(intent);
                } else {
                    Intent intent = new Intent(context, ListActivity.class);
                    context.startActivity(intent);
                }
            }
        });
    }

    private void configurePanel(PlayerService playerService) {
        playerService.addPlayerProgressListener(this);
        VKApiAudio audio = playerService.getPlayingAudio();
        if (audio != null) {
            rootView.setVisibility(View.VISIBLE);
            setAudio(playerService.getPlayingAudioIndex(), audio);
            setRepeatState(playerService.getRepeatState());
            setRandomState(playerService.getRandomState());
        }
    }

    @Override
    public void onEvent(int position, VKApiAudio audio, Player.PlayerEvent event) {
        switch (event) {
            case PLAY:
                setAudio(position, audio);
                break;
        }
    }

    public void setAudio(int position, VKApiAudio audio) {
        if (audio != null) {
            if (rootView.getVisibility() != View.VISIBLE) {
                rootView.setVisibility(View.VISIBLE);
            }
            songName.setText(position + 1 + ". " + audio.title);
            author.setText(audio.artist);
            progress.setMax(audio.duration * 1000);
            progress.setProgress(0);
            progress.setSecondaryProgress(0);
            if (audioActivity) {
                numberAudio.setText("#" + (position + 1) + "/");
                try {
                    SizeTask sizeTask = new SizeTask(new URL(audio.url));
                    sizeTask.execute();
                    String size = String.format("%.1f", Double.valueOf(sizeTask.get()));
                    sizeAudio.setText(size + "Mb");
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    class SizeTask extends AsyncTask<Void, Void, Double> {

        private URL url;

        public SizeTask(URL url) {
            this.url = url;
        }

        @Override
        protected Double doInBackground(Void... params) {

            URLConnection urlConnection = null;
            double size = 0;
            try {
                urlConnection = url.openConnection();
                urlConnection.connect();
                size = urlConnection.getContentLength() / (double) 1024 / (double) 1024;
            } catch (IOException e) {
                e.printStackTrace();
            }
            return size;
        }
    }

    @SuppressWarnings("deprecation")
    public void setRepeatState(Player.RepeatState repeatState) {
        switch (repeatState) {
            case NO_REPEAT:
                repeat.setImageDrawable(resources.getDrawable(R.drawable.ic_player_repeat_light_grey_18dp));
                break;
            case ALL_REPEAT:
                repeat.setImageDrawable(resources.getDrawable(R.drawable.ic_player_repeat_all_light_grey_18dp));
                break;
            case ONE_REPEAT:
                repeat.setImageDrawable(resources.getDrawable(R.drawable.ic_player_repeat_one_light_grey_18dp));
                break;
        }
    }

    @SuppressWarnings("deprecation")
    private void setRandomState(boolean randomState) {
        if (randomState) {
            random.setImageDrawable(resources.getDrawable(R.drawable.ic_player_random_on_light_grey_18dp));
        } else {
            random.setImageDrawable(resources.getDrawable(R.drawable.ic_player_random_light_grey_18dp));
        }
    }

    @Override
    public void onProgressChanged(int milliseconds) {
        if (!dragMode) {
            progress.setProgress(milliseconds);
        }
    }

    @Override
    public void onBufferingUpdate(int milliseconds) {
        progress.setSecondaryProgress(milliseconds);
    }
}
