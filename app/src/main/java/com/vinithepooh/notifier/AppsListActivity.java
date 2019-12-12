package com.vinithepooh.notifier;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;
import android.util.Log;
import android.view.MenuItem;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by Niranjan Nagaraju on 24/11/19.
 */

public class AppsListActivity extends AppCompatPreferenceActivity {
    private static final String TAG = "bulletin_board_appslist";
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
            addPreferencesFromResource(R.xml.prefs_apps);

            bindPreferenceSummaryToValue(findPreference("check_none"));

            PreferenceCategory targetCategory = (PreferenceCategory)findPreference("apps_category");

            PackageManager pm = context.getPackageManager();
            List<ApplicationInfo> apps_list = pm
                    .getInstalledApplications(PackageManager.GET_META_DATA);

            Collections.sort(apps_list, apps_name_comparator);
            int i = 0;
            for (ApplicationInfo ai: apps_list) {
                if ((ai.flags & ApplicationInfo.FLAG_INSTALLED) == 0) continue;
                if ((ai.flags & ApplicationInfo.FLAG_SYSTEM) == 0
                        && (ai.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0) {
                    SwitchPreference switchp = new SwitchPreference(context);
                    switchp.setKey(ai.packageName);
                    switchp.setTitle(pm.getApplicationLabel(ai));
                    switchp.setIcon(pm.getApplicationIcon(ai));
                    //switchp.setSummary(ai.packageName);
                    switchp.setDefaultValue(false);

                    targetCategory.addPreference(switchp);

                    bindPreferenceSummaryToValue(switchp);
                    Log.i(TAG, i + ": App: " + pm.getApplicationLabel(ai) + " pkg: "
                            + ai.packageName);
                    i++;
                }
            }
        }

        public static Comparator<ApplicationInfo> apps_name_comparator = new Comparator<ApplicationInfo>() {
            public int compare(ApplicationInfo a1, ApplicationInfo a2) {
                PackageManager pm = context.getPackageManager();

                String s1 = pm.getApplicationLabel(a1).toString();
                String s2 = pm.getApplicationLabel(a2).toString();
                // reverse chronological order of notifications
                return s1.compareToIgnoreCase(s2);
            }};
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
        //startActivity(new Intent(getApplicationContext(), SettingsActivity.class));
        super.onBackPressed();
    }

    private static void bindPreferenceSummaryToValue(Preference preference) {
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        if ((preference instanceof CheckBoxPreference) || (preference instanceof SwitchPreference)) {
            // Trigger the listener immediately with the preference's
            // current value.

            sBindPreferenceSummaryToValueListener.onPreferenceChange(
                    preference,
                    PreferenceManager.getDefaultSharedPreferences(
                            preference.getContext()).getBoolean(preference.getKey(), true));
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

            if ((preference instanceof CheckBoxPreference) ||
                    (preference instanceof SwitchPreference)) {

                if (stringValue == "true")
                    preference.setSummary("");
                else
                    preference.setSummary("");

            } else {
                preference.setSummary(stringValue);
            }

            return true;
        }
    };




}

