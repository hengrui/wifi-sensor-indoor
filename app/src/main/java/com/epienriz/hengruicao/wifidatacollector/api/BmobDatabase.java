package com.epienriz.hengruicao.wifidatacollector.api;

/**
 * Created by hengruicao on 5/5/16.
 */
public class BmobDatabase {
    public static final String baseUrl = "https://api.bmob.cn";

    public static final String postTestUrl = baseUrl + "/1/classes/Test/";

    public static final String postWifiUrl = baseUrl + "/1/classes/Wifi2/";

    public static final String postSensorUrl = baseUrl + "/1/classes/Sensor/";

    public static final String batchUrl = baseUrl + "/1/batch";


    //For js api
    /**
     * JS API callbacks
     * Syntax as follow ${apiBaseUrl}/${apiKey}/${method}
     */
    public static final String apiKey = "47212f1fa79e88a1";
    public static final String apiBaseUrl = "http://cloud.bmob.cn/";

    public static final String localizeUrl = apiBaseUrl + apiKey + "/localize2";
}
