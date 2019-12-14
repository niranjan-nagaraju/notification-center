package com.vinithepooh.notifier;

import java.util.TreeSet;

/**
 * Created by Niranjan Nagaraju on 13/12/19.
 *
 * Captures preferences settings
 */

public class NotifierConfiguration {
    /** Default values */
    private final static boolean svc_notification_enabled = true;
    private final static boolean cache_notifications_enabled = true;
    private final static int cache_expiry_interval = 2;
    private final static boolean persistent_notifications_enabled = false;
    private final static boolean system_notifications_enabled = false;

    private final static boolean auto_remove_statusbar = false;
    private final static boolean default_expanded_view = false;
    private final static boolean notifier_paused = false;


    /** Notification values */
    public static boolean cfg_svc_notification_enabled = svc_notification_enabled;
    public static boolean cfg_cache_notifications_enabled = cache_notifications_enabled;
    public static int cfg_cache_expiry_interval = cache_expiry_interval;
    public static boolean cfg_persistent_notifications_enabled = persistent_notifications_enabled;
    public static boolean cfg_system_notifications_enabled = system_notifications_enabled;

    public static boolean cfg_auto_remove_statusbar = auto_remove_statusbar;
    public static boolean cfg_default_expanded_view = default_expanded_view;
    public static boolean cfg_notifier_paused = notifier_paused;
    public static TreeSet<String> installed_packages = null;
    public static TreeSet<String> excluded_packages = new TreeSet<>();
}
