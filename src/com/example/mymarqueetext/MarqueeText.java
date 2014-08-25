package com.example.mymarqueetext;

import java.util.ArrayList;
import java.util.concurrent.Semaphore;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;

public class MarqueeText extends Activity implements SensorEventListener {

    public static final int MSG_POKE = 0;
    MarqueeTextView view;
    Handler handler;
    boolean showing = false;
    int degree = 0;
    private int bgColorCycle = 0;
    boolean color = false;
    private DisplayMetrics metrics;

    private final Semaphore available = new Semaphore(0, true);
    private ColorQueue mColorQueue;

    private SensorManager mSensorManager;
    private Sensor mSensor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);

        mColorQueue = new ColorQueue(1000, Color.RED, Color.GREEN, Color.CYAN,
                Color.BLUE);
        view = new MarqueeTextView(this);
        view.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                | View.SYSTEM_UI_FLAG_IMMERSIVE);
        setContentView(view);
        log("onCreate " + metrics.density + " " + metrics.densityDpi);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager
                .getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
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
        mColorQueue.reset();
        mSensorManager.registerListener(this, mSensor,
                SensorManager.SENSOR_DELAY_FASTEST);
    }

    @Override
    protected void onPause() {
        // TODO Auto-generated method stub
        super.onPause();
        log("onPause");
        mSensorManager.unregisterListener(this);
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

    class MarqueeTextView extends View {

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

            // canvas.drawColor(color ? Color.RED : Color.BLUE);
            canvas.drawColor(mColorQueue.getColor());
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

    class ColorQueue {
        boolean started = false;
        long startTime, endTime;

        long now, pulse;

        ArrayList<Integer> colorList = new ArrayList<Integer>();
        int pos = 0;
        int size = 0;

        int color;

        public ColorQueue(long p, int... colors) {
            pulse = p;
            if (colors == null || colors.length == 0) {
                colorList.add(Color.BLACK);
                colorList.add(Color.WHITE);
            } else {
                for (int c : colors) {
                    colorList.add(c);
                }
            }
            size = colorList.size();
            log("ColorQueue() size = " + size);
        }

        public int getColor() {
            startIfNeeded();
            log("getColor() pos = " + pos + " startTime = " + startTime
                    + " endTime = " + endTime);

            int colorFrom = colorList.get(pos);
            int colorTo = colorList.get((pos + 1) % size);
            now = System.currentTimeMillis();
            final float t = (now - startTime) / (float) (pulse);

            color = Color.argb(
                    (int) (t * Color.alpha(colorTo) + Color.alpha(colorFrom)
                            * (1 - t)),
                    (int) (t * Color.red(colorTo) + Color.red(colorFrom)
                            * (1 - t)),
                    (int) (t * Color.green(colorTo) + Color.green(colorFrom)
                            * (1 - t)),
                    (int) (t * Color.blue(colorTo) + Color.blue(colorFrom)
                            * (1 - t)));
            if (now > endTime) {
                startTime = now;
                endTime = startTime + pulse;
                pos = (pos + 1) % size;
            }
            log("getColor() color = " + color);
            return color;
        }

        private void startIfNeeded() {
            if (started)
                return;
            log("startIfNeeded()");
            startTime = System.currentTimeMillis();
            endTime = startTime + pulse;
            started = true;
        }

        public void reset() {
            log("reset()");
            started = false;
        }

    }

    void log(String msg) {
        Log.d("felixx", msg);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        log("onSensorChanged()-----------");
        log("onSensorChanged() X " + event.values[0]);
        log("onSensorChanged() Y " + event.values[1]);
        log("onSensorChanged() Z " + event.values[2]);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        log("onAccuracyChanged()");
    }
}
