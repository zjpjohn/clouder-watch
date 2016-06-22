package com.clouder.watch.setting.activity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.clouder.watch.common.sync.SyncMessage;
import com.clouder.watch.common.sync.SyncMessagePathConfig;
import com.clouder.watch.common.sync.SyncServiceHelper;
import com.clouder.watch.common.sync.message.CurrentWatchFaceSyncMessage;
import com.clouder.watch.common.ui.SwipeRightActivity;
import com.clouder.watch.common.utils.Constant;
import com.clouder.watch.common.utils.SettingsKey;
import com.clouder.watch.common.utils.WatchFaceHelper;
import com.clouder.watch.setting.R;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Created by yang_shoulai on 7/14/2015.
 */
public class WatchFaceActivity extends SwipeRightActivity {

    private static final String TAG = "WatchFaceActivity";

    private ViewPager viewPager;

    private List<WatchFaceHelper.LiveWallpaperInfo> watchFaces;

    private LayoutInflater inflater;

    private PackageManager packageManager;

    private PagerAdapter adapter;

    private Handler uiHandler = new Handler();

    private SyncServiceHelper syncServiceHelper = new SyncServiceHelper(this, new SyncServiceHelper.ISyncServiceCallback() {
        @Override
        public void onBindSuccess() {
            Log.d(TAG, "Bind Sync Service Success!");

        }

        @Override
        public void onSendSuccess(SyncMessage syncMessage) {
            Log.d(TAG, "Send message success!");
        }

        @Override
        public void onSendFailed(SyncMessage syncMessage, int reason) {
            Log.w(TAG, "Send Message failed with message = " + syncMessage + ", reason = " + reason);

        }
    });


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.setContentView(R.layout.activity_watch_face);
        initView();
        syncServiceHelper.bind();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        syncServiceHelper.unbind();
    }

    private void initView() {
        this.viewPager = (ViewPager) this.findViewById(R.id.vp_watch_face_preview);
        this.inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.packageManager = getPackageManager();
        ((View) viewPager.getParent()).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return viewPager.dispatchTouchEvent(event);
            }
        });
        viewPager.setOffscreenPageLimit(3);
        viewPager.setPageMargin(0);
        adapter = new MyPagerAdapter();
        viewPager.setAdapter(adapter);
        new Thread(new Runnable() {
            @Override
            public void run() {
                long beginLoad = System.currentTimeMillis();
                watchFaces = WatchFaceHelper.loadWatchFaces(WatchFaceActivity.this);
                long endLoad = System.currentTimeMillis();
                Log.d(TAG, "Load Watch Faces Cost = " + (endLoad - beginLoad));
                uiHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (adapter != null) {
                            adapter.notifyDataSetChanged();
                        }
                    }
                });
            }
        }).start();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }


    /**
     * when user click on a watch face preview image
     * this will set the clicked watch face to wall paper
     * as this action requires the interfaces not in android sdk and requires
     * permission[android.permission.SET_WALLPAPER_COMPONENT], [android.permission.BIND_WALLPAPER]
     * which are always used by system apps so we need this app run as a system application.
     */
    private class WatchFacePreviewOnClickListener implements View.OnClickListener {

        private static final String TAG = "WatchFacePreviewOnClick";

        private WatchFaceHelper.LiveWallpaperInfo watchFacePreview;

        public WatchFacePreviewOnClickListener(WatchFaceHelper.LiveWallpaperInfo liveWallpaperInfo, Context context) {
            this.watchFacePreview = liveWallpaperInfo;
        }

        @Override
        public void onClick(View v) {
            Log.d(TAG, "Watch Face \"" + watchFacePreview.info.getPackageName() + "\" Clicked!");
            WatchFaceHelper.setWatchFace(WatchFaceActivity.this, watchFacePreview);
            Settings.System.putString(getContentResolver(), SettingsKey.WATCH_FACE_PKG, watchFacePreview.info.getPackageName());
            Settings.System.putString(getContentResolver(), SettingsKey.WATCH_FACE_SERVICE_NAME, watchFacePreview.info.getServiceName());

            CurrentWatchFaceSyncMessage syncMessage = new CurrentWatchFaceSyncMessage(Constant.CLOUDER_SETTINGS_PKG, SyncMessagePathConfig.CURRENT_WATCH_FACE);
            syncMessage.setPackageName(watchFacePreview.info.getPackageName());
            syncMessage.setServiceName(watchFacePreview.info.getServiceName());
            syncServiceHelper.send(syncMessage);
            Intent intent = new Intent();
            intent.setComponent(new ComponentName(Constant.CLOUDER_LAUNCHER_PKG, Constant.CLOUDER_LAUNCHER_ACTIVITY));
            intent.putExtra("setWatchFace", true);
            try {
                Log.d(TAG, "Start to launcher activity!");
                startActivity(intent);
            } catch (Exception e) {
                Log.e(TAG, "Start launcher activity failed! have you installed launcher app?");
            }
            finish();
        }
    }

    private class MyPagerAdapter extends PagerAdapter {

        @Override
        public int getCount() {
            return watchFaces == null ? 0 : watchFaces.size();
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {

        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            View root = inflater.inflate(R.layout.item_watch_face, null);
            CircleImageView imageView = (CircleImageView) root.findViewById(R.id.thumbnail);
            TextView textView = (TextView) root.findViewById(R.id.title);
            final WatchFaceHelper.LiveWallpaperInfo wallpaperInfo = watchFaces.get(position);
            if (wallpaperInfo.thumbnail != null) {
                imageView.setImageDrawable(wallpaperInfo.thumbnail);
            }
            if (wallpaperInfo.info != null) {
                textView.setText(wallpaperInfo.info.loadLabel(packageManager));
            }
            imageView.setOnClickListener(new WatchFacePreviewOnClickListener(wallpaperInfo, WatchFaceActivity.this));
            container.addView(root);
            return root;
        }
    }

}
