package com.clouder.watch.launcher;

import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ResolveInfo;
import android.database.ContentObserver;
import android.media.AudioManager;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.clouder.watch.common.sync.IMessageListener;
import com.clouder.watch.common.sync.SyncMessage;
import com.clouder.watch.common.sync.SyncMessagePathConfig;
import com.clouder.watch.common.sync.SyncServiceHelper;
import com.clouder.watch.common.sync.message.SearchPhoneSyncMessage;
import com.clouder.watch.common.ui.BaseAbstractActivity;
import com.clouder.watch.common.utils.Constant;
import com.clouder.watch.common.utils.DateUtils;
import com.clouder.watch.common.utils.SystemSettingsUtils;
import com.clouder.watch.common.widget.WatchToast;
import com.clouder.watch.launcher.ui.SlidingDrawerView;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;


/**
 * Created by Administrator on 2015/8/13.
 */
public class LauncherActivity extends BaseAbstractActivity {

    private static final String TAG = "LauncherActivity";

    /**
     * widgets in quick setting page
     */
    private TextView power;
    private ProgressBar powerBar;
    private TextView time;
    private TextView week;
    private TextView date;
    private ImageButton btnLight;
    private ImageButton btnAirPlane;
    private ImageButton btnPhoneRing;
    private ImageButton btnMode;
    /**
     * widgets in app list page
     */
    private GridView gridView;
    private ListAdapter listAdapter;
    private static final int[] background = new int[]{R.drawable.selector_app_list_bg_blue, R.drawable.selector_app_list_bg_green, R.drawable.selector_app_list_bg_red, R.drawable.selector_app_list_bg_yellow};

    /**
     * container which contains 3 layouts and can sliding and drawer
     */
    private SlidingDrawerView container;

    private SyncServiceHelper syncServiceHelper;

    private SyncServiceHelper.ISyncServiceCallback callback = new SyncServiceHelper.ISyncServiceCallback() {
        @Override
        public void onBindSuccess() {
            syncServiceHelper.setMessageListener(SyncMessagePathConfig.SEARCH_PHONE, messageListener);
        }

        @Override
        public void onSendSuccess(SyncMessage syncMessage) {
            Log.d(TAG, "Send  message success");
            if (syncMessage instanceof SearchPhoneSyncMessage) {
                boolean search = ((SearchPhoneSyncMessage) syncMessage).isStartSearch();
                btnPhoneRing.setTag(search);
                if (search) {
                    btnPhoneRing.setBackgroundResource(R.drawable.quick_setting_find_phone_on);
                } else {
                    btnPhoneRing.setBackgroundResource(R.drawable.quick_setting_find_phone_off);
                }

            }
        }

        @Override
        public void onSendFailed(SyncMessage syncMessage, int reason) {
            Log.w(TAG, "Send message failed! reason : " + reason);
            if (reason == SyncServiceHelper.STATUS_FAILED_CMS_SERVICE_DISCONNECTED) {
                WatchToast.make(LauncherActivity.this, getString(R.string.bluetooth_disconnected), Toast.LENGTH_SHORT).show();
            } else {
                WatchToast.make(LauncherActivity.this, getString(R.string.message_send_failed), Toast.LENGTH_SHORT).show();
            }
        }
    };

