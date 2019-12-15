package com.vinithepooh.notifier;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

/**
 * Created by Niranjan Nagaraju on 13/12/19.
 *
 * Captures preferences settings
 */

public class NotifierConfiguration {
    private final static String TAG = "bulletin_board_cfg";

    /** Default values */
    private final static boolean default_svc_notification_enabled = true;
    private final static boolean default_cache_notifications_enabled = true;
    private final static Integer default_cache_expiry_interval = 2;
    private final static boolean default_persistent_notifications_enabled = false;
    private final static boolean default_system_notifications_enabled = false;

    private final static boolean default_auto_remove_statusbar = false;
    private final static boolean default_expanded_view = false;
    private final static boolean default_notifier_paused = false;

    static Context context;

    /** Notification values */
    public static boolean cfg_svc_notification_enabled = default_svc_notification_enabled;
    public static boolean cfg_cache_notifications_enabled = default_cache_notifications_enabled;
    public static int cfg_cache_expiry_interval = default_cache_expiry_interval;
    public static boolean cfg_persistent_notifications_enabled = default_persistent_notifications_enabled;
    public static boolean cfg_system_notifications_enabled = default_system_notifications_enabled;

    public static boolean cfg_auto_remove_statusbar = default_auto_remove_statusbar;
    public static boolean cfg_default_expanded_view = default_expanded_view;
    public static boolean cfg_notifier_paused = default_notifier_paused;
    public static HashSet<String> excluded_packages = new HashSet<>();

    private static SharedPreferences sharedPreferences = null;

    public static void initialize_cfg_from_sharedPrefs(Context app_context) {
        context = app_context;
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        /**
         * Initialize with default value for cache timeout, so it'll be returned
         *  if sharedPreferences reference is null
         */
        if (sharedPreferences == null) {
            // unlikely but return if it does happen
            // with default values for all configurations
            Log.i(TAG, "Shared preferences returned null");
            return;
        }

        /** service notification */
        cfg_svc_notification_enabled = sharedPreferences.getBoolean(
                "service_notification_switch",
                default_svc_notification_enabled);
        Log.i(TAG, "Init: Service notification enabled cfg: " +
                cfg_svc_notification_enabled);


        /** Cache cleared notifications */
        cfg_cache_notifications_enabled = sharedPreferences.getBoolean(
                "cache_ntfcn_switch",
                default_cache_notifications_enabled);
        Log.i(TAG, "Init: Cache cleared notification cfg: " +
                cfg_cache_notifications_enabled);


        /** Cache expiry interval */
        cfg_cache_expiry_interval = Integer.parseInt(
                sharedPreferences.getString("cache_expiry_list",
                        default_cache_expiry_interval.toString()));
        Log.i(TAG, "Init: Expiry interval from cfg: " + cfg_cache_expiry_interval);

        /** Show persistent notifications */
        cfg_persistent_notifications_enabled = sharedPreferences.getBoolean(
                "ongoing_switch", default_persistent_notifications_enabled);
        Log.i(TAG, "Init: Persistent notification cfg: " +
                cfg_persistent_notifications_enabled);

        /** System notifications */
        cfg_system_notifications_enabled = sharedPreferences.getBoolean("system_ntfcns_switch",
                default_system_notifications_enabled);
        Log.i(TAG, "Init: System notifications cfg: " +
                cfg_system_notifications_enabled);

        /** Exclusion list */
        Set<String> exclusion_set = sharedPreferences.getStringSet("excluded_packages",
                new HashSet<String>());
        for (String s: exclusion_set) {
            NotifierConfiguration.excluded_packages.add(s);
        }
        Log.i(TAG, "Init: Exclusion list cfg: " + NotifierConfiguration.excluded_packages);


        /** auto remove status bar notifications */
        cfg_auto_remove_statusbar = sharedPreferences.getBoolean("autoremove_switch",
                default_auto_remove_statusbar);
        Log.i(TAG, "Init: Auto-remove status bar notifications cfg: " +
                cfg_auto_remove_statusbar);


        /** Show expanded view */
        cfg_default_expanded_view = sharedPreferences.getBoolean("expanded_switch",
                default_expanded_view);
        Log.i(TAG, "Init: Expanded view by default cfg: " +
                cfg_default_expanded_view);


        /** Pause Notification center */
        cfg_notifier_paused = sharedPreferences.getBoolean("pause_switch",
                default_notifier_paused);
        Log.i(TAG, "Init: Notification pause cfg: " +
                cfg_notifier_paused);
    }

    public static Integer getCache_expiry_interval() {
        return cfg_cache_expiry_interval;
    }
}
