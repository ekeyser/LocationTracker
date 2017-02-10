package com.ekeyser.locationtrackerv2;

import android.Manifest;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.*;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Timer;
import java.util.TimerTask;

public class GPSService extends Service implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    private LocationRequest mLocationRequest;
    private GoogleApiClient mGoogleApiClient;
    private static final String LOGSERVICE = "------->";
    private Timer timer;
    private HandlerThread mLocThread;

    @Override
    public void onCreate() {
        super.onCreate();
        initThread();
        buildGoogleApiClient();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!mGoogleApiClient.isConnected())
            mGoogleApiClient.connect();
        return START_STICKY;
    }

    private void initThread() {
        mLocThread = new HandlerThread("locationThread");
        mLocThread.start();
    }

    public class LocalBinder extends Binder {
        GPSService getService() {
            return GPSService.this;
        }
    }

    private final IBinder mBinder = new LocalBinder();

    @Override
    public void onConnected(Bundle bundle) {
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                startLocationUpdate();
            }
        }, 0, 60000);
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(LOGSERVICE, "onConnectionSuspended " + i);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.w(LOGSERVICE, "onConnectionFailed ");

    }

    private void startLocationUpdate() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.create().setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }

        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
//                Log.i(LOGSERVICE, "Got a fix: " + location);
                PostTask pt = new PostTask();
                pt.doInBackground(String.valueOf(location.getTime()), String.valueOf(location.getLatitude()), String.valueOf(location.getLongitude()));
            }
        }, mLocThread.getLooper());
    }

    private synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addOnConnectionFailedListener(this)
                .addConnectionCallbacks(this)
                .addApi(LocationServices.API)
                .build();
    }

    private class PostTask extends AsyncTask<String, String, String> {
        @Override
        protected String doInBackground(String... data) {
            HttpsURLConnection urlConnection = null;
            try {
                URL url = new URL("https://location-api.400lbs.com/");
                urlConnection = (HttpsURLConnection) url.openConnection();
                urlConnection.setDoOutput(true);
                urlConnection.setChunkedStreamingMode(0);

                OutputStream out = new BufferedOutputStream(urlConnection.getOutputStream());
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, "UTF-8"));
                StringBuilder streamBuilder = new StringBuilder();
                streamBuilder.append(URLEncoder.encode("ts", "UTF-8"));
                streamBuilder.append("=");
                streamBuilder.append(URLEncoder.encode(data[0], "UTF-8"));
                streamBuilder.append("&");
                streamBuilder.append(URLEncoder.encode("lat", "UTF-8"));
                streamBuilder.append("=");
                streamBuilder.append(URLEncoder.encode(data[1], "UTF-8"));
                streamBuilder.append("&");
                streamBuilder.append(URLEncoder.encode("lon", "UTF-8"));
                streamBuilder.append("=");
                streamBuilder.append(URLEncoder.encode(data[2], "UTF-8"));
                writer.write(streamBuilder.toString());
                writer.flush();
                writer.close();
                out.close();

                InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                byte[] contents = new byte[1024];
                int bytesRead;
                String strFileContents = "";
                while ((bytesRead = in.read(contents)) != -1) {
                    strFileContents += new String(contents, 0, bytesRead);
                }

//                Log.v(LOGSERVICE, "response was " + strFileContents);
            } catch (MalformedURLException error) {
            } catch (SocketTimeoutException error) {
            } catch (IOException error) {
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }

            return "Done";
        }
    }
}
