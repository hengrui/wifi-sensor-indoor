package com.epienriz.hengruicao.wifidatacollector;

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
import android.view.MotionEvent;
import android.view.View;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.epienriz.hengruicao.wifidatacollector.api.HKUSTMapBase;
import com.epienriz.hengruicao.wifidatacollector.api.VolleySingleton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TODO: document your custom view class.
 */
public class HKUSTMapView extends View {
    /**
     * Dimension of the bitmap in x and y
     */
    private static int DIM_X = 3;
    private static int DIM_Y = 3;

    /**
     * Use map dimension of 200 defined by hkust
     */
    private static int MAP_DIM = 200;

    private Bitmap[][] maps;

    private String mFloor;
    private int mScale;

    private ImageLoader mImageLoader;
    int mCenterX, mCenterY;

    private Drawable mLocationIcon;


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

    Paint mPaint;

    private void init(AttributeSet attrs, int defStyle) {
        mDiffX = 0f;
        mDiffY = 0f;
        mCenterX = 1000;
        mCenterY = 600;
        mFloor = "G";
        mScale = 3;
        floor_locations = new HashMap<>();
        mPaint = new Paint();
        mPaint.setColor(getResources().getColor(android.R.color.white));
        mLocationIcon = getResources().getDrawable(R.drawable.tick_green);
        fetchImages();
    }

    private Rect getContentRect() {
        int width = MAP_DIM * 200;
        int height = MAP_DIM * 200;
        Rect rt = new Rect(-width / 2, -height / 2, width / 2, height / 2);

        rt.offset(mCenterX, mCenterY);
        return rt;
    }

    private int getScreenWidth() {
        return getWidth() - getPaddingLeft() - getPaddingRight();
    }

    private int getScreenHeight() {
        return getHeight() - getPaddingTop() - getPaddingBottom();
    }

    private PointF getScreenContentRatio() {
        float contentWidth = (DIM_X - 1) * MAP_DIM;
        float contentHeight = (DIM_Y - 1) * MAP_DIM;
        PointF rt = new PointF(contentWidth / getScreenWidth(), contentHeight / getScreenHeight());

        Log.d("rt ", String.format("%f %f", rt.x, rt.y));
        return rt;
    }

    private Point getMapImageOffset(int x, int y) {
        int centerX = x - x % MAP_DIM;
        int centerY = y - y % MAP_DIM;
        return new Point(centerX, centerY);
    }

    private Point getMapImageOffsetFromIndex(int x, int y) {
        Point center = getMapImageOffset(mCenterX, mCenterY);
        int cx = center.x + (x - (maps[y].length) / 2) * MAP_DIM;
        int cy = center.y + (y - (maps.length) / 2) * MAP_DIM;
        return new Point(cx, cy);
    }

    public PointF convertTouchToPosition(PointF touch) {
        PointF rt = getScreenContentRatio();
        rt.x *= touch.x;
        rt.y *= touch.y;
        rt.x += mCenterX - DIM_X / 2 * MAP_DIM;
        rt.y += mCenterY - DIM_Y / 2 * MAP_DIM;
        return rt;
    }

    Map<String, List<PointF>> floor_locations;

    private void updatePosition() {
        PointF ratio = getScreenContentRatio();

        mCenterX -= mDiffX * ratio.x;
        mCenterY -= mDiffY * ratio.y;
        mDiffX = 0f;
        mDiffY = 0f;

        mCenterX = Math.max(0, mCenterX);
        mCenterY = Math.max(0, mCenterY);

        //mCenterX = Math.max(0, mBorderX);
        //mCenterY = Math.max(0, mBorderY);

        Log.d("Center ", String.format("%d %d", mCenterX, mCenterY));
        fetchImages();
    }

