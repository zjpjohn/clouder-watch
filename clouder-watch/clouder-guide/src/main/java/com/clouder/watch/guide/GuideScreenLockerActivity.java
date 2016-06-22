package com.clouder.watch.guide;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.clouder.watch.common.utils.Constant;
import com.clouder.watch.common.utils.SettingsKey;
import com.clouder.watch.common.utils.StringUtils;
import com.clouder.watch.common.utils.WatchFaceHelper;
import com.clouder.watch.common.widget.WatchToast;

import java.util.List;

/**
 * Created by yang_shoulai on 7/22/2015.
 */
public class GuideScreenLockerActivity extends Activity implements View.OnClickListener {

    private static final String TAG = "GuideScreenLocker";

    private Button btnSkip;

    private Button btnSettingPwd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityCache.add(this);
        setContentView(R.layout.guide_screen_locker);
        initView();
    }

    private void initView() {
        btnSkip = (Button) findViewById(R.id.btn_setting_skip);
        btnSettingPwd = (Button) findViewById(R.id.btn_setting_pwd);
        btnSkip.setOnClickListener(this);
        btnSettingPwd.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        Log.d(TAG, "Button with id equals " + v.getId() + " clicked!");
        int id = v.getId();
        if (id == R.id.btn_setting_pwd) {
            try {
                Intent intent = new Intent();
                intent.setComponent(new ComponentName(Constant.CLOUDER_LOCKER_PKG, Constant.CLOUDER_LOCKER_SET_PASSWORD));
                startActivity(intent);
            } catch (ActivityNotFoundException e) {
                Log.e(TAG, "ActivityNotFoundException", e);
                WatchToast.make(this, getString(R.string.clouder_locker_app_not_found), Toast.LENGTH_SHORT).show();
            }
        } else if (id == R.id.btn_setting_skip) {
            try {
                Settings.System.putInt(getContentResolver(), SettingsKey.SCREEN_LOCKER_ENABLE, 0);
                Settings.Global.putInt(getContentResolver(), Settings.Global.DEVICE_PROVISIONED, 1);
                PackageManager pm = getPackageManager();
                ComponentName name = new ComponentName(this, GuideLanguageActivity.class);
                pm.setComponentEnabledSetting(name, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
            } catch (Exception e) {
                e.printStackTrace();
            }
            //选择跳过则默认不启用屏幕锁
            if (StringUtils.isEmpty(SettingsKey.getLockPassword(this))) {
                Settings.System.putInt(getContentResolver(), SettingsKey.SCREEN_LOCKER_ENABLE, 0);
            } else {
                Settings.System.putInt(getContentResolver(), SettingsKey.SCREEN_LOCKER_ENABLE, 1);
                Intent i = new Intent();
                i.putExtra("first_set_password", 1 + "");
                i.setComponent(new ComponentName("com.clouder.watch.mobile", "com.clouder.watch.mobile.notification.TestService"));
                startService(i);
            }
            Settings.System.putInt(getContentResolver(), SettingsKey.DEVICE_PAIRED, 1);

            //设置系统初始化默认表盘
            setWatchFace();

            //用户向导结束，进入Launcher App
            try {
                Intent launcher = new Intent();
                launcher.setComponent(new ComponentName(Constant.CLOUDER_LAUNCHER_PKG, Constant.CLOUDER_LAUNCHER_ACTIVITY));
                startActivity(launcher);
            } catch (Exception e) {
                Log.e(TAG, "Error when start device launcher!", e);
            }

            //退出GUIDE
            ActivityCache.finishAll();
            System.exit(0);

        }


    }

    /**
     * 检查上次选择的表盘并且设置
     */
    private void setWatchFace() {
        Log.d(TAG, "Set watch face before checking if device is paired!");
        List<WatchFaceHelper.LiveWallpaperInfo> watchFaces = WatchFaceHelper.loadWatchFaces(this);
        if (watchFaces != null && !watchFaces.isEmpty()) {
            boolean exist = false;
            String savedPkg = Settings.System.getString(getContentResolver(), SettingsKey.WATCH_FACE_PKG);
            String savedService = Settings.System.getString(getContentResolver(), SettingsKey.WATCH_FACE_SERVICE_NAME);
            if (StringUtils.isEmpty(savedPkg) || StringUtils.isEmpty(savedService)) {
                Log.d(TAG, "No watch face set record found, we will pick the first one to set.");
            } else {
                Log.d(TAG, String.format("Found last watch face set record! watch face app package [%s], service name [%s].", savedPkg, savedService));
                for (WatchFaceHelper.LiveWallpaperInfo watchFace : watchFaces) {
                    ComponentName componentName = watchFace.intent.getComponent();
                    if (componentName.getPackageName().equals(savedPkg) && componentName.getClassName().equals(savedService)) {
                        Log.d(TAG, "We find it and will set it");
                        WatchFaceHelper.setWatchFace(this, watchFace);
                        exist = true;
                        break;
                    }
                }
            }
            if (!exist) {
                ComponentName componentName = watchFaces.get(0).intent.getComponent();
                Log.d(TAG, String.format("No watch face installed match, we will choose the first one to set. witch package is [%s] and service name is [%s]",
                        componentName.getPackageName(), componentName.getClassName()));
                WatchFaceHelper.setWatchFace(this, watchFaces.get(0));
                Settings.System.putString(getContentResolver(), SettingsKey.WATCH_FACE_PKG, componentName.getPackageName());
                Settings.System.putString(getContentResolver(), SettingsKey.WATCH_FACE_SERVICE_NAME, componentName.getClassName());
            }
        } else {
            Log.e(TAG, "No watch face found! are you sure you have installed at least one watch face?");
        }
    }

}
