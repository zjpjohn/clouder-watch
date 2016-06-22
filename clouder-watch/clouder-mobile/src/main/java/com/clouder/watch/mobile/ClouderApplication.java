package com.clouder.watch.mobile;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Handler;
import android.util.Log;

import com.clouder.watch.common.utils.StringUtils;
import com.clouder.watch.mobile.notification.NotificationService;
import com.clouder.watch.mobile.utils.LockStatusService;
import com.cms.android.wearable.service.common.FileUtil;
import com.cms.android.wearable.service.common.LogTool;
import com.cms.android.wearable.service.common.Utils;
import com.cms.android.wearable.service.impl.BLECentralService;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by yang_shoulai on 7/28/2015.
 */
public class ClouderApplication extends Application {

    private static final String TAG = "ClouderApplication";

    private static ClouderApplication application;

    private static final String SHARED_PREFERENCE_NAME = "sp_clouder_mobile";

    /**
     * 绑定的手表蓝牙地址
     */
    public static final String BIND_BT_ADDRESS = "com.clouder.watch.mobile.bind_bt_address";

    /**
     * 绑定的手表蓝牙名称
     */
    public static final String BIND_BT_NAME = "com.clouder.watch.mobile.bind_bt_name";

    /**
     * 绑定的手表ble设备地址
     */
    public static final String BIND_BLE_ADDRESS = "com.clouder.watch.mobile.bind_ble_address";

    /**
     * 是否消息推送
     * false,手表不接受任何推送消息
     * true,打开消息推送
     */
    public static final String NOTIFICATION_PUSH = "com.clouder.watch.mobile.notification_push";

    /**
     * 手机接收消息时是否震动
     * false,静默
     * true,接收到消息时震动
     */
    public static final String NOTIFICATION_SHOCK = "com.clouder.watch.mobile.notification_shock";

    /**
     * 消息推送黑名单
     * 黑名单中的应用将无法推送消息
     */
    public static final String NOTIFICATION_BLACK_LIST = "com.clouder.watch.mobile.notification_black_list";


    /**
     * 存储的手表端所有的表盘应用
     * 格式："packageNameA:serviceA:nameA,packageNameB:serviceB:nameB"
     */
    public static final String WATCH_FACES = "com.clouder.watch.mobile.watch_face_list";

    /**
     * 选中的表盘应用包名
     */
    public static final String CURRENT_WATCH_FACE_PKG = "com.clouder.watch.mobile.current_watch_face_pkg";

    /**
     * 选中的表盘应用的服务名
     */
    public static final String CURRENT_WATCH_FACE_SERVICE = "com.clouder.watch.mobile.current_watch_face_service";

    /**
     * 是否启用屏幕锁
     * true,启用
     * false,不启用
     */
    public static final String SCREEN_LOCKER_ENABLE = "com.clouder.watch.mobile.screen_locker_enable";

    /**
     * 存储手表端锁屏的密码
     */
    public static final String SCREEN_LOCKER_PWD = "com.clouder.watch.mobile.screen_locker_password";

    /**
     * 已经配对的设备列表
     */
    public static final String PAIRED_BLUETOOTH_DEVICES = "com.clouder.watch.mobile.paired_bluetooth_devices";


    public static final String DEVICE_FIRST_START = "com.clouder.watch.mobile.device_first_start";


    public static class PairedBluetooth {

        private String bluetoothName;

        private String bluetoothAddress;

        public PairedBluetooth() {

        }

        public PairedBluetooth(String bluetoothName, String bluetoothAddress) {
            this.bluetoothName = bluetoothName;
            this.bluetoothAddress = bluetoothAddress;
        }

        public String getBluetoothName() {
            return bluetoothName;
        }

        public void setBluetoothName(String bluetoothName) {
            this.bluetoothName = bluetoothName;
        }

        public String getBluetoothAddress() {
            return bluetoothAddress;
        }

        public void setBluetoothAddress(String bluetoothAddress) {
            this.bluetoothAddress = bluetoothAddress;
        }
    }

    private Timer timer = new Timer();

