package com.app.checker;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    ProgressDialog pd;
    TextView tvResponse;
    Button btnFetch;
    View view;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvResponse = findViewById(R.id.tvResponse);
        btnFetch= findViewById(R.id.btnTest);
        view = this.getWindow().getDecorView();

        btnFetch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new JsonTask().execute("http://172.16.60.92:80/test.json");
                notifty();
            }
        });



    }

    // Test function
    void notifty(){
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(MainActivity.this, "M_CH_ID");
        notificationBuilder.setAutoCancel(true)
                .setDefaults(Notification.DEFAULT_ALL)
                .setWhen(System.currentTimeMillis())
                .setSmallIcon(R.mipmap.ic_launcher)
                .setTicker("Checker")
                .setPriority(Notification.PRIORITY_MAX)
                .setContentTitle("HTTP Request Sent")
                .setContentText("JSON Fetched Successfully")
                .setContentInfo("Info");
        NotificationManager notificationManager = (NotificationManager) MainActivity.this.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(1, notificationBuilder.build());
    }

    // HTTP Request for JSON objects
    private class JsonTask extends AsyncTask<String,String,String>{
        protected void onPreExecute()
        {
            super.onPreExecute();
            pd = new ProgressDialog(MainActivity.this);
            pd.setMessage("Please wait");
            pd.setCancelable(false);
            pd.show();
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
            if (pd.isShowing()){
                pd.dismiss();
            }
            try {
                JSONObject json = new JSONObject(result);
                task = json.getString("task");
                if(task.equals("kill")){
                    finish();
                    System.exit(0);
                } else {
                    // Functions to be implemented
                    tvResponse.setText(task);
                }
            } catch (JSONException e) {
                Log.e("Error",e.getMessage());
            }
        }
    }
}