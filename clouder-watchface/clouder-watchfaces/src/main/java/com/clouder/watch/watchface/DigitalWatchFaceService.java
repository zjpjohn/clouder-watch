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
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.format.Time;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.WindowInsets;

import java.util.Calendar;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Created by zhou_wenchong on 7/15/15.
 */
public class DigitalWatchFaceService extends CanvasWatchFaceService {
    @Override
    public Engine onCreateEngine() {
        return new WatchFaceEngine();
    }

    private class WatchFaceEngine extends CanvasWatchFaceService.Engine {

        //Member variables
        private Typeface WATCH_TEXT_TYPEFACE = Typeface.create(Typeface.SERIF, Typeface.NORMAL);

        private static final int MSG_UPDATE_TIME_ID = 42;
        private static final long DEFAULT_UPDATE_RATE_MS = 1000;
        private long mUpdateRateMs = 1000;
        private Time mDisplayTime;

        private Paint mSecoPaint;
        private Paint mTextColorPaint;
        private Paint mYearColorPaint;
        private Paint mWeakColorPaint;
        String yearText;

        private boolean mHasTimeZoneReceiverBeenRegistered = false;
        private boolean mIsInMuteMode;
        private boolean mIsLowBitAmbient;

        private int mTextColor = Color.parseColor("white");
        private int mYearColor = Color.parseColor("#cccccc");
        private int mWeakColor = Color.parseColor("#ff932b");
        private int[] array = {R.drawable.second_1, R.drawable.second_2, R.drawable.second_3, R.drawable.second_4, R.drawable.second_5, R.drawable.second_6, R.drawable.second_7, R.drawable.second_8, R.drawable.second_9, R.drawable.second_10,
                R.drawable.second_11, R.drawable.second_12, R.drawable.second_13, R.drawable.second_14, R.drawable.second_15, R.drawable.second_16, R.drawable.second_17, R.drawable.second_18, R.drawable.second_19, R.drawable.second_20,
                R.drawable.second_21, R.drawable.second_22, R.drawable.second_23, R.drawable.second_24, R.drawable.second_25, R.drawable.second_26, R.drawable.second_27, R.drawable.second_28, R.drawable.second_29, R.drawable.second_30,
                R.drawable.second_31, R.drawable.second_32, R.drawable.second_33, R.drawable.second_34, R.drawable.second_35, R.drawable.second_36, R.drawable.second_37, R.drawable.second_38, R.drawable.second_39, R.drawable.second_40,
                R.drawable.second_41, R.drawable.second_42, R.drawable.second_43, R.drawable.second_44, R.drawable.second_45, R.drawable.second_46, R.drawable.second_47, R.drawable.second_48, R.drawable.second_49, R.drawable.second_50,
                R.drawable.second_51, R.drawable.second_52, R.drawable.second_53, R.drawable.second_54, R.drawable.second_55, R.drawable.second_56, R.drawable.second_57, R.drawable.second_58, R.drawable.second_59, R.drawable.second_60,};

