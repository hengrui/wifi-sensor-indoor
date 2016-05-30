package com.epienriz.hengruicao.wifidatacollector.api;

import android.util.Log;

/**
 * Created by hengruicao on 5/22/16.
 */
public class HKUSTMapBase {
    public static final String baseUrl = "http://pathadvisor.ust.hk/";

    //@param
    //x, y, floor, level
    public static final String mapImageUrl = baseUrl + "map_pixel.php";

    //@param
    //floor, mapcoorx, mapcoory, level
    public static final String mapLabelUrl = baseUrl + "get_map_data_2.php";

    //@param
    //floor, keyword
    public static final String queryUrl = baseUrl + "phplib/search.php";

    public static String formatMapImageUrl(int x, int y, String floor, int level) {
        String rt = String.format(mapImageUrl + "?x=%d&y=%d&floor=%s&level=%d", x, y, floor, level);
        Log.d("format map url", rt);
        return rt;
    }
}
