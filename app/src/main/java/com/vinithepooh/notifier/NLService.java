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
    private HashMap<StatusBarNotification, NtfcnsData> ntfcns_table = new HashMap<>();
    private static RecyclerView.Adapter adapter;
    private static ArrayList<NtfcnsDataModel> data;


    public class NLBinder extends Binder {
        NLService getService() {
            return NLService.this;
        }
    }

    public void onCreate() {
        Log.i(TAG,"**********  Service Created!");
        super.onCreate();

        data = new ArrayList<NtfcnsDataModel>();


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
        }
        adapter = new Ntfcns_adapter(data);
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

        this.ntfcns_table.put(sbn_cloned, new NtfcnsData(
                sbn.getPackageName() + sbn.getNotification().tickerText,
                Ntfcns_state.ACTIVE
                ));
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        Log.i(TAG,"**********  onNotificationRemoved");

        Log.i(TAG,"ID :" + sbn.getId() + "\t" + sbn.getNotification().tickerText
                + "\t" + sbn.getPackageName());

        this.ntfcns_table.remove(sbn);
    }


    /** public API for clients */
    public String get_notifications() {
        String  ntfcns = "";

            /*
            if(intent.getStringExtra("command").equals("clearall")){
                NLService.this.cancelAllNotifications();
            }
            else
             */
        Log.i(TAG,"**********  get_notifications");

        Log.i(TAG,"**********  Showing notifications");

        Log.i(TAG, "Notifications: " + getActiveNotifications());

        int i=1;
        for (StatusBarNotification sbn : getActiveNotifications()) {
            ntfcns += "ID :" + sbn.getId() + "\t" + sbn.getNotification().tickerText + "\t" + sbn.getPackageName()
                    + "\n";
            /**
             Gson ntfcn_gson = new Gson();
             String ntfcn_json = ntfcn_gson.toJson(sbn.getNotification());
             Log.i(TAG, "SBN JSON - \n"); // + ntfcn_json);
             */

            Log.i(TAG,"\n **********  START Notification #" + i + "\n");

            try {
                PackageManager pm = getPackageManager();

                String app_name = (String) pm.getApplicationLabel(
                        pm.getApplicationInfo(sbn.getPackageName(), PackageManager.GET_META_DATA));


                Log.i(TAG,"ID :" + sbn.getId() + "\t" + sbn.getNotification().tickerText +
                        "\t" + sbn.getPackageName());

                Log.i(TAG,"App name :" + app_name +  "\n");


                /*

                String title = sbn.getNotification().extras.getString(EXTRA_TITLE);
                String text = sbn.getNotification().extras.getString(EXTRA_TEXT);



                Log.i(TAG,"Title :" + title +  "\n");
                Log.i(TAG,"Text :" + text + "\n");
                Log.i(TAG, "Extra conv titles: " + sbn.getNotification().extras.getString(EXTRA_CONVERSATION_TITLE));

                Log.i(TAG, "Extra info text: " + sbn.getNotification().extras.getString(EXTRA_INFO_TEXT));
                Log.i(TAG, "Extra Messages: " + sbn.getNotification().extras.getString(EXTRA_MESSAGES));

                Log.i(TAG, "Extra big text lines" + sbn.getNotification().extras.getString(EXTRA_BIG_TEXT));
                Log.i(TAG, "Extra text lines" + sbn.getNotification().extras.getString(EXTRA_TEXT_LINES));
                Log.i(TAG, "Extra sub text " + sbn.getNotification().extras.getString(EXTRA_SUB_TEXT));
                Log.i(TAG, "Extra summary text " + sbn.getNotification().extras.getString(EXTRA_SUMMARY_TEXT));
                Log.i(TAG, "Ticker text " + sbn.getNotification().tickerText);
                Log.i(TAG, "SBN group? " + sbn.isGroup());

                Log.i(TAG, "Extra title big: " + sbn.getNotification().extras.getString(EXTRA_TITLE_BIG));
                Log.i(TAG, "Clearable? " + sbn.isClearable());

                Log.i(TAG,"Click Action :" + sbn.getNotification().contentIntent.toString());


                for (Notification.Action action: sbn.getNotification().actions) {
                    Log.i(TAG,"Action :" + action.title + " Intent: " + action.actionIntent.toString() +  "\n");
                }

                /** How to execute a pending intent
                 if (app_name.equals("Tasker")) {
                 Log.i(TAG,"Found tasker");
                 // execute action
                 Notification.Action[] actions = sbn.getNotification().actions;
                 if (actions[0].title.equals("Disable")) {
                 Log.i(TAG, "Found disable intent for tasker");
                 actions[0].actionIntent.send(context, 0, intent);
                 }
                 } */

                Log.i(TAG, "COMPATS");



                Log.i(TAG,"Title :" + sbn.getNotification().extras.get(NotificationCompat.EXTRA_TITLE) +  "\n");
                Log.i(TAG,"Text :" + sbn.getNotification().extras.get(NotificationCompat.EXTRA_TEXT) + "\n");
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
                Log.i(TAG,"Click Action :" + sbn.getNotification().contentIntent.toString());
                Log.i(TAG,"Delete Action :" + sbn.getNotification().deleteIntent.toString());


                for (Notification.Action action: sbn.getNotification().actions) {
                    Log.i(TAG,"Action :" + action.title + " Intent: " + action.actionIntent.toString() +  "\n");
                }

            } catch(Exception e) {
                Log.e(TAG, "Exception occurred while printing notifications: " + e.getMessage());
            }

            Log.i(TAG,"**********  END Notification #" + i + "\n\n");

            i++;
        }

        return ntfcns;
    }


    public RecyclerView.Adapter getAdapter() {
        return adapter;
    }


}
