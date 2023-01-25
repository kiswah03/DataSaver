package com.example.kabeer.datasaver;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.PasswordTransformationMethod;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.support.v7.widget.ShareActionProvider;
import android.widget.ProgressBar;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.NumberFormat;
import java.util.Calendar;

import static android.net.TrafficStats.getMobileRxBytes;
import static android.net.TrafficStats.getMobileTxBytes;
import static android.net.TrafficStats.getTotalRxBytes;
import static android.net.TrafficStats.getTotalTxBytes;

public class DataSaver extends AppCompatActivity {

    TextView wTextView, mTextView,wmtextView,mmtextView,wifileft,wifilimit,timeleft,timelimit,mobileleft,mobilelimit;
    public SharedPreferences prefs,check;
    boolean connected=false;
    float useddata;
    ProgressBar wpb,tpb,mpb;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_saver);
        addTabHost();

        check = getApplicationContext().getSharedPreferences("Check", 0);
        prefs=getApplicationContext().getSharedPreferences("UsedData", 0);
        if(!check.getBoolean("firstTime",false)) {
            ConnectivityManager manager = (ConnectivityManager)getSystemService(DataSaver.CONNECTIVITY_SERVICE);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                final Network network = manager.getActiveNetwork();
                final NetworkCapabilities capabilities = manager.getNetworkCapabilities(network);
                connected=(capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED));
                firstTime();
            }else{
                new Thread(new Runnable() {
                    public void run() {
                        connected=isInternetWorking();
                        firstTime();
                    }
                }).start();
            }

            float usedData;
            usedData=calculateTotalData("Wifi");
            prefs.edit().putFloat("wifi",usedData).apply();

            usedData=calculateTotalData("Mobile");
            prefs.edit().putFloat("mobile",usedData).apply();

            LockSwitchNotification ls=new LockSwitchNotification();
            ls.notify(getApplicationContext(),"Lock Off",R.drawable.lockoff);

            check.edit().putBoolean("firstTime", true).apply();
        }

        wTextView=findViewById(R.id.datatext);
        mTextView=findViewById(R.id.mdatatext);
        wmtextView=findViewById(R.id.wmdatatext);
        mmtextView=findViewById(R.id.mmdatatext);
        //Calculating Data
        NumberFormat nf = NumberFormat.getInstance();
        nf.setMinimumFractionDigits(1);
        nf.setMaximumFractionDigits(2);
        float lastUsed;


        //Wifi Data-------------------------------------------------------------------------------------
        useddata=calculateTotalData("Wifi")-prefs.getFloat("wifi",0);
        if(useddata>=1024) {
            wTextView.setText(stringBuilder(nf.format(useddata/1024),"GB","#FF1EBACC"));
        }
        else
        {
            wTextView.setText(stringBuilder(nf.format(useddata),"MB","#FF1EBACC"));
        }
        lastUsed=useddata;
        useddata = useddata- prefs.getFloat("wlastUsed", 0);
        if(useddata>0.5) {
            useddata += prefs.getFloat("wifiMonth", 0);
            prefs.edit().putFloat("wifiMonth", useddata).apply();
            prefs.edit().putFloat("wlastUsed", lastUsed).apply();
        }
        else {
            useddata=prefs.getFloat("wifiMonth",0);
        }
        if (useddata >= 1048576) {
            wmtextView.setText(stringBuilder(nf.format(useddata/1048576),"TB","#ff669900"));
        } else if (useddata >= 1024) {
            wmtextView.setText(stringBuilder(nf.format(useddata/1024),"GB","#ff669900"));
        } else {
            wmtextView.setText(stringBuilder(nf.format(useddata),"MB","#ff669900"));
        }

        //Mobile Data-----------------------------------------------------------------------------------
        useddata=calculateTotalData("Mobile")-prefs.getFloat("mobile",0);
        if(useddata>=1024) {
            mTextView.setText(stringBuilder(nf.format(useddata/1024),"GB","#FF1EBACC"));
        }
        else
        {
            mTextView.setText(stringBuilder(nf.format(useddata),"MB","#FF1EBACC"));
        }
        lastUsed=useddata;
        useddata=useddata-prefs.getFloat("mlastUsed",0);
        if(useddata>0.5) {
            useddata += prefs.getFloat("mobileMonth", 0);
            prefs.edit().putFloat("mobileMonth", useddata).apply();
            prefs.edit().putFloat("mlastUsed", lastUsed).apply();
        }
        else{
            useddata=prefs.getFloat("mobileMonth", 0);
        }
        if (useddata >= 1048576) {
            mmtextView.setText(stringBuilder(nf.format(useddata/1048576),"TB","#ff669900"));
        } else if (useddata >= 1024) {
            mmtextView.setText(stringBuilder(nf.format(useddata/1024),"GB","#ff669900"));
        } else {
            mmtextView.setText(stringBuilder(nf.format(useddata),"MB","#ff669900"));
        }
        setProgressBar(DataSaver.this);
        setData();
        //scheduling alarm for 12 am
        scheduleAlarmDaily();

        //scheduling job for controling wifi and automatic Lock
        if(!check.getBoolean("lockBtn",false))
        {
            LockSwitchNotification ls=new LockSwitchNotification();
            ls.notify(getApplicationContext(),"Lock Off",R.drawable.lockoff);
            SharedPreferences autoLock= PreferenceManager.getDefaultSharedPreferences(this);
            String time=autoLock.getString("autoLock_list","Never");
            if(!time.equalsIgnoreCase("Never"))
            {
                scheduleAutoLock(this);
            }
        }
        else
        {
            LockSwitchNotification ls=new LockSwitchNotification();
            ls.notify(getApplicationContext(),"Lock On",R.drawable.lockon);
            scheduleJob();
        }
        scheduleCalculateData();
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
    private void firstTime(){
        ConnectivityManager manager = (ConnectivityManager)getSystemService(DataSaver.CONNECTIVITY_SERVICE);
        android.net.NetworkInfo net = manager.getActiveNetworkInfo();

        float mobiledata=((getMobileRxBytes()+getMobileTxBytes())/ 1048576)+prefs.getFloat("lastmobiledata",0);
        float totaldata=((getTotalRxBytes()+getTotalTxBytes())/ 1048576)+prefs.getFloat("lasttotaldata", 0);

        if(net!= null && net.getType() == ConnectivityManager.TYPE_MOBILE && connected){
            prefs.edit().putFloat("currentmobiledata",mobiledata).apply();
            prefs.edit().putFloat("currentwifidata",totaldata-mobiledata).apply();
            check.edit().putString("lastusedntw","Mobile").apply();
            check.edit().putString("usingntw","m").apply();
            check.edit().putBoolean("entered",true).apply();

        }else if(net!= null && net.getType() == ConnectivityManager.TYPE_WIFI && connected){
            prefs.edit().putFloat("currentwifidata",totaldata-mobiledata).apply();
            check.edit().putString("lastusedntw","Wifi").apply();
            check.edit().putString("usingntw","w").apply();
            check.edit().putBoolean("entered",true).apply();
        }
    }
    public Spanned stringBuilder(String a, String b,String color)
    {
        SpannableStringBuilder f=new SpannableStringBuilder(b);
        f.setSpan(new AbsoluteSizeSpan(30),0,2, 0);
        f.setSpan(new ForegroundColorSpan(Color.parseColor(color)),0,2,0);
        SpannableStringBuilder d=new SpannableStringBuilder(a);
        d.append(" ");
        Spanned sp = (Spanned)TextUtils.concat(d,f);
        return sp;
    }
    public void setProgressBar(Context context)
    {
        float limit,tlimit,usedData,left,setLimit;
        String format="MB";
        SharedPreferences gprefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs=getApplicationContext().getSharedPreferences("UsedData", 0);
        check=getApplicationContext().getSharedPreferences("Check", 0);

        wpb=findViewById(R.id.wifiProgressBar);
        wifileft=findViewById(R.id.wifiLeftTxt);
        wifilimit=findViewById(R.id.wifiLimitTxt);
        tpb=findViewById(R.id.wTimeProgressBar);
        timeleft=findViewById(R.id.wTimeLeftTxt);
        timelimit=findViewById(R.id.wTimeLimitTxt);
        mpb=findViewById(R.id.mobileProgressBar);
        mobileleft=findViewById(R.id.mobileLeftTxt);
        mobilelimit=findViewById(R.id.mobileLimitTxt);

        NumberFormat nf = NumberFormat.getInstance();
        nf.setMinimumFractionDigits(1);
        nf.setMaximumFractionDigits(1);

        //Wifi Progress Bar
        if (gprefs.getBoolean("wifi_switch", false)) {
            limit=Float.parseFloat(gprefs.getString("wifi_text", "0"));
            setLimit=limit;
            if(gprefs.getString("wifi_list","MB").equalsIgnoreCase("GB")) {
                limit = limit*1024;
                format="GB";
            }
            usedData=calculateTotalData("Wifi")-prefs.getFloat("wifi",0);
            if(usedData<limit)
            {
                int pgs=(int)((usedData/limit)*100);
                wpb.setProgress(pgs);
                wifilimit.setText(nf.format(setLimit)+" "+format);
                if((limit-usedData)>=1024)
                {
                    left=(limit-usedData)/1024;
                }
                else {
                    left=limit-usedData;
                    format="MB";
                }
                wifileft.setText(nf.format(left)+" "+format+" left");
            }else{
                wpb.setProgress(100);
                wifileft.setText("0 MB left");
                wifilimit.setText(nf.format(setLimit)+" "+format);
            }
        }else
        {
            wifileft.setText("");
            wifilimit.setText("Data limit not set");
        }
        // Mobile Progress Bar
        if (gprefs.getBoolean("mobile_switch", false)) {
            limit = Float.parseFloat(gprefs.getString("mobile_text", "0"));
            setLimit=limit;
            if (gprefs.getString("mobile_list", "MB").equalsIgnoreCase("GB")) {
                limit = limit * 1024;
                format="GB";
            }
            usedData=calculateTotalData("Mobile")-prefs.getFloat("mobile",0);
            if(usedData<limit)
            {
                int pgs=(int)((usedData/limit)*100);
                mpb.setProgress(pgs);
                mobilelimit.setText(nf.format(setLimit)+" "+format);
                if((limit-usedData)>=1024)
                {
                    left=(limit-usedData)/1024;
                }
                else {
                    left=limit-usedData;
                    format="MB";
                }
                mobileleft.setText(nf.format(left)+" "+format+" left");
            }else{
                mpb.setProgress(100);
                mobileleft.setText("0 MB left");
                mobilelimit.setText(nf.format(setLimit)+" "+format);
            }
        }else
        {
            mobileleft.setText("");
            mobilelimit.setText("Data limit not set");
        }
        //Time Progress Bar
        nf.setMinimumFractionDigits(0);
        nf.setMaximumFractionDigits(0);
        if (gprefs.getBoolean("time_switch", false)) {
            tlimit = Integer.parseInt(gprefs.getString("time_list", "15"));
            int usedtime=prefs.getInt("time",0);
            if(usedtime<tlimit)
            {
                int pgs=(int)((usedtime/tlimit)*100);
                tpb.setProgress(pgs);
                timeleft.setText(nf.format(tlimit-usedtime)+" min left");
                timelimit.setText(nf.format(tlimit)+" min");
            }
            else{
                tpb.setProgress(100);
                timeleft.setText("0 min left");
                timelimit.setText(nf.format(tlimit)+" min");
            }
        }else{
            timeleft.setText("");
            timelimit.setText("Time limit not set");
        }

        tpb=findViewById(R.id.mTimeProgressBar);
        timeleft=findViewById(R.id.mTimeLeftTxt);
        timelimit=findViewById(R.id.mTimeLimitTxt);
        if (gprefs.getBoolean("time_switch", false)) {
            tlimit = Integer.parseInt(gprefs.getString("time_list", "15"));
            int usedtime=prefs.getInt("time",0);
            if(usedtime<tlimit)
            {
                int pgs=(int)((usedtime/tlimit)*100);
                tpb.setProgress(pgs);
                timeleft.setText(nf.format(tlimit-usedtime)+" min left");
                timelimit.setText(nf.format(tlimit)+" min");
            }
            else{
                tpb.setProgress(100);
                timeleft.setText("0 min left");
                timelimit.setText(nf.format(tlimit)+" min");
            }
        }else{
            timeleft.setText("");
            timelimit.setText("Time limit not set");
        }
    }
    public void scheduleAlarmDaily()
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
        Intent intent = new Intent(this, AlarmReciever.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, DATA_FETCHER_RC,intent, PendingIntent.FLAG_UPDATE_CURRENT);
        //Create an alarm manager
        AlarmManager mAlarmManager = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
        mAlarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP,cal, AlarmManager.INTERVAL_DAY, pendingIntent);
    }
    public float calculateTotalData(String nw)
    {
        float totaldata,mobiledata,data=0;
        mobiledata=((getMobileRxBytes()+getMobileTxBytes())/ 1048576)+prefs.getFloat("lastmobiledata",0);
        totaldata=((getTotalRxBytes()+getTotalTxBytes())/ 1048576)+prefs.getFloat("lasttotaldata", 0);

        if(check.getString("lastusedntw","").equalsIgnoreCase("Wifi")){
            if (nw.equalsIgnoreCase("Wifi")) {
                if(check.getString("usingntw","no internet").equalsIgnoreCase("w")){
                   data= totaldata-prefs.getFloat("currentmobiledata",0);
                }else{data=prefs.getFloat("currentwifidata",0);}
            } else if (nw.equalsIgnoreCase("Mobile")) {
                data = prefs.getFloat("currentmobiledata",0);
            }
        }else if(check.getString("lastusedntw","").equalsIgnoreCase("Mobile")) {
            if (nw.equalsIgnoreCase("Wifi")) {
                data= prefs.getFloat("currentwifidata",0);
            } else if (nw.equalsIgnoreCase("Mobile")) {
                if(check.getString("usingntw","no internet").equalsIgnoreCase("m")) {
                    data = totaldata - prefs.getFloat("currentwifidata", 0);
                }else{data=prefs.getFloat("currentmobiledata",0);}
            }
        }else if(check.getString("lastusedntw","").equals("")){
            if (nw.equalsIgnoreCase("Wifi")) {
                data= totaldata-mobiledata;
            } else if (nw.equalsIgnoreCase("Mobile")) {
                data = mobiledata;
            }
        }
        //Toast.makeText(getApplicationContext(), mobiledata+" "+prefs.getFloat("currentmobiledata",0), Toast.LENGTH_LONG).show();
        return data;
    }
    private void setData()
    {
        float totaldata,dataused=0,mobiledata;
        totaldata=((getTotalRxBytes()+getTotalTxBytes())/ 1048576)+prefs.getFloat("lasttotaldata", 0);
        mobiledata=((getMobileRxBytes()+getMobileTxBytes())/ 1048576)+prefs.getFloat("lastmobiledata",0);
        ConnectivityManager manager = (ConnectivityManager)getSystemService(DataSaver.CONNECTIVITY_SERVICE);
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
    public void scheduleJob()
    {
        JobScheduler jobScheduler = (JobScheduler)getSystemService(getApplicationContext().JOB_SCHEDULER_SERVICE);
        boolean hasBeenScheduled = false;

        for (JobInfo jobInfo : jobScheduler.getAllPendingJobs()) {
            if (jobInfo.getId() == 12) {
                hasBeenScheduled = true;
                break;
            }
        }
        if(!hasBeenScheduled) {
            ConnectivityManager manager = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
            android.net.NetworkInfo net = manager.getActiveNetworkInfo();

            if(net!=null && (net.getType() == ConnectivityManager.TYPE_WIFI || net.getType()==ConnectivityManager.TYPE_MOBILE))
            {
                ComponentName componentName = new ComponentName(getPackageName(), MyJobScheduler.class.getName());
                JobInfo jobInfo = new JobInfo.Builder(12, componentName)
                        .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                        .setOverrideDeadline(1000*3)
                        .setBackoffCriteria(2,JobInfo.BACKOFF_POLICY_LINEAR)
                        .setPersisted(true)
                        .build();
                jobScheduler.schedule(jobInfo);
            }else {
                ComponentName componentName = new ComponentName(getPackageName(), MyJobScheduler.class.getName());
                JobInfo jobInfo = new JobInfo.Builder(12, componentName)
                        .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                        .setBackoffCriteria(3, JobInfo.BACKOFF_POLICY_LINEAR)
                        .setPersisted(true)
                        .build();
                jobScheduler.schedule(jobInfo);
            }
        }
    }
    public void scheduleAutoLock(Context context)
    {
        JobScheduler jobScheduler = (JobScheduler)getSystemService(context.JOB_SCHEDULER_SERVICE);
        boolean hasBeenScheduled = false;

        for (JobInfo jobInfo : jobScheduler.getAllPendingJobs()) {
            if (jobInfo.getId() == 15) {
                hasBeenScheduled = true;
                break;
            }
        }
        if(!hasBeenScheduled) {
            ComponentName componentName = new ComponentName(context.getPackageName(), LockFrequencyScheduler.class.getName());
            JobInfo jobInfo = new JobInfo.Builder(15, componentName)
                    .setMinimumLatency(2 * 1000 * 60)
                    .setBackoffCriteria(5, JobInfo.BACKOFF_POLICY_LINEAR)
                    .setPersisted(true)
                    .build();
            jobScheduler.schedule(jobInfo);
        }
    }
    public void scheduleCalculateData()
    {
        JobScheduler jobScheduler = (JobScheduler)getSystemService(getApplicationContext().JOB_SCHEDULER_SERVICE);
            boolean hasBeenScheduled = false;

            for (JobInfo jobInfo : jobScheduler.getAllPendingJobs()) {
                if (jobInfo.getId() == 17) {
                    hasBeenScheduled = true;
                    break;
                }
            }
            if(!hasBeenScheduled)
            {
            ComponentName componentName = new ComponentName(getPackageName(), CalculateMobileDataService.class.getName());
            JobInfo jobInfo = new JobInfo.Builder(17, componentName)
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .setPeriodic(1000*60*15)
                    .setPersisted(true)
                    .build();
            jobScheduler.schedule(jobInfo);
        }
    }

    public void createDialog()
    {
        final EditText edittxt = new EditText(DataSaver.this);
        edittxt.setInputType(InputType.TYPE_CLASS_TEXT |InputType.TYPE_TEXT_VARIATION_PASSWORD);
        edittxt.setTransformationMethod(PasswordTransformationMethod.getInstance());
        edittxt.setTextSize(15);
        edittxt.setY(20);
        edittxt.setX(30);
        edittxt.setTypeface(Typeface.SERIF);
        edittxt.setTextColor(Color.BLACK);
        FrameLayout container = new FrameLayout(DataSaver.this);
        FrameLayout.LayoutParams params = new  FrameLayout.LayoutParams(370, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = getResources().getDimensionPixelSize(R.dimen.dialog_margin);
        edittxt.setLayoutParams(params);
        container.addView(edittxt);
        final AlertDialog dialog = new AlertDialog.Builder(DataSaver.this)
                .setView(container)
                .setTitle("Password")
                .setPositiveButton(android.R.string.ok, null) //Set to null. We override the onclick
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,int id) {
                                dialog.dismiss();
                            }
                        })
                .setCancelable(false)
                .create();

        dialog.setOnShowListener(new DialogInterface.OnShowListener() {

            @Override
            public void onShow(DialogInterface dialogInterface) {

                Button button = (dialog).getButton(AlertDialog.BUTTON_POSITIVE);
                button.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {
                        SharedPreferences gprefs = PreferenceManager.getDefaultSharedPreferences(DataSaver.this);
                        String password=gprefs.getString("password_text","");
                        if(password.equals(edittxt.getText().toString())) {
                            Intent i = new Intent(DataSaver.this, SettingsActivity.class);
                            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(i);
                            dialog.dismiss();
                        }
                        else
                        {
                            Toast.makeText(getApplicationContext(),"Incorrect password. Please retype password.", Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }
        });
        dialog.show();
    }
    private ShareActionProvider mShareActionProvider;
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menurs,menu);
        // Locate MenuItem with ShareActionProvider
        MenuItem item = menu.findItem(R.id.share);

        // Fetch and store ShareActionProvider
        mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(item);
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("text/plain");
        i.putExtra(Intent.EXTRA_SUBJECT, "Data Saver");
        String sAux = "\nData Saver is a parental control app that ensures your teen doesn’t hog all the data in your family cell phone plan\n\n";
        sAux = sAux + "https://play.google.com/store/apps/details?id=the.package.id \n\n";
        i.putExtra(Intent.EXTRA_TEXT, sAux);
        setShareIntent(i);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.settings:
                prefs = PreferenceManager.getDefaultSharedPreferences(DataSaver.this);
                if(prefs.getBoolean("password_switch",false))
                {
                  createDialog();
                }
                else
                {
                    Intent i = new Intent(this, SettingsActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(i);
                }
                break;

            case R.id.refresh:
                //Calculating Data
                NumberFormat nf = NumberFormat.getInstance();
                nf.setMinimumFractionDigits(1);
                nf.setMaximumFractionDigits(2);
                float lastUsed;

                prefs=getApplicationContext().getSharedPreferences("UsedData", 0);
                check=getApplicationContext().getSharedPreferences("Check",0);

                //Wifi Data-----------------------------------------------------------------------------------------
                useddata=calculateTotalData("Wifi")-prefs.getFloat("wifi",0);
                if(useddata>=1024) {
                    wTextView.setText(stringBuilder(nf.format(useddata/1024),"GB","#FF1EBACC"));
                }
                else
                {
                    wTextView.setText(stringBuilder(nf.format(useddata),"MB","#FF1EBACC"));
                }
                lastUsed=useddata;
                useddata=useddata-prefs.getFloat("wlastUsed",0);
                if(useddata>0.5) {
                    useddata += prefs.getFloat("wifiMonth", 0);
                    prefs.edit().putFloat("wifiMonth", useddata).apply();
                    prefs.edit().putFloat("wlastUsed",lastUsed).apply();
                    if (useddata >= 1048576) {
                        wmtextView.setText(stringBuilder(nf.format(useddata/1048576),"TB","#ff669900"));
                    } else if (useddata >= 1024) {
                        wmtextView.setText(stringBuilder(nf.format(useddata/1024),"GB","#ff669900"));
                    } else {
                        wmtextView.setText(stringBuilder(nf.format(useddata),"MB","#ff669900"));
                    }
                }
                //Mobile Data-----------------------------------------------------------------------------------------
                useddata=calculateTotalData("Mobile")-prefs.getFloat("mobile",0);
                if(useddata>=1024) {
                    mTextView.setText(stringBuilder(nf.format(useddata/1024),"GB","#FF1EBACC"));
                }
                else
                {
                    mTextView.setText(stringBuilder(nf.format(useddata),"MB","#FF1EBACC"));
                }
                lastUsed=useddata;
                useddata=useddata-prefs.getFloat("mlastUsed",0);
                if(useddata>0.5) {
                    useddata += prefs.getFloat("mobileMonth", 0);
                    prefs.edit().putFloat("mobileMonth", useddata).apply();
                    prefs.edit().putFloat("mlastUsed", lastUsed).apply();
                    if (useddata >= 1048576) {
                        mmtextView.setText(stringBuilder(nf.format(useddata/1048576),"TB","#ff669900"));
                    } else if (useddata >= 1024) {
                        mmtextView.setText(stringBuilder(nf.format(useddata/1024),"GB","#ff669900"));
                    } else {
                        mmtextView.setText(stringBuilder(nf.format(useddata),"MB","#ff669900"));
                    }
                }
                setProgressBar(DataSaver.this);
                setData();
                Toast.makeText(getApplicationContext(), "Data usage updated successfully!", Toast.LENGTH_LONG).show();
                break;
        }
        return true;
    }

    // Call to update the share intent
    private void setShareIntent(Intent shareIntent) {
        if (mShareActionProvider != null) {
            mShareActionProvider.setShareIntent(shareIntent);
        }
    }
    public void addTabHost()
    {
        TabHost host = findViewById(R.id.tabHost);
        host.setup();

        //Tab 1
        TabHost.TabSpec spec = host.newTabSpec("Wifi");
        spec.setContent(R.id.tab1);
        spec.setIndicator("Wifi");
        host.addTab(spec);

        //Tab 2
        spec = host.newTabSpec("Mobile");
        spec.setContent(R.id.tab2);
        spec.setIndicator("Mobile");
        host.addTab(spec);
    }
}
