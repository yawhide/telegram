package com.telegram.telegram;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;

public class ClusterTelegram implements ClusterItem {
    private LatLng mPosition;
    private Telegram t;

    public ClusterTelegram(Telegram telegram) {
        this.t = telegram;
        this.mPosition = new LatLng(telegram.getLat(), telegram.getLng());
    }

    public Telegram getTelegram() {
        return this.t;
    }

    public boolean getSeen() {
        return this.t.getSeen();
    }

    public Boolean isLocked() { return this.t.isLocked(); }

    public String getTid() { return this.t.getTid(); }

    @Override
    public LatLng getPosition() {
        return mPosition;
    }

}
