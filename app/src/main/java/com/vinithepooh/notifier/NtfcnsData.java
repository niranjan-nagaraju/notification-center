package com.vinithepooh.notifier;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.service.notification.StatusBarNotification;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Niranjan Nagaraju on 20/02/19.
 */


public class NtfcnsData {
    public class NtfcnDataItem {
        StatusBarNotification sbn = null;
        boolean active = false;
        String app_name = null;

        String subtext = null;
        String title = null;
        String text = null;
        String bigtext = null;

        Drawable app_icon = null;
        Bitmap big_icon = null;
        Bitmap big_picture = null;


        long cleared_time = 0;


        public NtfcnDataItem(StatusBarNotification sbn,
                             boolean active,
                             String app_name,
                             String subtext,
                             String title,
                             String text,
                             String bigtext,
                             Drawable app_icon,
                             Bitmap big_icon,
                             Bitmap big_picture) {
            this.sbn = sbn;
            this.active = active;
            this.app_name = app_name;
            this.subtext = subtext;
            this.title = title;
            this.text = text;
            this.bigtext = bigtext;

            this.app_icon = app_icon;
            this.big_icon = big_icon;
            this.big_picture = big_picture;
            this.cleared_time = 0;
        }


        public StatusBarNotification get_sbn() {
            return sbn;
        }

        public boolean isActive() {
            return active;
        }

        public void setActive(boolean active) {
            this.active = active;
        }

        public void setSBN(StatusBarNotification sbn) {
            this.sbn = sbn;
        }


        public String getApp_name() {
            return app_name;
        }

        public String getSubtext() {
            return subtext;
        }

        public String getTitle() {
            return title;
        }

        public String getText() {
            return text;
        }

        public String getBigText() {
            return bigtext;
        }


        public Drawable getApp_icon() {
            return app_icon;
        }

        public Bitmap getBig_icon() {
            return big_icon;
        }

        public Bitmap getBig_picture() {
            return big_picture;
        }


        public long getCleared_time() {
            return cleared_time;
        }

        public void setCleared_time(long cleared_time) {
            this.cleared_time = cleared_time;
        }
    }


    private ConcurrentHashMap<String, NtfcnDataItem> ntfcns_table = new ConcurrentHashMap<>();
    private Context context = null;
    private final String TAG = "bulletin_board_data";
    private long expire_after =
            NotifierConfiguration.cfg_cache_expiry_interval*60*60*1000;

    public void set_expiry_timeout(long expire_after) {
        this.expire_after = expire_after;
    }

    public long get_expiry_timeout() {
        return expire_after;
    }

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
     * check if notifications table contains an entry with condensed string as key
     */
    public boolean contains(String key) {
        return this.ntfcns_table.containsKey(key);
    }


    /**
     * Scour through the active table by value
     * to see if we already have the same status bar notification stored
     * under a different text key (presumably because the notification changed/updated)
     */
    public boolean findActive (StatusBarNotification sbn) {
        Iterator<Map.Entry<String, NtfcnDataItem>> iter =
                ntfcns_table.entrySet().iterator();

        while (iter.hasNext()) {
            Map.Entry<String, NtfcnDataItem> entry = iter.next();

            /** skip Inactive entries */
            if (!entry.getValue().isActive())
                continue;

            String key = entry.getKey();
            StatusBarNotification sbn_entry = entry.getValue().get_sbn();

            /** Found Status bar notification reference and is active */
            if (sbn.equals(sbn_entry)) {
                Log.i(TAG, "SBN already exists with key text: " + key);
                return true;
            }
        }
        return false;
    }


    /**
     * Mark everything inactive in the notifications table
     */
    public void markAllInactive () {
        Iterator<Map.Entry<String, NtfcnDataItem>> iter =
                ntfcns_table.entrySet().iterator();

        while (iter.hasNext()) {
            Map.Entry<String, NtfcnDataItem> entry = iter.next();

            entry.getValue().setActive(false);
        }
    }

