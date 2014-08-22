package com.example.mymarqueetext;

import java.util.ArrayList;
import java.util.concurrent.Semaphore;

import android.R.integer;
import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Bundle;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;

public class MarqueeText extends Activity {

    public static final int MSG_POKE = 0;
    MarqueeTextView view;
    Handler handler;
    boolean showing = false;
    int degree = 0;
    private static final int BG_CYCLE = 50;
    private int bgColorCycle = 0;
    boolean color = false;
    private DisplayMetrics metrics;

    private final Semaphore available = new Semaphore(0, true);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);

        view = new MarqueeTextView(this);
        setContentView(view);
        log("onCreate " + metrics.density + " " + metrics.densityDpi);
        handler = new Handler(new Callback() {

            @Override
            public boolean handleMessage(Message msg) {
                switch (msg.what) {
                case MSG_POKE:
                    log("MSG_POKE");
                    if (showing) {
                        view.invalidate();
                        // handler.sendEmptyMessageDelayed(MSG_POKE, 10);
                    }
                    break;

                default:
                    break;
                }
                return false;
            }
        });

        new DrawingThread().start();
    }

    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();
        log("onResume");
        // handler.sendEmptyMessageDelayed(MSG_POKE, 100);
        // handler.sendEmptyMessage(MSG_POKE);
    }

    @Override
    protected void onPause() {
        // TODO Auto-generated method stub
        super.onPause();
        log("onPause");
    }

    private class DrawingThread extends Thread {

        @Override
        public void run() {
            log("DrawingThread run");
            while (true) {
                try {
                    log("available.acquire()");
                    available.acquire();
                    // handler.sendEmptyMessage(MSG_POKE);
                    handler.sendEmptyMessageDelayed(MSG_POKE, 10);
                    if (++bgColorCycle % BG_CYCLE == 0) {
                        color = !color;
                        bgColorCycle = 0;
                    }
                    sleep(10);
                    log("available.release()");
                    available.release();
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                log("DrawingThread run 2");
            }
        }
    }

    private class MarqueeTextView extends View {

        private Paint mPaint = new Paint();
        private Path mPath = new Path();
        float density = metrics.density;

        public MarqueeTextView(Context context) {
            super(context);
            log("MarqueeTextView");
            // Construct a wedge-shaped path
            mPath.moveTo(0, -50);
            mPath.lineTo(-20, 60);
            mPath.lineTo(0, 50);
            mPath.lineTo(20, 60);
            mPath.close();
            Matrix mx = new Matrix();
            mx.setScale(density, density);
            mPath.transform(mx);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            log("onDraw " + degree + "  " + bgColorCycle);
            // TODO Auto-generated method stub
            Paint paint = mPaint;

            canvas.drawColor(color ? Color.RED : Color.BLUE);
            paint.setAntiAlias(true);
            paint.setColor(Color.BLACK);
            paint.setStyle(Paint.Style.FILL);

            int w = canvas.getWidth();
            int h = canvas.getHeight();
            int cx = w / 2;
            int cy = h / 2;

            canvas.translate(cx, cy);
            canvas.rotate(degree);
            canvas.drawPath(mPath, mPaint);
            degree = (degree + 1) % 360;
        }

        @Override
        protected void onAttachedToWindow() {
            // TODO Auto-generated method stub
            super.onAttachedToWindow();
            log("onAttachedToWindow");
            showing = true;
            log("available.release()");
            available.release();
        }

        @Override
        protected void onDetachedFromWindow() {
            // TODO Auto-generated method stub
            super.onDetachedFromWindow();
            log("onDetachedFromWindow");
            showing = false;
            try {
                log("available.acquire()");
                available.acquire();
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    private class ColorQueue {
        boolean started = false;
        long startTime;
        long pulse;
        ArrayList<Integer> colorList = new ArrayList<Integer>();
        int[] argb = new int[4];

        public ColorQueue(long p, int... colors) {
            pulse = p;
            if (colors.length == 0) {
                colorList.add(Color.BLACK);
                colorList.add(Color.WHITE);
                return;
            }

            for (int c : colors) {
                colorList.add(c);
            }
        }

        public int[] getARGB() {

        }

    }

    private void log(String msg) {
        Log.d("felixx", msg);
    }
}
