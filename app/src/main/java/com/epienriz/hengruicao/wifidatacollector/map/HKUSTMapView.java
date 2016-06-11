package com.epienriz.hengruicao.wifidatacollector.map;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.DragEvent;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import com.epienriz.hengruicao.wifidatacollector.R;
import com.epienriz.hengruicao.wifidatacollector.core.DataListener;
import com.epienriz.hengruicao.wifidatacollector.data.HKUSTLabel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TODO: document your custom view class.
 */
public class HKUSTMapView extends View implements View.OnDragListener {

    private Rect mCanvasBounds = new Rect();

    public interface WifiScanListener {
        void WifiScanPosition(String floor, PointF position);
    }

    private HKUSTMap map;
    private HKUSTMapLabel mapLabels;
    private MapInteractionDetector interactionDetector;

    private Drawable mLocationIcon;
    private Drawable mConfirmIcon;

    //Gesture handling
    private WifiScanListener mWifiScanListener;
    private PointF mWaitForTrigger = null;

    //Constructors
    public HKUSTMapView(Context context) {
        super(context);
        init(null, 0);
    }

    public HKUSTMapView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public HKUSTMapView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    private void init(AttributeSet attrs, int defStyle) {
        map = new HKUSTMap(getContext());
        //Redraw if image changed
        map.setImageChangeListener(new DataListener<Void>() {
            @Override
            public void notifyResult(Void result) {
                invalidate();
            }
        });

        floor_locations = new HashMap<>();
        mPaint = new Paint();
        mPaint.setColor(getResources().getColor(android.R.color.black));
        mLocationIcon = getResources().getDrawable(R.drawable.tick_green);
        mConfirmIcon = getResources().getDrawable(R.drawable.location_icon);

        map.fetchImages();
        this.setOnDragListener(this);

        interactionDetector = new MapInteractionDetector(getContext());
        interactionDetector.setRedrawListener(new DataListener<Void>() {
            @Override
            public void notifyResult(Void result) {
                invalidate();
            }
        });

        interactionDetector.setDragListener(new DataListener<PointF>() {
            @Override
            public void notifyResult(PointF result) {
                updatePosition();
            }
        });

        interactionDetector.setTapListener(new DataListener<PointF>() {
            @Override
            public void notifyResult(PointF result) {
                if (mWaitForTrigger != null) {
                    PointF p = mWaitForTrigger;
                    //Test for collision here
                    Rect bound = mConfirmIcon.getBounds();
                    int clickx = (int)(result.x / interactionDetector.mScaleFactor + mCanvasBounds.left);
                    int clicky = (int)(result.y / interactionDetector.mScaleFactor + mCanvasBounds.top);
                    Log.w("Bounds ", String.format("%d %d", bound.left, bound.right));
                    Log.w("Click region ", String.format("%d %d", clickx, clicky));
                    if (bound.contains(
                            clickx, clicky
                    ) && mWifiScanListener != null) {
                        mWifiScanListener.WifiScanPosition(getFloor(), mWaitForTrigger);
                        mWaitForTrigger = null;
                        invalidate();
                    }
                }
            }
        });
        mapLabels = new HKUSTMapLabel(getContext());
        mapLabels.setLabelListener(new DataListener<List<HKUSTLabel>>() {
            @Override
            public void notifyResult(List<HKUSTLabel> result) {
                invalidate();
            }
        });
    }


    Paint mPaint;
    public void setWifiScanListener(WifiScanListener mWifiScanListener) {
        this.mWifiScanListener = mWifiScanListener;
    }


    private int getScreenWidth() {
        return getWidth() - getPaddingLeft() - getPaddingRight();
    }

    private int getScreenHeight() {
        return getHeight() - getPaddingTop() - getPaddingBottom();
    }

    private PointF getScreenContentRatio() {
        float contentWidth = (HKUSTMap.DIM_X - 1) * HKUSTMap.MAP_DIM;
        float contentHeight = (HKUSTMap.DIM_Y - 1) * HKUSTMap.MAP_DIM;
        PointF rt = new PointF(contentWidth / getScreenWidth(), contentHeight / getScreenHeight());

        Log.d("rt ", String.format("%f %f", rt.x, rt.y));
        return rt;
    }

    public PointF convertTouchToPosition(PointF touch) {
        PointF rt = getScreenContentRatio();
        rt.x *= touch.x / interactionDetector.mScaleFactor;
        rt.y *= touch.y / interactionDetector.mScaleFactor;
        rt.x += map.mCenterX - HKUSTMap.DIM_X / 2 * HKUSTMap.MAP_DIM;
        rt.y += map.mCenterY - HKUSTMap.DIM_Y / 2 * HKUSTMap.MAP_DIM;
        return rt;
    }

    Map<String, List<PointF>> floor_locations;