    /**
     * If there are entries previously marked inactive, but doesn't have its cleared time set,
     * Set its cleared time to now
     */
    public void update_cleared_time_if_zero () {
        Iterator<Map.Entry<String, NtfcnDataItem>> iter =
                ntfcns_table.entrySet().iterator();

        long now = System.currentTimeMillis();
        while (iter.hasNext()) {
            Map.Entry<String, NtfcnDataItem> entry = iter.next();

            /**
             * NOTE: This is needed for apps like Gmail
             * which when 'deleted' update notification with an 'Undo'
             * and null content, so notifier has no idea what the
             * removed notification was for.
             * Next sync, it'll be silently marked inactive, but its cleared time
             * would remain 0. This readjusts such notifications.
             */
            if (!entry.getValue().isActive() && entry.getValue().cleared_time == 0)
                entry.getValue().setCleared_time(now);
        }
    }


    /**
     * Filter notifications from a matching package
     */
    public ArrayList<NtfcnsDataModel> filter_app(String pkg,
                                                 String searchKey,
                                                 boolean active,
                                                 Comparator<NtfcnsDataModel> comparator) {
        ArrayList<NtfcnsDataModel> data = new ArrayList<>();
        String[] searchKeys = null;

        searchKey = searchKey.trim();

        if (searchKey.equals("")) {
            searchKeys = new String[] {""};
        } else {
            StringBuilder sb = new StringBuilder(searchKey);

            /** If search string had " " in them, remove them for a full word search */
            if (sb.charAt(0) == '"' && sb.charAt(sb.length() - 1) == '"') {
                sb.deleteCharAt(0);
                sb.deleteCharAt(sb.length() - 1);
                searchKeys = new String[] {sb.toString()};
            } else {
                searchKeys = searchKey.split("\\s+");
            }
        }

        for (String s: searchKeys) {
            Log.i(TAG, "Search words: " + s);
        }

        Boolean expand_by_default = NotifierConfiguration.cfg_default_expanded_view;

        for(Map.Entry<String, NtfcnDataItem> entry : ntfcns_table.entrySet()) {
            String key = entry.getKey();

            /** Notification's active status doesn't match filter's active status */
            if (entry.getValue().isActive() != active)
                continue;

            /** Notification's package names dont match */
            if (!entry.getValue().sbn.getPackageName().equals(pkg))
                continue;


            /** skip search if the search string is empty, and list everything */
            if (searchKeys[0].length() != 0) {
                /** match all words in search string individually */
                boolean match = true;
                for (String s : searchKeys) {
                    match = match && key.toLowerCase().contains(s.toLowerCase());
                    if (!match)
                        break;
                }

                if (!match)
                    continue;
            }

            StatusBarNotification sbn = entry.getValue().get_sbn();
            String app_name = entry.getValue().getApp_name();
            String subtext = entry.getValue().getSubtext();
            String title = entry.getValue().getTitle();
            String text = entry.getValue().getText();
            String bigtext = entry.getValue().getBigText();

            Drawable app_icon = entry.getValue().getApp_icon();
            Bitmap big_icon = entry.getValue().getBig_icon();
            Bitmap big_picture = entry.getValue().getBig_picture();

            data.add(new NtfcnsDataModel(
                    sbn,
                    (active ? "Active Notifications" : "Cached Notifications"),
                    app_icon,
                    app_name,
                    subtext,
                    sbn.getPostTime(),
                    active,
                    title,
                    text,
                    bigtext,
                    big_icon,
                    big_picture,
                    expand_by_default  /** notifications are collapsed by default */
            ));
        }

        Collections.sort(data, comparator);

        return data;
    }


    /**
     * Filter all active notifications into a sorted list
     * by default, sort by reverse chronological order based on post time.
     */
    public ArrayList<NtfcnsDataModel> filter_active_app(String pkg, String searchKey) {
        return this.filter_app(pkg, searchKey, true, postTimeComparator);
    }


