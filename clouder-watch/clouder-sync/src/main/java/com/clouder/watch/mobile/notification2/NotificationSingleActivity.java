package com.clouder.watch.mobile.notification2;

import android.app.Service;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.clouder.watch.common.ui.SwipeRightActivity;
import com.clouder.watch.mobile.R;

/**
 * Created by yang_shoulai on 12/8/2015.
 */
public class NotificationSingleActivity extends SwipeRightActivity {

    private static final String TAG = "NotificationSingle";

    private TextView title;

    private TextView content;

    private ImageView icon;

    private GestureDetector gestureDetector;

    private static final int MIN_FLING_DISTANCE = 80;

    private TextView time;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_single_notification);
        intWidgets();
        initView();
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (e1 == null || e2 == null) return super.onFling(e1, e2, velocityX, velocityY);
                if ((e1.getRawY() - e2.getRawY()) > MIN_FLING_DISTANCE) {
                    Intent intent = getIntent();
                    if (intent != null) {
                        Notification notification = intent.getParcelableExtra(NotificationListenerService.EXTRA_NOTIFICATION);
                        if (notification != null) {
                            Intent detail = new Intent(NotificationSingleActivity.this, NotificationDetailActivity.class);
                            detail.putExtra(NotificationListenerService.EXTRA_NOTIFICATION, notification);
                            //start detail activity
                            startActivity(detail);
                            overridePendingTransition(R.anim.base_slide_bottom_in, 0);
                            finish();
                        }
                    }
                    return true;
                }
                return super.onFling(e1, e2, velocityX, velocityY);
            }
        });

        findViewById(R.id.container).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                gestureDetector.onTouchEvent(event);
                return true;
            }
        });
    }

    private void intWidgets() {
        title = (TextView) findViewById(R.id.title);
        content = (TextView) findViewById(R.id.content);
        icon = (ImageView) findViewById(R.id.icon);
        time = (TextView) findViewById(R.id.time);
    }

    private void initView() {
        Intent intent = getIntent();
        if (intent != null) {
            final Notification notification = intent.getParcelableExtra(NotificationListenerService.EXTRA_NOTIFICATION);
            if (notification != null) {
                title.setText(notification.getTitle());
                content.setText(notification.getContent());
                time.setText(notification.getDate());
                icon.setImageBitmap(notification.getIcon() == null ? BitmapFactory.decodeResource(getResources(),
                        R.drawable.ic_launcher) : notification.getIcon());
                if (notification.isShock()) {
                    Vibrator vib = (Vibrator) getSystemService(Service.VIBRATOR_SERVICE);
                    //vibrator.vibrate(1000);// Only shock second, once
                    long[] pattern = {0, 300};
                    vib.vibrate(pattern, -1);
                }

                /*btnMore.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent detail = new Intent(NotificationSingleActivity.this, NotificationDetailActivity.class);
                        detail.putExtra(NotificationListenerService.EXTRA_NOTIFICATION, notification);
                        //start detail activity
                        startActivity(detail);
                        finish();
                    }
                });*/
            }

        }

    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.d(TAG, "onNewIntent");
        setIntent(intent);
        initView();
    }


}
