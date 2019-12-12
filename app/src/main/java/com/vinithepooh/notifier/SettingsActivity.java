package com.vinithepooh.notifier;

/**
 * Created by Niranjan Nagaraju on 20/11/19.
 */

import android.content.Context;
import android.content.Intent;
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
                    startActivity(intent);
                    return true;
                }
            });


        }
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
                preference.setSummary(
                        index >= 0
                                ? listPreference.getEntries()[index]
                                : null);

            } else if ((preference instanceof CheckBoxPreference) ||
                    (preference instanceof SwitchPreference)) {
                if (stringValue == "true")
                    preference.setSummary("Enabled");
                else
                    preference.setSummary("Disabled");
            } else {
                preference.setSummary(stringValue);
            }

            return true;
        }
    };
}