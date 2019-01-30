package com.vinithepooh.notifier;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.IBinder;
import android.service.notification.NotificationListenerService;
import android.content.BroadcastReceiver;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import com.google.gson.Gson;

import static android.app.Notification.EXTRA_TEXT;
import static android.app.Notification.EXTRA_TEXT_LINES;
import static android.app.Notification.EXTRA_TITLE;


public class NLService extends NotificationListenerService {
    private final String TAG = "bulletin_board_svc";
    private final IBinder mBinder = new NLBinder();

    public class NLBinder extends Binder {
        NLService getService() {
            return NLService.this;
        }
    }

    public void onCreate() {
        Log.i(TAG,"**********  Service Created!");
        super.onCreate();
    }

    @Override
    public IBinder onBind(Intent mIntent) {
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

        Log.i(TAG,"**********  Service destroyed");

    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        Log.i(TAG,"**********  onNotificationPosted");
        Log.i(TAG,"ID :" + sbn.getId() + "\t" + sbn.getNotification().tickerText + "\t" + sbn.getPackageName());

        /**
        Gson ntfcn_gson = new Gson();
        String ntfcn_json = ntfcn_gson.toJson(sbn.getNotification());
        Log.i(TAG, "Posted SBN JSON - \n" + ntfcn_json);


        Intent i = new  Intent("com.vinithepooh.notifier.NOTIFICATION_LISTENER");

        i.putExtra("notification_event","onNotificationPosted :" + sbn.getPackageName() + "\n");

        sendBroadcast(i);
         */
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        Log.i(TAG,"**********  onNotificationRemoved");

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


                String title = sbn.getNotification().extras.getString(EXTRA_TITLE);
                String text = sbn.getNotification().extras.getString(EXTRA_TEXT);

                Log.i(TAG,"App name :" + app_name +  "\n");

                Log.i(TAG,"Title :" + title +  "\n");
                Log.i(TAG,"Text :" + text + "\n");
                Log.i(TAG, "Extra text lines" + sbn.getNotification().extras.getString(EXTRA_TEXT_LINES));
                Log.i(TAG, "Clearable? " + sbn.isClearable());

                Log.i(TAG,"Click Action :" + sbn.getNotification().contentIntent.toString());


                for (Notification.Action action: sbn.getNotification().actions) {
                    Log.i(TAG,"Action :" + action.title + " Intent: " + action.actionIntent.toString() +  "\n");
                }

                /**
                 if (app_name.equals("Tasker")) {
                 Log.i(TAG,"Found tasker");
                 // execute action
                 Notification.Action[] actions = sbn.getNotification().actions;
                 if (actions[0].title.equals("Disable")) {
                 Log.i(TAG, "Found disable intent for tasker");
                 actions[0].actionIntent.send(context, 0, intent);
                 }
                 } */

            } catch(Exception e) {
                Log.e(TAG, "Exception occurred while printing notifications: " + e.getMessage());
            }

            Log.i(TAG,"**********  END Notification #" + i + "\n\n");

            i++;
        }

        return ntfcns;
    }
}
