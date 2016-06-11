package com.epienriz.hengruicao.wifidatacollector.data;

import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.util.Log;

import org.apache.commons.lang3.ObjectUtils;

/**
 * Created by hengruicao on 6/8/16.
 * Label to display on map
 */
public class HKUSTLabel {
    public int x;
    public int y;
    public String label;
    public String type;
    public String type2;
    public String type3;
    public String identifier;

    final static String [] POSSIBLE_TYPES = new String[]{
            "male toilet",
            "female toilet",
            "stair",
            "drinking fountain",
            "escalator",
            "restaurant",
            "general" //probably no specific icons
    };

    private HKUSTLabel(String csv) {
        String[] ps = csv.split(";");
        String[] coords = ps[0].split(",");
        try {
            x = Integer.parseInt(coords[0]);
            y = Integer.parseInt(coords[1]);
            label = ps[1];
            type = ps[2];
            type2 = ps[3];
            type3 = ps[4];
            identifier = ps[5];
        }catch (Exception e) {
            Log.e("error", csv);
        }
    }

    public HKUSTLabel() {
    }

    public static HKUSTLabel fromCSV(String csv) {
        if (csv.length() == 0)
            return null;
        HKUSTLabel hkustLabel = new HKUSTLabel(csv);
        return hkustLabel;
    }

    public Point getCoord() {
        return new Point(x, y);
    }

}
