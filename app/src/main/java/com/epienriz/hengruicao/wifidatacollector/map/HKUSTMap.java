package com.epienriz.hengruicao.wifidatacollector.map;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.PointF;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.epienriz.hengruicao.wifidatacollector.api.HKUSTMapBase;
import com.epienriz.hengruicao.wifidatacollector.api.VolleySingleton;
import com.epienriz.hengruicao.wifidatacollector.core.DataListener;

/**
 * Created by hengruicao on 6/10/16.
 */
public class HKUSTMap {
    /**
     * Dimension of the bitmap in x and y
     */
    public static int DIM_X = 3;
    public static int DIM_Y = 3;
    /**
     * Use map dimension of 200 defined by hkust
     */
    public static int MAP_DIM = 200;
    private int HKUST_SCALE = 3;

    public Bitmap[][] images;
    private String mFloor = "G";
    public float mCenterX, mCenterY;

    DataListener<Void> imageChangeListener;
    private final ImageLoader mImageLoader;


    public HKUSTMap(Context context) {
        images = new Bitmap[3][3];
        mImageLoader = VolleySingleton.getInstance(context).getImageLoader();
        mCenterX = 1000;
        mCenterY = 600;
    }

    //http://pathadvisor.ust.hk/super_global.js?20160417
    public static float[] scaleLevels = new float[]{1.0f,0.85f,0.6f,0.5f,0.4f};

    public float scaleNormalize(float value) {
        return (value / scaleLevels[HKUST_SCALE - 1]);
    }

    public float unscaleNormalize(float value) {
        return (value * scaleLevels[HKUST_SCALE - 1]);
    }


    public void fetchImages() {
        //images = new Bitmap[DIM_Y][DIM_X];
        final Bitmap[][] localmaps = images;

        for (int y = 0; y < images.length; ++y) {
            for (int x = 0; x < images[y].length; ++x) {
                Point offset = getMapImageOffsetFromIndex(x, y);
                final int ix = x;
                final int iy = y;
                mImageLoader.get(HKUSTMapBase.formatMapImageUrl(offset.x, offset.y, mFloor, HKUST_SCALE),
                        new ImageLoader.ImageListener() {
                            @Override
                            public void onResponse(ImageLoader.ImageContainer response, boolean isImmediate) {
                                localmaps[iy][ix] = response.getBitmap();
                                imageChangeListener.notifyResult(null);
                            }
                            @Override
                            public void onErrorResponse(VolleyError error) {
                            }
                        });
            }
        }
    }


    public Point getMapImageOffset(int x, int y) {
        int centerX = x - x % MAP_DIM;
        int centerY = y - y % MAP_DIM;
        return new Point(centerX, centerY);
    }

    public Point getMapImageOffsetFromIndex(int x, int y) {
        Point center = getMapImageOffset((int)mCenterX, (int)mCenterY);
        int cx = center.x + (x - (DIM_X) / 2) * MAP_DIM;
        int cy = center.y + (y - (DIM_Y) / 2) * MAP_DIM;
        return new Point(cx, cy);
    }

    public void setImageChangeListener(DataListener<Void> imageChangeListener) {
        this.imageChangeListener = imageChangeListener;
    }

    public String getFloor() {
        return mFloor;
    }

    public void setFloor(String mFloor) {
        if (this.mFloor.compareToIgnoreCase(mFloor) != 0) {
            this.mFloor = mFloor;
            fetchImages();
        }
    }

}
