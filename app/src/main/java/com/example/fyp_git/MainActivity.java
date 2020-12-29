package com.example.fyp_git;


import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Looper;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

import com.example.fyp_git.Constants;
import com.example.fyp_git.LocationEntry;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Calendar;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;

public class MainActivity extends WearableActivity {
    private Boolean mRequestingLocationUpdates;
    private Location mCurrentLocation;
    private Calendar mCalendar;
    //Creates an instance of FusedLocationProviderClient
    private FusedLocationProviderClient fusedLocationClient;
    private LocationRequest mLocationRequest;
    private Button mStartUpdatesButton;
    private Button mStopUpdatesButton;
    protected static final int REQUEST_CHECK_SETTINGS = 0x1;
    private SettingsClient mSettingsClient;
    private LocationCallback mLocationCallback;
    private String mLastUpdateTime;
    private final static String KEY_REQUESTING_LOCATION_UPDATES = "requesting-location-updates";
    private final static String KEY_LOCATION = "location";
    private final static String KEY_LAST_UPDATED_TIME_STRING = "last-updated-time-string";
    private EditText destInputE2;

    private EditText destInputN2;
    StringBuilder data = new StringBuilder();

    @Override
    //The onCreate method includes everything the application creates when it is created
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        //Log add a message to the logcat when the onCreate is called
        // .d adds it to the debug
        Log.d("MainActivity", "onCreate()");
        Log.d("MainActivity", "onCreate()");

        data.append("Time,Distance");
        //Sets the layout for the main activity
        setContentView(R.layout.activity_main);

        mCalendar = Calendar.getInstance();

        mRequestingLocationUpdates = false;
        mLastUpdateTime = "";
        updateValuesFromBundle(savedInstanceState);
        //Calls the method which requests permission for the location
        //requestPermission();
        //Creates an instance of the fused Location Client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mSettingsClient = LocationServices.getSettingsClient(this);

        // requestSettings();
        //The button is called button
        mStartUpdatesButton = (Button) findViewById(R.id.getLocation);
        mStopUpdatesButton = (Button) findViewById(R.id.stopLocation);
        destInputE2 = findViewById(R.id.destInputE);
        destInputN2 = findViewById(R.id.destInputN);


        createLocationCallback();
        createLocationRequest();


