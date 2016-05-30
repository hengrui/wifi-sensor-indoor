package com.epienriz.hengruicao.wifidatacollector;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
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
import android.view.MenuItem;
import android.view.MotionEvent;
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

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener
        , View.OnClickListener {

    private static final int TIME_TO_SCAN = 10;
    private static final int SCAN_DELAY = 300;
    List<JSONObject> scanResults = new ArrayList<>();
    Handler wifiQueue = new Handler();
    ProgressDialog wifiDialog;
    HKUSTMapView collectorView;
    ScanResultFilter mFilter;

    void bmobPostTest(){
        try {
            JSONObject test = new JSONObject()
                    .put("test", "this is my test gg")
                    .put("timestamp", new Date().getTime());
            VolleySingleton.getInstance(this).addToRequestQueue
                    (BmobRequest.build(Request.Method.POST, BmobDatabase.postTestUrl, test,
                            new Response.Listener<JSONObject>() {
                                @Override
                                public void onResponse(JSONObject response) {
                                    try {
                                        Log.d("posted id", response.getString("objectId"));
                                    } catch (JSONException ignored) {
                                    }
                                }
                            }, null)
                    );
        } catch (JSONException e) {
            e.printStackTrace();
        }
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
                wifiQueue.removeCallbacksAndMessages(null);
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        if (collectorView != null)
            collectorView.setOnDragListener(new CollectorDragListener());
        mFilter = new ScanResultFilter();
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        HKUSTFloor[] ids = new HKUSTFloor[]{
                new HKUSTFloor(R.id.nav_lg1, "LG1"),
                new HKUSTFloor(R.id.nav_lg3, "LG3"),
                new HKUSTFloor(R.id.nav_lg7, "LG7"),
                new HKUSTFloor(R.id.nav_g, "G"),
                new HKUSTFloor(R.id.nav_lg5, "LG5"),
                new HKUSTFloor(R.id.nav_lg4, "LG4")
        };
        for (HKUSTFloor f : ids) {
            if (f.id == item.getItemId()) {
                changeFloor(f.floor);
                DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
                drawer.closeDrawer(GravityCompat.START);
                return true;
            }
        }
        return false;
    }

    private void changeFloor(String floor) {
        collectorView.setFloor(floor);
    }
    class CollectorDragListener implements View.OnDragListener{
        @Override
        public boolean onDrag(View v, DragEvent event) {
            int action = event.getAction();
            switch (event.getAction()) {
                case DragEvent.ACTION_DRAG_STARTED:
                    // do nothing
                    break;
                case DragEvent.ACTION_DRAG_ENTERED:
                    Log.d("Drag", "Enter");
                    break;
                case DragEvent.ACTION_DRAG_EXITED:
                    Log.d("Drag", "Exit");
                    break;
                case DragEvent.ACTION_DROP:
                    int [] viewLoc = new int[2];
                    Rect hitRect = new Rect();
                    View col = findViewById(R.id.collector);
                    col.getLocationInWindow(viewLoc);
                    col.getHitRect(hitRect);
                    Log.d("Drag drop", String.format("Ended %f %f ",
                            event.getX(), event.getY()));
                    Log.d("View location", String.format("x %d y %d, w %d h %d",
                            hitRect.left, hitRect.top, hitRect.width(), hitRect.height()));
                    Log.d("View location", String.format("x %d y %d",
                            viewLoc[0], viewLoc[1]));
                    wifiScan(collectorView.convertTouchToPosition(new PointF(
                            event.getX(), event.getY())));
                    break;
                case DragEvent.ACTION_DRAG_ENDED:
                    Log.d("Drag", "Ended");
                default:
                    break;
            }
            return true;
        }
    }

    private void wifiScan(final PointF location) {
        final ProgressDialog wifiDialog = FireCollectDialog();
        final WifiManager mWifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
        final VolleySingleton vs = VolleySingleton.getInstance(this);
        for (int i = 0; i < TIME_TO_SCAN; ++i) {
            final int idx = i;
            Runnable scanrun = new Runnable() {
                public void run() {
                    List<ScanResult> results = mWifiManager.getScanResults();
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
                            .put("scan_idx", idx)
                            .put("location", "undefined");
                        } catch (JSONException ignored) {
                        }
                        scanResults.add(wifirecord);
                    }
                    wifiDialog.setProgress(idx + 1);
                    if (idx + 1 == TIME_TO_SCAN) {
                        if (wifiDialog.isShowing())
                            wifiDialog.dismiss();
                        wifiFinish(location);
                        collectorView.addLocation(location, null);
                    }
                }
            };

            if (i == 0)
                scanrun.run();
            else
                wifiQueue.postDelayed(
                        scanrun
                        , SCAN_DELAY * i);
        }
    }

    private void wifiFinish(PointF location) {
        try {
            JSONObject loc = new JSONObject()
                    .put("x", location.x).put("y", location.y)
                    .put("floor", collectorView.getFloor());
            for (JSONObject obj : scanResults) {
                obj.put("location", loc);
                Log.d(obj.getString("SSID"),
                        String.format("level: %d, bsid: %s, Scan: %d", obj.getInt("strength"),
                                obj.getString("BSSID"), obj.getInt("scan_idx")));
            }
            VolleySingleton.getInstance(this).addManyToRequestQueue(
                    BmobRequest.batchManyPost(BmobDatabase.postWifiUrl, scanResults, null, null)
            );
            scanResults.clear();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            default:
                break;
        }
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
        super.onDestroy();
    }

}
