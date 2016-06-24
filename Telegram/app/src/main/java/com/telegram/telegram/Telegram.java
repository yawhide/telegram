package com.telegram.telegram;

import java.io.Serializable;

import okhttp3.FormBody;
import okhttp3.RequestBody;

public class Telegram implements Serializable{

    private String uid;
    private String tid;
    private String msg;
    private String img;
    private Double lat;
    private Double lng;
    private Boolean locked;

    public Telegram(String uid, String tid, String msg, String img, Double lat, Double lng, Boolean locked) {
        this.uid = uid;
        this.tid = tid;
        this.msg = msg;
        this.img = img;
        this.lat = lat;
        this.lng = lng;
        this.locked = locked;
    }

    public Telegram (String uid, String msg, String img, Double lat, Double lng, Boolean locked) {
        this(uid, "", msg, img, lat, lng, locked);
    }

    public double getLat() {
        return lat;
    }

    public double getLng() {
        return lng;
    }

    public boolean isLocked() { return locked; }

    public RequestBody createFormBody() {
        return new FormBody.Builder()
                .add("uid", uid)
                .add("tid", tid)
                .add("msg", msg)
                .add("img", img)
                .add("lat", lat.toString())
                .add("lng", lng.toString())
                .build();
    }
}
