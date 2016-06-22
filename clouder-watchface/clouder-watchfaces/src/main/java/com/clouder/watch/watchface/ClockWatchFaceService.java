package com.clouder.watch.watchface;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.format.Time;
import android.util.Log;
import android.view.SurfaceHolder;

import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class ClockWatchFaceService extends CanvasWatchFaceService {
    private static final String TAG = "AnalogWatchFaceService";

    /**
     * Update rate in milliseconds for interactive mode. We update once a second to advance the
     * second hand.
     */
    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1);

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private class Engine extends CanvasWatchFaceService.Engine {
        private Typeface WATCH_TEXT_TYPEFACE = Typeface.create(Typeface.SERIF, Typeface.NORMAL);
        static final int MSG_UPDATE_TIME = 0;

        private Paint mHourPaints;
        private boolean mMute;
        private Paint mYearColorPaint;
        private Paint mYearColorPaints;
        private Time mTime;
        private int mYearColor = Color.parseColor("white");

        /**
         * Handler to update the time once a second in interactive mode.
         */
        final Handler mUpdateTimeHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case MSG_UPDATE_TIME:
                        if (Log.isLoggable(TAG, Log.VERBOSE)) {
                            Log.v(TAG, "updating time");
                        }
                        invalidate();
                        if (shouldTimerBeRunning()) {
                            long timeMs = System.currentTimeMillis();
                            long delayMs = INTERACTIVE_UPDATE_RATE_MS
                                    - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                            mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
                        }
                        break;
                    default:
                        break;
                }
            }
        };

        final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mTime.clear(intent.getStringExtra("time-zone"));
                mTime.setToNow();
            }
        };
        boolean mRegisteredTimeZoneReceiver = false;

        /**
         * Whether the display supports fewer bits for each color in ambient mode. When true, we
         * disable anti-aliasing in ambient mode.
         */
        boolean mLowBitAmbient;

        private Bitmap mBackgroundBitmap;
        private Bitmap mBackgroundBitmaps;
        private Bitmap mBackgroundSquareBitmap;
        private Bitmap mBackgroundScaledBitmap;
        private Bitmap mBackgroundScaledBitmaps;
        private Bitmap mSecond;
        private Bitmap mMinute;
        private Bitmap mHour;
        private Bitmap mYearBackgroundBitmap;
        private Bitmap mDayBackgroundBitmap;
        private String month;
        private Drawable iconSecond;
        private float centerX;
        private float centerY;

        @Override
        public void onCreate(SurfaceHolder holder) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onCreate");
            }
            super.onCreate(holder);
            setWatchFaceStyle(new WatchFaceStyle.Builder(ClockWatchFaceService.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .build());

            initDisplayText();
            initResource();
            mTime = new Time();

        }

        @Override
        public void onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            super.onDestroy();
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onPropertiesChanged: low-bit ambient = " + mLowBitAmbient);
            }
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onTimeTick: ambient = " + isInAmbientMode());
            }
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onAmbientModeChanged: " + inAmbientMode);
            }
            if (mLowBitAmbient) {
                boolean antiAlias = !inAmbientMode;
                mHourPaints.setAntiAlias(antiAlias);
            }
            invalidate();

            // Whether the timer should be running depends on whether we're in ambient mode (as well
            // as whether we're visible), so we may need to start or stop the timer.
            updateTimer();
        }

        @Override
        public void onInterruptionFilterChanged(int interruptionFilter) {
            super.onInterruptionFilterChanged(interruptionFilter);
            boolean inMuteMode = (interruptionFilter == WatchFaceService.INTERRUPTION_FILTER_NONE);
            if (mMute != inMuteMode) {
                mMute = inMuteMode;
                invalidate();
            }
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            mTime.setToNow();
            drawBackground(canvas, bounds);
            drawTimeText(canvas);
            drawTimeBitmap(canvas, bounds);
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onVisibilityChanged: " + visible);
            }
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
            ClockWatchFaceService.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            ClockWatchFaceService.this.unregisterReceiver(mTimeZoneReceiver);
        }

        /**
         * Starts the {@link #mUpdateTimeHandler} timer if it should be running and isn't currently
         * or stops it if it shouldn't be running but currently is.
         */
        private void updateTimer() {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "updateTimer");
            }
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


        private void initDisplayText() {

            mYearColorPaint = new Paint();
            mYearColorPaint.setColor(mYearColor);
            mYearColorPaint.setTypeface(WATCH_TEXT_TYPEFACE);
            mYearColorPaint.setAntiAlias(true);
            mYearColorPaint.setTextSize(getResources().getDimension(R.dimen.day_size));

            mYearColorPaints = new Paint();
            mYearColorPaints.setColor(mYearColor);
            mYearColorPaints.setTypeface(WATCH_TEXT_TYPEFACE);
            mYearColorPaints.setAntiAlias(true);
            mYearColorPaints.setTextSize(getResources().getDimension(R.dimen.year_sizes));

            mHourPaints = new Paint();
            mHourPaints.setAntiAlias(true);
            mHourPaints.setFilterBitmap(true);
        }

        private void drawBackground(Canvas canvas, Rect bounds) {
            int width = bounds.width();
            int height = bounds.height();
            Log.d(TAG, "drawBackground------> " + ", width " + width + ", height " + height);
            centerX = width / 2f;
            centerY = height / 2f;

            if (mBackgroundScaledBitmap == null
                    || mBackgroundScaledBitmap.getWidth() != width
                    || mBackgroundScaledBitmap.getHeight() != height) {
                mBackgroundScaledBitmap = Bitmap.createScaledBitmap(mBackgroundBitmap,
                        width, height, true /* filter */);
            }
            canvas.drawBitmap(mBackgroundScaledBitmap, 0, 0, null);

            if (width >= 360) {
                if (mBackgroundScaledBitmaps == null
                        || mBackgroundScaledBitmaps.getWidth() != width
                        || mBackgroundScaledBitmaps.getHeight() != height) {
                    mBackgroundScaledBitmaps = Bitmap.createScaledBitmap(mBackgroundBitmaps,
                            width, height, true /* filter */);
                }
                canvas.drawBitmap(mBackgroundScaledBitmaps, 0, 0, null);
            }
            if (width < 360) {
                if (mBackgroundScaledBitmaps == null
                        || mBackgroundScaledBitmaps.getWidth() != width
                        || mBackgroundScaledBitmaps.getHeight() != height) {
                    mBackgroundScaledBitmaps = Bitmap.createScaledBitmap(mBackgroundSquareBitmap,
                            width, height, true /* filter */);
                }
                canvas.drawBitmap(mBackgroundScaledBitmaps, 0, 0, null);
            }
        }

        private void drawTimeBitmap(Canvas canvas, Rect bounds) {
            int height = bounds.height();
            if (mSecond.getHeight() != height) {
                mSecond = Bitmap.createScaledBitmap(mSecond, mSecond.getWidth(), height, true /* filter */);
            }

            canvas.save();
            canvas.rotate(mTime.minute / 2 + 180 + mTime.hour * 30, centerX, centerY);
            canvas.drawBitmap(mHour, (float) ((centerX - mHour.getWidth() / 2)), (float) (centerY - (mHour.getHeight() / 2)), mHourPaints);
            canvas.restore();

            canvas.save();
            canvas.rotate(mTime.minute * 6 + 180, centerX, centerY);
            canvas.drawBitmap(mMinute, (int) (centerX - mMinute.getWidth() / 2), (int) (centerY - (mMinute.getHeight() / 2)), mHourPaints);
            canvas.restore();

            //save power
            if (Settings.Global.getInt(ClockWatchFaceService.this.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 0) == 1) {
                Log.d("isAirPlaneOn", "save power");
            } else {
                canvas.save();
                canvas.rotate(mTime.second * 6 + 180, centerX, centerY);
                canvas.drawBitmap(mSecond, centerX - mSecond.getWidth() / 2, centerY - (mSecond.getHeight() / 2), mHourPaints);
                canvas.restore();
            }

        }

        private void drawTimeText(Canvas canvas) {

            String year = mTime.year + "";
            if (mTime.month + 1 >= 10) {
                month = mTime.month + 1 + "";
            } else {
                month = "0" + (mTime.month + 1);
            }
            String day = mTime.monthDay + "";
            canvas.drawBitmap(mDayBackgroundBitmap, 6 * centerX / 5 - 29, 7 * centerY / 5, null);
            canvas.drawBitmap(mDayBackgroundBitmap, 4 * centerX / 5 - 17, 7 * centerY / 5, null);
            canvas.drawBitmap(mYearBackgroundBitmap, 4 * centerX / 5 - 17, 8 * centerY / 5, null);
            canvas.drawText(day, 6 * centerX / 5 - 13, 7 * centerY / 5 + 28, mYearColorPaint);
            canvas.drawText(year, 6 * centerX / 5 - 20, 8 * centerY / 5 + 23, mYearColorPaints);
            canvas.drawText(month, 4 * centerX / 5 - 9, 7 * centerY / 5 + 28, mYearColorPaint);
            canvas.drawText(getDayString(), 4 * centerX / 5 - 10, 8 * centerY / 5 + 23, mYearColorPaints);

        }

        private String getDayString() {
            if (mTime.weekDay == 0)
                return "Sun";
            else if (mTime.weekDay == 1)
                return "Mon";
            else if (mTime.weekDay == 2)
                return "Tue";
            else if (mTime.weekDay == 3)
                return "Wed";
            else if (mTime.weekDay == 4)
                return "Thu";
            else if (mTime.weekDay == 5)
                return "Fri";
            else
                return "Sat";
        }

        private void initResource() {
            Rect bounds = new Rect();
            bounds.height();

            Resources squareResource = ClockWatchFaceService.this.getResources();
            Drawable squareBackgroundDrawables = squareResource.getDrawable(R.drawable.bg_dial);
            mBackgroundBitmaps = ((BitmapDrawable) squareBackgroundDrawables).getBitmap();

            Resources resource = ClockWatchFaceService.this.getResources();
            Drawable backgroundDrawables = resource.getDrawable(R.drawable.bg_02);
            mBackgroundSquareBitmap = ((BitmapDrawable) backgroundDrawables).getBitmap();

            Resources resources = ClockWatchFaceService.this.getResources();
            Drawable backgroundDrawable = resources.getDrawable(R.drawable.bg1);
            mBackgroundBitmap = ((BitmapDrawable) backgroundDrawable).getBitmap();

            Resources resourceD = ClockWatchFaceService.this.getResources();
            iconSecond = resourceD.getDrawable(R.drawable.icon_second1);
            mSecond = ((BitmapDrawable) iconSecond).getBitmap();

            Resources min = ClockWatchFaceService.this.getResources();
            Drawable mins = min.getDrawable(R.drawable.icon_minute1);
            mMinute = ((BitmapDrawable) mins).getBitmap();

            Resources hour = ClockWatchFaceService.this.getResources();
            Drawable hours = hour.getDrawable(R.drawable.icon_hour1);
            mHour = ((BitmapDrawable) hours).getBitmap();

            Resources resourceYear = ClockWatchFaceService.this.getResources();
            Drawable backgroundDrawableYear = resourceYear.getDrawable(R.drawable.bg_year);
            mYearBackgroundBitmap = ((BitmapDrawable) backgroundDrawableYear).getBitmap();

            Resources resourceDay = ClockWatchFaceService.this.getResources();
            Drawable backgroundDrawableDayr = resourceDay.getDrawable(R.drawable.bg_day);
            mDayBackgroundBitmap = ((BitmapDrawable) backgroundDrawableDayr).getBitmap();

        }
    }
}
