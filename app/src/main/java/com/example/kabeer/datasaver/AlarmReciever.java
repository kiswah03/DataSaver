package com.example.kabeer.datasaver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.preference.PreferenceManager;

import java.util.Calendar;
import java.util.GregorianCalendar;

import static android.net.TrafficStats.getMobileRxBytes;
import static android.net.TrafficStats.getMobileTxBytes;
import static android.net.TrafficStats.getTotalRxBytes;
import static android.net.TrafficStats.getTotalTxBytes;

public class AlarmReciever extends BroadcastReceiver {


    float mobiledata,totaldata;
    public SharedPreferences prefs,check;

    @Override
    public void onReceive(Context context, Intent intent)
    {
        float useddata;
        prefs = context.getSharedPreferences("UsedData", 0);
        check = context.getSharedPreferences("Check", 0);

        mobiledata=((getMobileRxBytes()+getMobileTxBytes())/ 1048576)+prefs.getFloat("lastmobiledata",0); ;
        totaldata=((getTotalRxBytes()+getTotalTxBytes())/ 1048576)+prefs.getFloat("lasttotaldata", 0);

        useddata = calculatedataused("Wifi")-(prefs.getFloat("wifi",0)+ prefs.getFloat("wlastUsed", 0));
        if(useddata>0.5) {
            useddata += prefs.getFloat("wifiMonth", 0);
            prefs.edit().putFloat("wifiMonth", useddata).apply();
        }
        useddata = calculatedataused("Mobile")-(prefs.getFloat("mobile",0)+ prefs.getFloat("mlastUsed", 0));
        if(useddata>0.5) {
            useddata += prefs.getFloat("mobileMonth", 0);
            prefs.edit().putFloat("mobileMonth", useddata).apply();
        }
        prefs.edit().putFloat("wifi", calculatedataused("Wifi")).apply();
        prefs.edit().putFloat("mobile",calculatedataused("Mobile")).apply();

        setData(context);
        prefs.edit().putInt("time", 0).apply();
        prefs.edit().putFloat("wlastUsed",0).apply();
        prefs.edit().putFloat("mlastUsed",0).apply();

        boolean test=checkDay(context);
        if(test)
        {
            prefs.edit().putFloat("wifiMonth", 0).apply();

            prefs.edit().putFloat("mobileMonth", 0).apply();
        }
    }
    private float calculatedataused(String ntw)
    {
        float dataused=0;
        if(check.getString("lastusedntw","").equalsIgnoreCase("Wifi")){
            if(ntw.equals("Wifi")) {
                if(check.getString("usingntw","no internet").equalsIgnoreCase("w")){
                    dataused= totaldata-prefs.getFloat("currentmobiledata",0);
                }else{dataused=prefs.getFloat("currentwifidata",0);}
            }else if(ntw.equals("Mobile")){
                dataused=prefs.getFloat("currentmobiledata", 0);}
        }else if(check.getString("lastusedntw","").equalsIgnoreCase("Mobile")){
            if(ntw.equals("Wifi")) {
                dataused = prefs.getFloat("currentwifidata", 0);
            }else if(ntw.equals("Mobile")){
                if(check.getString("usingntw","no internet").equalsIgnoreCase("m")) {
                    dataused = totaldata - prefs.getFloat("currentwifidata", 0);
                }else{dataused=prefs.getFloat("currentmobiledata",0);}}
        }else if(check.getString("lastusedntw","").equals("")){
            if(ntw.equals("Wifi")) {
                dataused = totaldata-mobiledata;
            }else if(ntw.equals("Mobile")){
                dataused=mobiledata;}
        }
        return dataused;
    }
    private void setData(Context context)
    {
        float dataused=0;
        ConnectivityManager manager = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        android.net.NetworkInfo net = manager.getActiveNetworkInfo();
        if(net!=null && net.getType() == ConnectivityManager.TYPE_WIFI){
            if(check.getString("usingntw","no internet").equalsIgnoreCase("w")) {
                dataused = totaldata - prefs.getFloat("currentmobiledata", 0);
                prefs.edit().putFloat("currentwifidata", dataused).apply();
            }
        }else if(net!= null && net.getType() == ConnectivityManager.TYPE_MOBILE) {
            if (check.getString("usingntw", "no internet").equalsIgnoreCase("m")) {
                dataused=totaldata-mobiledata;
                prefs.edit().putFloat("currentwifidata", dataused).apply();
                dataused = mobiledata;
                prefs.edit().putFloat("currentmobiledata", dataused).apply();
            }
        }
    }
    private boolean checkDay(Context context)
    {
        Calendar cal = Calendar.getInstance();
        GregorianCalendar gcal = new GregorianCalendar();
        boolean leap=gcal.isLeapYear(Calendar.YEAR);
        SharedPreferences day=PreferenceManager.getDefaultSharedPreferences(context);
        if(day.getString("mnthStart_text","1").equals(String.valueOf(cal.get(Calendar.DAY_OF_MONTH))))
        {
            return true;
        }
        else if(day.getString("mnthStart_text","1").equals("31") && String.valueOf(cal.get(Calendar.MONTH)).matches("3|5|7|10|12")
                && cal.get(Calendar.DAY_OF_MONTH)==1)
        {
            return true;
        }
        else if(day.getString("mnthStart_text","1").equals("30") && cal.get(Calendar.MONTH)==3 && cal.get(Calendar.DAY_OF_MONTH)==1)
        {
                return true;

        }else if(day.getString("mnthStart_text","1").equals("29") && !leap && cal.get(Calendar.MONTH)==3 && cal.get(Calendar.DAY_OF_MONTH)==1)
        {
            return true;
        }
        else {
            return false;}
    }
}
