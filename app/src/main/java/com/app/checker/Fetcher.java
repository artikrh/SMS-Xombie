package com.app.checker;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;

import static java.lang.Thread.sleep;

public class Fetcher extends Service {
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Toast toast = Toast.makeText(getApplicationContext(), "Service successfully invoked", Toast.LENGTH_SHORT);
        toast.show();

        SharedPreferences sPrefs= PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String check =sPrefs.getString("uuid",null);
        if (check == null){
            SharedPreferences.Editor editor = sPrefs.edit();
            UUID uuid = java.util.UUID.randomUUID();
            editor.putString("uuid", uuid.toString());
            editor.commit(); // Store UUID permanently in the device
        }

        final String id =sPrefs.getString("uuid",null);

        try{
            new JsonTask().execute("http://192.168.1.4:80/test.json?id="+id);
        }
        catch (Exception e){
            e.printStackTrace();
        }

        return Service.START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    // HTTP Request for JSON objects
    private class JsonTask extends AsyncTask<String,String,String> {
        protected void onPreExecute()
        {
            super.onPreExecute();
        }

        private static final String REQUEST_METHOD = "GET";
        private static final int READ_TIMEOUT = 15000;
        private static final int CONNECTION_TIMEOUT = 15000;

        @Override
        protected String doInBackground(String... params) {
            HttpURLConnection conn = null;
            BufferedReader buff = null;
            try{
                URL url = new URL(params[0]);
                conn = (HttpURLConnection)url.openConnection();
                conn.setRequestMethod(REQUEST_METHOD);
                conn.setReadTimeout(READ_TIMEOUT);
                conn.setConnectTimeout(CONNECTION_TIMEOUT);
                conn.connect();

                InputStream stream = conn.getInputStream();

                buff = new BufferedReader(new InputStreamReader(stream));
                StringBuffer buffer = new StringBuffer();

                String line;
                while ((line = buff.readLine()) != null) {
                    buffer.append(line);
                }
                return buffer.toString();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
                try {
                    if (buff != null) {
                        buff.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        // After fetching JSON response
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            String task;

            try {
                JSONObject json = new JSONObject(result);
                task = json.getString("message");
                if(task.equals("kill")){
                    // Terminate zombie
                    stopForeground(true);
                    stopSelf();
                    // Useless as of now because the service gets invoked by AlarmManager nevertheless
                } else {
                    // Functions to be implemented
                    Toast toast = Toast.makeText(getApplicationContext(), task, Toast.LENGTH_SHORT);
                    toast.show();
                }
            } catch (JSONException e) {
                Log.e("Error",e.getMessage());
            }
        }
    }
}