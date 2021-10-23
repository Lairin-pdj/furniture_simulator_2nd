package com.example.cameratest.helper;

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.View.OnTouchListener;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Helper to detect taps using Android GestureDetector, and pass the taps between UI thread and
 * render thread.
 */
public final class TapHelper implements OnTouchListener {
    private final GestureDetector gestureDetector;
    private final BlockingQueue<MotionEvent> queuedSingleTaps = new ArrayBlockingQueue<>(16);
    private int checkStatus = 0;
    private float distance;

    /**
     * Creates the tap helper.
     *
     * @param context the application's context.
     */
    public TapHelper(Context context) {
        gestureDetector =
                new GestureDetector(
                        context,
                        new GestureDetector.SimpleOnGestureListener() {
                            @Override
                            public boolean onSingleTapConfirmed(MotionEvent e){
                                queuedSingleTaps.offer(e);
                                checkStatus = 0;
                                distance = 0;
                                return true;
                            }

                            //확실한 구분을 위해 변경되기 전 코드드
                           @Override
                            public boolean onSingleTapUp(MotionEvent e) {
                                /*
                                queuedSingleTaps.offer(e);
                                checkStatus = 0;
                                distance = 0;
                                 */
                                return true;
                            }

                            @Override
                            public boolean onDoubleTap(MotionEvent e){
                                queuedSingleTaps.offer(e);
                                checkStatus = 1;
                                return true;
                            }

                            @Override
                            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY){
                                if(distanceX >= 2) {
                                    queuedSingleTaps.offer(e2);
                                    distance = distanceX;
                                    checkStatus = 2;
                                }
                                else if(distanceX <= -2){
                                    queuedSingleTaps.offer(e2);
                                    distance = distanceX;
                                    checkStatus = 2;
                                }
                                else{
                                    queuedSingleTaps.offer(e2);
                                    checkStatus = 9;
                                    distance = 0;
                                }
                                return true;
                            }

                            @Override
                            public boolean onDown(MotionEvent e) {
                                return true;
                            }
                        });

    }

    /**
     * Polls for a tap.
     *
     * @return if a tap was queued, a MotionEvent for the tap. Otherwise null if no taps are queued.
     */
    public MotionEvent poll() {
        return queuedSingleTaps.poll();
    }

    public int getCheckStatus(){
        return checkStatus;
    }

    public float getDistance(){
        return distance;
    }

    public void setCheckStatus(int status){
        checkStatus = status;
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        return gestureDetector.onTouchEvent(motionEvent);
    }
}
