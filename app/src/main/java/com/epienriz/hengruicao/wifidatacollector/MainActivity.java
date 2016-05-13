package com.epienriz.hengruicao.wifidatacollector;

import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.epienriz.hengruicao.wifidatacollector.api.BmobDatabase;
import com.epienriz.hengruicao.wifidatacollector.api.BmobRequest;
import com.epienriz.hengruicao.wifidatacollector.api.VolleySingleton;
import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final int TIME_TO_SCAN = 1;
    private static final int SCAN_DELAY = 300;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.scan).setOnClickListener(this);

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

    private void wifiScan() {
        final WifiManager mWifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
        final VolleySingleton vs = VolleySingleton.getInstance(this);
        for (int i = 0; i < TIME_TO_SCAN; ++i) {
            final int idx = i;
            Runnable scanrun = new Runnable() {
                public void run() {
                    List<ScanResult> results = mWifiManager.getScanResults();
                    Date now = new Date();
                    for (ScanResult result : results) {
                        Log.d(result.SSID, String.format("level: %d, bsid: %s, Scan: %d", result.level, result.BSSID, idx));
                        JSONObject wifirecord = new JSONObject();
                        try {
                            wifirecord.put("BSSID", result.BSSID)
                            .put("strength", result.level)
                            .put("SSID", result.SSID)
                            .put("timestamp", now.getTime())
                            .put("user", "temporary")
                            .put("location", "undefined");
                        } catch (JSONException ignored) {
                        }
                        vs.addToRequestQueue(
                                BmobRequest.build(Request.Method.POST, BmobDatabase.postWifiUrl, wifirecord, null, null)
                        );
                        //break ;
                    }
                }
            };

            if (i == 0)
                scanrun.run();
            else
                new Handler().postDelayed(
                        scanrun
                        , SCAN_DELAY * i);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.scan:
                wifiScan();
                break;
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
}
