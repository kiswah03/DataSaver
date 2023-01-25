package com.example.kabeer.datasaver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Build;
import android.widget.Toast;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import static android.net.TrafficStats.getMobileRxBytes;
import static android.net.TrafficStats.getMobileTxBytes;
import static android.net.TrafficStats.getTotalRxBytes;
import static android.net.TrafficStats.getTotalTxBytes;

public class InternetReciever extends BroadcastReceiver {

    float useddata,totaldata,mobiledata;
    SharedPreferences prefs,check;
    boolean connected=false;
    @Override
    public void onReceive(final Context context, Intent intent) {
        prefs=context.getSharedPreferences("UsedData",0);
        check=context.getSharedPreferences("Check",0);

        ConnectivityManager manager = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            final Network network = manager.getActiveNetwork();
            final NetworkCapabilities capabilities = manager.getNetworkCapabilities(network);
            connected=(capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED));
            checkConnection(context);
        }else{
            new Thread(new Runnable() {
                public void run() {
                    connected=isInternetWorking();
                    checkConnection(context);
                }
            }).start();
        }
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
    public void checkConnection(Context context)
    {
        ConnectivityManager manager = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        android.net.NetworkInfo net = manager.getActiveNetworkInfo();

        mobiledata=((getMobileRxBytes()+getMobileTxBytes())/ 1048576)+prefs.getFloat("lastmobiledata",0);
        totaldata=((getTotalRxBytes()+getTotalTxBytes())/ 1048576)+prefs.getFloat("lasttotaldata", 0);

        if(net!= null && net.getType() == ConnectivityManager.TYPE_MOBILE && connected){
            if(!check.getString("lastusedntw","").equalsIgnoreCase("Mobile"))  {
                if(prefs.getFloat("currentmobiledata",0)==0 || check.getString("lastusedntw","").equals(""))
                {
                    if(check.getBoolean("firstTime",false))
                    {   float lastused;
                        useddata=totaldata-prefs.getFloat("wifi",0);
                        lastused=useddata-prefs.getFloat("wlastUsed",0);
                        if(lastused>0.5) {
                            lastused += prefs.getFloat("wifiMonth", 0);
                            prefs.edit().putFloat("wifiMonth", lastused).apply();
                        }
                        prefs.edit().putFloat("wlastUsed",useddata).apply();
                    }
                    useddata=totaldata-(mobiledata+prefs.getFloat("wlastUsed",0));
                    prefs.edit().putFloat("wifi",useddata).apply();
                    prefs.edit().putFloat("mobile",mobiledata).apply();
                }
                useddata=totaldata-mobiledata;
                prefs.edit().putFloat("currentwifidata", useddata).apply();
                prefs.edit().putFloat("currentmobiledata",mobiledata).apply();
                check.edit().putString("lastusedntw","Mobile").apply();
            }else{
                useddata=totaldata-mobiledata;
                prefs.edit().putFloat("currentwifidata", useddata).apply();
                useddata=mobiledata;
                prefs.edit().putFloat("currentmobiledata",useddata).apply();
            }
            check.edit().putBoolean("entered",true).apply();
            check.edit().putString("usingntw","m").apply();
        }
        else if(net!= null && net.getType() == ConnectivityManager.TYPE_WIFI && connected){
            if(check.getBoolean("entered",false) && check.getString("lastusedntw","").equalsIgnoreCase("Mobile")){
                useddata = totaldata - prefs.getFloat("currentwifidata", 0);
                prefs.edit().putFloat("currentmobiledata", useddata).apply();
                check.edit().putString("lastusedntw","Wifi").apply();
            }else {
                useddata = totaldata - prefs.getFloat("currentmobiledata", 0);
                prefs.edit().putFloat("currentwifidata", useddata).apply();
                if (!check.getString("lastusedntw", "").equalsIgnoreCase("Wifi")) {
                    check.edit().putString("lastusedntw", "Wifi").apply();
                }
                check.edit().putBoolean("entered", true).apply();
            }
            check.edit().putString("usingntw","w").apply();
        }else if(check.getString("lastusedntw","").equalsIgnoreCase("Mobile"))
        {
            useddata = totaldata-prefs.getFloat("currentwifidata", 0);
            prefs.edit().putFloat("currentmobiledata",useddata).apply();
            check.edit().putBoolean("entered",false).apply();
            if(!connected){
                check.edit().putString("usingntw","no internet").apply();
            }

        }else if(check.getString("lastusedntw","").equalsIgnoreCase("Wifi"))
        {
            useddata = totaldata-prefs.getFloat("currentmobiledata", 0);
            prefs.edit().putFloat("currentwifidata",useddata).apply();
            check.edit().putBoolean("entered",false).apply();
            if(!connected){
                check.edit().putString("usingntw","no internet").apply();
            }
        }
    }
}
