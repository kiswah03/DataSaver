package com.example.kabeer.datasaver;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class Dialog extends Activity {

    Button ok;
    SharedPreferences check;
    TextView mTextView;
    String tone;
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dialog);
        setTitle("WARNING");
        setFinishOnTouchOutside(false);

        check = Dialog.this.getSharedPreferences("Check", 0);
        check.edit().putBoolean("running", true).apply();
        mTextView=findViewById(R.id.textView1);
        mTextView.setText(check.getString("message",""));

        ok = findViewById(R.id.ok_btn_id);
        ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean dataOn=false;

                ConnectivityManager manager = (ConnectivityManager)getSystemService(Dialog.this.CONNECTIVITY_SERVICE);
                android.net.NetworkInfo net = manager.getActiveNetworkInfo();
                if(net!= null && net.getType() == ConnectivityManager.TYPE_MOBILE && !check.getBoolean("warning",false)){
                    dataOn=true;
                }
                if(!dataOn) {
                    Dialog.this.finish();
                    check = Dialog.this.getSharedPreferences("Check", 0);
                    check.edit().putBoolean("running", false).apply();
                }
            }
        });

    }
    @Override
    protected void onStart()
    {
        super.onStart();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(Dialog.this);
        if (prefs.getBoolean("notifications_new_message", false)) {
            try{
                tone=prefs.getString("notifications_ringtone", "DEFAULT_SOUND");
                Uri path = Uri.parse(tone);
                Ringtone r = RingtoneManager.getRingtone(Dialog.this, path);
                if(r!=null) {
                    r.play();
                }
            }
            catch(Exception e){
               e.printStackTrace();
            }
        }
        if(prefs.getBoolean("notifications_new_message_vibrate",false))
        {
            Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            // Vibrate for 500 milliseconds
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(400, VibrationEffect.DEFAULT_AMPLITUDE));
            }else{
                //deprecated in API 26
                v.vibrate(400);
            }
        }
    }
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        if ((keyCode == KeyEvent.KEYCODE_BACK))
        {
            check = Dialog.this.getSharedPreferences("Check", 0);
            check.edit().putBoolean("running", false).apply();
            Dialog.this.finish();
            return false;
        }
        return super.onKeyDown(keyCode, event);
    }
    @Override
    public void onPause() {
        super.onPause();
        check = Dialog.this.getSharedPreferences("Check", 0);
        check.edit().putBoolean("running", false).apply();
        Dialog.this.finish();

    }

}
