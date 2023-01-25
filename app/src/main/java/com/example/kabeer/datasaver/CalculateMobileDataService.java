package com.example.kabeer.datasaver;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Build;
import android.util.Log;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import static android.net.TrafficStats.getMobileRxBytes;
import static android.net.TrafficStats.getMobileTxBytes;
import static android.net.TrafficStats.getTotalRxBytes;
import static android.net.TrafficStats.getTotalTxBytes;

public class CalculateMobileDataService extends JobService {
    private static final String TAG = CalculateMobileDataService.class.getSimpleName();
    boolean isWorking = false;
    boolean jobCancelled = false;
    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        isWorking = true;
        startWorkOnNewThread(jobParameters);
        return isWorking;
    }

    private void startWorkOnNewThread(final JobParameters jobParameters) {
        new Thread(new Runnable() {
            public void run() {
                doWork(jobParameters);
            }
        }).start();
    }
    public boolean isInternetWorking() {
        boolean success = false;
        try {
            URL url = new URL("http://clients3.google.com/generate_204");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("User-Agent", "Android");
            connection.setRequestProperty("Connection", "close");
            connection.setConnectTimeout(1500);
            connection.connect();
            success = connection.getResponseCode() == 204 && connection.getContentLength()==0;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return success;
    }
    private void doWork(JobParameters jobParameters)
    {
        float useddata,mobiledata,totaldata;
        boolean connected;

        SharedPreferences prefs=this.getSharedPreferences("UsedData",0);
        SharedPreferences check=this.getSharedPreferences("Check",0);
        ConnectivityManager manager = (ConnectivityManager)getSystemService(CalculateMobileDataService.CONNECTIVITY_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            final Network network = manager.getActiveNetwork();
            final NetworkCapabilities capabilities = manager.getNetworkCapabilities(network);
            connected=(capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED));
        }else{
            connected=isInternetWorking();
        }
        mobiledata=((getMobileRxBytes()+getMobileTxBytes())/ 1048576)+prefs.getFloat("lastmobiledata",0);
        totaldata=((getTotalRxBytes()+getTotalTxBytes())/ 1048576)+prefs.getFloat("lasttotaldata", 0);

        android.net.NetworkInfo net = manager.getActiveNetworkInfo();
        if(net!= null && net.getType() == ConnectivityManager.TYPE_MOBILE && connected){
            useddata=totaldata-mobiledata;
            prefs.edit().putFloat("currentwifidata", useddata).apply();
            useddata = mobiledata;
            prefs.edit().putFloat("currentmobiledata", useddata).apply();
        }else if(net!= null && net.getType() == ConnectivityManager.TYPE_WIFI && connected){
            if(check.getString("lastusedntw","").equalsIgnoreCase("wifi")){
               useddata = totaldata - prefs.getFloat("currentmobiledata", 0);
               prefs.edit().putFloat("currentwifidata",useddata).apply();
            }
        }
        isWorking = false;
        jobFinished(jobParameters,false);
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        Log.d(TAG, "Job cancelled before being completed.");
        jobCancelled = true;
        boolean needsReschedule = isWorking;
        jobFinished(jobParameters, needsReschedule);
        return needsReschedule;
    }

}
