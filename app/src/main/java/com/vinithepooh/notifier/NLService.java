package com.vinithepooh.notifier;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.pm.PackageManager;
import android.graphics.drawable.Icon;
import android.os.Binder;
import android.os.IBinder;
import android.service.notification.NotificationListenerService;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.service.notification.StatusBarNotification;
import android.support.v4.app.NotificationCompat;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;


public class NLService extends NotificationListenerService {
    private final String TAG = "bulletin_board_svc";
    private final IBinder mBinder = new NLBinder();

    private static RecyclerView.Adapter adapter;

    private NtfcnsData ntfcn_items;
    private long time_since_last_resync = 0;

    /** Has listener service connected to notfication manager */
    private boolean listenerConnected = false;
    public boolean isListenerConnected() {
        return listenerConnected;
    }


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
        //adapter = new Ntfcns_adapter(data);
        ntfcn_items = new NtfcnsData(this.getApplicationContext());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "OnStartCommand");
        Runnable r = new Runnable() {
            public void run() {
                String debug_tag = TAG + "_thread";
                while (true) {
                    try {
                        /** Resync active notifications every 10 minutes */
                        time_since_last_resync += 1;
                        if (time_since_last_resync > 10) {
                            time_since_last_resync = 0;
                            Log.i(TAG, "Resync in progress!");
                            sync_notifications();
                        }

                        /** sleep for 1 minute */
                        Thread.sleep(60*1000);

                        Log.i(debug_tag, "Pruning old entries");
                        ntfcn_items.prune();
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



    /** Add a status bar notification from active notifications to the active table */
    private void addActiveSBN(StatusBarNotification sbn) {
        if ( sbn.isOngoing() || !sbn.isClearable() ) {
            Log.i(TAG, "Ongoing or not clearable, ignoring");
            return;
        }

        Object extra_text = sbn.getNotification().extras.get(NotificationCompat.EXTRA_TEXT);
        if (extra_text == null || extra_text.toString().isEmpty()) {
            Log.i(TAG, "Notifications text is empty. Ignoring");
            return;
        }

        if (sbn.getPackageName().equals("android")) {
            Log.i(TAG, "System notifications. Ignoring");
            return;
        }

        String condensed_string = ntfcn_items.getCondensedString(sbn);
        if (! ntfcn_items.addActive(condensed_string, sbn)) {
            Log.i(TAG, "key: " + condensed_string + " already in active table");
        } else {
            Log.i(TAG, "Adding key: " + condensed_string + " to active table");
        }
    }



    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        Log.i(TAG,"**********  onNotificationPosted");
        Log.i(TAG,"ID :" + sbn.getId() + "\t" + sbn.getNotification().tickerText
                + "\t" + sbn.getPackageName());

        addActiveSBN(sbn);
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        Log.i(TAG,"**********  onNotificationRemoved");
        Log.i(TAG,"ID :" + sbn.getId() + "\t" + sbn.getNotification().tickerText
                + "\t" + sbn.getPackageName());

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

        Log.i(TAG,"**********  Showing notifications");

        /**
         * Initially mark everything in notifications table as inactive
         */
        ntfcn_items.markAllInactive();

        for (StatusBarNotification sbn : getActiveNotifications()) {
            //StatusBarNotification asbn = sbn.clone();
            String condensed_string = ntfcn_items.getCondensedString(sbn);

            Log.i(TAG,"Condensed string: " + condensed_string);

            try {

                PackageManager pm = getPackageManager();

                String app_name = (String) pm.getApplicationLabel(
                        pm.getApplicationInfo(sbn.getPackageName(), PackageManager.GET_META_DATA));

                Log.i(TAG,"ID :" + sbn.getId() + "\t" + sbn.getNotification().tickerText +
                        "\t" + sbn.getPackageName());

                Log.i(TAG,"App name :" + app_name +  "\n");

                /** Add a new active notification entry or
                 * just mark it as active if it already exists
                 */
                addActiveSBN(sbn);

                /**
                Log.i(TAG, "Template :" +
                        sbn.getNotification().extras.get(NotificationCompat.EXTRA_TEMPLATE) + "\n");


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
                 */

                /**
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
                Log.e(TAG, "Exception occurred while printing notifications: " + e.getMessage());
            }
        }
    }



    public void filter_active() {
        ArrayList active = ntfcn_items.filter_active("");
        adapter = new Ntfcns_adapter(active);
    }

    public void filter_all() {
        ArrayList all = ntfcn_items.filter_active("");
        all.addAll(ntfcn_items.filter_inactive(""));
        adapter = new Ntfcns_adapter(all);
    }


    public void filter_active(String searchKey) {
        ArrayList active = ntfcn_items.filter_active(searchKey);
        adapter = new Ntfcns_adapter(active);
    }

    public void filter_all(String searchKey) {
        ArrayList all = ntfcn_items.filter_active(searchKey);
        all.addAll(ntfcn_items.filter_inactive(searchKey));

        adapter = new Ntfcns_adapter(all);
    }


    public RecyclerView.Adapter getAdapter() {
        return adapter;
    }


}
