package com.cloudywaters.locationtrackerv2;

import android.Manifest;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.support.v4.app.ActivityCompat;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import org.json.JSONException;
import org.json.JSONObject;

public class GPSService extends Service implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    private GoogleApiClient mGoogleApiClient;
    private static final String LOGSERVICE = "------->";

    @Override
    public void onCreate() {
        super.onCreate();
        buildGoogleApiClient();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!mGoogleApiClient.isConnected())
            mGoogleApiClient.connect();
        return START_STICKY;
    }

    class LocalBinder extends Binder {
        GPSService getService() {
            return GPSService.this;
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        startLocationUpdate();
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
        LocationRequest mLocationRequest;
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(60000);
        mLocationRequest.setFastestInterval(60000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

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
                PostTask pt = new PostTask();
                pt.doInBackground(String.valueOf(location.getTime()), String.valueOf(location.getLatitude()), String.valueOf(location.getLongitude()));
            }
        });
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
            JSONObject jsonParam = new JSONObject();
            try {
                jsonParam.put("ts", data[0]);
                jsonParam.put("lat", data[1]);
                jsonParam.put("lon", data[2]);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            HttpsURLConnection urlConnection = null;
            try {
//                URL url = new URL("https://location-api.400lbs.com/");
                URL url = new URL("https://q1d5cw0tkj.execute-api.us-west-2.amazonaws.com/prod/");
                urlConnection = (HttpsURLConnection) url.openConnection();
                urlConnection.setDoOutput(true);
                urlConnection.setChunkedStreamingMode(0);
                urlConnection.setRequestProperty("Content-Type", "application/json");

                DataOutputStream wr = new DataOutputStream(urlConnection.getOutputStream());
                wr.writeBytes(jsonParam.toString());
                wr.flush();
                wr.close();

                InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                byte[] contents = new byte[1024];
                int bytesRead;
                StringBuilder strFileContents = new StringBuilder();
                while ((bytesRead = in.read(contents)) != -1) {
                    strFileContents.append(new String(contents, 0, bytesRead));
                }

                Log.v(LOGSERVICE, "response was " + strFileContents);
            } catch (MalformedURLException error) {
                Log.e(LOGSERVICE, "malformed url exception");
            } catch (SocketTimeoutException error) {
                Log.e(LOGSERVICE, "socket timeout");
            } catch (IOException error) {
                Log.e(LOGSERVICE, "IO Exception");
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }

            return "Done";
        }
    }
}

