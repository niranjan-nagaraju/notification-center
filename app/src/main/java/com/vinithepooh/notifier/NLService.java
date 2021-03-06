package com.vinithepooh.notifier;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.service.notification.NotificationListenerService;

import android.content.Intent;
import android.service.notification.StatusBarNotification;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.widget.RecyclerView;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.TreeMap;


public class NLService extends NotificationListenerService {
    private final String TAG = "bulletin_board_svc";
    private final IBinder mBinder = new NLBinder();

    private static RecyclerView.Adapter adapter;

    private NtfcnsData ntfcn_items;
    private int num_active = 0;

    private long time_since_last_resync = 0;
    private long time_last_pruned = System.currentTimeMillis();

    /** Has listener service connected to notfication manager */
    private boolean listenerConnected = false;
    public boolean isListenerConnected() {
        return listenerConnected;
    }

    private NotificationCompat.Builder pnotif_builder;
    private NotificationManager notificationManager;

    public boolean isSync_in_progress() {
        return sync_in_progress;
    }

    /** flag to indicate if sync is in progress on any thread */
    private boolean sync_in_progress = false;


    public class NLBinder extends Binder {
        NLService getService() {
            return NLService.this;
        }
    }

    public void onCreate() {
        Log.i(TAG,"**********  Service Created!");
        super.onCreate();

        /**
        for (int i = 0; i < SampleNotifications.app_names.length; i++) {
            data.add(new NtfcnsDataModel(
                    SampleNotifications.placeholders[i],
                    null,
                    SampleNotifications.app_names[i],
                    SampleNotifications.subtexts[i],
                    SampleNotifications.post_times[i],
                    SampleNotifications.ntfcns_titles[i],
                    SampleNotifications.ntfcns_strings[i],
                    SampleNotifications.ntfcns_bigtexts[i],
                    null,
                    null
            ));
        } */

        /** Initialize configuration from shared preferences at startup */
        NotifierConfiguration.initialize_cfg_from_sharedPrefs(this.getApplicationContext());

        /** Create a persistent notification */
         // Create the NotificationChannel, but only on API 26+ because
         // the NotificationChannel class is new and not in the support library
         if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
             CharSequence name = getString(R.string.channel_name);
             String description = getString(R.string.channel_desc);
             int importance = NotificationManager.IMPORTANCE_LOW;

             NotificationChannel channel = new NotificationChannel("notifier", name, importance);
             channel.setDescription(description);

             // Register the channel with the system; you can't change the importance
             // or other notification behaviors after this
             notificationManager = getSystemService(NotificationManager.class);
             notificationManager.createNotificationChannel(channel);
         }

