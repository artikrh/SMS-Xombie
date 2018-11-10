package com.app.checker;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Button to force test the service; It is replaced with service invocation upon app runtime
        Button btnFetch= findViewById(R.id.btnTest);
        btnFetch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i= new Intent(getApplicationContext(), Fetcher.class);
                startService(i);
            }
        });

        Intent invokeService = new Intent(this, Fetcher.class);
        PendingIntent pintent = PendingIntent.getService(this, 0, invokeService, 0);
        AlarmManager alarm = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
        //alarm.cancel(pintent);
        alarm.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(),60000, pintent);

    }
}