/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.aqeelp.ataglance;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.ContactsContract;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.format.Time;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.WindowInsets;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.lang.ref.WeakReference;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Digital watch face with seconds. In ambient mode, the seconds aren't displayed. On devices with
 * low-bit ambient mode, the text is drawn without anti-aliasing in ambient mode.
 */
public class GlanceFace extends CanvasWatchFaceService implements
        GoogleApiClient.ConnectionCallbacks {
    private final static String TAG = "myTag";

    private GoogleApiClient mGoogleApiClient;;

    private static final Typeface NORMAL_TYPEFACE =
            Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL);

    private static int textraCount, messengerCount, snapchatCount, emailCount;

    /**
     * Update rate in milliseconds for interactive mode. We update once a second since seconds are
     * displayed in interactive mode.
     */
    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1);

    private static final String[] week_days = {"Sun", "Mon", "Tues", "Wed", "Thurs", "Fri",
        "Sat"};

    private static final String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul",
        "Aug", "Sep", "Oct", "Nov", "Dec"};

    /**
     * Handler message id for updating the time periodically in interactive mode.
     */
    private static final int MSG_UPDATE_TIME = 0;

    @Override
    public void onCreate() {
        super.onCreate();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .build();
        mGoogleApiClient.connect();
    }

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private class Engine extends CanvasWatchFaceService.Engine {
        final Handler mUpdateTimeHandler = new EngineHandler(this);

        final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mTime.setTimeZone(TimeZone.getDefault());
            }
        };

        boolean mRegisteredTimeZoneReceiver = false;

        Paint mBackgroundPaint;
        Paint mTextPaint;
        Paint mTintPaint;
        Paint mSmallPaint;

        boolean mAmbient;

        Calendar mTime;

        float mXOffset;
        float mYOffset;

        /**
         * Whether the display supports fewer bits for each color in ambient mode. When true, we
         * disable anti-aliasing in ambient mode.
         */
        boolean mLowBitAmbient;

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            Log.v(TAG, "Glance Watchface started!");

            setWatchFaceStyle(new WatchFaceStyle.Builder(GlanceFace.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)
                    .setAmbientPeekMode(WatchFaceStyle.AMBIENT_PEEK_MODE_HIDDEN)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_PERSISTENT)
                    .setShowSystemUiTime(false)
                    .setAcceptsTapEvents(true)
                    .build());
            Resources resources = GlanceFace.this.getResources();
            mYOffset = resources.getDimension(R.dimen.digital_y_offset);

            mBackgroundPaint = new Paint();
            mBackgroundPaint.setColor(Color.parseColor("#ff000000"));

            mTintPaint = new Paint();
            mTintPaint.setColor(Color.parseColor("#55000000"));

            mTextPaint = new Paint();
            mTextPaint = createTextPaint(resources.getColor(R.color.digital_text));
            mTextPaint.setTextAlign(Paint.Align.CENTER);

            mSmallPaint = new Paint();
            mSmallPaint.setAntiAlias(true);
            mSmallPaint.setColor(Color.parseColor("#ff707070"));
            mSmallPaint.setTextSize(30);
            mSmallPaint.setTextAlign(Paint.Align.CENTER);
            mSmallPaint.setShadowLayer(2, 1, 1, Color.parseColor("#88000000"));

            mTime = Calendar.getInstance();

            textraCount = 0;
            messengerCount = 0;
            snapchatCount = 0;
            emailCount = 0;
        }

        @Override
        public void onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            super.onDestroy();
        }

        private Paint createTextPaint(int textColor) {
            Paint paint = new Paint();
            paint.setColor(textColor);
            paint.setTypeface(NORMAL_TYPEFACE);
            paint.setAntiAlias(true);
            return paint;
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                registerReceiver();

                // Update time zone in case it changed while we weren't visible.
                mTime.setTimeZone(TimeZone.getDefault());
                invalidate();
            } else {
                unregisterReceiver();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            GlanceFace.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            GlanceFace.this.unregisterReceiver(mTimeZoneReceiver);
        }

        @Override
        public void onApplyWindowInsets(WindowInsets insets) {
            super.onApplyWindowInsets(insets);
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            if (mAmbient != inAmbientMode) {
                mAmbient = inAmbientMode;
                if (mLowBitAmbient) {
                    mTextPaint.setAntiAlias(!inAmbientMode);
                    mSmallPaint.setAntiAlias(!inAmbientMode);
                }

                if (mAmbient) {
                    mTextPaint.setAlpha(190);
                    mSmallPaint.setColor(Color.parseColor("#ff000000"));
                    mSmallPaint.clearShadowLayer();
                } else {
                    mTextPaint.setAlpha(255);
                    mSmallPaint.setColor(Color.parseColor("#ff707070"));
                    mSmallPaint.setShadowLayer(2, 1, 1, Color.parseColor("#88000000"));
                }
                invalidate();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            // Draw the background.
            canvas.drawRect(0, 0, bounds.width(), bounds.height(), mBackgroundPaint);

            if (!mAmbient) {
                Drawable background = getResources().getDrawable(R.drawable.winter_forest);
                background.setBounds(-20, 0, 320 + 20, 290);
                background.draw(canvas);

                // Darken the background:
                canvas.drawRect(0, 0, bounds.width(), bounds.height(), mTintPaint);
            }

            /* // Mockup:
            Drawable d = getResources().getDrawable(R.drawable.watchface_mockup);
            d.setBounds(0, 0, 320, 290);
            d.draw(canvas);
            */

            // Paint main clock:
            long now = System.currentTimeMillis();
            mTime.setTimeInMillis(now);

            mTextPaint.setTextSize(90);
            int hour = mTime.get(Calendar.HOUR) % 12;
            if (hour == 0) hour = 12;
            String text = String.format("%d:%02d", hour, mTime.get(Calendar.MINUTE));
            canvas.drawText(text, canvas.getWidth() / 2, 190, mTextPaint);

            // Paint date:
            if (!mAmbient) {
                mTextPaint.setTextSize(20);
                String day = week_days[mTime.get(Calendar.DAY_OF_WEEK) - 1];
                String month = months[mTime.get(Calendar.MONTH)];
                String date = day + ", " + month + " " + mTime.get(Calendar.DAY_OF_MONTH);
                canvas.drawText(date, canvas.getWidth() / 2, 118, mTextPaint);
            }

            drawIcon(textraCount, getResources().getDrawable(R.drawable.textra), 57, 46, canvas, bounds);
            drawIcon(messengerCount, getResources().getDrawable(R.drawable.messenger), 210, 42, canvas, bounds);
            drawIcon(snapchatCount, getResources().getDrawable(R.drawable.snapchat), 57, 222, canvas, bounds);
            drawIcon(emailCount, getResources().getDrawable(R.drawable.mail), 210, 222, canvas, bounds);
        }

        private void drawIcon(int count, Drawable icon, int x, int y, Canvas canvas, Rect bounds) {
            final int ICON_SIZE = 53;
            int transparencyBuffer = 0;
            if (mAmbient) transparencyBuffer = 85;

            icon.setBounds(x, y, x + ICON_SIZE, y + ICON_SIZE);
            if (count == 0) {
                icon.setAlpha(85 - transparencyBuffer);
                icon.draw(canvas);
            } else {
                icon.setAlpha(220 - transparencyBuffer);
                icon.draw(canvas);

                int text_x = 83;
                if (x > bounds.exactCenterX()) text_x = 236;
                int text_y = 76;
                if (y > bounds.exactCenterY()) text_y = 256;
                canvas.drawText(count + "", text_x, text_y, mSmallPaint);
            }
        }

        /**
         * Starts the {@link #mUpdateTimeHandler} timer if it should be running and isn't currently
         * or stops it if it shouldn't be running but currently is.
         */
        private void updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        /**
         * Returns whether the {@link #mUpdateTimeHandler} timer should be running. The timer should
         * only run when we're visible and in interactive mode.
         */
        private boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }

        /**
         * Handle updating the time periodically in interactive mode.
         */
        private void handleUpdateTimeMessage() {
            invalidate();
            if (shouldTimerBeRunning()) {
                long timeMs = System.currentTimeMillis();
                long delayMs = INTERACTIVE_UPDATE_RATE_MS
                        - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
            }
        }

        @Override
        public void onTapCommand(
                @TapType int tapType, int x, int y, long eventTime) {
            switch (tapType) {
                case WatchFaceService.TAP_TYPE_TAP:
                    DataMap activity = null;
                    if ((57 <= x || x >= 57 + 53) && (46 <= y || y >= 46 + 53)) {
                        activity = new DataMap();
                        if (textraCount > 0)
                            activity.putString("package", "com.textra");
                    } else if ((210 <= x || x >= 210 + 53) && (42 <= y || y >= 42 + 53)) {
                        activity = new DataMap();
                        if (messengerCount > 0)
                            activity.putString("package", "com.facebook.orca");
                    } else if ((57 <= x || x >= 57 + 53) && (222 <= y || y >= 222 + 53)) {
                        activity = new DataMap();
                        if (snapchatCount > 0)
                            activity.putString("package", "com.snapchat.android");
                    } else if ((210 <= x || x >= 210 + 53) && (222 <= y || y >= 222 + 53)) {
                        activity = new DataMap();
                        if (emailCount > 0)
                            activity.putString("package", "com.google.android.gm");
                    }
                    if (activity != null) sendMessage(activity);
                    break;

                default:
                    super.onTapCommand(tapType, x, y, eventTime);
                    break;
            }
        }
    }

    private static class EngineHandler extends Handler {
        private final WeakReference<GlanceFace.Engine> mWeakReference;

        public EngineHandler(GlanceFace.Engine reference) {
            mWeakReference = new WeakReference<>(reference);
        }

        @Override
        public void handleMessage(Message msg) {
            GlanceFace.Engine engine = mWeakReference.get();
            if (engine != null) {
                switch (msg.what) {
                    case MSG_UPDATE_TIME:
                        engine.handleUpdateTimeMessage();
                        break;
                }
            }
        }
    }

    public static void parseNotifPackage(DataMap dataMap) {
        Log.d(TAG, "Notification package received... " + dataMap);

        textraCount = dataMap.getInt("textra");
        messengerCount = dataMap.getInt("messenger");
        snapchatCount = dataMap.getInt("snapchat");
        emailCount = dataMap.getInt("emails");
    }

    @Override
    public void onConnected(Bundle bundle) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "onConnected: " + bundle);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "onConnectionSuspended: " + i);
        }
    }

    private void sendMessage(DataMap dataMap) {
        final String APP_LAUNCH_PATH = "/glance/app_launch";

        Log.d(TAG, "Attempting to send message from wearable... " + dataMap);
        final byte[] rawData = dataMap.toByteArray();

        if (mGoogleApiClient == null) return;

        new Thread( new Runnable() {
            @Override
            public void run() {
                NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes( mGoogleApiClient ).await();
                for(Node node : nodes.getNodes()) {
                    MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(
                            mGoogleApiClient, node.getId(), APP_LAUNCH_PATH, rawData).await();
                    if (result.getStatus().isSuccess())
                        Log.d(TAG, "Message sent successfully");
                    else
                        Log.d(TAG, "Message failed");
                }
            }
        }).start();
    }
}
