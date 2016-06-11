package com.epienriz.hengruicao.wifidatacollector.map;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.text.TextPaint;
import android.util.Log;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.Volley;
import com.epienriz.hengruicao.wifidatacollector.api.HKUSTMapBase;
import com.epienriz.hengruicao.wifidatacollector.api.VolleySingleton;
import com.epienriz.hengruicao.wifidatacollector.core.DataListener;
import com.epienriz.hengruicao.wifidatacollector.data.HKUSTLabel;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by hengruicao on 6/11/16.
 */
public class HKUSTMapLabel {
    public List<HKUSTLabel> labels;

    DataListener<List<HKUSTLabel> > labelListener;
    VolleySingleton volley;

    public HKUSTMapLabel(Context context){
        labels = new ArrayList<>();
        volley = VolleySingleton.getInstance(context);
    }

    public void setLabelListener(DataListener<List<HKUSTLabel>> labelListener) {
        this.labelListener = labelListener;
    }

    public void updatePosition(HKUSTMap map) {

        int leftX = Math.max((int)(map.scaleNormalize(map.mCenterX - 0.5f * HKUSTMap.DIM_X * HKUSTMap.MAP_DIM)), 0);
        int topY = Math.max((int)(map.scaleNormalize(map.mCenterY - 0.5f * HKUSTMap.DIM_Y * HKUSTMap.MAP_DIM)), 0);
        int sizeX = (int)map.scaleNormalize(HKUSTMap.DIM_X * HKUSTMap.MAP_DIM);
        int sizeY = (int)map.scaleNormalize(HKUSTMap.DIM_Y * HKUSTMap.MAP_DIM);

        Log.d("HKUSTLabel fetch addr", String.format("%d %d %d %d", leftX, topY, sizeX, sizeY));
        volley.addToRequestQueue(new HKUSTLabelRequest(Request.Method.GET,
                HKUSTMapBase.formatMapLabelUrl(map.getFloor(), leftX, topY, sizeX, sizeY),
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {

                    }
                }
        ));
    }

    TextPaint mTextPaint = new TextPaint();

    public void drawOnCanvas(HKUSTMap map, Canvas canvas, int w, int h) {
        mTextPaint.setColor(Color.BLACK);
        mTextPaint.setTextSize(12);
        mTextPaint.setTextAlign(Paint.Align.CENTER);
        for (HKUSTLabel label : labels) {
            int x = (int)(w * (map.unscaleNormalize(label.x) - map.mCenterX) / HKUSTMap.MAP_DIM );
            int y = (int)(h * (map.unscaleNormalize(label.y) - map.mCenterY) / HKUSTMap.MAP_DIM );
            //only draw this with no type for now
            if (true) {
                String text = label.label;
                String []words = text.split(" ");
                y -= words.length / 2 * (mTextPaint.descent() - mTextPaint.ascent());
                for (String word : words) {
                    canvas.drawText(word, x, y, mTextPaint);
                    y += mTextPaint.descent() - mTextPaint.ascent();
                }
            }
        }
    }

    private class HKUSTLabelRequest extends Request<List<HKUSTLabel> > {

        public HKUSTLabelRequest(int method, String url, Response.ErrorListener listener) {
            super(method, url, listener);
        }

        @Override
        protected Response<List<HKUSTLabel>> parseNetworkResponse(NetworkResponse response) {
            try {
                String csvString = new String(response.data,
                        HttpHeaderParser.parseCharset(response.headers));
                List<HKUSTLabel> result = new ArrayList<>();
                String[] csvs = csvString.split("\n");
                for (int i = 0; i < csvs.length; ++i) {
                    HKUSTLabel toAdd = HKUSTLabel.fromCSV(csvs[i]);
                    if (toAdd == null)
                        continue;
                    result.add(toAdd);
                }
                return Response.success(result,
                        HttpHeaderParser.parseCacheHeaders(response));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            Log.e("CSV label", "failed");
            return Response.error(new VolleyError());
        }

        @Override
        protected void deliverResponse(List<HKUSTLabel> response) {
            labels = response;
            labelListener.notifyResult(response);
        }
    }
}
