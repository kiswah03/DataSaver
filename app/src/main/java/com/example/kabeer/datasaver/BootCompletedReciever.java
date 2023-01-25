package com.example.kabeer.datasaver;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.preference.PreferenceManager;

import java.util.Calendar;

public class BootCompletedReciever extends BroadcastReceiver {

    SharedPreferences check,prefs;
    float lastuseddata;
    @Override
    public void onReceive(Context context, Intent intent) {
        check=context.getSharedPreferences("Check",0);
        prefs=context.getSharedPreferences("UsedData",0);
        lastuseddata=prefs.getFloat("currentwifidata", 0)+prefs.getFloat("currentmobiledata", 0);
        prefs.edit().putFloat("lasttotaldata",lastuseddata).apply();
        lastuseddata=prefs.getFloat("currentmobiledata",0);
        prefs.edit().putFloat("lastmobiledata",lastuseddata).apply();
        scheduleAlarmDaily(context);
        if(!check.getBoolean("lockBtn",false))
        {
            LockSwitchNotification ls=new LockSwitchNotification();
            ls.notify(context,"Lock Off",R.drawable.lockoff);
        }
        else
        {
            LockSwitchNotification ls=new LockSwitchNotification();
            ls.notify(context,"Lock On",R.drawable.lockon);
        }
        scheduleCalculateData(context);
    }
    public void scheduleAlarmDaily(Context context)
    {
        //System request code
        int DATA_FETCHER_RC = 123;
        long t=System.currentTimeMillis(), cal;
        //Create the time of day you would like it to go off. Use a calendar
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        if(t>=calendar.getTimeInMillis())
        {
            cal=calendar.getTimeInMillis()+86400000;
        }
        else
        {
            cal=calendar.getTimeInMillis();
        }
        //Create an intent that points to the receiver. The system will notify the app about the current time, and send a broadcast to the app
        Intent intent = new Intent(context, AlarmReciever.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, DATA_FETCHER_RC,intent, PendingIntent.FLAG_UPDATE_CURRENT);
        //Create an alarm manager
        AlarmManager mAlarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        mAlarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP,cal, AlarmManager.INTERVAL_DAY, pendingIntent);
    }
    public void scheduleCalculateData(Context context)
    {
        JobScheduler jobScheduler = (JobScheduler)context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        boolean hasBeenScheduled = false;

        for (JobInfo jobInfo : jobScheduler.getAllPendingJobs()) {
            if (jobInfo.getId() == 17) {
                hasBeenScheduled = true;
                break;
            }
        }
        if(!hasBeenScheduled)
        {
            ComponentName componentName = new ComponentName(context.getPackageName(), CalculateMobileDataService.class.getName());
            JobInfo jobInfo = new JobInfo.Builder(17, componentName)
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .setPeriodic(1000*60*15)
                    .setPersisted(true)
                    .build();
            jobScheduler.schedule(jobInfo);
        }
    }
}
