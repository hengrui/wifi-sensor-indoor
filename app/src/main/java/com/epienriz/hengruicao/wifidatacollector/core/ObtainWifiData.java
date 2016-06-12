package com.epienriz.hengruicao.wifidatacollector.core;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;

import com.android.volley.Response;
import com.epienriz.hengruicao.wifidatacollector.activity.MainActivity;
import com.epienriz.hengruicao.wifidatacollector.data.ScanResultFilter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by hengruicao on 6/8/16.
 */
public class ObtainWifiData {
    MainActivity mActivity;
    WifiManager wManager;
    ScanResultFilter wifiFilter = new ScanResultFilter();

    private int wifiCount = 0;
    private static final int TIME_TO_SCAN = 5;
    private static final int SCAN_DELAY = 300;
    private JSONObject locationJson;
    List<JSONObject> scanResults = new ArrayList<>();

    private BroadcastReceiver wifiReceiver;

    DataListener<List<JSONObject>> collectListener;
    DataListener<Void> scanListener;

    /**
     * Callback when wifiManager finishes scanning
     */
    private class WifiCollectReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context c, Intent intent) {
            wifiCount = wifiCount + 1;

            if (wifiCount > TIME_TO_SCAN) {
                System.out.println("WiFi Measurement Finished!");

//                if (wifiDialog.isShowing()) {
//                    wifiDialog.dismiss();
//                    collectorView.addLocation(mLastTriggerLocation, collectorView.getFloor());
//                }
                collectFinish();
                if (wifiReceiver != null) {
                    mActivity.unregisterReceiver(wifiReceiver);
                    wifiReceiver = null;
                }
            } else {
                Log.d("WiFi Measurement", "count " + wifiCount + "...");
                List<ScanResult> results = wManager.getScanResults();
                Date now = new Date();

                JSONArray wifis = new JSONArray();
                for (ScanResult result : results) {
                    if (!wifiFilter.filter(result)) {
                        continue;
                    }
                    //Log.d(result.SSID, String.format("level: %d, bsid: %s, Scan: %d", result.level, result.BSSID, idx));
                    JSONObject wifirecord = new JSONObject();
                    try {
                        wifirecord.put("BSSID", result.BSSID)
                                .put("strength", result.level)
                                .put("SSID", result.SSID);
                    } catch (JSONException ignored) {
                    }
                    wifis.put(wifirecord);
                }
                JSONObject scanResult = new JSONObject();
                try {
                    scanResult.put("wifi", wifis)
                            .put("timestamp", now.getTime())
                            .put("user", mActivity.getUserIdentifier())
                            .put("scan_idx", wifiCount)
                            .put("location", locationJson);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                scanResults.add(scanResult);
//                wifiDialog.incrementProgressBy(1);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        new MeasureWifi().execute();
                    }
                }, SCAN_DELAY);
            }
        }
    }

    /**
     * Measure Wifi Async Task to Call WifiManager startScan
     */
    private class MeasureWifi extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            final WifiManager wManager = (WifiManager) mActivity.getSystemService(Context.WIFI_SERVICE);
            wManager.startScan();
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

    public ObtainWifiData(WifiManager wifiManager, MainActivity mainAty) {
        wManager = wifiManager;
        mActivity = mainAty;
    }

    /**
     * To be called upon destroy
     */
    public void CleanUp() {
        if (wifiReceiver != null)
            mActivity.unregisterReceiver(wifiReceiver);
    }

    public void setCollectListener(DataListener<List<JSONObject> > collectListener) {
        this.collectListener = collectListener;
    }

    public void setScanListener(DataListener<Void> scanListener) {
        this.scanListener = scanListener;
    }

    private void collectFinish() {
        collectListener.notifyResult(scanResults);
        scanResults.clear();
    }

    private BroadcastReceiver wifiScanReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                scanListener.notifyResult(null);
                mActivity.unregisterReceiver(wifiScanReceiver);
            } catch (Exception ignored) {
                ignored.printStackTrace();
            }
        }
    };

    /**
     * Scan and call scnListener once
     */
    public void scanOnce() {
        mActivity.registerReceiver(wifiScanReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        wManager.startScan();
    }

    /**
     * collect data at given location descriptor
     * @param mLocation description about the location
     */
    public void startCollect(JSONObject mLocation) {
        locationJson = mLocation;
        wifiCount = 0;
        wifiReceiver = new WifiCollectReceiver();
        mActivity.registerReceiver(wifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        new MeasureWifi().execute();
    }

    /**
     * Cancel collection and unregister receiver
     */
    public void cancelCollect() {
        if (wifiReceiver != null) {
            mActivity.unregisterReceiver(wifiReceiver);
            wifiReceiver = null;
        }

    }

}
