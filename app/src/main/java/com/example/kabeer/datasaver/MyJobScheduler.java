package com.example.kabeer.datasaver;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import static android.net.TrafficStats.getMobileRxBytes;
import static android.net.TrafficStats.getMobileTxBytes;
import static android.net.TrafficStats.getTotalRxBytes;
import static android.net.TrafficStats.getTotalTxBytes;

public class MyJobScheduler extends JobService {
    private static final String TAG = MyJobScheduler.class.getSimpleName();
    boolean isWorking = false;
    boolean jobCancelled = false;
    float limit=0;
    int tlimit=0;
    SharedPreferences prefs,check;
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

    private float calculateUsedData(String prefname)
    {
        float totaldata,useddata=0,mobiledata;

        mobiledata=((getMobileRxBytes()+getMobileTxBytes())/ 1048576)+prefs.getFloat("lastmobiledata",0);
        totaldata=((getTotalRxBytes()+getTotalTxBytes())/ 1048576)+prefs.getFloat("lasttotaldata", 0);

        if(prefname.equals("wifi")) {
            if(check.getString("lastusedntw","").equalsIgnoreCase("Mobile")&& check.getBoolean("entered",false)) {
                mobiledata = totaldata - prefs.getFloat("currentwifidata", 0);
                prefs.edit().putFloat("currentmobiledata", mobiledata).apply();
                check.edit().putBoolean("entered",false).apply();
                useddata=prefs.getFloat("currentwifidata",0)-prefs.getFloat(prefname,0);
            }else{
                totaldata = totaldata - prefs.getFloat("currentmobiledata", 0);
                prefs.edit().putFloat("currentwifidata", totaldata).apply();
                useddata = totaldata - prefs.getFloat(prefname, 0);
            }
        }
        else if(prefname.equals("mobile"))
        {   if(check.getString("lastusedntw","").equalsIgnoreCase("Wifi")&& check.getBoolean("entered",false)){
               check.edit().putBoolean("entered",false).apply();
            }
            totaldata=totaldata-mobiledata;
            prefs.edit().putFloat("currentwifidata", totaldata).apply();
            useddata = mobiledata- prefs.getFloat(prefname, 0);
            prefs.edit().putFloat("currentmobiledata",mobiledata).apply();
        }
        return useddata;
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
    private String checkConnection()
    {
        String networkStatus;
        boolean connected;
        ConnectivityManager manager = (ConnectivityManager)getSystemService(MyJobScheduler.CONNECTIVITY_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            final Network network = manager.getActiveNetwork();
            final NetworkCapabilities capabilities = manager.getNetworkCapabilities(network);
            connected=(capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED));
        }else{
            connected=isInternetWorking();
        }

        android.net.NetworkInfo net = manager.getActiveNetworkInfo();

        if(net!=null && net.getType() == ConnectivityManager.TYPE_WIFI && connected) {
            networkStatus = "wifi";
        }else if(net!= null && net.getType() == ConnectivityManager.TYPE_MOBILE && connected){
            networkStatus = "mobileData";
        }else{
            networkStatus="noNetwork";
        }
        return networkStatus;
    }
    private boolean checkSettings(String network)
    {
       Boolean lockon=check.getBoolean("lockBtn",false);
       if(lockon) {
           SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(MyJobScheduler.this);
           if (pref.getBoolean("wifi_switch", false) && network.equalsIgnoreCase("wifi")) {
               limit=Float.parseFloat(pref.getString("wifi_text", "0"));
               if(pref.getString("wifi_list","MB").equalsIgnoreCase("GB")) {
                   limit = limit*1024;
               }
               if (pref.getBoolean("time_switch", false)) {
                   tlimit = Integer.parseInt(pref.getString("time_list", "15"));
               }
               return true;
           } else if (pref.getBoolean("mobile_switch", false) && network.equalsIgnoreCase("mobileData")) {
               limit = Float.parseFloat(pref.getString("mobile_text", "0"));
               if(pref.getString("mobile_list","MB").equalsIgnoreCase("GB")) {
                   limit = limit*1024;
               }
               if (pref.getBoolean("time_switch", false)) {
                   tlimit = Integer.parseInt(pref.getString("time_list", "15"));
               }
               return true;
           } else if (pref.getBoolean("time_switch", false) && !network.equalsIgnoreCase("noNetwork")) {
               tlimit = Integer.parseInt(pref.getString("time_list", "15"));
               return true;
           } else {
               return false;
           }
       }else {return false;}
    }

