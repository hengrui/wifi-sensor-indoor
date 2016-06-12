package com.epienriz.hengruicao.wifidatacollector.activity;

import android.annotation.TargetApi;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
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
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.epienriz.hengruicao.wifidatacollector.core.LocalizeKNN;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends BaseActivity
        implements NavigationView.OnNavigationItemSelectedListener
        , View.OnClickListener{

    Context context;
    HKUSTMapView collectorView;
    ScanResultFilter mFilter = new ScanResultFilter();

    ProgressDialog wifiDialog;
    private PointF mLastTriggerLocation;
    private WifiManager.WifiLock wifiLock;
    private JSONObject locationJson;

    private LocalizeKNN localizer;
    ObtainSensorData sensorMeasure;
    ObtainWifiData wifiMeasure;


    Map<String, Integer> postedScan;

    private void wifiScan(final String floor, final PointF location) throws JSONException {
        mLastTriggerLocation = location;
        locationJson = new JSONObject()
                .put("x", location.x).put("y", location.y)
                .put("floor", floor);
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

        localizer = new LocalizeKNN(this);
        postedScan = new HashMap<>();

        Intent intent = getIntent();
        Bundle args = intent.getExtras();
        if (args.containsKey("floor")) {
            if (getSupportActionBar() != null)
                getSupportActionBar().setTitle(args.getString("floor"));
            float pointx = (float)intent.getFloatExtra("pointx", 1000.0f);
            float pointy = (float)intent.getFloatExtra("pointy", 600.0f);
            Log.d("point", String.format("%f %f", pointx, pointy));
            collectorView.centerTo(new PointF(pointx, pointy), args.getString("floor"));
        }
    }

    private void changeFloor(String floor) {
        if (getSupportActionBar() != null)
            getSupportActionBar().setTitle(floor);
        collectorView.setFloor(floor);
    }


    boolean onNavigationItemSelectedHelper(MenuItem item) {
        switch (item.getItemId()){
            case R.id.nav_scan:
                localize();
                return true;
            case R.id.nav_log:
                scan_log();
                return true;
        }
        return false;
    }

    //Some empty body function onStart etc
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        if (onNavigationItemSelectedHelper(item)) {
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
                                LocalizeKNN.KEntry best = localizer.localize(response);
                                int distance = best.distance;
                                String floor = best.floor;
                                PointF xy = new PointF((float)best.x, (float)best.y);

                                Toast.makeText(context, String.format("estimate: %d, floor: %s, x:%f, y%f",
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

    private void scan_log() {
        AlertDialog.Builder builderSingle = new AlertDialog.Builder(this);
        builderSingle.setTitle("Log");

        WifiManager wifiManager = (WifiManager)getSystemService(WIFI_SERVICE);
        final wifiAdapter arrayAdapter = new wifiAdapter(
                this,
                wifiManager.getScanResults());
        builderSingle.setNegativeButton(
                "cancel",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        builderSingle.setAdapter(
                arrayAdapter, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
        builderSingle.show();
    }

    class wifiAdapter extends ArrayAdapter<ScanResult> {

        public wifiAdapter(Context context, List<ScanResult> objects) {
            super(context, 0, objects);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // Get the data item for this position
            ScanResult result = getItem(position);
            // Check if an existing view is being reused, otherwise inflate the view
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.scan_item, parent, false);
            }
            // Lookup view for data population
            TextView tvBSSID = (TextView) convertView.findViewById(R.id.scan_BSSID);
            TextView tvLevel = (TextView) convertView.findViewById(R.id.scan_level);
            // Populate the data into the template view using the data object
            tvBSSID.setText(result.BSSID);
            tvLevel.setText(String.format("level %d", result.level));
            // Return the completed view to render on screen
            return convertView;
        }

    }
}
