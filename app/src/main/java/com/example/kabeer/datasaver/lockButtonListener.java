package com.example.kabeer.datasaver;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.preference.PreferenceManager;
import android.widget.Toast;

public class lockButtonListener extends BroadcastReceiver {
    String msg;
    SharedPreferences check;
    @Override
    public void onReceive(Context context, Intent intent) {

        check=context.getSharedPreferences("Check", 0);
        if(!check.getBoolean("lockBtn",false)) {
            check.edit().putBoolean("lockBtn", true).apply();
            msg="Lock On";
            jobCancel(context,15);
            LockSwitchNotification ls=new LockSwitchNotification();
            ls.notify(context,msg,R.drawable.lockon);
            scheduleJob(context);
        }
        else
        {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            if(prefs.getBoolean("password_switch",false)) {
                Intent a = new Intent(context, PasswordActivity.class);
                a.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(a);
            }
            else {
                jobCancel(context,12);
                check.edit().putBoolean("lockBtn", false).apply();
                msg = "Lock Off";
                LockSwitchNotification ls = new LockSwitchNotification();
                ls.notify(context, msg, R.drawable.lockoff);

                SharedPreferences autoLock= PreferenceManager.getDefaultSharedPreferences(context);
                String time=autoLock.getString("autoLock_list","Never");
                if(!time.equalsIgnoreCase("Never"))
                {
                    scheduleAutoLock(context);
                }
            }
        }
    }
    public void scheduleJob(Context context)
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
            JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
            ComponentName componentName = new ComponentName(context.getPackageName(), MyJobScheduler.class.getName());
            JobInfo jobInfo = new JobInfo.Builder(12, componentName)
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .setBackoffCriteria(2, JobInfo.BACKOFF_POLICY_LINEAR)
                    .setPersisted(true)
                    .build();
            jobScheduler.schedule(jobInfo);
        }
    }
    public void jobCancel(Context context,int id)
     {
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        boolean hasBeenScheduled = false;

        for (JobInfo jobInfo : jobScheduler.getAllPendingJobs()) {
            if (jobInfo.getId() == id) {
                hasBeenScheduled = true;
                break;
            }
        }
        if (hasBeenScheduled) {
            jobScheduler.cancel(id);
        }
    }
    public void scheduleAutoLock(Context context)
    {
        JobScheduler jobScheduler = (JobScheduler)context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        ComponentName componentName = new ComponentName(context.getPackageName(), LockFrequencyScheduler.class.getName());
        JobInfo jobInfo = new JobInfo.Builder(15, componentName)
                .setMinimumLatency(2*1000*60)
                .setBackoffCriteria(5,JobInfo.BACKOFF_POLICY_LINEAR)
                .setPersisted(true)
                .build();
        jobScheduler.schedule(jobInfo);
    }
}