    /**
     * Filter all inactive notifications into a sorted list
     * by default, sort by reverse chronological order based on post time.
     */
    public ArrayList<NtfcnsDataModel> filter_inactive_app(String pkg, String searchKey) {
        return this.filter_app(pkg, searchKey, false, postTimeComparator);
    }





    /**
     * Filter notifications matching a search string and active status into a list
     */
    public ArrayList<NtfcnsDataModel> filter(String searchKey,
                                             boolean active,
                                             Comparator<NtfcnsDataModel> comparator) {
        ArrayList<NtfcnsDataModel> data = new ArrayList<>();
        String[] searchKeys = null;

        searchKey = searchKey.trim();

        if (searchKey.equals("")) {
            searchKeys = new String[] {""};
        } else {
            StringBuilder sb = new StringBuilder(searchKey);

            /** If search string had " " in them, remove them for a full word search */
            if (sb.charAt(0) == '"' && sb.charAt(sb.length() - 1) == '"') {
                sb.deleteCharAt(0);
                sb.deleteCharAt(sb.length() - 1);
                searchKeys = new String[] {sb.toString()};
            } else {
                searchKeys = searchKey.split("\\s+");
            }
        }

        for (String s: searchKeys) {
            Log.i(TAG, "Search words: " + s);
        }

        Boolean expand_by_default = NotifierConfiguration.cfg_default_expanded_view;
        for(Map.Entry<String, NtfcnDataItem> entry : ntfcns_table.entrySet()) {
            String key = entry.getKey();

            /** Notification's active status doesn't match filter's active status */
            if (entry.getValue().isActive() != active)
                continue;

            /** skip search if the search string is empty, and list everything */
            if (searchKeys[0].length() != 0) {
                /** match all words in search string individually */
                boolean match = true;
                for (String s : searchKeys) {
                    match = match && key.toLowerCase().contains(s.toLowerCase());
                    if (!match)
                        break;
                }

                if (!match)
                    continue;
            }

            StatusBarNotification sbn = entry.getValue().get_sbn();
            String app_name = entry.getValue().getApp_name();
            String subtext = entry.getValue().getSubtext();
            String title = entry.getValue().getTitle();
            String text = entry.getValue().getText();
            String bigtext = entry.getValue().getBigText();

            Drawable app_icon = entry.getValue().getApp_icon();
            Bitmap big_icon = entry.getValue().getBig_icon();
            Bitmap big_picture = entry.getValue().getBig_picture();

            data.add(new NtfcnsDataModel(
                    sbn,
                    (active ? "Active Notifications" : "Cached Notifications"),
                    app_icon,
                    app_name,
                    subtext,
                    sbn.getPostTime(),
                    active,
                    title,
                    text,
                    bigtext,
                    big_icon,
                    big_picture,
                    expand_by_default  /** notifications are collapsed by default */
            ));
        }

        Collections.sort(data, comparator);

        return data;
    }


    /**
     * Filter all active notifications into a sorted list
     * by default, sort by reverse chronological order based on post time.
     */
    public ArrayList<NtfcnsDataModel> filter_active(String searchKey) {
        return this.filter(searchKey, true, postTimeComparator);
    }


    /**
     * Filter all inactive notifications into a sorted list
     * by default, sort by reverse chronological order based on post time.
     */
    public ArrayList<NtfcnsDataModel> filter_inactive(String searchKey) {
        return this.filter(searchKey, false, postTimeComparator);
    }