        // Enables Always-on
        setAmbientEnabled();
    }

    private void updateValuesFromBundle(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            Log.d("MainActivity", "updateValuesFromBundle" + mRequestingLocationUpdates);
            // Update the value of mRequestingLocationUpdates from the Bundle, and make sure that
            // the Start Updates and Stop Updates buttons are correctly enabled or disabled.
            if (savedInstanceState.keySet().contains(KEY_REQUESTING_LOCATION_UPDATES)) {
                Log.d("MainActivity", "updateValuesFromBundle" + mRequestingLocationUpdates);
                mRequestingLocationUpdates = savedInstanceState.getBoolean(
                        KEY_REQUESTING_LOCATION_UPDATES);
            }

            // Update the value of mCurrentLocation from the Bundle and update the UI to show the
            // correct latitude and longitude.
            if (savedInstanceState.keySet().contains(KEY_LOCATION)) {
                // Since KEY_LOCATION was found in the Bundle, we can be sure that mCurrentLocation
                // is not null.
                mCurrentLocation = savedInstanceState.getParcelable(KEY_LOCATION);
            }

            // Update the value of mLastUpdateTime from the Bundle and update the UI.
            if (savedInstanceState.keySet().contains(KEY_LAST_UPDATED_TIME_STRING)) {
                mLastUpdateTime = savedInstanceState.getString(KEY_LAST_UPDATED_TIME_STRING);
            }

        }
    }

    //Method to ask for permission to access the precise location
    private void requestPermission() {
        ActivityCompat.requestPermissions(this, new String[]{ACCESS_FINE_LOCATION}, 1);
    }


    private void createLocationCallback() {

        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {

                if (locationResult == null) {
                    return;
                }
                for (Location mCurrentLocation : locationResult.getLocations()) {
                    addLocationEntrytoDataLayer(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());
                    convertCoordinates(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());
                }

            }
        };
    }

    private void convertCoordinates(double latitude, double longitude) {
        ////Maths for converting from eastings to northings
        double pi = 3.14159265359;
        double F0 = 1.000035;
        double a = 6377340.189 * F0;
        double b = 6356034.447 * F0;
        double n = ((a - b) / (a + b));

        double phi = latitude * (pi / 180);
        double phi0 = 53.5 * (pi / 180);
        double lambda = longitude * (pi / 180);
        double lambda0 = -8 * (pi / 180);

        double i = ((1 + n + ((5 / 4) * (Math.pow(n, 2))) + ((5 / 4) * (Math.pow(n, 3)))) * (phi - phi0));
        double ii = (((3 * n) + (3 * (Math.pow(n, 2))) + ((21 / 8) * (Math.pow(n, 3)))) * Math.sin(phi - phi0) * Math.cos(phi + phi0));
        double iii = ((((15 / 8) * (Math.pow(n, 2))) + ((15 / 8) * (Math.pow(n, 3)))) * Math.sin(2 * (phi - phi0)) * Math.cos(2 * (phi + phi0)));
        double iv = ((((35 / 24) * (Math.pow(n, 3)))) * Math.sin(3 * (phi - phi0)) * Math.cos(3 * (phi + phi0)));

        double A = b * (i - ii + iii - iv);
        double eSquared = 0.00667054015;
        double v = a / (Math.pow(1 - (eSquared * (Math.pow(Math.sin(phi), 2))), 0.5));
        double p = (a * (1 - eSquared)) / (Math.pow(1 - (eSquared * (Math.pow(Math.sin(phi), 2))), 1.5));

        double nSquared = (v / p) - 1;


        double P = lambda - lambda0;

        double M = A * F0;
        double N0 = 250000;
        double I = M + N0;
        double II = ((v / 2) * Math.sin(phi) * Math.cos(phi));
        double III = ((v / 24) * Math.sin(phi) * Math.pow(Math.cos(phi), 3) * ((5 - Math.pow(Math.tan(phi), 2) + (9 * nSquared))));
        double IV = ((v / 720) * Math.sin(phi) * (Math.pow(Math.cos(phi), 5)) * ((61 - 58 * Math.pow(Math.tan(phi), 2)) + (Math.pow(Math.tan(phi), 4))));
        double V = v * Math.cos(phi);
        double VI = ((v / 6) * (Math.pow(Math.cos(phi), 3)) * ((v / p) - (Math.pow(Math.tan(phi), 2))));
        double VII = ((v / 120) * (Math.pow(Math.cos(phi), 5)) * (5 - (18 * Math.pow(Math.tan(phi), 2)) + (Math.pow(Math.tan(phi), 4)) + (14 * nSquared) - (58 * Math.pow(Math.tan(phi), 2) * nSquared)));

        double E0 = 200000;
        double E = (E0 + (F0 * ((P * V) + ((Math.pow(P, 3)) * VI) + ((Math.pow(P, 5)) * VII)))) + 49;

        double N = (I + ((Math.pow(P, 2)) * II) + ((Math.pow(P, 4)) * III) + ((Math.pow(P, 6)) * IV)) - 23.4;

//        //Prints the Eastings to the location box
//        TextView textViewEast = findViewById(R.id.eastCo);
//        textViewEast.setText(String.format("%.0f",E));
//
//        //Prints the Northings to the location box
//        TextView textViewNorth = findViewById(R.id.northCo);
//        textViewNorth.setText(String.format("%.0f",N));

        double destE = 310265.1859;
        double destN = 236545.0508;


        data.append("\n" + String.valueOf(E) + "," + String.valueOf(N));
        calculateDistanceING(E, N);

        double INGSEast = E / 100000;
        double INGSNorth = N / 100000;
        double NewE = E % 100000;
        double NewEast = NewE / 100;
        double NewN = N % 100000;
        double NewNorth = NewN / 100;
        String CoE = (String.format("%03.0f", NewEast));
        String CoN = (String.format("%03.0f", NewNorth));
        String Co = " " + CoE + " " + CoN;
        TextView textViewGID = findViewById(R.id.IGCo);
        if (INGSEast > 4 && INGSEast < 5) {
            if (INGSNorth < 5 && INGSNorth > 4) {
                textViewGID.setText("E" + Co);
            }
            if (INGSNorth < 4 && INGSNorth > 3) {
                textViewGID.setText("K" + Co);
            }
            if (INGSNorth < 3 && INGSNorth > 2) {
                textViewGID.setText("P" + Co);
            }
            if (INGSNorth < 2 && INGSNorth > 1) {
                textViewGID.setText("V" + Co);
            }
            if (INGSNorth < 1 && INGSNorth > 0) {
                textViewGID.setText("Z" + Co);
            }
        }
        if (INGSEast > 3 && INGSEast < 4) {
            if (INGSNorth < 5 && INGSNorth > 4) {
                textViewGID.setText("D" + Co);
            }
            if (INGSNorth < 4 && INGSNorth > 3) {
                textViewGID.setText("J" + Co);
            }
            if (INGSNorth < 3 && INGSNorth > 2) {
                textViewGID.setText("O" + Co);
            }
            if (INGSNorth < 2 && INGSNorth > 1) {
                textViewGID.setText("T" + Co);
            }
            if (INGSNorth < 1 && INGSNorth > 0) {
                textViewGID.setText("Y" + Co);
            }
        }
        if (INGSEast > 2 && INGSEast < 3) {
            if (INGSNorth < 5 && INGSNorth > 4) {
                textViewGID.setText("C" + Co);
            }
            if (INGSNorth < 4 && INGSNorth > 3) {
                textViewGID.setText("H" + Co);
            }
            if (INGSNorth < 3 && INGSNorth > 2) {
                textViewGID.setText("N" + Co);
            }
            if (INGSNorth < 2 && INGSNorth > 1) {
                textViewGID.setText("S" + Co);
            }
            if (INGSNorth < 1 && INGSNorth > 0) {
                textViewGID.setText("X" + Co);
            }
        }
        if (INGSEast > 1 && INGSEast < 2) {
            if (INGSNorth < 5 && INGSNorth > 4) {
                textViewGID.setText("B" + Co);
            }
            if (INGSNorth < 4 && INGSNorth > 3) {
                textViewGID.setText("G" + Co);
            }
            if (INGSNorth < 3 && INGSNorth > 2) {
                textViewGID.setText("M" + Co);
            }
            if (INGSNorth < 2 && INGSNorth > 1) {
                textViewGID.setText("R" + Co);
            }
            if (INGSNorth < 1 && INGSNorth > 0) {
                textViewGID.setText("W" + Co);
            }
        }
        if (INGSEast > 0 && INGSEast < 1) {
            if (INGSNorth < 5 && INGSNorth > 4) {
                textViewGID.setText("A" + Co);
            }
            if (INGSNorth < 4 && INGSNorth > 3) {
                textViewGID.setText("F" + Co);
            }
            if (INGSNorth < 3 && INGSNorth > 2) {
                textViewGID.setText("L" + Co);
            }
            if (INGSNorth < 2 && INGSNorth > 1) {
                textViewGID.setText("Q" + Co);
            }
            if (INGSNorth < 1 && INGSNorth > 0) {
                textViewGID.setText("V" + Co);
            }
        }
    }

    private void calculateDistanceING(double E, double N) {
        double destE2 = Double.parseDouble(destInputE2.getText().toString());
        destInputE2.setText(String.format("%.0f", destE2));
        double destN2 = Double.parseDouble(destInputN2.getText().toString());
        destInputN2.setText(String.format("%.0f", destN2));


        double changeE = Math.pow(destE2 - E, 2);
        double changeN = Math.pow(destN2 - N, 2);
        double distanceX = Math.sqrt(changeE + changeN);
        double distance = distanceX / 1000;
        TextView textViewDistance = findViewById(R.id.distanceM);
        textViewDistance.setText(String.format("%.3f", distance));
    }

    public void export(View view) {
        try {
            FileOutputStream out = openFileOutput("data.csv", Context.MODE_PRIVATE);
            out.write((data.toString()).getBytes());
            out.close();

            Context context = getApplicationContext();
            File filelocation = new File(getFilesDir(), "data.csv");
            Uri path = FileProvider.getUriForFile(context, "com.example.fyp_firstedition.fileprovider", filelocation);
            Intent fileIntent = new Intent(Intent.ACTION_SEND);
            fileIntent.setType("text/csv");
            fileIntent.putExtra(Intent.EXTRA_SUBJECT, "Data");
            fileIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            fileIntent.putExtra(Intent.EXTRA_STREAM, path);
            startActivity(Intent.createChooser(fileIntent, "Send mail"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addLocationEntrytoDataLayer(double latitude, double longitude) {
        mCalendar.setTimeInMillis(System.currentTimeMillis());
        LocationEntry entry = new LocationEntry(mCalendar, latitude, longitude);
        String path = Constants.PATH + "/" + mCalendar.getTimeInMillis();
        PutDataMapRequest putDataMapReq = PutDataMapRequest.create(path);
        putDataMapReq.getDataMap().putDouble(Constants.KEY_LATITUDE, entry.latitude);
        putDataMapReq.getDataMap().putDouble(Constants.KEY_LONGITUDE, entry.longitude);

        putDataMapReq.getDataMap()
                .putLong(Constants.KEY_TIME, entry.calendar.getTimeInMillis());

        PutDataRequest request = putDataMapReq.asPutDataRequest();
        request.setUrgent();

        Task<DataItem> dataItemTask = Wearable.getDataClient(getApplicationContext()).putDataItem(request);
        Log.d("MainActivity", "addLocation" + latitude + longitude + mCalendar);
    }

    private void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);


    }

    protected void onResume() {
        Log.d("MainActivity", "onResume" + mRequestingLocationUpdates);
        super.onResume();
        if (mRequestingLocationUpdates) {
            Log.d("MainActivity", "onResume in if");
            startLocationUpdates();
        }
    }

    public void startUpdatesButtonHandler(View view) {
        Log.d("MainActivity", "startUpdatesButtonHandler" + mRequestingLocationUpdates);
        if (!mRequestingLocationUpdates) {
            mRequestingLocationUpdates = true;
            setButtonsEnabledState();
            requestSettings();
        }
    }

    public void stopUpdatesButtonHandler(View view) {
        setButtonsEnabledState();
        stopLocationUpdates();


    }

    private void setButtonsEnabledState() {
        Log.d("MainActivity", "setButtonsEnabledState" + mRequestingLocationUpdates);

        if (mRequestingLocationUpdates) {
            mStartUpdatesButton.setEnabled(false);
            mStopUpdatesButton.setEnabled(true);
        } else {
            mStartUpdatesButton.setEnabled(true);
            mStopUpdatesButton.setEnabled(false);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        //stopLocationUpdates();
    }

    private void stopLocationUpdates() {
        Log.d("MainActivity", "stopLocationUpdates");
        if (!mRequestingLocationUpdates) {
            Log.d("MainActivity", "stopLocationUpdates: updates never requested, no-op.");
            return;
        }
//
        fusedLocationClient.removeLocationUpdates(mLocationCallback)
                .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        mRequestingLocationUpdates = false;
                        setButtonsEnabledState();
                    }
                });
    }

    private void startLocationUpdates() {

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
        fusedLocationClient.requestLocationUpdates(mLocationRequest,
                mLocationCallback, Looper.myLooper());

    }

    private void requestSettings(){

        requestPermission();
        Log.d("MainActivity", "requestSettings()");
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest);


        Task<LocationSettingsResponse> task = mSettingsClient.checkLocationSettings(builder.build());

        task.addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
            @Override
            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                Log.d("MainActivity", "requestSettings()...success");
                // All location settings are satisfied. The client can initialize
                // location requests here.
                // ...

                startLocationUpdates();
            }
        });

        task.addOnFailureListener(this, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d("MainActivity", "requestSettings()....failure");
                if (e instanceof ResolvableApiException) {
                    // Location settings are not satisfied, but this can be fixed
                    // by showing the user a dialog.
                    Log.d("MainActivity", "requestSettings()....failure123");
                    try {
                        // Show the dialog by calling startResolutionForResult(),
                        // and check the result in onActivityResult().

                        ResolvableApiException resolvable = (ResolvableApiException) e;
                        resolvable.startResolutionForResult(MainActivity.this,
                                REQUEST_CHECK_SETTINGS);
                    } catch (IntentSender.SendIntentException sendEx) {
                        Log.i("MainActivity", "PendingIntent unable to execute request.");
                        // Ignore the error.
                    }
                }
            }
        });
    }
}