package com.app.checker;

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
        Intent intent = new Intent(context,Fetcher.class);
        context.startService(intent);
        Log.i("Autostart", "Service Fetcher started at boot time");
    }
}