    /**
     * Build a list of packages from notifications sorted by their app name,
     * and their active count
     */
    public TreeMap<String, Integer> build_apps_list() {
        final PackageManager pm = context.getPackageManager();

        Comparator<String> apps_name_comparator = new Comparator<String>() {
            public int compare(String a1, String a2) {
                try {
                    String s1 = pm.getApplicationLabel(
                            pm.getApplicationInfo(a1, PackageManager.GET_META_DATA)).toString();
                    String s2 = pm.getApplicationLabel(
                            pm.getApplicationInfo(a2, PackageManager.GET_META_DATA)).toString();

                    return s1.compareToIgnoreCase(s2);
                } catch (PackageManager.NameNotFoundException e) {
                    return a1.compareToIgnoreCase(a2);
                }
            }
        };

        TreeMap<String, Integer> apps_list = new TreeMap<>(apps_name_comparator);

        for(Map.Entry<String, NtfcnDataItem> entry : ntfcns_table.entrySet()) {
            String pkg = entry.getValue().sbn.getPackageName();
            Boolean isActive = entry.getValue().isActive();

            if (apps_list.containsKey(pkg)) {
                Integer count = apps_list.get(pkg);

                /** If the notification is active, Increase active count for pkg */
                if (isActive)
                    apps_list.put(pkg, count+1);
            } else {
                apps_list.put(pkg, 0);

                /** If the notification is active, Increase active count for pkg */
                if (isActive)
                    apps_list.put(pkg, 1);
            }

        }

        return  apps_list;
    }


    /**
     * Remove a notification entry from the table if cached
     * Mark inactive if it was an active notification
     * NOTE: The card's active status is as seen in the view
     *       and not from the dataset because they may no longer
     *       be in sync.
     */
    public void remove(StatusBarNotification sbn, boolean active) {
        String key = getCondensedString(sbn);

        if (active) {
            /** if the notification was active, mark it as cached and return */
            this.addInactive(key, sbn);
        } else {
            /** if the notification was cached, remove it from the hash table */
            this.ntfcns_table.remove(this.getCondensedString(sbn));
        }
    }


    /** Remove everything from the notifications table */
    public void reset() {
        this.ntfcns_table.clear();
    }


    /**
     * Add a status bar notification to the table
     * Mark it as active
     *
     * Return true if we are adding it for the first time,
     * false otherwise
     *
     * if it already exists, just mark it as active and return
     */
    public boolean addActive(String key, StatusBarNotification sbn) {
        if (this.ntfcns_table.containsKey(key)) {
            StatusBarNotification sbn_c = sbn;
            this.ntfcns_table.get(key).setSBN(sbn_c);
            this.ntfcns_table.get(key).setActive(true);

            /**
             * If large icon wasnt available earlier,
             * check if the new clone has it,
             * sort of like the movie, Moon. :)
             */
            if (this.ntfcns_table.get(key).big_icon == null) {
                Bitmap big_icon = null;
                try {
                    Drawable d;
                    if (sbn_c.getNotification().getLargeIcon() != null) {
                        d = sbn_c.getNotification().getLargeIcon().loadDrawable(context.getApplicationContext());
                        big_icon = ((BitmapDrawable) d).getBitmap();
                    }
                } catch(Exception e) {
                    big_icon = null;
                }
                this.ntfcns_table.get(key).big_icon = big_icon;
            }

            return false;
        }

        String app_name;
        PackageManager pm = context.getPackageManager();

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
            app_icon = pm.getApplicationIcon(sbn.getPackageName());
            //app_icon = sbn.getNotification().getSmallIcon().loadDrawable(context);
        } catch(Exception e) {
            Log.e(TAG, "Error occurred getting app icon - using null" +
                    e.getMessage());
            app_icon = null;
        }

        Bitmap big_icon = null;
        try {
            Drawable d;
            if (sbn.getNotification().getLargeIcon() != null) {
                d = sbn.getNotification().getLargeIcon().loadDrawable(context.getApplicationContext());
                big_icon = ((BitmapDrawable) d).getBitmap();
            }
            else {
                Log.e(TAG, "App: " + app_name + " has no large icon");
            }
        } catch(Exception e) {
            Log.e(TAG, "Error occurred getting large icon - using null: " +
                    e.getMessage());
            big_icon = null;
        }

        Bitmap big_picture = null;
        try {
            if (sbn.getNotification().extras.get(NotificationCompat.EXTRA_PICTURE) != null)
                big_picture = (Bitmap)sbn.getNotification().extras.get(NotificationCompat.EXTRA_PICTURE);
        } catch(Exception e) {
            Log.e(TAG, "Error occurred getting big picture - using null: " +
                    e.getMessage());
            big_picture = null;
        }

