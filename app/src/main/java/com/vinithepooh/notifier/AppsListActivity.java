package com.vinithepooh.notifier;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;
import android.util.Log;
import android.view.MenuItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.TreeSet;

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
        private UpdateAppsAsyncTask updateApps_task;
        private Preference apploader_header;
        private List<ApplicationInfo> apps_list = null;
        private HashSet<String> installed_packages;
        private PreferenceCategory targetCategory;

        @Override
        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.prefs_apps);

            targetCategory = (PreferenceCategory)findPreference("apps_category");
            apploader_header = findPreference("apploader_header");
        }

        @Override
        public void onStart() {
            super.onStart();

            Log.i(TAG,"Apps list activity onstart: starting async task");
            updateApps_task = new UpdateAppsAsyncTask();
            updateApps_task.execute();
        }

        @Override
        public void onStop() {
            super.onStop();
            Log.i(TAG, "Apps list onstop()");
            //updateApps_task.cancel(true);

            for(String s: installed_packages) {
                targetCategory.removePreference(findPreference(s));
            }
            installed_packages.clear();
        }

        private class UpdateAppsAsyncTask extends AsyncTask<Void, String, Void> {
            PackageManager pm = context.getPackageManager();

            @Override
            protected Void doInBackground(Void... voids) {
                apps_list = pm
                        .getInstalledApplications(PackageManager.GET_META_DATA);

                Comparator<ApplicationInfo> apps_name_comparator = new Comparator<ApplicationInfo>() {
                    public int compare(ApplicationInfo a1, ApplicationInfo a2) {
                        PackageManager pm = context.getPackageManager();

                        String s1 = pm.getApplicationLabel(a1).toString();
                        String s2 = pm.getApplicationLabel(a2).toString();
                        // reverse chronological order of notifications
                        return s1.compareToIgnoreCase(s2);
                    }};

                Collections.sort(apps_list, apps_name_comparator);
                installed_packages = new HashSet<>();
                return null;
            }

            @Override
            protected void onPreExecute() {
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                try {
                    int i = 0;
                    for (ApplicationInfo ai : apps_list) {
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

                            installed_packages.add(ai.packageName);
                            i++;
                        }
                    }
                    targetCategory.removePreference(apploader_header);
                } catch (Exception e) {
                    Log.i(TAG, "Error in appslist async postexecute: " + e.getMessage());
                }
            }

            @Override
            protected void onProgressUpdate(String... values) {
            }

            @Override
            protected void onCancelled(Void aVoid) {
                Log.i(TAG, "Apps update async task cancelled with params");
            }

            @Override
            protected void onCancelled() {
                Log.i(TAG, "Apps update async task cancelled");
            }
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
            SharedPreferences sharedPreferences =
                    PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
            SharedPreferences.Editor editor = sharedPreferences.edit();

            Log.i(TAG, "Preference changed: " + preference.getKey());

            if (preference instanceof SwitchPreference) {
                if (stringValue.equals("true")) {
                    /** TODO: Have an include/exclude all switch someday */
                    if (preference.getKey().equals("exclude_all")) {
                        Log.i(TAG, "Excluding all applications");
                    } else {
                        if (!NotifierConfiguration.excluded_packages.contains(preference.getKey())) {
                            Log.i(TAG, "Adding " + preference.getKey() + " to exclusion list");
                            NotifierConfiguration.excluded_packages.add(preference.getKey());
                            editor.putStringSet("excluded_packages",
                                    NotifierConfiguration.excluded_packages);
                            editor.commit();
                        }
                    }
                } else { // switch preference unchecked.
                    if (preference.getKey().equals("exclude_all")) {
                        Log.i(TAG, "Including all applications");
                    } else {
                        if (NotifierConfiguration.excluded_packages.contains(preference.getKey())) {
                            Log.i(TAG, "Removing " + preference.getKey() + " from exclusion list");
                            NotifierConfiguration.excluded_packages.remove(preference.getKey());
                            editor.putStringSet("excluded_packages",
                                    NotifierConfiguration.excluded_packages);
                            editor.commit();
                        }
                    }
                }
            } else {
                preference.setSummary(stringValue);
            }

            return true;
        }
    };
}

