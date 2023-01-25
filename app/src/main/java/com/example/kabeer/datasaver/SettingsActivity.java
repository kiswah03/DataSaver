package com.example.kabeer.datasaver;

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.content.SharedPreferences;
import android.preference.Preference;
import android.preference.SwitchPreference;
import android.support.v7.app.ActionBar;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.RingtonePreference;
import android.text.TextUtils;
import android.view.MenuItem;
import android.support.v4.app.NavUtils;
import android.widget.Toast;

import java.util.List;



public class SettingsActivity extends AppCompatPreferenceActivity {

    static Preference pref;
    static SharedPreferences check;
    static boolean finishedLoading=false;

    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue=value.toString();

            if(preference.getKey().matches("wifi_text|wifi_list|mobile_text|mobile_list|time_list") && finishedLoading) {
                check = preference.getContext().getSharedPreferences("Check", 0);
                Boolean lockon = check.getBoolean("lockBtn", false);
                if (lockon ) {
                    lockJobCancel(preference.getContext());
                }
            }
            if (preference instanceof ListPreference) {
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);

                // Set the summary to reflect the new value.
                String strValue=(index >= 0 ? listPreference.getEntries()[index] : "").toString();
                preference.setSummary(
                        index >= 0
                                ? listPreference.getEntries()[index]
                                : null);
                if(preference.getKey().equals("mobile_list")|| preference.getKey().equals("wifi_list"))
                {
                    EditTextPreference prefCat=(EditTextPreference) pref;
                    prefCat.setTitle("Limit in "+ strValue);
                }

            } else if (preference instanceof RingtonePreference) {
                // For ringtone preferences, look up the correct display value
                // using RingtoneManager.
                if (TextUtils.isEmpty(stringValue)) {
                    // Empty values correspond to 'silent' (no ringtone).
                    preference.setSummary(R.string.pref_ringtone_silent);

                } else {
                    Ringtone ringtone = RingtoneManager.getRingtone(
                            preference.getContext(), Uri.parse(stringValue));

                    if (ringtone == null) {
                        // Clear the summary if there was a lookup error.
                        preference.setSummary(null);
                    } else {
                        // Set the summary to reflect the new ringtone display
                        // name.
                        String name = ringtone.getTitle(preference.getContext());
                        preference.setSummary(name);
                    }
                }
            } else {
                if(preference.getKey().equals("password_text"))
                {
                    if(stringValue.equals(""))
                    {
                        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(preference.getContext());
                        sharedPreferences.edit().putBoolean("password_switch", false).apply();
                        preference.setSummary(stringValue);
                    }else
                    {
                        preference.setSummary(stringValue);
                    }
                }
                else if(preference.getKey().equals("mnthStart_text"))
                {
                    if(Integer.parseInt(stringValue)<=0 || Integer.parseInt(stringValue)>31)
                    {
                        Toast.makeText(preference.getContext(), "Please enter a valid day number.", Toast.LENGTH_LONG).show();
                    }
                    else
                    {
                        int num=Integer.parseInt(stringValue);
                        if(num==1 || num==21 || num==31)
                        {
                            stringValue=stringValue+"st of every month";
                        }
                        else if(num==2 || num==22)
                        {
                            stringValue=stringValue+"nd of every month";
                        }
                        else if(num==3 || num==23)
                        {
                            stringValue=stringValue+"rd of every month";
                        }
                        else
                        {
                            stringValue=stringValue+"th of every month";
                        }
                        preference.setSummary(stringValue);
                    }
                }
                else {
                    preference.setSummary(stringValue);
                }
            }
            if(preference.getKey().matches("wifi_text|wifi_list|mobile_text|mobile_list|time_list") && finishedLoading)
            {
                check=preference.getContext().getSharedPreferences("Check", 0);
                Boolean lockon=check.getBoolean("lockBtn",false);
                if(lockon)
                {
                    scheduleJob(preference.getContext());
                }
            }
            return true;
        }
    };
    private static boolean isXLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    private static void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        // Trigger the listener immediately with the preference's
        // current value.
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupActionBar();
    }

    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            if (!super.onMenuItemSelected(featureId, item)) {
                NavUtils.navigateUpFromSameTask(this);
            }
            return true;
        }
        return super.onMenuItemSelected(featureId, item);
    }

    @Override
    public boolean onIsMultiPane() {
        return isXLargeTablet(this);
    }

    @Override
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.pref_headers, target);
    }

    /**
     * This method stops fragment injection in malicious applications.
     * Make sure to deny any unknown fragments here.
     */
    protected boolean isValidFragment(String fragmentName) {
        return PreferenceFragment.class.getName().equals(fragmentName)
                || GeneralPreferenceFragment.class.getName().equals(fragmentName)
                || TimePreferenceFragment.class.getName().equals(fragmentName)
                || WifiPreferenceFragment.class.getName().equals(fragmentName)
                || MobileDataPreferenceFragment.class.getName().equals(fragmentName)
                || NotificationPreferenceFragment.class.getName().equals(fragmentName);
    }


   private static Preference.OnPreferenceChangeListener listener =
            new Preference.OnPreferenceChangeListener() {
                public boolean onPreferenceChange(Preference preference, Object value) {
                    if(preference.getKey().matches("wifi_switch|mobile_switch|time_switch") && finishedLoading)
                    {
                        check = preference.getContext().getSharedPreferences("Check", 0);
                        Boolean lockon = check.getBoolean("lockBtn", false);
                        if (lockon ) {
                            lockJobCancel(preference.getContext());
                        }
                    }else if(preference.getKey().equals("autoLock_list") && finishedLoading)
                    {
                        jobCancel(preference.getContext());
                    }
                    if (preference instanceof ListPreference) {
                        String lvalue=value.toString();
                        ListPreference listPreference = (ListPreference) preference;
                        int index = listPreference.findIndexOfValue(lvalue);

                        preference.setSummary(
                                index >= 0
                                        ? listPreference.getEntries()[index]
                                        : null);
                        if (!lvalue.equalsIgnoreCase("never") && finishedLoading) {
                            autoLockJobSchedule(preference.getContext());
                        } else if(finishedLoading){
                            jobCancel(preference.getContext());
                        }
                    }
                    else if(preference instanceof SwitchPreference)
                    {
                        if(preference.getKey().matches("wifi_switch|mobile_switch|time_switch") && finishedLoading)
                        {
                            check = preference.getContext().getSharedPreferences("Check", 0);
                            Boolean lockon = check.getBoolean("lockBtn", false);
                            if (lockon ) {
                                scheduleJob(preference.getContext());
                            }
                        }
                    }
                    return true;
                }};
    private static void lockJobCancel(Context context)
    {
        JobScheduler jobScheduler = (JobScheduler)context.getSystemService(SettingsActivity.JOB_SCHEDULER_SERVICE);
        boolean hasBeenScheduled = false;

        for (JobInfo jobInfo : jobScheduler.getAllPendingJobs()) {
            if (jobInfo.getId() == 12) {
                hasBeenScheduled = true;
                break;
            }
        }
        if (hasBeenScheduled) {
            jobScheduler.cancel(12);
        }
    }
    private static void scheduleJob(Context context)
     {
         ConnectivityManager manager = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
         android.net.NetworkInfo net = manager.getActiveNetworkInfo();

         if(net!=null && (net.getType() == ConnectivityManager.TYPE_WIFI || net.getType()==ConnectivityManager.TYPE_MOBILE))
         {
             JobScheduler jobScheduler = (JobScheduler)context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
             ComponentName componentName = new ComponentName(context.getPackageName(), MyJobScheduler.class.getName());
             JobInfo jobInfo = new JobInfo.Builder(12, componentName)
                     .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                     .setOverrideDeadline(1000*3)
                     .setBackoffCriteria(2,JobInfo.BACKOFF_POLICY_LINEAR)
                     .setPersisted(true)
                     .build();
             jobScheduler.schedule(jobInfo);
         }else {
             JobScheduler jobScheduler = (JobScheduler) context.getSystemService(context.JOB_SCHEDULER_SERVICE);
             ComponentName componentName = new ComponentName(context.getPackageName(), MyJobScheduler.class.getName());
             JobInfo jobInfo = new JobInfo.Builder(12, componentName)
                     .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                     .setBackoffCriteria(2, JobInfo.BACKOFF_POLICY_LINEAR)
                     .setPersisted(true)
                     .build();
             jobScheduler.schedule(jobInfo);
         }
    }
    private static void autoLockJobSchedule(Context context)
    {
        JobScheduler jobScheduler = (JobScheduler)context.getSystemService(SettingsActivity.JOB_SCHEDULER_SERVICE);
        ComponentName componentName = new ComponentName(context.getPackageName(), LockFrequencyScheduler.class.getName());
        JobInfo jobInfo = new JobInfo.Builder(15, componentName)
                .setMinimumLatency(2*1000*60)
                .setBackoffCriteria(5,JobInfo.BACKOFF_POLICY_LINEAR)
                .setPersisted(true)
                .build();
        jobScheduler.schedule(jobInfo);
    }
    private static void jobCancel(Context context)
    {
        JobScheduler jobScheduler = (JobScheduler)context.getSystemService(SettingsActivity.JOB_SCHEDULER_SERVICE);
        boolean hasBeenScheduled = false;

        for (JobInfo jobInfo : jobScheduler.getAllPendingJobs()) {
            if (jobInfo.getId() == 15) {
                hasBeenScheduled = true;
                break;
            }
        }
        if (hasBeenScheduled) {
            jobScheduler.cancel(15);
        }
    }
    private static void lockPreferenceChanged(Preference prefs)
    {
        prefs.setOnPreferenceChangeListener(listener);
        if(prefs instanceof ListPreference) {
            listener.onPreferenceChange(prefs,
                    PreferenceManager
                            .getDefaultSharedPreferences(prefs.getContext())
                            .getString(prefs.getKey(), ""));
        }
    }
    private static void switchCheck(Preference calling,String sh1,String sh2)
    {
        SharedPreferences prefs;
        prefs= PreferenceManager.getDefaultSharedPreferences(calling.getContext());
        if(prefs.getBoolean(sh1,false) && (prefs.getString(sh2, "0").equals("0")|| prefs.getString(sh2, "").equals("")))
        {
            prefs.edit().putBoolean(sh1,false).apply();
        }
    }
    private static void setDayNumber(Preference preference)
    {
        SharedPreferences prefs;
        prefs= PreferenceManager.getDefaultSharedPreferences(preference.getContext());
        int day=Integer.parseInt(prefs.getString("mnthStart_text","1"));
        if(day<1 || day>31) {
            String summary = preference.getSummary().toString();
            if (summary.length() < 19) {
                summary = summary.substring(0, 1);
            } else {
                summary = summary.substring(0, 2);
            }
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(preference.getContext());
            sharedPreferences.edit().putString("mnthStart_text", summary).apply();
        }
    }
    //General Preference Fragment
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class GeneralPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_general);
            setHasOptionsMenu(true);

            bindPreferenceSummaryToValue(findPreference("password_text"));
            lockPreferenceChanged(findPreference("autoLock_list"));
            bindPreferenceSummaryToValue(findPreference("mnthStart_text"));
            finishedLoading=true;

        }
        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                switchCheck(findPreference("general_preference"),"password_switch","password_text");
                setDayNumber(findPreference("mnthStart_text"));
                finishedLoading=false;
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
        @Override
        public void onResume() {
            switchCheck(findPreference("general_preference"),"password_switch","password_text");
            setDayNumber(findPreference("mnthStart_text"));
            finishedLoading=true;
            super.onResume();
        }

        @Override
        public void onPause() {
            super.onPause();
            switchCheck(findPreference("general_preference"),"password_switch","password_text");
            setDayNumber(findPreference("mnthStart_text"));
            finishedLoading=false;
        }
    }
    //Notification Preference
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class NotificationPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_notification);
            setHasOptionsMenu(true);

            bindPreferenceSummaryToValue(findPreference("notifications_ringtone"));
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }

    //Mobile Data preference
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class MobileDataPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_mobiledata);
            setHasOptionsMenu(true);

            pref=findPreference("mobile_text");
            lockPreferenceChanged(findPreference("mobile_switch"));
            bindPreferenceSummaryToValue(findPreference("mobile_text"));
            bindPreferenceSummaryToValue(findPreference("mobile_list"));
            finishedLoading=true;
        }
        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                switchCheck(findPreference("mobiledata_preference"),"mobile_switch","mobile_text");
                finishedLoading=false;
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
        @Override
        public void onResume() {
            switchCheck(findPreference("mobiledata_preference"),"mobile_switch","mobile_text");
            finishedLoading=true;
            super.onResume();
        }

        @Override
        public void onPause() {
            super.onPause();
            switchCheck(findPreference("mobiledata_preference"),"mobile_switch","mobile_text");
            finishedLoading=false;
        }
    }
    //Wifi preference
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class WifiPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_wifi);
            setHasOptionsMenu(true);

            pref=findPreference("wifi_text");
            lockPreferenceChanged(findPreference("wifi_switch"));
            bindPreferenceSummaryToValue(findPreference("wifi_text"));
            bindPreferenceSummaryToValue(findPreference("wifi_list"));
            finishedLoading=true;
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                switchCheck(findPreference("wifi_preference"),"wifi_switch","wifi_text");
                finishedLoading=false;
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
        @Override
        public void onResume() {
            switchCheck(findPreference("wifi_preference"),"wifi_switch","wifi_text");
            finishedLoading=true;
            super.onResume();
        }

        @Override
        public void onPause() {
            super.onPause();
            switchCheck(findPreference("wifi_preference"),"wifi_switch","wifi_text");
            finishedLoading=false;
        }
    }
    //Time preference
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class TimePreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_time);
            setHasOptionsMenu(true);

            lockPreferenceChanged(findPreference("time_switch"));
            bindPreferenceSummaryToValue(findPreference("time_list"));
            finishedLoading=true;
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                finishedLoading=false;
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
        @Override
        public void onResume() {
            finishedLoading=true;
            super.onResume();
        }

        @Override
        public void onPause() {
            super.onPause();
            finishedLoading=false;
        }
    }
}