         // Create an explicit intent for an Activity in your app
         Intent intent = new Intent(this, MainActivity.class);
         intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
         PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);


        Intent action_intent = new Intent();
        action_intent.setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
        action_intent.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
        action_intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent action_pendingIntent = PendingIntent.getActivity(
                this, 0, action_intent, 0);
        NotificationCompat.Action action =
                new NotificationCompat.Action.Builder(R.drawable.ic_launcher,
                        "Settings", action_pendingIntent).build();

        pnotif_builder = new NotificationCompat.Builder(this, "notifier")
         .setSmallIcon(R.drawable.ic_launcher)
         .setContentTitle("Notifications Center")
         .setContentText("Tap to open Notifications Center")
         .setSubText("caching notifications")
         .setPriority(NotificationCompat.PRIORITY_LOW)
         .setAutoCancel(false)
         .setContentIntent(pendingIntent)
         .setShowWhen(false)
         .setOngoing(true)
         .addAction(action);

         NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

         if (NotifierConfiguration.cfg_svc_notification_enabled) {
             show_notification();
         }

        //adapter = new Ntfcns_adapter(data);
        ntfcn_items = new NtfcnsData(this.getApplicationContext());
    }


    /** Show persistent notification */
    public void show_notification() {
        Log.i(TAG, "Showing persistent notification");

        if (NotifierConfiguration.cfg_notifier_paused) {
            Log.i(TAG, "Notifier paused. Notify as paused");
            pnotif_builder.setSubText("service paused");
        } else {
            pnotif_builder.setSubText("caching notifications");
        }

        /** Send out persistent notification */
        notificationManager.notify(01, pnotif_builder.build());
    }



    /** Hide persistent notification */
    public void hide_notification() {
        Log.i(TAG, "Hiding persistent notification");

        notificationManager.cancel(01);
    }



    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "OnStartCommand");
        Runnable r = new Runnable() {
            public void run() {
                String debug_tag = TAG + "_thread";
                while (true) {
                    try {
                        /** sleep for 1 minute */
                        Thread.sleep(60000);

                        if (System.currentTimeMillis() > time_last_pruned + 60*1000) {
                            Log.i(debug_tag, "Pruning old entries");

                            if (sync_in_progress) {
                                /** If a sync is in progress, skip this prune */
                                Log.i(TAG, "Sync in progress, skipping prune");
                            } else {
                                ntfcn_items.prune();

                                time_last_pruned = System.currentTimeMillis();
                            }
                        }

                        /** Resync active notifications every 5 minutes */
                        if (System.currentTimeMillis() > time_since_last_resync + 5*60*1000) {
                            time_since_last_resync = System.currentTimeMillis();
                            if (!sync_in_progress) {
                                Log.i(TAG, "Resync in progress!");
                                sync_notifications();
                            } else {
                                Log.i(TAG, "Another sync already in progress");
                            }
                        }
                    } catch (Exception e) {
                        Log.e(debug_tag, "Error in service thread" + e.getMessage());
                        break;
                    }

                }
                stopSelf();
            }
        };

        Thread t = new Thread(r);
        t.start();
        return Service.START_STICKY;
    }

    @Override
    public IBinder onBind(Intent mIntent) {
        /** Ref:
         * https://stackoverflow.com/questions/34625022/android-service-not-yet-bound-but-onbind-is-called/34640217#34640217
         * If your service is correctly declared in the manifest, and Notification Access is enabled in Security /
         * Sound & Notification, the system will bind to the service using action
         * NotificationListenerService.SERVICE_INTERFACE. For this bind request, the service must return the
         * binder for NotificationListenerService, which is super.onBind(intent).
         *
         * Earlier: onBind() was getting called and returning mBinder, but
         * getActiveNotifications() used to fail with the message -
         *   01-30 19:17:16.387  2818  2818 W NLService: Notification listener service not yet bound.
         * and returned NULL, not to mention onNotificationsPosted() wasn't even printing anything
         */
        Log.i(TAG, "onBind");
        String action = mIntent.getAction();
        Log.d(TAG, "onBind: " + action);

        if (SERVICE_INTERFACE.equals(action)) {
            Log.d(TAG, "Bound by system");
            return super.onBind(mIntent);
        } else {
            Log.d(TAG, "Bound by application");
            return mBinder;
        }
    }

    @Override
    public boolean onUnbind(Intent mIntent) {
        boolean mOnUnbind = super.onUnbind(mIntent);
        Log.i(TAG, "onUnbind");
        try {
        } catch (Exception e) {
            Log.e(TAG, "Error during unbind", e);
        }
        return mOnUnbind;
    }

    @Override
    public void onListenerConnected() {
        Log.i(TAG, "Listener connected");
        listenerConnected = true;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        stopSelf();

        Log.i(TAG,"**********  Service destroyed");

    }

    /**
     * Remove a notification entry from the table if cached
     * Mark inactive if it was an active notification
     *
     * NOTE: The card's active status is as seen in the view
     *       and not from the dataset because they may no longer
     *       be in sync.
     */
    public void remove(StatusBarNotification sbn, boolean active) {
        this.ntfcn_items.remove(sbn, active);
    }


    /**
     * Clear all status bar notifications with matching condensed key (same content)
     * NOTE: Since we de-dup, we have only one instance stored for each repeated
     * status bar notification.
     * So when a swipe to clear is attempted, we'll have to remove all duplicates
     * from the status bar
     */
    public void clearAll(StatusBarNotification sbn) {
        String key = this.ntfcn_items.getCondensedString(sbn);

        for (StatusBarNotification asbn: getActiveNotifications()) {
            /** skip group headers
            if ( ((asbn.getNotification().flags & Notification.FLAG_GROUP_SUMMARY) != 0)) {
                Log.i(TAG, "skippiing group header key: " + asbn.getKey());
                continue;
             } */

            if (ntfcn_items.getCondensedString(asbn).equals(key))
                cancelNotification(asbn.getKey());
        }
    }


    /** Remove everything in the notifier hash table */
    public void reset() {
        Log.i(TAG, "Clearing everything from the notifications table");
        this.cancelAllNotifications();
        this.ntfcn_items.reset();
    }


    /** Add a status bar notification from active notifications to the active table */
    private boolean addActiveSBN(StatusBarNotification sbn) {
        if ( sbn.isOngoing() || !sbn.isClearable() ) {
            if (!NotifierConfiguration.cfg_persistent_notifications_enabled) {
                Log.i(TAG, "Ongoing or not clearable, ignoring");
                return false;
            }
            Log.i(TAG, "Persistent notifications allowed. Adding");
        }

        Object extra_text = sbn.getNotification().extras.get(NotificationCompat.EXTRA_TEXT);
        if (extra_text == null || extra_text.toString().isEmpty()) {
            Log.i(TAG, "Notifications text is empty. Ignoring");
            return false;
        }

        if (sbn.getPackageName().equals("android")) {
            if (!NotifierConfiguration.cfg_system_notifications_enabled) {
                Log.i(TAG, "System notifications. Ignoring");
                return false;
            }
            Log.i(TAG, "System notifications enabled. Adding");
        }

        String condensed_string = ntfcn_items.getCondensedString(sbn);
        if (! ntfcn_items.addActive(condensed_string, sbn)) {
            Log.i(TAG, "key: " + condensed_string + " already in active table");
        } else {
            Log.i(TAG, "Adding key: " + condensed_string + " to active table");
        }

        return true;
    }



    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        Log.i(TAG,"**********  onNotificationPosted");

        if (NotifierConfiguration.cfg_notifier_paused) {
            Log.i(TAG, "Notifier paused. Ignoring...");
            return;
        }

        Log.i(TAG,"ID :" + sbn.getId() + "\t" + sbn.getNotification().tickerText
                + "\t" + sbn.getPackageName());

        HashSet<String> exclusion_list = NotifierConfiguration.excluded_packages;
        if (exclusion_list.contains(sbn.getPackageName())) {
            Log.i(TAG, "Pkg: " + sbn.getPackageName() + " in exclusion list. Ignoring.");
            return;
        }

        Log.i(TAG, "SBN Key: " + sbn.getKey());
        Log.i(TAG, "Flags: " +
                ((sbn.getNotification().flags & Notification.FLAG_GROUP_SUMMARY) != 0));

        if (NotifierConfiguration.cfg_auto_remove_statusbar) {
            Log.i(TAG, "Auto-remove status bar enabled. Clearing notification");
            clearAll(sbn);
        }

        /** skip group headers */
        if ( ((sbn.getNotification().flags & Notification.FLAG_GROUP_SUMMARY) != 0))
            return;

        addActiveSBN(sbn.clone());

        if (sync_in_progress)
            return;


        /** if prune/refresh thread has been killed for some reason,
         * and prune hasnt been run for 30 minutes
         * try pruning in current thread
         */
        if(System.currentTimeMillis() > time_last_pruned+30*60*1000) {
            Log.i(TAG,"Prune thread has been inactive for over 30 minutes, pruning in main thread");
            ntfcn_items.prune();
            time_last_pruned = System.currentTimeMillis();
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        Log.i(TAG,"**********  onNotificationRemoved");

        if (NotifierConfiguration.cfg_notifier_paused) {
            Log.i(TAG, "Notifier paused. Ignoring...");
            return;
        }

        Log.i(TAG,"ID :" + sbn.getId() + "\t" + sbn.getNotification().tickerText
                + "\t" + sbn.getPackageName());


        if (!NotifierConfiguration.cfg_cache_notifications_enabled) {
            Log.i(TAG, "Not caching cleared notifications.");

            /** Remove notification completely from the cache */
            remove(sbn, false);
        }

        Log.i(TAG, "SBN Key: " + sbn.getKey());

        String condensed_string = ntfcn_items.getCondensedString(sbn);
        if ( ntfcn_items.addInactive(condensed_string, sbn) ) {
            Log.i(TAG, "key: " + condensed_string + " found in active table, removed");
        } else {
            Log.i(TAG, "Couldn't find key: " + condensed_string + " to remove");
        }
    }


    /**
     * Sync status bar notifications
     * mark ones that are no longer active as inactive in the table
     */
    public void sync_notifications() {
        Log.i(TAG,"**********  sync_notifications");

        if (NotifierConfiguration.cfg_notifier_paused) {
            Log.i(TAG, "Notifier paused. Ignoring...");
            return;
        }

        sync_in_progress = true;
        /**
         * Initially mark everything in notifications table as inactive
         */
        ntfcn_items.markAllInactive();

        for (StatusBarNotification asbn : getActiveNotifications()) {
            StatusBarNotification sbn = asbn.clone();

            String condensed_string = ntfcn_items.getCondensedString(sbn);

            Log.i(TAG,"Condensed string: " + condensed_string);

            try {
                PackageManager pm = getPackageManager();
                String app_name = (String) pm.getApplicationLabel(
                        pm.getApplicationInfo(sbn.getPackageName(), PackageManager.GET_META_DATA));

                Log.i(TAG,"ID :" + sbn.getId() + "\t" + sbn.getNotification().tickerText +
                        "\t" + sbn.getPackageName());

                Log.i(TAG,"App name :" + app_name +  "\n");

                HashSet<String> exclusion_list = NotifierConfiguration.excluded_packages;
                if (exclusion_list.contains(sbn.getPackageName())) {
                    Log.i(TAG, "Pkg: " + sbn.getPackageName() + " in exclusion list. Ignoring.");
                    continue;
                }

                /** skip group headers */
                if ( ((sbn.getNotification().flags & Notification.FLAG_GROUP_SUMMARY) != 0)) {
                    Log.i(TAG, "skippiing group header key: " + sbn.getKey());
                    continue;
                }

                /** Add a new active notification entry or
                 * just mark it as active if it already exists
                 */
                addActiveSBN(sbn);


                /**
                if (sbn.getNotification().extras.get(NotificationCompat.EXTRA_TEMPLATE).equals(
                        "android.app.Notification$MessagingStyle")
                        ) {

                    Log.e(TAG, "Messaging");
                    Log.i(TAG, "Extra Messages: " +
                            sbn.getNotification().extras.get(NotificationCompat.EXTRA_MESSAGES).toString());

                    Log.i(TAG, "Extra Messages History: " +
                            sbn.getNotification().extras.get(Notification.EXTRA_HISTORIC_MESSAGES));

                    Log.i(TAG, "Extra conversation title: " +
                            sbn.getNotification().extras.get(NotificationCompat.EXTRA_CONVERSATION_TITLE));
                }


                Log.i(TAG, "Flags: " +
                        ((sbn.getNotification().flags & Notification.FLAG_GROUP_SUMMARY) != 0));

                    Log.i(TAG, "Title :" + sbn.getNotification().extras.get(NotificationCompat.EXTRA_TITLE) + "\n");
                    Log.i(TAG, "Text :" + sbn.getNotification().extras.get(NotificationCompat.EXTRA_TEXT) + "\n");
                    Log.i(TAG, "Extra conv titles: " + sbn.getNotification().extras.get(NotificationCompat.EXTRA_CONVERSATION_TITLE));

                    Log.i(TAG, "Extra info text: " + sbn.getNotification().extras.get(NotificationCompat.EXTRA_INFO_TEXT));
                    Log.i(TAG, "Extra Messages: " + sbn.getNotification().extras.get(NotificationCompat.EXTRA_MESSAGES));

                    Log.i(TAG, "Extra big text lines" + sbn.getNotification().extras.get(NotificationCompat.EXTRA_BIG_TEXT));
                    Log.i(TAG, "Extra text lines" + sbn.getNotification().extras.get(NotificationCompat.EXTRA_TEXT_LINES));
                    Log.i(TAG, "Extra sub text " + sbn.getNotification().extras.get(NotificationCompat.EXTRA_SUB_TEXT));
                    Log.i(TAG, "Extra summary text " + sbn.getNotification().extras.get(NotificationCompat.EXTRA_SUMMARY_TEXT));

                    Log.i(TAG, "Extra title big: " + sbn.getNotification().extras.get(NotificationCompat.EXTRA_TITLE_BIG));


                    Log.i(TAG, "SBN group? " + sbn.isGroup());
                    Log.i(TAG, "Clearable? " + sbn.isClearable());
                    Log.i(TAG, "Posted at " + DateUtils.getRelativeTimeSpanString(sbn.getPostTime()));
                    Log.i(TAG, "Group key " + sbn.getGroupKey());
                    Log.i(TAG, "SBN key " + sbn.getKey());
                    Log.i(TAG, "TAG " + sbn.getTag());
                    Log.i(TAG, "Click Action :" + sbn.getNotification().contentIntent.toString());

                    Log.i(TAG, "Delete Action :" + sbn.getNotification().deleteIntent.toString());

                    for (Notification.Action action : sbn.getNotification().actions) {
                        Log.i(TAG, "Action :" + action.title + " Intent: " + action.actionIntent.toString() + "\n");
                    }
                 */
            } catch(Exception e) {
                Log.e(TAG, "Exception occurred while syncing notifications: " + e.getMessage());
            }
        }

        /**
         * If there are entries previously marked inactive, but doesn't have its cleared time set,
         * Set its cleared time to now
         */
        ntfcn_items.update_cleared_time_if_zero();
        this.num_active = ntfcn_items.getActiveCount();
        sync_in_progress = false;

        /** Update active notifications count in persistent notification */
        pnotif_builder.setContentText("Tap to open Notifications Center");

        if (NotifierConfiguration.cfg_svc_notification_enabled) {
            show_notification();
        }

        /**
         * TODO: if needed, remove all inactive applications at this point
         * if NotifierConfiguration.cfg_cache_notifications_enabled is false
         */
    }



    public int get_active_count() {
        return this.num_active;
    }

    public void filter_active() {
        ArrayList active = ntfcn_items.filter_active("");

        /** Add a group header */
        active.add(0, new NtfcnsDataModel(
                null,
                "Active Notifications",
                null,
                null,
                null,
                0,
                false,
                null,
                null,
                null,
                null,
                null,
                true   /** groups are expanded by default */
        ));

        adapter = new Ntfcns_adapter(active);
    }

    public void filter_all() {
        ArrayList all = ntfcn_items.filter_active("");

        /** Add a group header */
        all.add(0, new NtfcnsDataModel(
                null,
                "Active Notifications",
                null,
                null,
                null,
                0,
                false,
                null,
                null,
                null,
                null,
                null,
                true   /** groups are expanded by default */
        ));

        ArrayList inactive = ntfcn_items.filter_inactive("");

        /** Add a group header */
        inactive.add(0, new NtfcnsDataModel(
                null,
                "Cached Notifications",
                null,
                null,
                null,
                0,
                false,
                null,
                null,
                null,
                null,
                null,
                true   /** groups are expanded by default */
        ));

        all.addAll(inactive);
        adapter = new Ntfcns_adapter(all);
    }


    public void filter_active(String searchKey) {
        ArrayList active = ntfcn_items.filter_active(searchKey);

        /** Add a group header */
        active.add(0, new NtfcnsDataModel(
                null,
                "Active Notifications",
                null,
                null,
                null,
                0,
                false,
                null,
                null,
                null,
                null,
                null,
                true   /** groups are expanded by default */
        ));

        adapter = new Ntfcns_adapter(active);
    }


    /** Filter all notifications matching search key */
    public void filter_all(String searchKey) {
        ArrayList all = ntfcn_items.filter_active(searchKey);
        /** Add a group header */
        all.add(0, new NtfcnsDataModel(
                null,
                "Active Notifications",
                null,
                null,
                null,
                0,
                false,
                null,
                null,
                null,
                null,
                null,
                true   /** groups are expanded by default */
        ));

        ArrayList inactive = ntfcn_items.filter_inactive(searchKey);
        /** Add a group header */
        inactive.add(0, new NtfcnsDataModel(
                null,
                "Cached Notifications",
                null,
                null,
                null,
                0,
                false,
                null,
                null,
                null,
                null,
                null,
                true   /** groups are expanded by default */
        ));

        all.addAll(inactive);
        adapter = new Ntfcns_adapter(all);
    }


    /** Filter all notifications from package */
    public void filter_apps(String pkg, String searchKey) {
        ArrayList all = ntfcn_items.filter_active_app(pkg, searchKey);
        /** Add a group header */
        all.add(0, new NtfcnsDataModel(
                null,
                "Active Notifications",
                null,
                null,
                null,
                0,
                false,
                null,
                null,
                null,
                null,
                null,
                true   /** groups are expanded by default */
        ));

        ArrayList inactive = ntfcn_items.filter_inactive_app(pkg, searchKey);
        /** Add a group header */
        inactive.add(0, new NtfcnsDataModel(
                null,
                "Cached Notifications",
                null,
                null,
                null,
                0,
                false,
                null,
                null,
                null,
                null,
                null,
                true   /** groups are expanded by default */
        ));

        all.addAll(inactive);
        adapter = new Ntfcns_adapter(all);
    }



    /**
     * Build a list of packages from notifications sorted by their app name,
     * and their active count
     */
    public TreeMap<String, Integer> build_apps_list() {
        return this.ntfcn_items.build_apps_list();
    }

    /** remove all items from [startposition] in adapter
     * as long as they have the same group headers
     */
    public int collapse_group(int startposition) {
        ArrayList<NtfcnsDataModel> data = ((Ntfcns_adapter)adapter).getDataSet();
        int i = startposition+1;
        String group_header = data.get(startposition).getPlaceholder();

        Log.i(TAG, "Collapse group: " + group_header);

        /** when 'index' i is removed, next item to remove replaces
         *  index i.
         *  keep removing index i until we no longer find a match or
         *  if i < data.size() at which point we have reached the end of the list
         */
        int num_removed = 0;
        while (i < data.size() &&
                data.get(i).getPlaceholder().equals(group_header)) {
            data.remove(i);
            num_removed ++;
        }

        return num_removed;
    }


    /** Add (previously removed + new items that arrived later)to the
     *  arraylist if it matches group header at startposition
     *
     *  TODO: What about search view, what about app views?!
     */
    public int expand_group(int startposition,
                             String searchKey) {
        ArrayList<NtfcnsDataModel> data = ((Ntfcns_adapter)adapter).getDataSet();
        ArrayList<NtfcnsDataModel> items_to_add = new ArrayList<>();

        String group_header = data.get(startposition).getPlaceholder();

        Log.i(TAG, "Expand group: " + group_header);
        Log.i(TAG, "Current search key: " + searchKey);

        if (group_header.contains("Active Notifications")) {
            items_to_add = ntfcn_items.filter_active(searchKey);
        } else if (group_header.contains("Cached Notifications")) {
            items_to_add = ntfcn_items.filter_inactive(searchKey);
        }

        data.addAll(startposition+1, items_to_add);

        return items_to_add.size();
    }


    public RecyclerView.Adapter getAdapter() {
        return adapter;
    }


}
