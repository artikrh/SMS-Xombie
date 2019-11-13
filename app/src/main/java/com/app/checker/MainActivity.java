package com.app.checker;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Temporary button to force test the service (makes one request per click)
        Button btnRequest = findViewById(R.id.btnRequest);
        btnRequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(getApplicationContext(), Fetcher.class);
                startService(i);
            }
        });

        // Uncomment the following lines so you don't have to restart the device each time you want to test it
        /*
        Intent invokeService = new Intent(getApplicationContext(), Fetcher.class);
        PendingIntent pintent = PendingIntent.getService(getApplicationContext(), 0, invokeService, 0);
        AlarmManager alarm = (AlarmManager) getApplicationContext().getSystemService(Context.ALARM_SERVICE);
        alarm.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), 60000, pintent);
        */
    }
}