package com.ekeyser.locationtrackerv2;

import android.app.IntentService;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.AsyncTask;
import android.os.StrictMode;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Timer;
import java.util.TimerTask;

import javax.net.ssl.HttpsURLConnection;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private GoogleApiClient mGoogleApiClient;
    private static final String TAG = "LocatrFragment";
    private static final int MY_PERMISSION_ACCESS_FINE_LOCATION = 11;
    private Timer timer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        setContentView(R.layout.activity_main);

        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
    }

    @Override
    public void onConnectionSuspended(int n) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.v(TAG, "connection failed");
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                TimerMethod();
            }
        }, 0, 60000);
    }

    private void TimerMethod() {
        Log.v(TAG, "timer method");
        this.runOnUiThread(Timer_Tick);
    }

    private Runnable Timer_Tick = new Runnable() {
        public void run() {
            LocationRequest request = LocationRequest.create();
            request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
//            request.setInterval(0);

//            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSION_ACCESS_FINE_LOCATION);
//            }

            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, request, new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    Log.i(TAG, "Got a fix: " + location);
                    PostTask pt = new PostTask();
                    pt.doInBackground(String.valueOf(location.getTime()), String.valueOf(location.getLatitude()), String.valueOf(location.getLongitude()));
                }
            });

        }
    };

//    private class PostTask extends AsyncTask<String, String, String> {
//        @Override
//        protected String doInBackground(String... data) {
//            HttpsURLConnection urlConnection = null;
//            try {
//                URL url = new URL("https://location-api.400lbs.com/");
//                urlConnection = (HttpsURLConnection) url.openConnection();
//                urlConnection.setDoOutput(true);
//                urlConnection.setChunkedStreamingMode(0);
//
//                OutputStream out = new BufferedOutputStream(urlConnection.getOutputStream());
//                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, "UTF-8"));
//                StringBuilder streamBuilder = new StringBuilder();
//                streamBuilder.append(URLEncoder.encode("ts", "UTF-8"));
//                streamBuilder.append("=");
//                streamBuilder.append(URLEncoder.encode(data[0], "UTF-8"));
//                streamBuilder.append("&");
//                streamBuilder.append(URLEncoder.encode("lat", "UTF-8"));
//                streamBuilder.append("=");
//                streamBuilder.append(URLEncoder.encode(data[1], "UTF-8"));
//                streamBuilder.append("&");
//                streamBuilder.append(URLEncoder.encode("lon", "UTF-8"));
//                streamBuilder.append("=");
//                streamBuilder.append(URLEncoder.encode(data[2], "UTF-8"));
//                writer.write(streamBuilder.toString());
//                writer.flush();
//                writer.close();
//                out.close();
//
//                InputStream in = new BufferedInputStream(urlConnection.getInputStream());
//                byte[] contents = new byte[1024];
//                int bytesRead;
//                String strFileContents = "";
//                while ((bytesRead = in.read(contents)) != -1) {
//                    strFileContents += new String(contents, 0, bytesRead);
//                }
//
//                Log.v(TAG, "response was " + strFileContents);
//            } catch (MalformedURLException error) {
//            } catch (SocketTimeoutException error) {
//            } catch (IOException error) {
//            } finally {
//                if (urlConnection != null) {
//                    urlConnection.disconnect();
//                }
//            }
//
//            return "Done";
//        }
//    }

    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    protected void onStop() {
        super.onStop();
        mGoogleApiClient.disconnect();
    }
}
