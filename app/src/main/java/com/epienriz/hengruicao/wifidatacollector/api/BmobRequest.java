package com.epienriz.hengruicao.wifidatacollector.api;

import android.util.ArrayMap;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by hengruicao on 5/5/16.
 */
public class BmobRequest extends JsonObjectRequest {
    Map<String,String> mHeaders;

    /**
     *
     * @param method either get or post
     * @param url remote url to get or post
     * @param jsonRequest the object in JSON for the parameter
     * @param listener nullable, called on success
     * @param errorListener nullable, called on error
     * @return A JsonObjectRequest that can be added to VolleyQueue
     */
    public static BmobRequest apiRequest(int method, String url, JSONObject jsonRequest, final Response.Listener<JSONObject> listener, final Response.ErrorListener errorListener) {
        return new BmobRequest(method, url, jsonRequest, listener, errorListener);
    }

    public static BmobRequest build(int method, String url, JSONObject jsonRequest,
                                    final Response.Listener<JSONObject> listener, final Response.ErrorListener errorListener) {
        try {
            jsonRequest.put("ACL", new JSONObject().put("*", new JSONObject().put("read", true)));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return new BmobRequest(method, url, jsonRequest, listener, errorListener);
    }

    //bmob support maximum 50 batch requests
    public static List<BmobRequest> batchManyPost(String url, List<JSONObject> objects,
                                                  Response.Listener<JSONObject> listener,
                                                  Response.ErrorListener errorListener) {
        int i = 0;
        List<BmobRequest> rt = new ArrayList<>();
        List<JSONObject> tmp = new ArrayList<>();
        for (JSONObject robj : objects) {
            if (i % 50 == 0 && tmp.size() > 0) {
                rt.add(batchPost(url, tmp, listener, errorListener));
                tmp.clear();
            }
            tmp.add(robj);
            ++i;
        }
        if (tmp.size() > 0) {
            rt.add(batchPost(url, tmp, listener, errorListener));
        }
        return rt;
    }

    //batch number of objects to one url
    public static BmobRequest batchPost(String url, List<JSONObject> objects,
                                        Response.Listener<JSONObject> listener,
                                        Response.ErrorListener errorListener) {
        JSONObject obj = new JSONObject();
        try {
            JSONArray requests = new JSONArray();
            for (JSONObject o : objects) {
                JSONObject element = new JSONObject();
                o.put("ACL", new JSONObject().put("*", new JSONObject().put("read", true)));
                element.put("body", o);
                element.put("method", "POST");
                element.put("path", url.substring(BmobDatabase.baseUrl.length()));
                requests.put(element);
            }
            obj.put("requests", requests);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Log.d("Request obj", obj.toString());
        return new BmobRequest(Method.POST, BmobDatabase.batchUrl, obj, listener, errorListener);
    }

    protected BmobRequest(int method, String url, JSONObject jsonRequest,
                       final Response.Listener<JSONObject> listener, final Response.ErrorListener errorListener) {
        super(method, url, jsonRequest,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        if (!response.has("error")) {
                            if (listener != null)
                                listener.onResponse(response);
                        } else {
                            if (errorListener != null)
                                errorListener.onErrorResponse(null);
                            try {
                                Log.d("Bmob error", response.getString("error"));
                                Log.d("Bmob error code", String.valueOf(response.getInt("code")));
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        if (error == null || error.networkResponse == null)
                            return;
                        if (error.networkResponse.statusCode == 404) {
                            JSONObject response = null;
                            try {
                                response = new JSONObject(new String(error.networkResponse.data));
                                Log.d("Bmob error", response.getString("error"));
                                Log.d("Bmob error code", String.valueOf(response.getInt("code")));
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                        if (errorListener != null)
                            errorListener.onErrorResponse(error);
                    }
                });
        mHeaders = new HashMap<String, String>();
        mHeaders.put("X-Bmob-Application-Id", "7986eab98c018a5d3cc71f8af29e1d59");
        mHeaders.put("Content-Type", "application/json");
        mHeaders.put("X-Bmob-REST-API-Key", "4c9a650de051305c88efe57b150fe58b");
    }

    public void addHeader(String key, String value) {
        mHeaders.put(key, value);
    }

    @Override
    public Map<String, String> getHeaders() throws AuthFailureError {
        return mHeaders;
    }
}
