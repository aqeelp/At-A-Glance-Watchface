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
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.format.Time;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.WindowInsets;

import com.google.android.gms.wearable.DataMap;

import java.lang.ref.WeakReference;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Digital watch face with seconds. In ambient mode, the seconds aren't displayed. On devices with
 * low-bit ambient mode, the text is drawn without anti-aliasing in ambient mode.
 */
public class GlanceFace extends CanvasWatchFaceService {
    private String TAG = "GlanceFace";

    private static final Typeface NORMAL_TYPEFACE =
            Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL);

    private static int textraCount, messengerCount, snapchatCount, emailCount;

    /**
     * Update rate in milliseconds for interactive mode. We update once a second since seconds are
     * displayed in interactive mode.
     */
    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1);

    /**
     * Handler message id for updating the time periodically in interactive mode.
     */
    private static final int MSG_UPDATE_TIME = 0;

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private class Engine extends CanvasWatchFaceService.Engine {
        final Handler mUpdateTimeHandler = new EngineHandler(this);

        final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mTime.clear(intent.getStringExtra("time-zone"));
                mTime.setToNow();
            }
        };

        boolean mRegisteredTimeZoneReceiver = false;

        Paint mBackgroundPaint;
        Paint mTextPaint;

        boolean mAmbient;

        Time mTime;

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

            Log.v("myTag", "hello!!!!");

            setWatchFaceStyle(new WatchFaceStyle.Builder(GlanceFace.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)
                    .setAmbientPeekMode(WatchFaceStyle.AMBIENT_PEEK_MODE_HIDDEN)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_PERSISTENT)
                    .setShowSystemUiTime(false)
                    .build());
            Resources resources = GlanceFace.this.getResources();
            mYOffset = resources.getDimension(R.dimen.digital_y_offset);

            mBackgroundPaint = new Paint();
            mBackgroundPaint.setColor(Color.parseColor("#55000000"));

            mTextPaint = new Paint();
            mTextPaint = createTextPaint(resources.getColor(R.color.digital_text));
            mTextPaint.setTextAlign(Paint.Align.CENTER);

            mTime = new Time();

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
                mTime.clear(TimeZone.getDefault().getID());
                mTime.setToNow();
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
            Drawable background = getResources().getDrawable(R.drawable.winter_forest);
            background.setBounds(-20, 0, 320 + 20, 290);
            background.draw(canvas);
            // Darken the background:
            canvas.drawRect(0, 0, bounds.width(), bounds.height(), mBackgroundPaint);

            /* // Mockup:
            Drawable d = getResources().getDrawable(R.drawable.watchface_mockup);
            d.setBounds(0, 0, 320, 290);
            d.draw(canvas);
            */

            // Paint main clock:
            mTime.setToNow();
            mTextPaint.setTextSize(90);
            String text = String.format("%d:%02d", mTime.hour % 12, mTime.minute);
            canvas.drawText(text, canvas.getWidth() / 2, 190, mTextPaint);

            Paint smallPaint = new Paint();
            smallPaint.setAntiAlias(true);
            smallPaint.setColor(Color.parseColor("#ff707070"));
            smallPaint.setTextSize(30);
            smallPaint.setTextAlign(Paint.Align.CENTER);
            smallPaint.setShadowLayer(2, 1, 1, Color.parseColor("#88000000"));

            Drawable textra = getResources().getDrawable(R.drawable.textra);
            textra.setBounds(57, 46, 57 + 53, 46 + 53);
            if (textraCount == 0) {
                textra.setAlpha(85);
                textra.draw(canvas);
            } else {
                textra.setAlpha(220);
                textra.draw(canvas);
                canvas.drawText(textraCount + "", 83, 76, smallPaint);
            }

            Drawable messenger = getResources().getDrawable(R.drawable.messenger);
            messenger.setBounds(210, 46, 210 + 53, 46 + 53);
            if (messengerCount == 0) {
                messenger.setAlpha(85);
                messenger.draw(canvas);
            } else {
                messenger.setAlpha(220);
                messenger.draw(canvas);
                canvas.drawText(messengerCount + "", 236, 76, smallPaint);
            }

            Drawable snapchat = getResources().getDrawable(R.drawable.snapchat);
            snapchat.setBounds(57, 222, 57 + 53, 222 + 53);
            if (snapchatCount == 0) {
                snapchat.setAlpha(85);
                snapchat.draw(canvas);
            } else {
                snapchat.setAlpha(220);
                snapchat.draw(canvas);
                canvas.drawText(snapchatCount + "", 83, 256, smallPaint);
            }

            Drawable mail = getResources().getDrawable(R.drawable.mail);
            mail.setBounds(210, 222, 210 + 53, 222 + 53);
            if (emailCount == 0) {
                mail.setAlpha(85);
                mail.draw(canvas);
            } else {
                mail.setAlpha(220);
                mail.draw(canvas);
                canvas.drawText(emailCount + "", 236, 256, smallPaint);
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
        textraCount = dataMap.getInt("textra");
        messengerCount = dataMap.getInt("messenger");
        snapchatCount = dataMap.getInt("snapchat");
        emailCount = dataMap.getInt("email");
        // invalidate();
    }
}
