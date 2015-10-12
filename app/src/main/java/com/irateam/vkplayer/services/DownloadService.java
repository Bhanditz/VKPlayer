package com.irateam.vkplayer.services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.irateam.vkplayer.database.AudioDatabaseHelper;
import com.irateam.vkplayer.notifications.DownloadNotification;
import com.vk.sdk.api.model.VKApiAudio;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class DownloadService extends Service {

    public static final String AUDIO_SET = "audio_set";

    private Thread currentThread;
    private Queue<VKApiAudio> queue = new ConcurrentLinkedQueue<>();

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        ArrayList<VKApiAudio> list = (ArrayList<VKApiAudio>) intent.getSerializableExtra(AUDIO_SET);
        for (VKApiAudio audio : list) {
            queue.add(audio);
        }
        if (currentThread == null) {
            startDownload(queue.poll());
        }
        return START_NOT_STICKY;
    }

    private void startDownload(final VKApiAudio audio) {
        startForeground(DownloadNotification.ID, DownloadNotification.create(this, audio, 0));
        currentThread = new Thread(new Runnable() {
            @Override
            public void run() {
                BufferedInputStream inputStream = null;
                FileOutputStream fileOutputStream = null;
                URLConnection connection = null;
                File file = new File(getExternalCacheDir(), String.valueOf(audio.id));
                try {
                    connection = new URL(audio.url).openConnection();
                    int size = connection.getContentLength();
                    inputStream = new BufferedInputStream(connection.getInputStream());
                    fileOutputStream = new FileOutputStream(file);
                    final byte data[] = new byte[1024];
                    int count, total = 0, progress = 0, now;
                    while ((count = inputStream.read(data, 0, 1024)) != -1) {
                        total += count;
                        fileOutputStream.write(data, 0, count);
                        now = (int) ((double) total / size * 100);
                        if (now - progress > 3) {
                            progress = now;
                            DownloadNotification.update(DownloadService.this, audio, progress);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                audio.url = file.getAbsolutePath();
                System.out.println(String.valueOf(new AudioDatabaseHelper(DownloadService.this).insert(audio)));
                onFinish();
                stopForeground(true);
            }
        });
        currentThread.start();
    }

    private void onFinish() {
        VKApiAudio audio = queue.poll();
        if (audio != null) {
            startDownload(audio);
        }
    }
}
