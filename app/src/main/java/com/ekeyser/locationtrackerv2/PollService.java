package com.ekeyser.locationtrackerv2;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.AsyncTask;
import android.util.Log;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLEncoder;

public class PollService extends IntentService {
    private static final String TAG = "PollService";

    public static Intent newIntent(Context context) {
        return new Intent(context, PollService.class);
    }

    public PollService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.i(TAG, "Received an intent: " + intent);
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
                PostTask pt = new MainActivity.PostTask();
                pt.doInBackground(String.valueOf(location.getTime()), String.valueOf(location.getLatitude()), String.valueOf(location.getLongitude()));
            }
        });
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

                Log.v(TAG, "response was " + strFileContents);
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
