package com.app.checker;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class Autostart extends BroadcastReceiver
{
    public Autostart(){
        // To prevent java.lang.InstantiationException
    }

    public void onReceive(Context context, Intent arg1)
    {
        Intent invokeService = new Intent(context, Fetcher.class);
        PendingIntent pintent = PendingIntent.getService(context, 0, invokeService, 0);
        AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if(alarm != null){
            alarm.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(),60000, pintent);
        }

        Log.i("Autostart", "Service Fetcher started");
    }
}