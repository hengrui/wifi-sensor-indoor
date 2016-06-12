package com.epienriz.hengruicao.wifidatacollector.core;

import android.content.Context;

import com.epienriz.hengruicao.wifidatacollector.activity.MainActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by hengruicao on 6/11/16.
 * Temporary class to interpret server response of knn
 */
public class LocalizeKNN {

    Context mainActivity;
    public LocalizeKNN(Context mtv) {
        mainActivity = mtv;
    }

    /**
     * Return an object with location, x and y
     */
    public KEntry localize(JSONObject response) throws JSONException {
        List<KEntry> entries = new ArrayList<>();
        JSONArray array = response.getJSONArray("nearest");
        Map<String, Integer> floorCount = new HashMap<>();
        for (int i = 0; i < array.length(); ++i) {
            try {
                KEntry entry = new KEntry(array.getJSONObject(i));
                if (!floorCount.containsKey(entry.floor)) {
                    floorCount.put(entry.floor, 1);
                } else {
                    floorCount.put(entry.floor, floorCount.get(entry.floor) + 1);
                }
                entries.add(entry);
            }catch (Exception e) {
            }
        }
        int best = 0;
        List<KEntry> mostMatchedFloor = new ArrayList<>();
        for (KEntry entry : entries) {
            if (floorCount.get(entry.floor) > best) {
                mostMatchedFloor.clear();
                mostMatchedFloor.add(entry);
                best = floorCount.get(entry.floor);
            } else if (entry.floor.compareTo(mostMatchedFloor.get(0).floor) == 0) {
                mostMatchedFloor.add(entry);
            }
        }
        KEntry result = new KEntry();
        for (KEntry entry : mostMatchedFloor) {
            result.distance += entry.distance;
            result.floor = entry.floor;
            result.x += (entry.x);
            result.y += (entry.y);
        }
        result.distance /= (mostMatchedFloor.size());
        result.x /= mostMatchedFloor.size();
        result.y /= mostMatchedFloor.size();
        return result;
    }

    public class KEntry {
        public int distance;
        public String floor;
        public double x, y;
        public KEntry(){}

        public KEntry(JSONObject entry) throws JSONException {
            JSONObject location = entry.getJSONObject("location");
            floor = location.getString("floor");
            x = location.getDouble("x");
            y = location.getDouble("y");
            distance = entry.getInt("distance");
        }
    }
}