        final BroadcastReceiver mTimeZoneBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mDisplayTime.clear(intent.getStringExtra("time-zone"));
                mDisplayTime.setToNow();
            }
        };

        private final Handler mTimeHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case MSG_UPDATE_TIME_ID: {
                        invalidate();
                        if (isVisible() && !isInAmbientMode()) {
                            long currentTimeMillis = System.currentTimeMillis();
                            long delay = mUpdateRateMs - (currentTimeMillis % mUpdateRateMs);
                            mTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME_ID, delay);
                        }
                        break;
                    }
                    default:
                        break;
                }
            }
        };

        Bitmap mBackgroundBitmap;
        Bitmap mBackgroundBitmapLogo;
        Bitmap mBackgroundScaledBitmap;
        Bitmap mSecondBitmap;
        private int centerX;
        private int centerY;
        private String min;

        //Overridden methods
        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            setWatchFaceStyle(new WatchFaceStyle.Builder(DigitalWatchFaceService.this)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_VARIABLE)
                    .setShowSystemUiTime(false)
                    .build()
            );
            Resources resources = DigitalWatchFaceService.this.getResources();
            Drawable bg = resources.getDrawable(R.drawable.bg1);
            mBackgroundBitmap = ((BitmapDrawable) bg).getBitmap();

            Resources icon = DigitalWatchFaceService.this.getResources();
            Drawable iconLogo = icon.getDrawable(R.drawable.icon_logo);
            mBackgroundBitmapLogo = ((BitmapDrawable) iconLogo).getBitmap();
            initDisplayText();
            mDisplayTime = new Time();
        }

        @Override
        public void onDestroy() {
            mTimeHandler.removeMessages(MSG_UPDATE_TIME_ID);
            super.onDestroy();
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                if (!mHasTimeZoneReceiverBeenRegistered) {

                    IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
                    DigitalWatchFaceService.this.registerReceiver(mTimeZoneBroadcastReceiver, filter);

                    mHasTimeZoneReceiverBeenRegistered = true;
                }

                mDisplayTime.clear(TimeZone.getDefault().getID());
                mDisplayTime.setToNow();
            } else {
                if (mHasTimeZoneReceiverBeenRegistered) {
                    DigitalWatchFaceService.this.unregisterReceiver(mTimeZoneBroadcastReceiver);
                    mHasTimeZoneReceiverBeenRegistered = false;
                }
            }

            updateTimer();
        }

        @Override
        public void onApplyWindowInsets(WindowInsets insets) {
            super.onApplyWindowInsets(insets);
            if (insets.isRound()) {
            } else {
            }
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);

            if (properties.getBoolean(PROPERTY_BURN_IN_PROTECTION, false)) {
                mIsLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
            }
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();

            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);

            if (inAmbientMode) {
                mTextColorPaint.setColor(Color.parseColor("white"));
                mYearColorPaint.setColor(Color.parseColor("gray"));
                mWeakColorPaint.setColor(Color.parseColor("#ff932b"));
            } else {
                mTextColorPaint.setColor(Color.parseColor("red"));
                mYearColorPaint.setColor(Color.parseColor("white"));
            }

            if (mIsLowBitAmbient) {
                mTextColorPaint.setAntiAlias(!inAmbientMode);
                mYearColorPaint.setAntiAlias(!inAmbientMode);
            }

            invalidate();
            updateTimer();
        }

        @Override
        public void onInterruptionFilterChanged(int interruptionFilter) {
            super.onInterruptionFilterChanged(interruptionFilter);

            boolean isDeviceMuted = (interruptionFilter == android.support.wearable.watchface.WatchFaceService.INTERRUPTION_FILTER_NONE);
            if (isDeviceMuted) {
                mUpdateRateMs = TimeUnit.MINUTES.toMillis(1);

            } else {
                mUpdateRateMs = DEFAULT_UPDATE_RATE_MS;
            }

            if (mIsInMuteMode != isDeviceMuted) {
                mIsInMuteMode = isDeviceMuted;
                int alpha = (isDeviceMuted) ? 100 : 255;
                mTextColorPaint.setAlpha(alpha);
                mYearColorPaint.setAlpha(alpha);
                mWeakColorPaint.setAlpha(alpha);
                invalidate();
            }
            updateTimer();
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            super.onDraw(canvas, bounds);
            mDisplayTime.setToNow();
            int width = bounds.width();
            int height = bounds.height();
            centerX = width / 2;
            centerY = height / 2;
            Resources icons = DigitalWatchFaceService.this.getResources();

            Drawable iconLogos = icons.getDrawable(array[mDisplayTime.second]);
            mSecondBitmap = ((BitmapDrawable) iconLogos).getBitmap();

//             Draw the background, scaled to fit.
            if (mBackgroundScaledBitmap == null
                    || mBackgroundScaledBitmap.getWidth() != width
                    || mBackgroundScaledBitmap.getHeight() != height) {
                mBackgroundScaledBitmap = Bitmap.createScaledBitmap(mBackgroundBitmap,
                        width, height, true /* filter */);
            }

            if (mSecondBitmap == null
                    || mSecondBitmap.getWidth() != width
                    || mSecondBitmap.getHeight() != height) {
                mSecondBitmap = Bitmap.createScaledBitmap(mSecondBitmap,
                        width, height, true /* filter */);

                canvas.drawBitmap(mBackgroundScaledBitmap, 0, 0, mSecoPaint);
                canvas.drawBitmap(mBackgroundBitmapLogo, centerX - 45, centerY + 60, mSecoPaint);
                drawTimeText(canvas);

//                canvas.save();
//                canvas.scale(1.0f, 1.0f);
//                canvas.drawBitmap(mSecondBitmap, (width - mSecondBitmap.getWidth()) / 2, (height - mSecondBitmap.getHeight()) / 2, mSecoPaint);
//                canvas.restore();
                //save power
                if (Settings.Global.getInt(DigitalWatchFaceService.this.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 0) == 1) {
                    Log.d("isAirPlaneOn", "save power");
                } else {
                    canvas.save();
                    canvas.scale(1.0f, 1.0f);
                    canvas.drawBitmap(mSecondBitmap, (float) ((width - mSecondBitmap.getWidth()) / 2), (float) ((height - mSecondBitmap.getHeight()) / 2), mSecoPaint);
                    canvas.restore();
                }
            } else {
                canvas.drawBitmap(mBackgroundScaledBitmap, 0, 0, mSecoPaint);
                canvas.drawBitmap(mBackgroundBitmapLogo, centerX - 45, centerY + 60, mSecoPaint);
                drawTimeTexts(canvas);

//                canvas.save();
//                canvas.scale(1.0f, 1.0f);
//                canvas.drawBitmap(mSecondBitmap, (width - mSecondBitmap.getWidth()) / 2, (height - mSecondBitmap.getHeight()) / 2, mSecoPaint);
//                canvas.restore();
                //save power
                if (Settings.Global.getInt(DigitalWatchFaceService.this.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 0) == 1) {
                    Log.d("isAirPlaneOn", "save power");
                } else {
                    canvas.save();
                    canvas.scale(1.0f, 1.0f);
                    canvas.drawBitmap(mSecondBitmap, (width - mSecondBitmap.getWidth()) / 2, (height - mSecondBitmap.getHeight()) / 2, mSecoPaint);
                    canvas.restore();
                }
            }
        }


        private void initDisplayText() {
            mTextColorPaint = new Paint();
            mTextColorPaint.setColor(mTextColor);
            mTextColorPaint.setTypeface(WATCH_TEXT_TYPEFACE);
            mTextColorPaint.setAntiAlias(true);
            mTextColorPaint.setTextSize(getResources().getDimension(R.dimen.text_size));
            mTextColorPaint.setStrokeWidth(3);
            mTextColorPaint.setTextAlign(Paint.Align.LEFT);

            mYearColorPaint = new Paint();
            mYearColorPaint.setColor(mYearColor);
            mYearColorPaint.setTypeface(WATCH_TEXT_TYPEFACE);
            mYearColorPaint.setAntiAlias(true);
            mYearColorPaint.setTextSize(getResources().getDimension(R.dimen.year_size));

            mWeakColorPaint = new Paint();
            mWeakColorPaint.setColor(mWeakColor);
            mWeakColorPaint.setTypeface(WATCH_TEXT_TYPEFACE);
            mWeakColorPaint.setAntiAlias(true);
            mWeakColorPaint.setTextSize(getResources().getDimension(R.dimen.weak_size));

            mSecoPaint = new Paint();
            mSecoPaint.setAntiAlias(true);
            mSecoPaint.setFilterBitmap(true);
        }

        private void updateTimer() {
            mTimeHandler.removeMessages(MSG_UPDATE_TIME_ID);
            if (isVisible() && !isInAmbientMode()) {
                mTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME_ID);
            }
        }

        private void drawTimeText(Canvas canvas) {
            if (mDisplayTime.minute >= 0 && mDisplayTime.minute < 10) {
                min = "0" + mDisplayTime.minute;
            } else {
                min = mDisplayTime.minute + "";
            }
            String timeText = getHourString() + ":" + min;
            Rect bounds = new Rect();
            mTextColorPaint.getTextBounds(timeText, 0, timeText.length(), bounds);
            canvas.drawText(timeText, centerX - bounds.width() / 2, centerY + bounds.height() / 2, mTextColorPaint);

            String year = mDisplayTime.year + "";
            String month = mDisplayTime.month + 1 + "";
            String day = mDisplayTime.monthDay + "";
            if (mDisplayTime.month + 1 >= 10) {
                month = mDisplayTime.month + 1 + "";
            } else {
                month = "0" + (mDisplayTime.month + 1);
            }
            if (getResources().getConfiguration().locale.getCountry().equals("CN")) {
                yearText = month + getString(R.string.month) + day + getString(R.string.day);
            } else {
                yearText = year + "/" + month + "/" + day;
            }
//            canvas.drawText(yearText, centerX - 72, centerY - 90, mYearColorPaint);
            canvas.drawText(yearText, centerX - 90, centerY - 82, mYearColorPaint);
            canvas.drawText(getDayString(), centerX + 30, centerY - 82, mWeakColorPaint);

        }

        private void drawTimeTexts(Canvas canvas) {
            if (mDisplayTime.minute >= 0 && mDisplayTime.minute < 10) {
                min = "0" + mDisplayTime.minute;
            } else {
                min = mDisplayTime.minute + "";
            }
            String timeText = getHourString() + ":" + min;
            Rect bounds = new Rect();
            mTextColorPaint.getTextBounds(timeText, 0, timeText.length(), bounds);
            canvas.drawText(timeText, centerX - bounds.width() / 2, centerY + bounds.height() / 2, mTextColorPaint);

            String year = mDisplayTime.year + "";
            String month = mDisplayTime.month + 1 + "";
            String day = mDisplayTime.monthDay + "";
            if (mDisplayTime.month + 1 >= 10) {
                month = mDisplayTime.month + 1 + "";
            } else {
                month = "0" + (mDisplayTime.month + 1);
            }
            if (getResources().getConfiguration().locale.getCountry().equals("CN")) {
                yearText = month + getString(R.string.month) + day + getString(R.string.day);
            } else {
                yearText = year + "/" + month + "/" + day;
            }
//            canvas.drawText(yearText, centerX - 72, centerY - 90, mYearColorPaint);
            canvas.drawText(yearText, centerX - 90, centerY - 110, mYearColorPaint);
            canvas.drawText(getDayString(), centerX + 30, centerY - 110, mWeakColorPaint);

        }

        private String getHourString() {
            return String.valueOf(mDisplayTime.hour);
        }

        private String getDayString() {
            Calendar c = Calendar.getInstance();
            c.setTimeZone(TimeZone.getTimeZone("GMT+8:00"));
            String mWay = String.valueOf(c.get(Calendar.DAY_OF_WEEK));
            Log.d("WeekTime:", mWay);
            if ("1".equals(mWay)) {
                return getString(R.string.Sun);
            } else if ("2".equals(mWay)) {
                return getString(R.string.Mon);
            } else if ("3".equals(mWay)) {
                return getString(R.string.Tue);
            } else if ("4".equals(mWay)) {
                return getString(R.string.Wed);
            } else if ("5".equals(mWay)) {
                return getString(R.string.Thu);
            } else if ("6".equals(mWay)) {
                return getString(R.string.Fri);
            } else {
                return getString(R.string.Sat);
            }
//            return dateTime[2];

        }

    }

}
