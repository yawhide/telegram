package com.telegram.telegram;

import org.json.JSONException;
import org.json.JSONObject;

public class Telegram {

    private Double uid;
    private String msg;
    private String img;
    private Double lat;
    private Double lng;
    private Boolean unlocked = true;

    public Telegram(Double uid, String msg, String img, Double lat, Double lng) {
        this.uid = uid;
        this.msg = msg;
        this.img = img;
        this.lat = lat;
        this.lng = lng;
        this.unlocked = false;

    }

    public double getLat() {
        return lat;
    }

    public double getLng() {
        return lng;
    }
}
