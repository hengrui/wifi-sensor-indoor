package com.epienriz.hengruicao.wifidatacollector.activity;

import android.annotation.TargetApi;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.PointF;
import android.hardware.SensorManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SubMenu;
import android.view.View;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.epienriz.hengruicao.wifidatacollector.map.HKUSTMapView;
import com.epienriz.hengruicao.wifidatacollector.R;
import com.epienriz.hengruicao.wifidatacollector.api.BmobDatabase;
import com.epienriz.hengruicao.wifidatacollector.api.BmobRequest;
import com.epienriz.hengruicao.wifidatacollector.api.VolleySingleton;
import com.epienriz.hengruicao.wifidatacollector.core.DataListener;
import com.epienriz.hengruicao.wifidatacollector.core.ObtainSensorData;
import com.epienriz.hengruicao.wifidatacollector.core.ObtainWifiData;
import com.epienriz.hengruicao.wifidatacollector.data.HKUSTFloor;
import com.epienriz.hengruicao.wifidatacollector.data.ScanResultFilter;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.List;

public class MainActivity extends BaseActivity
        implements NavigationView.OnNavigationItemSelectedListener
        , View.OnClickListener{

    Context context;
    HKUSTMapView collectorView;
    ScanResultFilter mFilter = new ScanResultFilter();

    ProgressDialog wifiDialog;
    private Date mLastTriggerTime = new Date();
    private PointF mLastTriggerLocation;
    private WifiManager.WifiLock wifiLock;
    private JSONObject locationJson;

    ObtainSensorData sensorMeasure;
    ObtainWifiData wifiMeasure;

    private void wifiScan(final String floor, final PointF location) throws JSONException {
        mLastTriggerLocation = location;
        locationJson = new JSONObject()
                .put("x", location.x).put("y", location.y)
                .put("floor", floor);
        mLastTriggerTime = new Date();
        FireCollectDialog();
        wifiMeasure.setCollectListener(new wifiCollectFinish());
        wifiMeasure.startCollect(locationJson);
    }

    private class wifiCollectFinish implements DataListener<List<JSONObject> > {
        @Override
        public void notifyResult(List<JSONObject> result) {
            Log.d("wifi finish", "" + result.size());
            VolleySingleton.getInstance(context).addManyToRequestQueue(
                    BmobRequest.batchManyPost(BmobDatabase.postWifiUrl, result, null, null)
            );
            collectorView.addLocation(mLastTriggerLocation, collectorView.getFloor());
            if (wifiDialog.isShowing())
                wifiDialog.dismiss();
        }
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
                try {
                    wifiMeasure.cancelCollect();
                } catch (Exception ignored) {

                }
            }
        });
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
        context = this;
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
                    //TODO
                    //sensorScan(floor, position);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
        WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "Set wifi on for measurements");
        wifiLock.acquire();
        wifiMeasure = new ObtainWifiData(wifiManager, MainActivity.this);
        SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensorMeasure = new ObtainSensorData(sensorManager, this);
    }

    private void changeFloor(String floor) {
        if (getSupportActionBar() != null)
            getSupportActionBar().setTitle(floor);
        collectorView.setFloor(floor);
    }


    //Some empty body function onStart etc
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.nav_scan:
                localize();
                DrawerLayout mDrawerLayout;
                mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
                mDrawerLayout.closeDrawers();
                return true;
        }
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
        try {
        if (wifiDialog != null) {
            wifiDialog.dismiss();
        }
        wifiLock.release();
        sensorMeasure.CleanUp();
        wifiMeasure.CleanUp();
        } catch (Exception e) {
            Log.wtf("OnDestry", "Critical error");
            e.printStackTrace();
        }
        super.onDestroy();
    }


    /**
     * Localize call to api
     */
    private void localize() {
        final Context context = this;
        WifiManager wManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        wifiDialog = FireCollectDialog();
        wifiMeasure.setScanListener(new wifiLocalize());
        wifiMeasure.scanOnce();
        //careful, maybe unaccurate
    }


    private class wifiLocalize implements DataListener<Void> {
        @Override
        public void notifyResult(Void unused) {
            WifiManager wManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
            List<ScanResult> results = wManager.getScanResults();
            JSONObject obj = new JSONObject();
            try {
                JSONObject wifiRecords = new JSONObject();
                for (ScanResult result : results) {
                    if (!mFilter.filter(result))
                        continue;
                    wifiRecords.put(result.BSSID, result.level);
                }
                obj.put("wifi", wifiRecords);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            Log.d("localize post data", obj.toString());
            //temporary, to refactor to another dialog
            BmobRequest apiRequest = BmobRequest.apiRequest(Request.Method.POST,
                    BmobDatabase.localizeUrl,
                    obj, new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            try {
                                int distance = response.getInt("distance");
                                JSONObject locationJson = response.getJSONObject("location");
                                String floor = locationJson.getString("floor");
                                PointF xy = new PointF((float)locationJson.getDouble("x"), (float)locationJson.getDouble("y"));

                                Toast.makeText(context, String.format("confidence: %d, floor: %s, x:%f, y%f",
                                        distance, floor, xy.x, xy.y), Toast.LENGTH_LONG).show();
                                if (wifiDialog.isShowing()) {
                                    wifiDialog.dismiss();
                                    collectorView.centerTo(xy, floor);
                                }
                            } catch (JSONException e) {
                                Log.e("Receive", response.toString());
                                e.printStackTrace();
                            }
                        }
                    }, null);
            VolleySingleton.getInstance(context).addToRequestQueue(apiRequest);
        }
    }
}
