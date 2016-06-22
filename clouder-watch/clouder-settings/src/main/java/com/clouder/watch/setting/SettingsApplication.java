package com.clouder.watch.setting;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import com.clouder.watch.common.utils.Constant;

/**
 * Created by yang_shoulai on 7/14/2015.
 */
public class SettingsApplication extends Application {

    private static final String TAG = "SettingsApplication";

    private static SettingsApplication settingsApplication;


    @Override
    public void onCreate() {
        super.onCreate();
        settingsApplication = this;
    }

    public static SettingsApplication getInstance() {
        return settingsApplication;
    }

    /**
     * 取得应用的版本号
     *
     * @return
     */
    public int getAppVersionCode() {
        PackageManager pm = getPackageManager();
        try {
            PackageInfo pi = pm.getPackageInfo(getPackageName(), 0);
            return pi.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Can not find app version code.", e);
        }
        return 0;
    }

    /**
     * 取得应用的版本名
     *
     * @return
     */
    public String getAppVersionName() {
        PackageManager pm = getPackageManager();
        try {
            PackageInfo pi = pm.getPackageInfo(getPackageName(), 0);
            return pi.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Can not find app version name.", e);
        }
        return "";
    }




}
