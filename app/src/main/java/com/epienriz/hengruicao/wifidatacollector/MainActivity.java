package com.epienriz.hengruicao.wifidatacollector;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.provider.Settings;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.DragEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.epienriz.hengruicao.wifidatacollector.api.BmobDatabase;
import com.epienriz.hengruicao.wifidatacollector.api.BmobRequest;
import com.epienriz.hengruicao.wifidatacollector.api.VolleySingleton;
import com.epienriz.hengruicao.wifidatacollector.data.HKUSTFloor;
import com.epienriz.hengruicao.wifidatacollector.data.ScanResultFilter;
import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener
        , View.OnClickListener, SensorEventListener {

    private static final int TIME_TO_SCAN = 10;
    private static final int TIME_TO_SENSOR = 20;
    private static final int SENSOR_DELAY = 1000; //1second
    private static final int SCAN_DELAY = 200;
    List<JSONObject> scanResults = new ArrayList<>();
    HKUSTMapView collectorView;
    ScanResultFilter mFilter = new ScanResultFilter();

    ProgressDialog wifiDialog;
    private int wifiCount = 0;
    private Date mLastTriggerTime = new Date();
    private PointF mLastTriggerLocation;
    private WifiManager.WifiLock wifiLock;
    private JSONObject locationJson;

    private Sensor mAccelerometerSensor;
    private Sensor mGyroscopeSensor;
    private Sensor mGeomagneticSensor;

    // Wifi Receiver
    // Wifi Scan and else
    private BroadcastReceiver wifiReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent intent) {
            wifiCount = wifiCount + 1;

            if (wifiCount > TIME_TO_SCAN) {
                System.out.println("WiFi Measurement Finished!");

                if (wifiDialog.isShowing()) {
                    wifiDialog.dismiss();
                    wifiFinish();
                    collectorView.addLocation(mLastTriggerLocation, collectorView.getFloor());
                }
                unregisterReceiver(wifiReceiver);
            } else {
                Log.d("WiFi Measurement", "count " + wifiCount + "...");
                WifiManager wManager = (WifiManager) c.getSystemService(Context.WIFI_SERVICE);
                List<ScanResult> results = wManager.getScanResults();
                Date now = new Date();
                for (ScanResult result : results) {
                    if (!mFilter.filter(result)) {
                        continue;
                    }
                    //Log.d(result.SSID, String.format("level: %d, bsid: %s, Scan: %d", result.level, result.BSSID, idx));
                    JSONObject wifirecord = new JSONObject();
                    try {
                        wifirecord.put("BSSID", result.BSSID)
                                .put("strength", result.level)
                                .put("SSID", result.SSID)
                                .put("timestamp", now.getTime())
                                .put("user", Settings.Secure.getString(getContentResolver(),
                                        Settings.Secure.ANDROID_ID))
                                .put("scan_idx", wifiCount)
                                .put("location", locationJson);
                    } catch (JSONException ignored) {
                    }
                    scanResults.add(wifirecord);
                }
                wifiDialog.incrementProgressBy(1);
                new MeasureWifi().execute();
            }
        }
    };
    private Timer sensorTimer = new Timer();

    private void wifiScan(final String floor, final PointF location) throws JSONException {
        wifiCount = 0;
        scanResults.clear();
        mLastTriggerLocation = location;
        locationJson = new JSONObject()
                .put("x", location.x).put("y", location.y)
                .put("floor", floor);
        mLastTriggerTime = new Date();
        FireCollectDialog();
        new MeasureWifi().execute();
    }

    //Upload wifi stored in scanResults
    private void wifiFinish() {
        VolleySingleton.getInstance(this).addManyToRequestQueue(
                BmobRequest.batchManyPost(BmobDatabase.postWifiUrl, scanResults, null, null)
        );
        scanResults.clear();
    }

    //Option Menu for map toggle
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        SubMenu sMenu = null;
        int csubmenu = 0;
        for (int i = 0; i < HKUSTFloor.ids.length; ++i) {
            HKUSTFloor floor = HKUSTFloor.ids[i];
            if (sMenu == null || floor.category != null) {
                sMenu = menu.addSubMenu(0, i + csubmenu, 0, floor.category);
                ++csubmenu;
            }
            sMenu.add(0, i + csubmenu, 0, floor.floor);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int csubmenu = 0;
        for (int i = 0; i < HKUSTFloor.ids.length; ++i) {
            HKUSTFloor floor = HKUSTFloor.ids[i];
            if (floor.category != null)
                csubmenu++;
            if (i + csubmenu == item.getItemId()) {
                changeFloor(floor.floor);
                DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
                drawer.closeDrawer(GravityCompat.START);
                return true;
            }
        }
        return false;
    }

    ProgressDialog FireCollectDialog(){
        wifiDialog = new ProgressDialog(this);
        wifiDialog.setTitle(R.string.collect_title);
        wifiDialog.setMessage(getString(R.string.collect_message));
        wifiDialog.setCancelable(true);
        wifiDialog.setCanceledOnTouchOutside(false);
        wifiDialog.setIndeterminate(true);
        wifiDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                unregisterReceiver(wifiReceiver);
            }
        });
        wifiDialog.setMax(TIME_TO_SCAN);
        wifiDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.cancel),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
        wifiDialog.show();
        return wifiDialog;
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Some navbar and tool bar init
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        fab.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    ClipData data = ClipData.newPlainText("", "");
                    View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(view);
                    view.startDrag(data, shadowBuilder, view, 0);
                    //view.setVisibility(View.INVISIBLE);
                    return true;
                } else {
                    return false;
                }
            }
        });

        collectorView = (HKUSTMapView)findViewById(R.id.collector);
        //call wifi scan
        collectorView.setWifiScanListener(new HKUSTMapView.WifiScanListener() {
            @Override
            public void WifiScanPosition(String floor, PointF position) {
                try {
                    wifiScan(floor, position);
                    sensorScan(floor, position);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

        WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "Set wifi on for measurements");
        wifiLock.acquire();

        SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (mAccelerometerSensor != null) {
            sensorManager.registerListener(this, mAccelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
        mGeomagneticSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR);
        if (mGeomagneticSensor != null) {
            sensorManager.registerListener(this, mGeomagneticSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
        mGyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR);
        if (mGyroscopeSensor != null) {
            sensorManager.registerListener(this, mGyroscopeSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    private void changeFloor(String floor) {
        if (getSupportActionBar() != null)
            getSupportActionBar().setTitle(floor);
        collectorView.setFloor(floor);
    }


    //Some empty body function onStart etc
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        return false;
    }

    @Override
    public void onClick(View v) {
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroy(){
        if (wifiDialog != null) {
            wifiDialog.dismiss();
        }
        SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        sensorManager.unregisterListener(this);
        unregisterReceiver(wifiReceiver);
        wifiLock.release();
        sensorTimer.cancel();
        super.onDestroy();
    }

    List<JSONObject> mSensorResults = new ArrayList<>();

    private JSONArray floatToArray(float[] array) throws JSONException {
        if (array == null)
            return null;
        JSONArray rt = new JSONArray();
        for (float v : array) {
            rt.put((double)v);
        }
        return rt;
    }

    private void sensorScan(final String floor, final PointF location) {
        sensorTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
            try {
                JSONObject sensorData = new JSONObject();
                if (mAccelerometer != null) {
                        sensorData.put("acc", floatToArray(mAccelerometer));
                }
                if (mGyroscope != null) {
                    sensorData.put("gyroscope", floatToArray(mGyroscope));
                }
                if (mGeoMagnetic != null) {
                    sensorData.put("geomagnetic", floatToArray(mGeoMagnetic));
                }
                sensorData.put("timestamp", new Date().getTime());
                sensorData.put("user", Settings.Secure.getString(getContentResolver(),
                        Settings.Secure.ANDROID_ID));
                sensorData.put("scan_idx", mSensorResults.size());
                sensorData.put("location", locationJson);
                mSensorResults.add(sensorData);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                if (mSensorResults.size() >= TIME_TO_SENSOR) {
                    sensorFinish();
                }
            }
        }, 0, SENSOR_DELAY);
    }

    private void sensorFinish() {
        Log.d("Sensor scan", "finished");
        VolleySingleton.getInstance(this).addManyToRequestQueue(
                BmobRequest.batchManyPost(BmobDatabase.postSensorUrl, mSensorResults, null, null)
        );
        sensorTimer.cancel();
        mSensorResults.clear();
    }

    float[] mAccelerometer;
    float[] mGyroscope;
    float[] mGeoMagnetic;

    @Override
    public void onSensorChanged(SensorEvent event) {
        switch(event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                if (mAccelerometer == null) {
                    mAccelerometer = new float[3];
                }
                mAccelerometer[0] = event.values[0];
                mAccelerometer[1] = event.values[1];
                mAccelerometer[2] = event.values[2];
                break;
            case Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR:
                if (mGeoMagnetic == null) {
                    mGeoMagnetic = new float[3];
                }
                mGeoMagnetic[0] = event.values[0];
                mGeoMagnetic[1] = event.values[1];
                mGeoMagnetic[2] = event.values[2];
                break;
            case Sensor.TYPE_GYROSCOPE:
                if (mGyroscope == null) {
                    mGyroscope = new float[3];
                }
                mGyroscope[0] = event.values[0];
                mGyroscope[1] = event.values[1];
                mGyroscope[2] = event.values[2];
                break;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    //Measure Wifi Async Task to Call WifiManager startScan
    private class MeasureWifi extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            final WifiManager wManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
            wManager.startScan();
            registerReceiver(wifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
            return "Executed";
        }

        @Override
        protected void onPostExecute(String result) {
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected void onProgressUpdate(Void... values) {

        }
    }
}
