package com.epienriz.hengruicao.wifidatacollector.api;

import android.util.ArrayMap;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by hengruicao on 5/5/16.
 */
public class BmobRequest extends JsonObjectRequest {
    Map<String,String> mHeaders;

    public static BmobRequest build(int method, String url, JSONObject jsonRequest,
                                    final Response.Listener<JSONObject> listener, final Response.ErrorListener errorListener) {
        try {
            jsonRequest.put("ACL", new JSONObject().put("*", new JSONObject().put("read", true)));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return new BmobRequest(method, url, jsonRequest, listener, errorListener);
    }

    private BmobRequest(int method, String url, JSONObject jsonRequest,
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