    private void updatePosition() {
        PointF ratio = getScreenContentRatio();

        map.mCenterX -= interactionDetector.mDiffX * ratio.x / interactionDetector.mScaleFactor;
        map.mCenterY -= interactionDetector.mDiffY * ratio.y / interactionDetector.mScaleFactor;
        interactionDetector.mDiffX = 0f;
        interactionDetector.mDiffY = 0f;
        map.mCenterX = Math.max(0, map.mCenterX);
        map.mCenterY = Math.max(0, map.mCenterY);

        Log.d("Center ", String.format("%.1f %.1f", map.mCenterX, map.mCenterY));
        map.fetchImages();
        mapLabels.updatePosition(map);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int pl = getPaddingLeft();
        int pt = getPaddingTop();

        canvas.save();
        canvas.translate(getScreenWidth() / 2, getScreenHeight() / 2);
        canvas.translate(interactionDetector.mDiffX, interactionDetector.mDiffY);
        canvas.scale(interactionDetector.mScaleFactor, interactionDetector.mScaleFactor);

        canvas.getClipBounds(mCanvasBounds);
        int w = getScreenWidth() / (HKUSTMap.DIM_X - 1);
        int h = getScreenHeight() / (HKUSTMap.DIM_Y - 1);

        //Log.d(" w h", String.format("%d %d", w, h));
        for (int y = 0; y < HKUSTMap.DIM_Y; ++y) {
            for (int x = 0; x < HKUSTMap.DIM_X; ++x) {
                Point center = map.getMapImageOffsetFromIndex(x, y);
                int sx = (int)(w * (center.x - map.mCenterX) / HKUSTMap.MAP_DIM );
                int sy = (int)(h * (center.y - map.mCenterY) / HKUSTMap.MAP_DIM );

                Bitmap img = map.images[y][x];
                if (img != null) {
                    canvas.drawBitmap(img, null,
                            new Rect(pl + sx, pt + sy,
                                    pl + sx + w, pt + sy + h),
                            null);
                    //Log.d("img y x", String.format("%d %d: %d %d", y, x, sy, sx));
                }
            }
        }
        //canvas.save();
        mapLabels.drawOnCanvas(map, canvas, w, h);
        //canvas.restore();

        List<PointF> locations = getFloorLocation(map.getFloor());
        for (PointF p : locations) {
            int sx = (int)(w * (p.x - map.mCenterX) / HKUSTMap.MAP_DIM );
            int sy = (int)(h * (p.y - map.mCenterY) / HKUSTMap.MAP_DIM );
            mLocationIcon.setBounds(pl + sx - 20, pt + sy - 20,
                    pl + sx + 20, pt + sy + 20);
            mLocationIcon.draw(canvas);
        }
        if (mWaitForTrigger != null) {
            PointF p = mWaitForTrigger;
            int sx = (int)(w * (p.x - map.mCenterX) / HKUSTMap.MAP_DIM );
            int sy = (int)(h * (p.y - map.mCenterY) / HKUSTMap.MAP_DIM );
            mConfirmIcon.setBounds(pl + sx - 30, pt + sy - 50,
                    pl + sx + 30, pt + sy + 10);
            mConfirmIcon.draw(canvas);
        }
        canvas.restore();
    }

    public String getFloor() {
        return map.getFloor();
    }

    public void setFloor(String mFloor) {
        map.setFloor(mFloor);
        mapLabels.updatePosition(map);
    }

    public float getCenterX() {
        return map.mCenterX;
    }

    public float getCenterY() {
        return map.mCenterY;
    }

    @Override
    public boolean onDrag(View v, DragEvent event) {
         if (event.getAction() ==  DragEvent.ACTION_DROP) {
             confirmScanPosition(new PointF(event.getX(), event.getY()));
         }
        return true;
    }

    //add a confirm position into canvas
    private void confirmScanPosition(PointF pointF) {
        Rect hitRect = new Rect();
        this.getHitRect(hitRect);

        interactionDetector.mDiffY = (getScreenHeight() / 2.0f - pointF.y);
        interactionDetector.mDiffX = (getScreenWidth() / 2.0f - pointF.x);
        updatePosition();
        mWaitForTrigger = new PointF(map.mCenterX, map.mCenterY);
        Log.d("Wait for trigger", String.format("%.1f %.1f", mWaitForTrigger.x, mWaitForTrigger.y));
        invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        //other touch event
        return interactionDetector.onTouchEvent(ev);
    }

    List<PointF> getFloorLocation(String floor) {
        List<PointF> rt = floor_locations.get(floor);
        if (rt == null) {
            rt = new ArrayList<>();
            floor_locations.put(floor, rt);
        }
        return rt;
    }

    public void addLocation(PointF location, String floor) {
        if (floor == null) {
            floor = map.getFloor();
        }
        getFloorLocation(floor).add(location);
        invalidate();
    }

    public void centerTo(PointF location, String floor) {
        setFloor(floor);
        mWaitForTrigger = new PointF(location.x, location.y);
        map.mCenterX = location.x;
        map.mCenterY = location.y;
        updatePosition();
    }
}
