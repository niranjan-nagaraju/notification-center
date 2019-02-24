package com.vinithepooh.notifier;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.service.notification.StatusBarNotification;
import android.support.v4.app.NotificationCompat;
import android.text.format.DateUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by vinithepooh on 20/02/19.
 */


public class NtfcnsData {
    private HashMap<String, StatusBarNotification> active_ntfcns_table = new HashMap<>();
    private HashMap<String, StatusBarNotification> inactive_ntfcns_table = new HashMap<>();
    private Context context = null;
    private final String TAG = "bulletin_board_data";


    public NtfcnsData(Context context) {
        this.context = context;
    }

    public static String getCondensedString(StatusBarNotification sbn) {
        StringBuilder cStr = new StringBuilder();

        cStr.append(sbn.getPackageName() + "|");
        cStr.append(sbn.getNotification().tickerText + "|");
        cStr.append(sbn.getNotification().extras.get(NotificationCompat.EXTRA_TITLE) +  "|");
        cStr.append(sbn.getNotification().extras.get(NotificationCompat.EXTRA_SUB_TEXT) + "|");
        cStr.append(sbn.getNotification().extras.get(NotificationCompat.EXTRA_SUMMARY_TEXT) + "|");

        cStr.append(sbn.getNotification().extras.get(NotificationCompat.EXTRA_TEXT) + "|");
        cStr.append(sbn.getNotification().extras.get(NotificationCompat.EXTRA_BIG_TEXT) + "|");

        return cStr.toString();
    }


    /** Update active notifications into cards adapter */
    public ArrayList<NtfcnsDataModel> filter_active() {
        ArrayList<NtfcnsDataModel> data = new ArrayList<>();

        for(Map.Entry<String, StatusBarNotification> entry : active_ntfcns_table.entrySet()) {
            String key = entry.getKey();
            StatusBarNotification sbn = entry.getValue();

            PackageManager pm = context.getPackageManager();
            String app_name;

            try {
                app_name = (String) pm.getApplicationLabel(
                        pm.getApplicationInfo(sbn.getPackageName(), PackageManager.GET_META_DATA));
            } catch (Exception e) {
                app_name = sbn.getPackageName();
                Log.e(TAG, "Error occurred getting app name - using pkg name" +
                        e.getMessage());
            }

            Drawable app_icon = null;
            /** sbn.getNotification().getSmallIcon().loadDrawable(context);
             * small icon is not always set, using app icon instead.
             */

            try {
                app_icon = context.getPackageManager().getApplicationIcon(sbn.getPackageName());
            } catch(Exception e) {
                Log.e(TAG, "Error occurred getting app icon - using null" +
                        e.getMessage());
                app_icon = null;
            }

            data.add(new NtfcnsDataModel(
        "Active Notifications",
                    app_icon,
                    app_name,
                    "" + sbn.getNotification().extras.get(NotificationCompat.EXTRA_SUB_TEXT),
                    DateUtils.getRelativeTimeSpanString(sbn.getPostTime()).toString(),
                    "" + sbn.getNotification().extras.get(NotificationCompat.EXTRA_TITLE),
                    "" + sbn.getNotification().extras.get(NotificationCompat.EXTRA_TEXT),
                    "" + sbn.getNotification().extras.get(NotificationCompat.EXTRA_BIG_TEXT),
                    /** sbn.getNotification().getLargeIcon().loadDrawable(context) */ null,
                    null
            ));
        }

        return data;
    }

    public boolean addActive(String key, StatusBarNotification sbn) {
        if (this.active_ntfcns_table.containsKey(key))
            return false;

        this.active_ntfcns_table.put(key, sbn);
        return true;
    }

    public boolean addInactive(String key, StatusBarNotification sbn) {
        if (this.inactive_ntfcns_table.containsKey(key))
            return false;

        this.inactive_ntfcns_table.put(key, sbn);
        return true;
    }

    public StatusBarNotification removeActive(String key) {
        if (this.active_ntfcns_table.containsKey(key))
            return null;

        StatusBarNotification sbn = this.active_ntfcns_table.remove(key);
        return sbn;
    }

    public StatusBarNotification removeInactive(String key) {
        if (this.inactive_ntfcns_table.containsKey(key))
            return null;

        StatusBarNotification sbn = this.inactive_ntfcns_table.remove(key);
        return sbn;
    }
}
