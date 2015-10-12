package com.irateam.vkplayer.viewholders;

import android.content.Context;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.view.View;
import android.widget.TextView;

import com.irateam.vkplayer.R;
import com.irateam.vkplayer.services.PlayerService;
import com.vk.sdk.api.model.VKApiAudio;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class ActivityPlayerPanel extends PlayerPanel {

    public TextView currentTime;
    public TextView timeToFinish;
    public TextView numberAudio;
    public TextView sizeAudio;

    private Resources resources;

    public ActivityPlayerPanel(Context context, View view) {
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
        playPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (playerService.isPlaying()) {
                    playerService.pause();
                    playPause.setImageDrawable(resources.getDrawable(R.drawable.ic_player_play_grey_24dp));
                } else {
                    playerService.resume();
                    playPause.setImageDrawable(resources.getDrawable(R.drawable.ic_player_pause_grey_24dp));
                }
            }
        });

    }

    public void setAudio(int position, VKApiAudio audio) {
        super.setAudio(position, audio);
        if (audio != null) {
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
}