        String sub_text = "";
        if (sbn.getNotification().extras.get(NotificationCompat.EXTRA_SUB_TEXT) != null)
            sub_text += sbn.getNotification().extras.get(NotificationCompat.EXTRA_SUB_TEXT);

        String title = "";
        if(sbn.getNotification().extras.get(NotificationCompat.EXTRA_TITLE) != null)
            title += sbn.getNotification().extras.get(NotificationCompat.EXTRA_TITLE);

        String text = "";
        if (sbn.getNotification().extras.get(NotificationCompat.EXTRA_TEXT) != null)
            text += sbn.getNotification().extras.get(NotificationCompat.EXTRA_TEXT);

        String big_text = "";
        if (sbn.getNotification().extras.get(NotificationCompat.EXTRA_BIG_TEXT) != null)
            big_text += sbn.getNotification().extras.get(NotificationCompat.EXTRA_BIG_TEXT);

        this.ntfcns_table.put(key, new NtfcnDataItem(sbn.clone(), true,
                app_name, sub_text, title, text, big_text,
                app_icon, big_icon, big_picture));

        return true;
    }



    /** Get active notifications currently in the hash map */
    public int getActiveCount() {
        int active_count = 0;

        for(Map.Entry<String, NtfcnDataItem> entry : ntfcns_table.entrySet()) {
            /** Notification's status is active */
            if (entry.getValue().isActive())
                active_count++;
        }

        return active_count;
    }


    /**
     * Mark a previously-stored status bar notification as inactive
     * if it's already there in the table
     *
     * If it doesn't exist in the table, we don't know what this is
     * return false.
     */
    public boolean addInactive(String key, StatusBarNotification sbn) {
        if (this.ntfcns_table.containsKey(key)) {
            StatusBarNotification sbn_c = sbn;
            this.ntfcns_table.get(key).setSBN(sbn_c);
            this.ntfcns_table.get(key).setActive(false);

            /** Set cleared time as now, so we can track expiration timeout from here */
            this.ntfcns_table.get(key).setCleared_time(System.currentTimeMillis());

            /**
             * If large icon wasnt available earlier,
             * check if the new clone has it,
             * sort of like the movie, Moon. :)
             */
            if (this.ntfcns_table.get(key).big_icon == null) {
                Bitmap big_icon = null;
                try {
                    Drawable d;
                    if (sbn_c.getNotification().getLargeIcon() != null) {
                        d = sbn_c.getNotification().getLargeIcon().loadDrawable(context.getApplicationContext());
                        big_icon = ((BitmapDrawable) d).getBitmap();
                    }
                } catch(Exception e) {
                    big_icon = null;
                }
                this.ntfcns_table.get(key).big_icon = big_icon;
            }
            return true;
        }

        return false;
    }


    public boolean prune() {
        long current = System.currentTimeMillis();
        boolean changed = false;

        Log.i(TAG, "In prune: cache timeout: " +
                NotifierConfiguration.getCache_expiry_interval());

        expire_after = NotifierConfiguration.getCache_expiry_interval()*60*60*1000;
        Log.i(TAG, "Prune expiry interval: " + expire_after);

        Iterator<Map.Entry<String, NtfcnDataItem>> iter =
                ntfcns_table.entrySet().iterator();

        while (iter.hasNext()) {
            Map.Entry<String,NtfcnDataItem> entry = iter.next();

            /** skip active entries */
            if (entry.getValue().isActive())
                continue;

            String key = entry.getKey();
            /**
             * remove entries older than 'expiration time' hours
             * from the time they have been 'cleared'
             */
            if (current > (entry.getValue().getCleared_time() + expire_after)) {
                Log.i(TAG, "Pruning entry with key: " + key);
                iter.remove();
                changed = true;
            }
        }
        return changed;
    }

}