    private void doWork(JobParameters jobParameters)
    {
        float value=0;
        boolean state,lockOn;

        prefs = this.getSharedPreferences("UsedData", 0);
        check = this.getSharedPreferences("Check", 0);

        final String conn=checkConnection();
        lockOn=checkSettings(conn);

        if(lockOn) {
            if (conn.equalsIgnoreCase("wifi")) {
                value = calculateUsedData("wifi");
            } else if (conn.equalsIgnoreCase("mobileData")) {
                value = calculateUsedData("mobile");
            }

            boolean tcondition=(tlimit>0 && prefs.getInt("time",0)>=tlimit);

            if (value >= limit || tcondition) {
                state = check.getBoolean("running", false);
                check.edit().putBoolean("warning",false).apply();
                if (conn.equalsIgnoreCase("wifi") && !state) {
                    if(tcondition)
                    {
                        check.edit().putString("message","Time limit for using internet is over.").apply();
                    }
                    else
                    {
                        check.edit().putString("message","Exceeded wifi data limit.").apply();
                    }
                    Intent d = new Intent(MyJobScheduler.this, Dialog.class);
                    d.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(d);
                    WifiManager Wifi = (WifiManager) this.getSystemService(MyJobScheduler.WIFI_SERVICE);
                    Wifi.setWifiEnabled(false);

                } else if (conn.equalsIgnoreCase("mobileData") && !state) {
                    if(tcondition)
                    {
                        check.edit().putString("message","Time limit for using internet is over.").apply();
                    }
                    else
                    {
                        check.edit().putString("message","Exceeded data limit. Please turn off mobile data.").apply();
                    }
                    Intent i = new Intent(MyJobScheduler.this, Dialog.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(i);
                }

            } else {
                    String msg="";
                    state = check.getBoolean("running", false);
                    if((limit-value)<=50 && !check.getBoolean("warning",false)){
                        msg="You have only "+String.valueOf(limit-value)+" MB left till you reach data limit.";
                    }else if(tlimit>0 && (tlimit-prefs.getInt("time",0))<=2 && !check.getBoolean("warning",false)){
                        msg="You have only "+String.valueOf(tlimit-prefs.getInt("time",0))+"  minute left to use internet.";
                    }
                    if(!msg.equals("") && !state){
                        check.edit().putBoolean("warning",true).apply();
                        check.edit().putString("message",msg).apply();
                        Intent i = new Intent(MyJobScheduler.this, Dialog.class);
                        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(i);
                    }
                try {
                    long slp = 1000 * 60 * 2;
                    Thread.sleep(slp);
                    if(tlimit>0) {
                        prefs = this.getSharedPreferences("UsedData", 0);
                        prefs.edit().putInt("time", prefs.getInt("time", 0) + 2).apply();
                    }
                } catch (Exception e) {Log.d(TAG, "An error occured during execution of job.");}
            }
        }else
        {try {
                long slp = 1000 * 60 * 15;
                Thread.sleep(slp);
            } catch (Exception e) {Log.d(TAG, "An error occured during execution of job.");}
        }
        isWorking = true;
        jobFinished(jobParameters,true);
    }

    // Called if the job was cancelled before being finished
    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        Log.d(TAG, "Job cancelled before being completed.");
        jobCancelled = true;
        boolean needsReschedule = true;
        jobFinished(jobParameters, needsReschedule);
        return needsReschedule;
    }

}
