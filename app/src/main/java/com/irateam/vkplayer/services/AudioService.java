package com.irateam.vkplayer.services;

import com.vk.sdk.api.VKApi;
import com.vk.sdk.api.VKApiConst;
import com.vk.sdk.api.VKParameters;
import com.vk.sdk.api.VKRequest;
import com.vk.sdk.api.VKResponse;
import com.vk.sdk.api.model.VKApiAudio;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

public class AudioService {

    public static final String GENRE_ID = "genre_id";

    public List<VKApiAudio> getMyAudio() {
        final List<VKApiAudio> list = new ArrayList<>();
        VKApi.audio().get().executeWithListener(new VKRequest.VKRequestListener() {
            @Override
            public void onComplete(VKResponse response) {
                super.onComplete(response);
                try {
                    JSONArray array = response.json.getJSONObject("response").getJSONArray("items");
                    for (int i = 0; i < array.length(); i++)
                        list.add(new VKApiAudio().parse(array.getJSONObject(i)));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
        return list;
    }

    public List<VKApiAudio> getRecommendedAudio() {
        final List<VKApiAudio> list = new ArrayList<>();
        VKApi.audio().getRecommendations().executeWithListener(new VKRequest.VKRequestListener() {
            @Override
            public void onComplete(VKResponse response) {
                super.onComplete(response);
                try {
                    JSONArray array = response.json.getJSONObject("response").getJSONArray("items");
                    for (int i = 0; i < array.length(); i++)
                        list.add(new VKApiAudio().parse(array.getJSONObject(i)));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
        return list;
    }

    public List<VKApiAudio> getPopularAudio() {
        final List<VKApiAudio> list = new ArrayList<>();
        VKApi.audio().getPopular(VKParameters.from(GENRE_ID, 0)).executeWithListener(new VKRequest.VKRequestListener() {
            @Override
            public void onComplete(VKResponse response) {
                super.onComplete(response);
                try {
                    JSONArray array = response.json.getJSONArray("response");
                    for (int i = 0; i < array.length(); i++)
                        list.add(new VKApiAudio().parse(array.getJSONObject(i)));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
        return list;
    }

}
