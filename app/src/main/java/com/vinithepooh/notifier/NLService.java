package com.vinithepooh.notifier;

import android.app.Notification;
import android.app.PendingIntent;
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
    private boolean hasChangedSinceLastUpdate = false;


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
        return START_STICKY;
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
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        stopSelf();

        Log.i(TAG,"**********  Service destroyed");

    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        Log.i(TAG,"**********  onNotificationPosted");
        Log.i(TAG,"ID :" + sbn.getId() + "\t" + sbn.getNotification().tickerText
                + "\t" + sbn.getPackageName());

        StatusBarNotification sbn_cloned = sbn.clone();
        String condensed_string = NtfcnsData.getCondensedString(sbn);

        if (! ntfcn_items.addActive(condensed_string, sbn_cloned)) {
            Log.i(TAG, "key: " + condensed_string + " already in active table");
        } else {
            Log.i(TAG, "Adding key: " + condensed_string + " to active table");
            hasChangedSinceLastUpdate = true;
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        Log.i(TAG,"**********  onNotificationRemoved");
        Log.i(TAG,"ID :" + sbn.getId() + "\t" + sbn.getNotification().tickerText
                + "\t" + sbn.getPackageName());

        String condensed_string = NtfcnsData.getCondensedString(sbn);
        StatusBarNotification active_sbn;
        if ( (active_sbn = ntfcn_items.removeActive(condensed_string)) != null) {
            Log.i(TAG, "key: " + condensed_string + " found in active table, removed");
            /** Add to inactive table */
            ntfcn_items.addInactive(condensed_string, active_sbn);
            hasChangedSinceLastUpdate = true;
        } else {
            Log.i(TAG, "Couldn't find key: " + condensed_string + " to remove");
        }
    }


    /** public API for clients */
    public void get_notifications() {
            /*
            if(intent.getStringExtra("command").equals("clearall")){
                NLService.this.cancelAllNotifications();
            }
            else
             */
        Log.i(TAG,"**********  get_notifications");

        Log.i(TAG,"**********  Showing notifications");

        for (StatusBarNotification asbn : getActiveNotifications()) {
            StatusBarNotification sbn = asbn.clone();
            String condensed_string = NtfcnsData.getCondensedString(sbn);

            Log.i(TAG,"Condensed string: " + condensed_string);

            try {

                PackageManager pm = getPackageManager();

                String app_name = (String) pm.getApplicationLabel(
                        pm.getApplicationInfo(sbn.getPackageName(), PackageManager.GET_META_DATA));

                Log.i(TAG,"ID :" + sbn.getId() + "\t" + sbn.getNotification().tickerText +
                        "\t" + sbn.getPackageName());

                Log.i(TAG,"App name :" + app_name +  "\n");

                if (! ntfcn_items.addActive(condensed_string, sbn)) {
                    Log.i(TAG, "key: " + condensed_string + " already in active table");
                } else {
                    hasChangedSinceLastUpdate = true;
                    Log.i(TAG, "Adding key: " + condensed_string + " to active table");
                }

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
            } catch(Exception e) {
                Log.e(TAG, "Exception occurred while printing notifications: " + e.getMessage());
            }
        }
    }


    public boolean hasUpdates() {
        boolean status = hasChangedSinceLastUpdate;

        /** reset it to false upon query */
        hasChangedSinceLastUpdate = false;
        return status;
    }


    public void filter_active() {
        ArrayList active = ntfcn_items.filter_active();
        adapter = new Ntfcns_adapter(active);
    }

    public void filter_all() {
        ArrayList all = ntfcn_items.filter_all();
        adapter = new Ntfcns_adapter(all);
    }

    public void prune() {
        if (ntfcn_items.prune())
            hasChangedSinceLastUpdate = true;
    }

    public RecyclerView.Adapter getAdapter() {
        return adapter;
    }


}