    @Override
    public void onCreate() {
        super.onCreate();
        application = this;
        if (isPaired()) {
            Log.d(TAG, String.format("检测到设备已经配对，设备蓝牙地址[%s],蓝牙名称[%s]", getBindBtAddress(), getBindBtName()));
            startBLECentralService();
            startServices(this, getBindBtAddress(), getBindBtName());
            registerTimeTickReceiver();
        }
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                boolean toggle = Utils.getShardBondToggle(application);
                if (toggle) {
                    startBLECentralService();
                } else {
                    LogTool.d(TAG, "toggle is false,do nothing.");
                }
            }
        }, 15000, 15000);
    }


    public static void startServices(Context context, String btAddress, String btName) {
        Log.d(TAG, "启动CMS服务,btAddress = " + btAddress + ", btName = " + btName);
        Intent intent = new Intent(context, BLECentralService.class);
        intent.putExtra("bluetooth_bond_address", btAddress);
        intent.putExtra("bluetooth_bond_name", btName);
        context.startService(intent);
        Log.d(TAG, "启动同步服务");
        context.startService(new Intent(context, SyncService.class));
        Log.d(TAG, "启动通知同步服务");
        context.startService(new Intent(context, NotificationService.class));
        Log.d(TAG, "启动联系人同步服务");
        context.startService(new Intent(context, SyncContactsService.class));

        context.startService(new Intent(context, LockStatusService.class));
    }

    public void registerTimeTickReceiver() {
        LogTool.e(TAG, "注册TimeTickBroadcastReceiver");
        IntentFilter filter = new IntentFilter(Intent.ACTION_TIME_TICK);
        registerReceiver(new TimeTickBroadcastReceiver(), filter);
    }


    private static class TimeTickBroadcastReceiver extends BroadcastReceiver {

        @SuppressLint("SimpleDateFormat")
        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.getAction().equals(Intent.ACTION_TIME_TICK)) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                Calendar cal = Calendar.getInstance();
                LogTool.d(TAG, "ACTION_TIME_TICK->" + sdf.format(cal.getTime()));
                int minute = cal.get(Calendar.MINUTE);
                int second = cal.get(Calendar.SECOND);
                /**
                 * 0分0秒即意味着每一小时触发一次
                 */
                if (minute == 0 && second == 0) {
                    FileUtil.deletCacheFile();
                }
               /* boolean toggle = Utils.getShardBondToggle(context);
                if (toggle) {
                    startBLECentralService();
                } else {
                    LogTool.d(TAG, "toggle is false,do nothing.");
                }*/
            }
        }
    }

    private boolean isPaired() {
        String btAddress = getBindBtAddress();
        String btName = getPackageName();
        return com.clouder.watch.mobile.utils.StringUtils.isNotEmpty(btAddress)
                && com.clouder.watch.mobile.utils.StringUtils.isNotEmpty(btName);
    }

    public static ClouderApplication getInstance() {
        return application;
    }

    private SharedPreferences getClouderSharedPreferences() {
        return getSharedPreferences(SHARED_PREFERENCE_NAME, Context.MODE_PRIVATE);
    }


    private void putBoolean(String key, boolean value) {
        SharedPreferences.Editor editor = getClouderSharedPreferences().edit();
        editor.putBoolean(key, value);
        editor.commit();
    }

    private void putString(String key, String value) {
        SharedPreferences.Editor editor = getClouderSharedPreferences().edit();
        editor.putString(key, value);
        editor.commit();
    }


    private void putInt(String key, int value) {
        SharedPreferences.Editor editor = getClouderSharedPreferences().edit();
        editor.putInt(key, value);
        editor.commit();
    }

    private int getInt(String key, int def) {
        return getClouderSharedPreferences().getInt(key, def);
    }

    private String getString(String key, String def) {
        return getClouderSharedPreferences().getString(key, def);
    }

    private boolean getBoolean(String key, boolean def) {
        return getClouderSharedPreferences().getBoolean(key, def);
    }

    /**
     * 添加通知黑名单
     *
     * @param packageName
     */
    public void addNotificationBlackItem(String packageName) {
        List<String> list = getNotificationBlackList();
        if (list == null) {
            list = new ArrayList<>();
            list.add(packageName);
        } else {
            if (!list.contains(packageName)) {
                list.add(packageName);
            }
        }
        putString(NOTIFICATION_BLACK_LIST, new Gson().toJson(list));
    }

    /**
     * 删除通知黑名单
     *
     * @param packageName
     */
    public void removeNotificationBlackItem(String packageName) {
        List<String> list = getNotificationBlackList();
        if (list != null) {
            if (list.contains(packageName)) {
                list.remove(packageName);
                putString(NOTIFICATION_BLACK_LIST, new Gson().toJson(list));
            }
        }
    }

    /**
     * 取得所有处于黑名单中的app
     *
     * @return
     */
    public List<String> getNotificationBlackList() {
        String res = getString(NOTIFICATION_BLACK_LIST, null);
        if (res == null) {
            return new ArrayList<>();
        }
        List<String> list = new Gson().fromJson(res, new TypeToken<List<String>>() {
        }.getType());
        return list;
    }

    private void startBLECentralService() {
        try {
            Intent serviceIntent = new Intent();
            serviceIntent.putExtra(Utils.BLUETOOTH_BOND_ADDRESS,
                    Utils.getShardBondAddress(ClouderApplication.this));
            serviceIntent.setAction(BLECentralService.BLE_CENTRAL_SERVICE_ACTION);
            Boolean toggle = Utils.getShardBondToggle(this);
            serviceIntent.putExtra(Utils.BLUETOOTH_BOND_TOGGLE, toggle);
            Intent newIntent = Utils.createExplicitFromImplicitIntent(ClouderApplication.this, serviceIntent);
            if (newIntent != null) {
                startService(newIntent);
            }
        } catch (Exception e) {
            LogTool.e(TAG, "Exception", e);
        }
    }

    /**
     * 添加配对设备
     *
     * @param pairedBluetooth
     */
    public void addPairedBluetooth(PairedBluetooth pairedBluetooth) {
        List<PairedBluetooth> list = getPairedBluetooth();
        if (list.isEmpty()) {
            list = new ArrayList<>();
            list.add(pairedBluetooth);
        } else {
            Iterator<PairedBluetooth> iterator = list.iterator();
            while (iterator.hasNext()) {
                PairedBluetooth bluetooth = iterator.next();
                if (bluetooth.getBluetoothAddress().equals(pairedBluetooth.getBluetoothAddress())) {
                    iterator.remove();
                }
            }
            list.add(pairedBluetooth);
        }
        putString(PAIRED_BLUETOOTH_DEVICES, new Gson().toJson(list));
    }


    /**
     * 判断蓝牙远程蓝牙设备知否在配对历史记录之中
     *
     * @param address
     * @return
     */
    public boolean isBluetoothPaired(String address) {
        List<PairedBluetooth> list = getPairedBluetooth();
        if (list.isEmpty()) {
            return false;
        } else {
            for (PairedBluetooth bluetooth : list) {
                if (bluetooth.getBluetoothAddress().equals(address)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 返回已经配对过的设备列表
     *
     * @return
     */
    public List<PairedBluetooth> getPairedBluetooth() {
        String res = getString(PAIRED_BLUETOOTH_DEVICES, null);
        if (res == null) {
            return new ArrayList<>();
        }
        List<PairedBluetooth> list = new Gson().fromJson(res, new TypeToken<List<PairedBluetooth>>() {
        }.getType());
        return list;
    }


    /**
     * 设置绑定的蓝牙MAC地址
     *
     * @param btAddress
     */
    public void setBindBtAddress(String btAddress) {
        putString(BIND_BT_ADDRESS, btAddress);
    }

    /**
     * 取得绑定的蓝牙MAC地址
     *
     * @return
     */
    public String getBindBtAddress() {
        return getString(BIND_BT_ADDRESS, null);
    }

    /**
     * 设置已经绑定的蓝牙名称
     *
     * @param name
     */
    public void setBindBtName(String name) {
        putString(BIND_BT_NAME, name);
    }

    /**
     * 取得已经绑定的蓝牙名称
     *
     * @return
     */
    public String getBindBtName() {
        return getString(BIND_BT_NAME, null);
    }

    /**
     * 设置通知推送是否开启
     *
     * @param enable
     */
    public void setNotificationPushEnable(boolean enable) {
        putBoolean(NOTIFICATION_PUSH, enable);
    }

    /**
     * 取得通知推送是否启用
     *
     * @return
     */
    public boolean isNotificationPushEnable() {
        return getBoolean(NOTIFICATION_PUSH, true);
    }

    /**
     * 设置通知到达时手表是否震动
     *
     * @param shock
     */
    public void setNotificationShockEnable(boolean shock) {
        putBoolean(NOTIFICATION_SHOCK, shock);
    }

    /**
     * 取得通知到达时手表是否震动
     *
     * @return
     */
    public boolean isNotificationShockEnable() {
        return getBoolean(NOTIFICATION_SHOCK, true);
    }

    /**
     * 设置当前正在使用的表盘应用包名
     *
     * @param pkg
     */
    public void setCurrentWatchFacePackage(String pkg) {

        putString(CURRENT_WATCH_FACE_PKG, pkg);
    }

    /**
     * 取得当前正在使用的表盘的包名
     *
     * @return
     */
    public String getCurrentWatchFacePackage() {
        return getString(CURRENT_WATCH_FACE_PKG, "");
    }

    /**
     * 设置当前正在使用的表盘的服务名
     *
     * @param service
     */
    public void setCurrentWatchFaceService(String service) {
        putString(CURRENT_WATCH_FACE_SERVICE, service);
    }

    /**
     * 取得当前正在使用的表盘的服务名
     *
     * @return
     */
    public String getCurrentWatchFaceService() {
        return getString(CURRENT_WATCH_FACE_SERVICE, "");
    }


    /**
     * 设置屏幕锁是否启用
     *
     * @param enable
     */
    public void setScreenLockerEnable(boolean enable) {
        putBoolean(SCREEN_LOCKER_ENABLE, enable);
    }

    /**
     * 取得是否启用屏幕锁
     *
     * @return
     */
    public boolean isScreenLockerEnable() {
        return getBoolean(SCREEN_LOCKER_ENABLE, false);
    }

    /**
     * 设置屏幕锁的密码
     */
    public void setScreenLockerPassword(String password) {
        putString(SCREEN_LOCKER_PWD, password);
    }

    /**
     * 取得当前的屏幕锁密码
     *
     * @return
     */
    public String getScreenLockerPassword() {
        return getString(SCREEN_LOCKER_PWD, "");
    }

    /**
     * 保存表盘信息
     *
     * @param watchFace
     */
    public void addWatchFaces(WatchFace watchFace) {
        if (watchFace == null) {
            return;
        }
        String packageName = watchFace.getPackageName();
        String serviceName = watchFace.getServiceName();
        String name = watchFace.getName();
        if (StringUtils.isEmpty(packageName)
                || StringUtils.isEmpty(serviceName) || StringUtils.isEmpty(name)) {
            return;
        }
        List<WatchFace> watchFaces = getWatchFaces();

        if (watchFaces == null) {
            watchFaces = new ArrayList<>();
        }
        if (!watchFaceExist(watchFace, watchFaces)) {
            watchFaces.add(watchFace);
        }
        putString(WATCH_FACES, new Gson().toJson(watchFaces));

    }

    /**
     * remove the specified watch face
     *
     * @param watchFace
     */
    public void removeWatchFaces(WatchFace watchFace) {
        if (watchFace == null) {
            return;
        }
        String packageName = watchFace.getPackageName();
        String serviceName = watchFace.getServiceName();
        String name = watchFace.getName();
        if (StringUtils.isEmpty(packageName)
                || StringUtils.isEmpty(serviceName) || StringUtils.isEmpty(name)) {
            return;
        }
        List<WatchFace> watchFaces = getWatchFaces();

        if (watchFaces == null) {
            return;
        }
        if (watchFaceExist(watchFace, watchFaces)) {
            Iterator<WatchFace> iterator = watchFaces.iterator();
            while (iterator.hasNext()) {
                WatchFace face = iterator.next();
                if (face.getPackageName().equals(watchFace.getPackageName())
                        && face.getServiceName().equals(watchFace.getServiceName())
                        && face.getName().equals(watchFace.getName())) {
                    iterator.remove();
                }

            }
        }
        putString(WATCH_FACES, new Gson().toJson(watchFaces));

    }

    /**
     * whether the given watch face exist
     *
     * @param watchFace
     * @param watchFaces
     * @return
     */
    private boolean watchFaceExist(WatchFace watchFace, List<WatchFace> watchFaces) {
        for (WatchFace w : watchFaces) {
            if (w.getPackageName().equals(watchFace.getPackageName())
                    && w.getServiceName().equals(watchFace.getServiceName())
                    && w.getName().equals(watchFace.getName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 清空缓存的表盘信息
     */
    public void clearWatchFace() {
        putString(WATCH_FACES, null);
    }

    /**
     * 取得所有保存的表盘信息
     *
     * @return
     */
    public List<WatchFace> getWatchFaces() {
        String res = getString(WATCH_FACES, null);
        if (!StringUtils.isEmpty(res)) {
            return new Gson().fromJson(res, new TypeToken<List<WatchFace>>() {
            }.getType());
        }
        return null;
    }

    public void setDeviceFirstStart(boolean b) {
        putBoolean(DEVICE_FIRST_START, b);
    }

    public boolean isDeviceFirstStart() {
        return getBoolean(DEVICE_FIRST_START, true);
    }


    public static class WatchFace {
        private String packageName;
        private String serviceName;
        private String name;

        public String getPackageName() {
            return packageName;
        }

        public void setPackageName(String packageName) {
            this.packageName = packageName;
        }

        public String getServiceName() {
            return serviceName;
        }

        public void setServiceName(String serviceName) {
            this.serviceName = serviceName;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
