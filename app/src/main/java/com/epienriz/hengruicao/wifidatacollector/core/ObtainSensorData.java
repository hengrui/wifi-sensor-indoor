package com.epienriz.hengruicao.wifidatacollector.core;

import android.content.Context;
import android.graphics.PointF;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.wifi.WifiManager;
import android.provider.Settings;
import android.util.Log;

import com.epienriz.hengruicao.wifidatacollector.activity.MainActivity;
import com.epienriz.hengruicao.wifidatacollector.api.BmobDatabase;
import com.epienriz.hengruicao.wifidatacollector.api.BmobRequest;
import com.epienriz.hengruicao.wifidatacollector.api.VolleySingleton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by hengruicao on 6/8/16.
 */
public class ObtainSensorData implements SensorEventListener {
    MainActivity mActivity;
    private static final int TIME_TO_SENSOR = 20;
    private static final int SENSOR_DELAY = 1000; //1second

    private Sensor mAccelerometerSensor;
    private Sensor mGyroscopeSensor;
    private Sensor mGeomagneticSensor;

    float[] mAccelerometer;
    float[] mGyroscope;
    float[] mGeoMagnetic;

    List<JSONObject> mSensorResults = new ArrayList<>();
    private Timer sensorTimer = null;
    SensorManager sensorManager;

    public ObtainSensorData(SensorManager mSensor, MainActivity mainAty) {
        sensorManager = mSensor;
        mActivity = mainAty;

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

    private JSONArray floatToArray(float[] array) throws JSONException {
        if (array == null)
            return null;
        JSONArray rt = new JSONArray();
        for (float v : array) {
            rt.put((double)v);
        }
        return rt;
    }


    private void sensorScan(final JSONObject locationJson) {
        if (sensorTimer == null)
            sensorTimer = new Timer();
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
                    sensorData.put("user", mActivity.getUserIdentifier());
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
        VolleySingleton.getInstance(mActivity).addManyToRequestQueue(
                BmobRequest.batchManyPost(BmobDatabase.postSensorUrl, mSensorResults, null, null)
        );
        if (sensorTimer != null) {
            sensorTimer.cancel();
            sensorTimer = null;
        }
        mSensorResults.clear();
    }

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

    /**
     * To be called upon destroy
     */
    public void CleanUp() {
        if (sensorTimer != null)
            sensorTimer.cancel();
        sensorTimer = null;
        sensorManager.unregisterListener(this);
    }
}
