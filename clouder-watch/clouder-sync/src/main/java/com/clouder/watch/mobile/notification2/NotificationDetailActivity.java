package com.clouder.watch.mobile.notification2;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.clouder.watch.common.ui.SwipeRightActivity;
import com.clouder.watch.mobile.R;

/**
 * Created by yang_shoulai on 12/8/2015.
 */
public class NotificationDetailActivity extends SwipeRightActivity {

    private static final String TAG = "NotificationDetail";

    private TextView tvTitle;
    private TextView tvContent;
    private TextView tvDate;
    private ImageView ivIcon;
    private ImageView ivSeeOnPhone;
    private ImageView firstImage;
    private ImageView secondImage;
    private ViewPager viewPager;
    private ServiceConnection serviceConnection;
    private Messenger serviceMessenger;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail_notification);
        viewPager = (ViewPager) findViewById(R.id.viewPager);
        firstImage = (ImageView) findViewById(R.id.first);
        secondImage = (ImageView) findViewById(R.id.second);
        final View detailView = getLayoutInflater().inflate(R.layout.item_detail_notification, null);
        tvTitle = (TextView) detailView.findViewById(R.id.title);
        tvContent = (TextView) detailView.findViewById(R.id.content);
        ivIcon = (ImageView) detailView.findViewById(R.id.icon);
        tvDate = (TextView) detailView.findViewById(R.id.timeDate);
        final View seeOnPhoneView = getLayoutInflater().inflate(R.layout.item_see_on_phone, null);
        ivSeeOnPhone = (ImageView) seeOnPhoneView.findViewById(R.id.btn_see_on_phone);
        viewPager.setAdapter(new PagerAdapter() {
            @Override
            public int getCount() {
                return 2;
            }

            @Override
            public boolean isViewFromObject(View view, Object object) {
                return view == object;
            }


            @Override
            public Object instantiateItem(ViewGroup container, int position) {
                if (position == 0) {
                    container.addView(detailView);
                    return detailView;
                } else if (position == 1) {
                    container.addView(seeOnPhoneView);
                    return seeOnPhoneView;
                }
                return null;
            }

            @Override
            public void destroyItem(ViewGroup container, int position, Object object) {
                if (position == 0) {
                    container.removeView(detailView);
                } else if (position == 1) {
                    container.removeView(seeOnPhoneView);
                }
            }
        });
        initView();
        serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                serviceMessenger = new Messenger(service);
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {

            }
        };
        Intent notificationListenerService = new Intent(this, NotificationListenerService.class);
        bindService(notificationListenerService, serviceConnection, Context.BIND_AUTO_CREATE);


    }

    private void initView() {
        Intent intent = getIntent();
        if (intent != null) {
            final Notification notification = intent.getParcelableExtra(NotificationListenerService.EXTRA_NOTIFICATION);
            if (notification == null) {
                return;
            }
            tvTitle.setText(notification.getTitle());
            tvContent.setText(notification.getContent());
            tvDate.setText(notification.getTime());
            ivIcon.setImageBitmap(notification.getIcon() == null ?
                    BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher) : notification.getIcon());
            ivSeeOnPhone.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    seeOnPhone(notification.getUuid());
                }
            });
            viewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
                @Override
                public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

                }

                @Override
                public void onPageSelected(int position) {
                    switch (position) {
                        case 0:
                            firstImage.setImageDrawable(getResources().getDrawable(R.drawable.circle_2));
                            secondImage.setImageDrawable(getResources().getDrawable(R.drawable.circle_1));
                            break;
                        case 1:
                            firstImage.setImageDrawable(getResources().getDrawable(R.drawable.circle_1));
                            secondImage.setImageDrawable(getResources().getDrawable(R.drawable.circle_2));
                            break;
                        default:
                            break;
                    }
                }

                @Override
                public void onPageScrollStateChanged(int state) {

                }
            });

        }
    }

    private void seeOnPhone(String uuid) {
        Message msg = Message.obtain();
        msg.what = NotificationListenerService.MSG_WHAT_SEE_ON_PHONE;
        msg.getData().putString(NotificationListenerService.EXTRA_UUID, uuid);
        if (serviceMessenger != null) {
            try {
                serviceMessenger.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (serviceConnection != null) {
            unbindService(serviceConnection);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        initView();
        viewPager.setCurrentItem(0);
    }
}
