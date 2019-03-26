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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by vinithepooh on 20/02/19.
 */


public class NtfcnsData {
    private ConcurrentHashMap<String, StatusBarNotification> active_ntfcns_table = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, StatusBarNotification> inactive_ntfcns_table = new ConcurrentHashMap<>();
    private Context context = null;
    private final String TAG = "bulletin_board_data";
    private long expire_after = 120*60*1000;


    public NtfcnsData(Context context) {
        this.context = context;
    }

    public String getCondensedString(StatusBarNotification sbn) {
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

        StringBuilder cStr = new StringBuilder();

        cStr.append(app_name + "|");
        cStr.append(sbn.getPackageName() + "|");
        cStr.append(sbn.getNotification().extras.get(NotificationCompat.EXTRA_TITLE) +  "|");
        cStr.append(sbn.getNotification().extras.get(NotificationCompat.EXTRA_SUB_TEXT) + "|");
        cStr.append(sbn.getNotification().extras.get(NotificationCompat.EXTRA_SUMMARY_TEXT) + "|");

        cStr.append(sbn.getNotification().extras.get(NotificationCompat.EXTRA_TEXT) + "|");
        cStr.append(sbn.getNotification().extras.get(NotificationCompat.EXTRA_BIG_TEXT) + "|");

        return cStr.toString();
    }

    public static Comparator<NtfcnsDataModel> postTimeComparator = new Comparator<NtfcnsDataModel>() {
        public int compare(NtfcnsDataModel s1, NtfcnsDataModel s2) {
            // reverse chronological order of notifications
            return Long.compare(s2.postTime, s1.postTime);
        }};


    /**
     * Scour through the active table by value
     * to see if we already have the same status bar notification stored
     * under a different text key (presumably because the notification changed/updated)
     */
    public boolean find (StatusBarNotification sbn) {
        Iterator<Map.Entry<String, StatusBarNotification>> iter =
                active_ntfcns_table.entrySet().iterator();

        while (iter.hasNext()) {
            Map.Entry<String, StatusBarNotification> entry = iter.next();
            String key = entry.getKey();
            StatusBarNotification sbn_entry = entry.getValue();

            if (sbn.equals(sbn_entry)) {
                Log.i(TAG, "SBN already exists with key text: " + key);
                return true;
            }
        }
        return false;
    }


    /**
     * Scour through the active table by value
     * to see if we already have the same status bar notification stored
     * If yes, remove it
     */
    public boolean remove (StatusBarNotification sbn) {
        Iterator<Map.Entry<String, StatusBarNotification>> iter =
                active_ntfcns_table.entrySet().iterator();

        while (iter.hasNext()) {
            Map.Entry<String, StatusBarNotification> entry = iter.next();
            String key = entry.getKey();
            StatusBarNotification sbn_entry = entry.getValue();

            if (sbn.equals(sbn_entry)) {
                Log.i(TAG, "SBN already exists with key text: " + key);
                Log.i(TAG, "removing it from active table");

                iter.remove();

                return true;
            }
        }

        return false;
    }


    /**
     * Update active notifications into cards adapter
     * matching a search string
     */
    public ArrayList<NtfcnsDataModel> filter_active(String searchKey) {
        ArrayList<NtfcnsDataModel> data = new ArrayList<>();
        PackageManager pm = context.getPackageManager();

        for(Map.Entry<String, StatusBarNotification> entry : active_ntfcns_table.entrySet()) {
            String key = entry.getKey();
            StatusBarNotification sbn = entry.getValue();

            /** match search string */
            if (!key.toLowerCase().contains(searchKey.toLowerCase()))
                continue;

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
                    sbn.getPostTime(),
                    "" + sbn.getNotification().extras.get(NotificationCompat.EXTRA_TITLE),
                    "" + sbn.getNotification().extras.get(NotificationCompat.EXTRA_TEXT),
                    "" + sbn.getNotification().extras.get(NotificationCompat.EXTRA_BIG_TEXT),
                    /** sbn.getNotification().getLargeIcon().loadDrawable(context) */ null,
                    null
            ));
        }

        Collections.sort(data, postTimeComparator);
        return data;
    }


    /** Update all inactive notifications into cards adapter */
    public ArrayList<NtfcnsDataModel> filter_inactive(String searchKey) {
        ArrayList<NtfcnsDataModel> data = new ArrayList<>();
        PackageManager pm = context.getPackageManager();

        for(Map.Entry<String, StatusBarNotification> entry : inactive_ntfcns_table.entrySet()) {
            String key = entry.getKey();
            StatusBarNotification sbn = entry.getValue();

            /** match search string */
            if (!key.toLowerCase().contains(searchKey.toLowerCase()))
                continue;

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
                    "Past Notifications",
                    app_icon,
                    app_name,
                    "" + sbn.getNotification().extras.get(NotificationCompat.EXTRA_SUB_TEXT),
                    sbn.getPostTime(),
                    "" + sbn.getNotification().extras.get(NotificationCompat.EXTRA_TITLE),
                    "" + sbn.getNotification().extras.get(NotificationCompat.EXTRA_TEXT),
                    "" + sbn.getNotification().extras.get(NotificationCompat.EXTRA_BIG_TEXT),
                    /** sbn.getNotification().getLargeIcon().loadDrawable(context) */ null,
                    null
            ));
        }

        Collections.sort(data, postTimeComparator);
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
        return active_ntfcns_table.remove(key);
    }

    public StatusBarNotification removeInactive(String key) {
        return inactive_ntfcns_table.remove(key);
    }

    public boolean prune() {
        long current = System.currentTimeMillis();
        boolean changed = false;

        Iterator<Map.Entry<String, StatusBarNotification>> iter =
                inactive_ntfcns_table.entrySet().iterator();

        while (iter.hasNext()) {
            Map.Entry<String,StatusBarNotification> entry = iter.next();
            String key = entry.getKey();
            StatusBarNotification sbn = entry.getValue();

            /**
             * remove entries older than 2 hours
             * TODO: make this 'interval' configurable
             */
            if (current > (sbn.getPostTime() + expire_after)) {
                Log.i(TAG, "Pruning entry with key: " + key);
                iter.remove();
                changed = true;
            }
        }
        return changed;
    }
}
