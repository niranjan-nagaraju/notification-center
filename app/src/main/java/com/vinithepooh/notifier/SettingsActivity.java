package com.vinithepooh.notifier;

/**
 * Created by Niranjan Nagaraju on 20/11/19.
 */

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.RingtonePreference;
import android.preference.SwitchPreference;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;

import java.util.HashSet;

public class SettingsActivity extends AppCompatPreferenceActivity {
    private static final String TAG = "bulletin_board_settings";
    private static Context context;
    private static Preference exclusion_pref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        context = getApplicationContext();
        // load settings fragment
        getFragmentManager().beginTransaction().replace(android.R.id.content, new MainPreferenceFragment()).commit();
    }




    public static class MainPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);

            /** Basic settings */
            bindPreferenceSummaryToValue(findPreference("service_notification_switch"));
            bindPreferenceSummaryToValue(findPreference("cache_ntfcn_switch"));
            bindPreferenceSummaryToValue(findPreference("cache_expiry_list"));
            bindPreferenceSummaryToValue(findPreference("ongoing_switch"));
            bindPreferenceSummaryToValue(findPreference("system_ntfcns_switch"));

            /** Advanced settings */
            bindPreferenceSummaryToValue(findPreference("autoremove_switch"));
            bindPreferenceSummaryToValue(findPreference("expanded_switch"));
            bindPreferenceSummaryToValue(findPreference("pause_switch"));


            Preference epref = findPreference("apps_exclusion");
            exclusion_pref = epref;
            epref.setSummary("Excluding " +
                    NotifierConfiguration.excluded_packages.size() + " apps from Notifier");
            Log.i(TAG, "Epref excluding: " + NotifierConfiguration.excluded_packages.size());

            epref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {
                    Log.i(TAG, "Exclusion list");
                    startActivity(new Intent(context, AppsListActivity.class));

                    return true;
                }
            });

            Preference npref = findPreference("notif_access_settings");

            npref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {
                    Log.i(TAG, "Opening notification access settings");
                    Intent intent = new Intent(
                            "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    return true;
                }
            });


        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        Log.i(TAG, "Exclusion list: " + NotifierConfiguration.excluded_packages);
        exclusion_pref.setSummary("Excluding " +
                NotifierConfiguration.excluded_packages.size() + " apps from Notifier");
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        Log.i(TAG, "Back button pressed");
        //startActivity(new Intent(getApplicationContext(), MainActivity.class));
            super.onBackPressed();
            //finish();
    }

    private static void bindPreferenceSummaryToValue(Preference preference) {
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        if ((preference instanceof CheckBoxPreference) || (preference instanceof  SwitchPreference))
        {
            // Trigger the listener immediately with the preference's
            // current value.
            sBindPreferenceSummaryToValueListener.onPreferenceChange(
                    preference,
                    PreferenceManager.getDefaultSharedPreferences(
                            preference.getContext()).getBoolean(preference.getKey(),true));
        } else {
            sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                    PreferenceManager
                            .getDefaultSharedPreferences(preference.getContext())
                            .getString(preference.getKey(), ""));

        }

    }


    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value.
     */
    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            String stringValue = newValue.toString();

            if (preference instanceof ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);

                // Set the summary to reflect the new value.
                CharSequence entry =
                        (index >= 0
                                ? listPreference.getEntries()[index]
                                : "");
                listPreference.setSummary(entry);
                if (listPreference.getKey().equals("cache_expiry_list")) {
                    Log.i(TAG, "Cache expiry list changed to " + entry);
                    NotifierConfiguration.cfg_cache_expiry_interval =
                            Integer.parseInt(listPreference.getEntryValues()[index].toString());

                }
            } else if (preference instanceof SwitchPreference) {
                Boolean switch_value = Boolean.parseBoolean(stringValue);

                if (preference.getKey().equals("service_notification_switch")) {
                    NotifierConfiguration.cfg_svc_notification_enabled = switch_value;
                    Log.i(TAG, "Service notification switch changed to " + stringValue);
                    if (switch_value) {
                        preference.setSummary(
                                "Showing service notification on the status bar for quick access to the Notifier");
                    } else {
                        preference.setSummary(
                                "Service notification for quick access to Notifier will not be shown");
                    }
                } else if (preference.getKey().equals("cache_ntfcn_switch")) {
                    NotifierConfiguration.cfg_cache_notifications_enabled = switch_value;
                    Log.i(TAG, "Cache notification switch changed to " + stringValue);
                    if (switch_value) {
                        preference.setSummary(
                                "Caching notifications cleared from the status bar");
                    } else {
                        preference.setSummary(
                                "Notifier will not cache cleared status bar notifications");
                    }
                } else if (preference.getKey().equals("ongoing_switch")) {
                    NotifierConfiguration.cfg_persistent_notifications_enabled = switch_value;
                    Log.i(TAG, "Persistent notifications switch changed to " + stringValue);
                    if (switch_value) {
                        preference.setSummary(
                                "Showing persistent/ongoing notifications");
                    } else {
                        preference.setSummary(
                                "Ignoring persistent/ongoing notifications");
                    }
                } else if (preference.getKey().equals("system_ntfcns_switch")) {
                    NotifierConfiguration.cfg_system_notifications_enabled = switch_value;
                    Log.i(TAG, "System notifications switch changed to " + stringValue);
                    if (switch_value) {
                        preference.setSummary(
                                "Showing Android System notifications");
                    } else {
                        preference.setSummary(
                                "Ignoring Android System notifications");
                    }
                } else if (preference.getKey().equals("autoremove_switch")) {
                    NotifierConfiguration.cfg_auto_remove_statusbar = switch_value;
                    Log.i(TAG, "Auto-remove notifications switch changed to " + stringValue);
                    if (switch_value) {
                        preference.setSummary(
                                "Notifier will clear status bar notifications after reading them");
                    } else {
                        preference.setSummary(
                                "Notifier will not clear status bar notifications");
                    }
                } else if (preference.getKey().equals("expanded_switch")) {
                    NotifierConfiguration.cfg_default_expanded_view = switch_value;
                    Log.i(TAG, "Expanded notifications switch changed to " + stringValue);
                    if (switch_value) {
                        preference.setSummary(
                                "Show expanded notifications view by default, shrink when tapped");
                    } else {
                        preference.setSummary(
                                "Show shrinked notifications view by default, expand when tapped");
                    }
                } else if (preference.getKey().equals("pause_switch")) {
                    NotifierConfiguration.cfg_notifier_paused = switch_value;
                    Log.i(TAG, "Notifier pause switch changed to " + stringValue);
                    if (switch_value) {
                        preference.setSummary(
                                "Notifications center paused, Ignoring incoming notifications");
                    } else {
                        preference.setSummary(
                                "Notifications center will read and cache incoming notifications");
                    }
                } else {
                    Log.i(TAG, "Unknown switch - WHAT ARE YOU?");
                    if (stringValue == "true")
                        preference.setSummary("Enabled");
                    else
                        preference.setSummary("Disabled");
                }
            } else {
                preference.setSummary(stringValue);
            }

            return true;
        }
    };
}