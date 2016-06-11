package com.epienriz.hengruicao.wifidatacollector.map;

import android.content.Context;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import com.epienriz.hengruicao.wifidatacollector.core.DataListener;


public class MapInteractionDetector {

    private final GestureDetector mGestureDetector;
    private final static float MAX_SCALE = 3.f;
    private final static float MIN_SCALE = 1.f;

    //reference http://stackoverflow.com/questions/5743328/image-in-canvas-with-touch-events/5747233#5747233
    public float mScaleFactor = 1.f;
    public float mDiffX;
    public float mDiffY;

    private static final int INVALID_POINTER_ID = -1;

    private float mLastTouchX;
    private float mLastTouchY;
    private int mActivePointerId = INVALID_POINTER_ID;

    private ScaleGestureDetector mScaleDetector;

    private DataListener<PointF> dragListener;
    private DataListener<PointF> tapListener;

    private DataListener<Void> redrawListener;

    public MapInteractionDetector(Context context) {
        mScaleDetector = new ScaleGestureDetector(context, new ScaleListener());
        mGestureDetector = new GestureDetector(context, new SingleTapConfirm());
    }

    public void setTapListener(DataListener<PointF> tapListener) {
        this.tapListener = tapListener;
    }

    public void setDragListener(DataListener<PointF> dragListener) {
        this.dragListener = dragListener;
    }

    public void setRedrawListener(DataListener<Void> redrawListener) {
        this.redrawListener = redrawListener;
    }

    private class SingleTapConfirm extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent event) {
            return true;
        }
        @Override
        public boolean onSingleTapUp(MotionEvent event) {
            return true;
        }
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            mScaleFactor *= detector.getScaleFactor();

            // Don't let the object get too small or too large.
            mScaleFactor = Math.max(MIN_SCALE, Math.min(mScaleFactor, MAX_SCALE));
            redrawListener.notifyResult(null);
            return true;
        }
    }

    public boolean onTouchEvent(MotionEvent ev) {
        // Let the ScaleGestureDetector inspect all events.
        mScaleDetector.onTouchEvent(ev);
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
                if (pointerIndex < 0 || pointerIndex >= ev.getPointerCount()) {
                    return false;
                }
                final float x = ev.getX();
                final float y = ev.getY();

                // Only move if the ScaleGestureDetector isn't processing a gesture.
                if (!mScaleDetector.isInProgress()) {
                    final float dx = x - mLastTouchX;
                    final float dy = y - mLastTouchY;

                    Log.d("motion ", String.format("%f %f", dx, dy));
                    mDiffX += dx;
                    mDiffY += dy;
                    redrawListener.notifyResult(null);
                }
                mLastTouchX = x;
                mLastTouchY = y;
                break;
            }

            case MotionEvent.ACTION_UP: {
                mActivePointerId = INVALID_POINTER_ID;
                Log.d("MotionEvent", "Action up");
                dragListener.notifyResult(new PointF(mDiffX, mDiffY));
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
        if (mGestureDetector.onTouchEvent(ev)) {
            Log.d("Touched", String.format("%.1f %.1f", ev.getX(), ev.getY()));
            tapListener.notifyResult(new PointF(ev.getX(), ev.getY()));
        }
        return true;
    }
}
