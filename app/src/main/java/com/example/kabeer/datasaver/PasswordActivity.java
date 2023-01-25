package com.example.kabeer.datasaver;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;

public class PasswordActivity extends Activity {
    SharedPreferences check;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        createDialog();
        Intent it = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        sendBroadcast(it);
    }
    public void createDialog()
    {
        final EditText edittxt = new EditText(PasswordActivity.this);
        edittxt.setInputType(InputType.TYPE_CLASS_TEXT |InputType.TYPE_TEXT_VARIATION_PASSWORD);
        edittxt.setTransformationMethod(PasswordTransformationMethod.getInstance());
        edittxt.setTextSize(15);
        edittxt.setX(30);
        edittxt.setTypeface(Typeface.SERIF);
        edittxt.setTextColor(Color.BLACK);
        edittxt.setBackgroundResource(R.drawable.shape);
        FrameLayout container = new FrameLayout(PasswordActivity.this);
        FrameLayout.LayoutParams params = new  FrameLayout.LayoutParams(340, 50);
        //params.bottomMargin = getResources().getDimensionPixelSize(R.dimen.dialog_margin);
        edittxt.setLayoutParams(params);
        edittxt.requestFocus();
        container.addView(edittxt);
        final AlertDialog dialog = new AlertDialog.Builder(PasswordActivity.this,R.style.MyDialogTheme)
                .setView(container)
                .setTitle("Password")
                .setPositiveButton(android.R.string.ok, null) //Set to null. We override the onclick
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,int id) {
                                dialog.dismiss();
                                PasswordActivity.this.finish();
                            }
                        })
                .setCancelable(false)
                .create();

        dialog.setOnShowListener(new DialogInterface.OnShowListener() {

            @Override
            public void onShow(DialogInterface dialogInterface) {

                Button button = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE);
                button.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {
                        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(PasswordActivity.this);
                        String password=prefs.getString("password_text","");

                        if(password.equals(edittxt.getText().toString())) {
                            LockSwitchNotification ls = new LockSwitchNotification();
                            ls.notify(PasswordActivity.this, "Lock Off", R.drawable.lockoff);
                            check=PasswordActivity.this.getSharedPreferences("Check", 0);
                            check.edit().putBoolean("lockBtn", false).apply();

                            SharedPreferences autoLock= PreferenceManager.getDefaultSharedPreferences(PasswordActivity.this);
                            String time=autoLock.getString("autoLock_list","Never");
                            if(!time.equalsIgnoreCase("Never"))
                            {
                                scheduleAutoLock(PasswordActivity.this);
                            }
                            jobCancel(PasswordActivity.this);

                            dialog.dismiss();
                            PasswordActivity.this.finish();
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
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        if ((keyCode == KeyEvent.KEYCODE_BACK))
        {
            PasswordActivity.this.finish();
            return false;
        }
        return super.onKeyDown(keyCode, event);
    }
    @Override
    public void onPause() {
        super.onPause();
        PasswordActivity.this.finish();

    }
    @Override
    public void onWindowFocusChanged(boolean hasFocus)
    {
        PasswordActivity.this.finish();
    }

    public void scheduleAutoLock(Context context)
    {
        JobScheduler jobScheduler = (JobScheduler)context.getSystemService(PasswordActivity.JOB_SCHEDULER_SERVICE);
        ComponentName componentName = new ComponentName(context.getPackageName(), LockFrequencyScheduler.class.getName());
        JobInfo jobInfo = new JobInfo.Builder(15, componentName)
                .setMinimumLatency(2*60*1000)
                .setBackoffCriteria(5,JobInfo.BACKOFF_POLICY_LINEAR)
                .setPersisted(true)
                .build();
        jobScheduler.schedule(jobInfo);
    }

    public void jobCancel(Context context)
    {
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(context.JOB_SCHEDULER_SERVICE);
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
}
