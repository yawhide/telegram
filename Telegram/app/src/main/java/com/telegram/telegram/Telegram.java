package com.telegram.telegram;

/**
 * Created by Avie on 2016-06-22.
 */
public class Telegram {
    Double uid;
    String msg;
    String img;
    Double lat;
    Double lng;

    // This is the constructor of the class Employee
    public Telegram(Double uid, String msg, String img, Double lat, Double lng) {
        this.uid = uid;
        this.msg = msg;
        this.img = img;
        this.lat = lat;
        this.lng = lng;
    }
}
