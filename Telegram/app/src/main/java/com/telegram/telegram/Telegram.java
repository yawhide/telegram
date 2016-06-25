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
    private Integer expiry;

    public Telegram(String uid, String tid, String msg, String img, Double lat, Double lng, Boolean locked) {
        this.uid = uid;
        this.tid = tid;
        this.msg = msg;
        this.img = img;
        this.lat = lat;
        this.lng = lng;
        this.locked = locked;
        this.expiry = 172800;
    }

    public Telegram (String uid, String msg, String img, Double lat, Double lng, Boolean locked) {
        this(uid, "", msg, img, lat, lng, locked);
    }

    public String getUid() {
        return uid;
    }

    public String getTid() {
        return tid;
    }

    public String getMsg() {
        return msg;
    }

    public double getLat() {
        return lat;
    }

    public double getLng() {
        return lng;
    }

    public boolean isLocked() { return locked; }

    public RequestBody createSeenFormBody() {
        return new FormBody.Builder()
                .add ("uid", this.uid)
                .add ("tid", this.tid)
                .add ("exp", this.expiry.toString())
                .build();
    }

    public RequestBody createDropFormBody() {
        return new FormBody.Builder()
                .add("uid", this.uid)
                .add("tid", this.tid)
                .add("msg", this.msg)
                .add("img", this.img)
                .add("lat", this.lat.toString())
                .add("lng", this.lng.toString())
                .build();
    }
}