    private void fetchImages() {
        maps = new Bitmap[DIM_Y][DIM_X];
        mImageLoader = VolleySingleton.getInstance(getContext()).getImageLoader();
        final Bitmap[][] localmaps = maps;

        for (int y = 0; y < maps.length; ++y) {
            for (int x = 0; x < maps[y].length; ++x) {
                Point offset = getMapImageOffsetFromIndex(x, y);
                final int ix = x;
                final int iy = y;
                mImageLoader.get(HKUSTMapBase.formatMapImageUrl(offset.x, offset.y, mFloor, mScale),
                        new ImageLoader.ImageListener() {
                            @Override
                            public void onResponse(ImageLoader.ImageContainer response, boolean isImmediate) {
                                localmaps[iy][ix] = response.getBitmap();
                                invalidate();
                            }
                            @Override
                            public void onErrorResponse(VolleyError error) {

                            }
                        });
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int pl = getPaddingLeft();
        int pt = getPaddingTop();

        canvas.save();
        canvas.translate(mDiffX, mDiffY);
        int w = getScreenWidth() / (DIM_X - 1);
        int h = getScreenHeight() / (DIM_Y - 1);
        Log.d(" w h", String.format("%d %d", w, h));
        for (int y = 0; y < maps.length; ++y) {
            for (int x = 0; x < maps[y].length; ++x) {
                Point center = getMapImageOffsetFromIndex(x, y);
                int sx = w * (center.x - mCenterX) / MAP_DIM + getScreenWidth() / 2;
                int sy = h * (center.y - mCenterY) / MAP_DIM + getScreenHeight() / 2;

                Bitmap img = maps[y][x];
                if (img != null) {
                    canvas.drawBitmap(img, null,
                            new Rect(pl + sx, pt + sy,
                                    pl + sx + w, pt + sy + h),
                            null);
                    Log.d("img y x", String.format("%d %d: %d %d", y, x, sy, sx));
                }
            }
        }
        List<PointF> locations = getFloorLocation(mFloor);
        for (PointF p : locations) {
            int sx = (int)(w * (p.x - mCenterX) / MAP_DIM + getScreenWidth() / 2);
            int sy = (int)(h * (p.y - mCenterY) / MAP_DIM + getScreenHeight() / 2);
            mLocationIcon.setBounds(pl + sx - 20, pt + sy - 20,
                    pl + sx + 20, pt + sy + 20);
            mLocationIcon.draw(canvas);
        }
        canvas.restore();
    }

    public int getScale() {
        return mScale;
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

    public int getCenterX() {
        return mCenterX;
    }

    public int getCenterY() {
        return mCenterY;
    }


    //reference http://stackoverflow.com/questions/5743328/image-in-canvas-with-touch-events/5747233#5747233
    private static final int INVALID_POINTER_ID = -1;
    private float mDiffX;
    private float mDiffY;

    private float mLastTouchX;
    private float mLastTouchY;
    private int mActivePointerId = INVALID_POINTER_ID;
    private float mScaleFactor = 1.f;

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        // Let the ScaleGestureDetector inspect all events.
        // mScaleDetector.onTouchEvent(ev);

        final int action = ev.getAction();
        switch (action & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN: {
                final float x = ev.getX();
                final float y = ev.getY();

                mLastTouchX = x;
                mLastTouchY = y;
                mActivePointerId = ev.getPointerId(0);
                break;
            }

            case MotionEvent.ACTION_MOVE: {
                final int pointerIndex = ev.findPointerIndex(mActivePointerId);
                final float x = ev.getX(pointerIndex);
                final float y = ev.getY(pointerIndex);

                // Only move if the ScaleGestureDetector isn't processing a gesture.
//                if (!mScaleDetector.isInProgress()) {
                if (true){
                    final float dx = x - mLastTouchX;
                    final float dy = y - mLastTouchY;

                    Log.d("motion ", String.format("%f %f", dx, dy));
                    mDiffX += dx;
                    mDiffY += dy;
                    invalidate();
                }
                mLastTouchX = x;
                mLastTouchY = y;
                break;
            }

            case MotionEvent.ACTION_UP: {
                mActivePointerId = INVALID_POINTER_ID;
                Log.d("MotionEvent", "Action up");
                updatePosition();
                break;
            }

            case MotionEvent.ACTION_CANCEL: {
                mActivePointerId = INVALID_POINTER_ID;
                break;
            }

            case MotionEvent.ACTION_POINTER_UP: {
                final int pointerIndex = (ev.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK)
                        >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
                final int pointerId = ev.getPointerId(pointerIndex);
                if (pointerId == mActivePointerId) {
                    // This was our active pointer going up. Choose a new
                    // active pointer and adjust accordingly.
                    final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
                    mLastTouchX = ev.getX(newPointerIndex);
                    mLastTouchY = ev.getY(newPointerIndex);
                    mActivePointerId = ev.getPointerId(newPointerIndex);
                }
                break;
            }
        }
        return true;
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
            floor = mFloor;
        }
        getFloorLocation(floor).add(location);
        invalidate();
    }
}