    private IMessageListener messageListener = new IMessageListener() {
        @Override
        public void onMessageReceived(String path, SyncMessage message) {
            if (message instanceof SearchPhoneSyncMessage) {
                SearchPhoneSyncMessage msg = (SearchPhoneSyncMessage) message;
                if (!msg.isStartSearch()) {
                    btnPhoneRing.setTag(false);
                    btnPhoneRing.setBackgroundResource(R.drawable.quick_setting_find_phone_off);
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //bind connection with sync service
        syncServiceHelper = new SyncServiceHelper(this, callback);
        syncServiceHelper.bind();
        setContentView(R.layout.activity_launcher);
        initWidgets();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (this.btnLight != null) {
            initBrightness();
        }
        boolean isFromSetWatchFace = getIntent().getBooleanExtra("setWatchFace", false);
        Log.d(TAG, "isFromSetWatchFace ? " + isFromSetWatchFace);
        if (container != null && isFromSetWatchFace) {
            container.showContentView();
            getIntent().putExtra("setWatchFace", false);
        }

    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    @Override
    protected void onDestroy() {

        super.onDestroy();
        unregisterReceiver(appChangeReceiver);
        unregisterReceiver(quickSettingsReceiver);
        //取消监听飞行模式
        getContentResolver().unregisterContentObserver(airPlaneModeObserver);
        syncServiceHelper.unbind();
    }

    /**
     * 初始化页面组件
     */
    private void initWidgets() {
        this.container = (SlidingDrawerView) findViewById(R.id.container);
        this.container.setOnSwipeListener(new SlidingDrawerView.ISwipeListener() {
            @Override
            public void onSwipeRight() {
                Intent intent = new Intent(LauncherActivity.this, VoiceInputActivity.class);
                Log.d(TAG, "Start Voice Input Activity Intent :" + intent + ", activity class " + VoiceInputActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.activity_open_right, 0);

            }

            @Override
            public void onSwipeTop() {
                Log.d(TAG, "onSwipeTop");
                Intent intent = new Intent();
                intent.putExtra("onSwipeTop","onSwipeTop");
                intent.setComponent(new ComponentName(Constant.CLOUDER_NOTIFICATION_PKG, "com.clouder.watch.mobile.notification.SyncNotificationService"));
                try {
                    startService(intent);
//                    overridePendingTransition(R.anim.activity_open, 0);
                } catch (Exception e) {
                    Log.e(TAG, "start notification list activity failed! have you installed the app?", e);
                }
            }
        });
        //长按弹出表盘设置页面
        this.container.setContentViewOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                try {
                    Intent intent = new Intent();
                    intent.setComponent(new ComponentName(Constant.CLOUDER_SETTINGS_PKG, Constant.CLOUDER_SETTINGS_CHANGE_WATCH_FACE_ACTIVITY));
                    startActivity(intent);
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                } catch (ActivityNotFoundException e) {
                    Log.d(TAG, "clouder change watch face activity not found.", e);
                }
                return true;
            }
        });
        //点击弹出app list 页面
        this.container.setOnContentViewClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //container.showDrawer();
            }
        });
        initAppListUi();
        initQuickSettingsUi();
    }

    /**
     * 初始化快捷设置页面UI
     */
    private void initQuickSettingsUi() {
        this.power = (TextView) findViewById(R.id.power);
        this.powerBar = (ProgressBar) findViewById(R.id.power_bar);
        this.time = (TextView) findViewById(R.id.time);
        this.week = (TextView) findViewById(R.id.week);
        this.date = (TextView) findViewById(R.id.date);
        this.btnLight = (ImageButton) findViewById(R.id.btn_light);
        this.btnAirPlane = (ImageButton) findViewById(R.id.btn_air_plane);
        this.btnPhoneRing = (ImageButton) findViewById(R.id.btn_phone_ring);
        this.btnMode = (ImageButton) findViewById(R.id.btn_contextual_mode);

        initBrightness();

        if (SystemSettingsUtils.isAirPlaneOn(this)) {
            this.btnAirPlane.setBackground(getDrawable(R.drawable.quick_setting_air_plane_on));
        } else {
            this.btnAirPlane.setBackground(getDrawable(R.drawable.quick_setting_air_plane_off));
        }
        int mode = SystemSettingsUtils.getSystemSceneMode(this);
        if (mode == 0) {
            this.btnMode.setBackground(getDrawable(R.drawable.quick_setting_phone_shock_off));
        } else if (mode == 1) {
            this.btnMode.setBackground(getDrawable(R.drawable.quick_setting_phone_shock_on));
        } else if (mode == 2) {
            this.btnMode.setBackground(getDrawable(R.drawable.quick_setting_phone_shock_off));
        }
        String[] dateTime = DateUtils.resolve(new Date());
        time.setText(dateTime[1]);
        week.setText(dateTime[2]);
        date.setText(dateTime[0]);

        IntentFilter filter2 = new IntentFilter();
        filter2.addAction(Intent.ACTION_TIME_TICK);
        filter2.addAction(Intent.ACTION_BATTERY_CHANGED);
        filter2.addAction(Intent.ACTION_TIME_CHANGED);
        filter2.addAction(Intent.ACTION_DATE_CHANGED);
        filter2.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        registerReceiver(quickSettingsReceiver, filter2);
        /**
         *注册监听飞行模式是否改变
         */
        getContentResolver().registerContentObserver(Settings.Global.getUriFor(Settings.Global.AIRPLANE_MODE_ON), true, airPlaneModeObserver);
    }

    private void initBrightness() {
        int level = SystemSettingsUtils.judgeBrightnessLevel(this, SystemSettingsUtils.getLightness(this));
        if (level == -1) {
            this.btnLight.setBackgroundResource(R.drawable.quick_setting_light_auto);
        } else if (level == 1) {
            this.btnLight.setBackgroundResource(R.drawable.quick_setting_light_1);
        } else if (level == 2) {
            this.btnLight.setBackgroundResource(R.drawable.quick_setting_light_2);
        } else if (level == 3) {
            this.btnLight.setBackgroundResource(R.drawable.quick_setting_light_3);
        } else if (level == 4) {
            this.btnLight.setBackgroundResource(R.drawable.quick_setting_light_4);
        } else if (level == 5) {
            this.btnLight.setBackgroundResource(R.drawable.quick_setting_light_5);
        }
    }


    /**
     * 初始化应用列表页面UI
     */
    private void initAppListUi() {
        gridView = (GridView) super.findViewById(R.id.gridView);
        listAdapter = new ListAdapter(this, loadAllApps());
        gridView.setAdapter(listAdapter);
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addAction(Intent.ACTION_PACKAGE_REPLACED);
        filter.addDataScheme("package");
        registerReceiver(appChangeReceiver, filter);
    }

    private class ListAdapter extends BaseAdapter {

        private LayoutInflater inflater;

        private List<ResolveInfo> items;

        private Context context;

        public ListAdapter(Context context, List<ResolveInfo> items) {
            this.context = context;
            this.inflater = LayoutInflater.from(context);
            this.items = items;
        }

        public void setItems(List<ResolveInfo> items) {
            this.items.clear();
            this.items.addAll(items);
            this.notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return items == null ? 0 : items.size();
        }

        @Override
        public Object getItem(int position) {

            return items == null ? null : items.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                holder = new ViewHolder();
                convertView = inflater.inflate(R.layout.item_for_app_list, null);
                ImageButton firstImage = (ImageButton) convertView.findViewById(R.id.icon);
                TextView firstTitle = (TextView) convertView.findViewById(R.id.label);
                holder.icon = firstImage;
                holder.label = firstTitle;
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            final ResolveInfo item = (ResolveInfo) getItem(position);
            holder.icon.setBackground(context.getResources().getDrawable(background[position % background.length]));
            holder.icon.setImageDrawable(item.loadIcon(context.getPackageManager()));
            holder.label.setText(item.loadLabel(context.getPackageManager()));
            holder.icon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ComponentName component = new ComponentName(item.activityInfo.packageName, item.activityInfo.name);
                    Intent intent = new Intent();
                    intent.setComponent(component);
                    try {
                        context.startActivity(intent);
                    } catch (ActivityNotFoundException e) {
                        Log.d(TAG, "clouder change watch face activity not found.", e);
                        listAdapter.setItems(loadAllApps());
                    } catch (Exception e) {
                        Log.e(TAG, "", e);
                    }

                }
            });
            return convertView;
        }

        class ViewHolder {
            public ImageButton icon;
            public TextView label;
        }
    }


    /**
     * 应用安装、卸载时间广播接收处理器
     */
    private BroadcastReceiver appChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            String packageName = intent.getData().getSchemeSpecificPart();
            Log.d(TAG, "AppChangeReceiver action = " + action + ",Package name = " + packageName);
            listAdapter.setItems(loadAllApps());
            //gridView.setAdapter(new ListAdapter(LauncherActivity.this, loadAllApps()));
        }
    };


    /**
     * 快捷设置广播接收处理器
     */
    private BroadcastReceiver quickSettingsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_TIME_TICK.equals(intent.getAction())) {
                onTimeTick();
            } else if (Intent.ACTION_BATTERY_CHANGED.equals(intent.getAction())) {
                onBatteryChange(intent);
            } else if (Intent.ACTION_TIME_CHANGED.equals(intent.getAction())) {
                onTimeTick();
            } else if (Intent.ACTION_DATE_CHANGED.equals(intent.getAction())) {
                onTimeTick();
            } else if (Intent.ACTION_TIMEZONE_CHANGED.equals(intent.getAction())) {
                onTimeTick();
            }
        }
    };

    private void onTimeTick() {
        String[] array = DateUtils.resolve(new Date());
        time.setText(array[1]);
        week.setText(array[2]);
        date.setText(array[0]);
    }

    private void onBatteryChange(Intent intent) {
        int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        int curPower = (int) (level * 100 / (float) scale);
        power.setText(curPower + "%");
        if (curPower > 20) {
            power.setTextColor(getResources().getColor(R.color.green));
        } else {
            power.setTextColor(getResources().getColor(R.color.red));
        }
        this.powerBar.setProgress(curPower);
    }

    /**
     * 监听系统亮度的值是否发生改变
     */
    private ContentObserver airPlaneModeObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            if (SystemSettingsUtils.isAirPlaneOn(LauncherActivity.this)) {
                btnAirPlane.setBackground(getDrawable(R.drawable.quick_setting_air_plane_on));
            } else {
                btnAirPlane.setBackground(getDrawable(R.drawable.quick_setting_air_plane_off));
            }

        }
    };


    public void quickSettingClick(View view) {
        switch (view.getId()) {
            case R.id.btn_light:
                changeBrightness(view);
                break;

            case R.id.btn_air_plane:
                changeAirPlane(view);
                break;

            case R.id.btn_settings:
                try {
                    Intent intent = new Intent();
                    intent.setComponent(new ComponentName("com.clouder.watch.setting", "com.clouder.watch.setting.activity.SettingActivity"));
                    startActivity(intent);
                } catch (android.content.ActivityNotFoundException e) {
                    WatchToast.make(this, "Clouder settings app does not exist.", Toast.LENGTH_LONG).show();
                }
                break;

            case R.id.btn_phone_ring:
                ringPhone(view);
                break;

            case R.id.btn_contextual_mode:
                changeContextualMode(view);
                break;
            default:
                break;
        }
    }


    private void changeBrightness(View view) {
        int level = SystemSettingsUtils.judgeBrightnessLevel(this, SystemSettingsUtils.getLightness(this));
        if (level == -1) {
            SystemSettingsUtils.stopAutoBrightness(this);
            SystemSettingsUtils.setBrightness(this, 50);
            this.btnLight.setBackgroundResource(R.drawable.quick_setting_light_1);
        } else if (level == 5) {
            SystemSettingsUtils.startAutoBrightness(this);
            this.btnLight.setBackgroundResource(R.drawable.quick_setting_light_auto);
        } else if (level == 1) {
            SystemSettingsUtils.stopAutoBrightness(this);
            SystemSettingsUtils.setBrightness(this, (level + 1) * 50);
            this.btnLight.setBackgroundResource(R.drawable.quick_setting_light_2);
        } else if (level == 2) {
            SystemSettingsUtils.stopAutoBrightness(this);
            SystemSettingsUtils.setBrightness(this, (level + 1) * 50);
            this.btnLight.setBackgroundResource(R.drawable.quick_setting_light_3);
        } else if (level == 3) {
            SystemSettingsUtils.stopAutoBrightness(this);
            SystemSettingsUtils.setBrightness(this, (level + 1) * 50);
            this.btnLight.setBackgroundResource(R.drawable.quick_setting_light_4);
        } else if (level == 4) {
            SystemSettingsUtils.stopAutoBrightness(this);
            SystemSettingsUtils.setBrightness(this, (level + 1) * 50);
            this.btnLight.setBackgroundResource(R.drawable.quick_setting_light_5);
        }
    }

    private void changeAirPlane(View view) {
        if (SystemSettingsUtils.isAirPlaneOn(this)) {
            SystemSettingsUtils.setAirPlaneOn(this, false);
        } else {
            SystemSettingsUtils.setAirPlaneOn(this, true);
        }
    }

    private void ringPhone(View view) {
        //WatchToast.make(this, getString(R.string.function_not_complete), Toast.LENGTH_SHORT).show();
        //Log.d(TAG, "This function is not complete!");
        Object on = view.getTag();
        Log.d(TAG, "Ring on ? " + on + ", bind ? " + syncServiceHelper.isBind());
        if (!syncServiceHelper.isBind()) {
            WatchToast.make(this, getString(R.string.unbind_with_sync_service), Toast.LENGTH_SHORT).show();
        } else {
            SearchPhoneSyncMessage message = new SearchPhoneSyncMessage(SyncMessagePathConfig.SEARCH_PHONE);
            if (on == null || !(boolean) on) {
                message.setStartSearch(true);
            } else {
                message.setStartSearch(false);
            }
            syncServiceHelper.send(message);
        }


    }

    private void changeContextualMode(View view) {
        //AudioManager.RINGER_MODE_SILENT -> AudioManager.RINGER_MODE_VIBRATE --> AudioManager.RINGER_MODE_NORMAL
        ImageButton imageButton = (ImageButton) view;
        //TODO set view src and background
        int mode = SystemSettingsUtils.getSystemSceneMode(this);
        if (mode == AudioManager.RINGER_MODE_SILENT) {
            Log.d(TAG, "当前模式为 : AudioManager.RINGER_MODE_SILENT");
            SystemSettingsUtils.setSystemSceneMode(this, AudioManager.RINGER_MODE_VIBRATE);
            imageButton.setBackgroundResource(R.drawable.quick_setting_phone_shock_on);
        } else if (mode == AudioManager.RINGER_MODE_VIBRATE) {
            Log.d(TAG, "当前模式为 : AudioManager.RINGER_MODE_VIBRATE");
            SystemSettingsUtils.setSystemSceneMode(this, AudioManager.RINGER_MODE_NORMAL);
            imageButton.setBackgroundResource(R.drawable.quick_setting_phone_shock_off);
        } else if (mode == AudioManager.RINGER_MODE_NORMAL) {
            Log.d(TAG, "当前模式为 : AudioManager.RINGER_MODE_NORMAL");
            SystemSettingsUtils.setSystemSceneMode(this, AudioManager.RINGER_MODE_VIBRATE);
            imageButton.setBackgroundResource(R.drawable.quick_setting_phone_shock_on);
        }

    }


    /**
     * 加载已安装的应用
     *
     * @return
     */
    private List<ResolveInfo> loadAllApps() {
        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> list = getPackageManager().queryIntentActivities(mainIntent, 0);
        Iterator<ResolveInfo> iterator = list.iterator();
        while (iterator.hasNext()) {
            ResolveInfo info = iterator.next();
            if (info.activityInfo.packageName.equals(this.getPackageName())) {
                iterator.remove();
                break;
            }
        }
        return new ArrayList<>(list);
    }

}
