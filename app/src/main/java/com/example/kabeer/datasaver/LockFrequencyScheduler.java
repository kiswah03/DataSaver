package com.example.kabeer.datasaver;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.Log;

public class LockFrequencyScheduler extends JobService {
    private static final String TAG = LockFrequencyScheduler.class.getSimpleName();
    boolean isWorking = false;
    boolean jobCancelled = false;
    SharedPreferences check;
    // Called by the Android system when it's time to run the job
    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        isWorking = true;
        // We need 'jobParameters' so we can call 'jobFinished'
        startWorkOnNewThread(jobParameters); // Services do NOT run on a separate thread

        return isWorking;
    }
    private void startWorkOnNewThread(final JobParameters jobParameters) {
        new Thread(new Runnable() {
            public void run() {
                doWork(jobParameters);
            }
        }).start();
    }
    private void doWork(JobParameters jobParameters)
    {
        boolean needsReschedule=false;

        check=LockFrequencyScheduler.this.getSharedPreferences("Check", 0);
        if(!check.getBoolean("lockBtn",false)) {
            PowerManager pm=(PowerManager)getSystemService(LockFrequencyScheduler.this.POWER_SERVICE);
            boolean isScreenOn=pm.isInteractive();

            if(!isScreenOn) {
                try{
                    SharedPreferences autoLock= PreferenceManager.getDefaultSharedPreferences(LockFrequencyScheduler.this);
                    String time=autoLock.getString("autoLock_list","5");
                    long slp = 1000*Long.parseLong(time)*60;
                    Thread.sleep(slp);

                    pm=(PowerManager)getSystemService(LockFrequencyScheduler.this.POWER_SERVICE);
                    isScreenOn=pm.isInteractive();
                    if(!isScreenOn) {
                        check.edit().putBoolean("lockBtn", true).apply();
                        String msg = "Lock On";
                        LockSwitchNotification ls = new LockSwitchNotification();
                        ls.notify(LockFrequencyScheduler.this, msg, R.drawable.lockon);
                        scheduleJob(LockFrequencyScheduler.this);
                    }
                    else
                    {
                        slp=1000*5*60;
                        Thread.sleep(slp);
                        doWork(jobParameters);
                        //needsReschedule=true;
                    }
                }catch (Exception e){Log.d(TAG, "Job cancelled before being completed.");}
            }
            else {
                try {
                    long slp=1000*5*60;
                    Thread.sleep(slp);
                    doWork(jobParameters);
                    //needsReschedule=true;
                }catch (Exception e){Log.d(TAG, "Job cancelled before being completed.");}
            }
        }
        isWorking = false;
        jobFinished(jobParameters,needsReschedule);
    }
    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        Log.d(TAG, "Job cancelled before being completed.");
        jobCancelled = true;
        boolean needsReschedule = isWorking;
        jobFinished(jobParameters, needsReschedule);
        return needsReschedule;
    }
    public void scheduleJob(Context context)
    {
        JobScheduler jobScheduler = (JobScheduler)context.getSystemService(context.JOB_SCHEDULER_SERVICE);
        ComponentName componentName = new ComponentName(context.getPackageName(), MyJobScheduler.class.getName());
        JobInfo jobInfo = new JobInfo.Builder(12, componentName)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setBackoffCriteria(2,JobInfo.BACKOFF_POLICY_LINEAR)
                .setPersisted(true)
                .build();
        jobScheduler.schedule(jobInfo);
    }
}
