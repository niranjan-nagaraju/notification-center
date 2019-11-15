package com.vinithepooh.notifier;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.service.notification.StatusBarNotification;

/**
 * Created by vinithepooh on 14/02/19.
 */

public class NtfcnsDataModel {
    StatusBarNotification sbn;
    String placeholder;

    Drawable appIcon;
    String app_name;
    String subtext;
    long postTime;

    String ntfcn_title;
    String ntfcn_contents;
    String ntfcn_bigtext;
    Bitmap largeIcon;
    Bitmap ntfcn_bigpicture;



    public NtfcnsDataModel(StatusBarNotification sbn,
                           String placeholder,
                           Drawable appIcon, String app_name, String subtext, long postTime,
                           String ntfcn_title, String ntfcn_contents, String ntfcn_bigtext,
                           Bitmap largeIcon, Bitmap ntfcn_bigpicture) {
        this.sbn = sbn;
        this.placeholder = placeholder;
        this.appIcon = appIcon;
        this.app_name = app_name;
        this.subtext = subtext;
        this.postTime = postTime;
        this.ntfcn_title = ntfcn_title;
        this.ntfcn_contents = ntfcn_contents;
        this.ntfcn_bigtext = ntfcn_bigtext;
        this.largeIcon = largeIcon;
        this.ntfcn_bigpicture = ntfcn_bigpicture;
    }


    public StatusBarNotification getSbn() {
        return sbn;
    }


    public String getApp_name() {
        return app_name;
    }


    public String getPlaceholder() {
        return placeholder;
    }

    public Drawable getAppIcon() {
        return appIcon;
    }

    public String getSubtext() {
        return subtext;
    }

    public long getPostTime() {
        return postTime;
    }

    public String getNtfcn_title() {
        return ntfcn_title;
    }

    public String getNtfcn_contents() {
        return ntfcn_contents;
    }

    public String getNtfcn_bigtext() {
        return ntfcn_bigtext;
    }

    public Bitmap getLargeIcon() {
        return largeIcon;
    }

    public Bitmap getNtfcn_bigpicture() {
        return ntfcn_bigpicture;
    }
}
