package com.clouder.watch.guide;

import android.app.Activity;
import android.app.backup.BackupManager;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.provider.Settings;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.ActionMode;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;


import com.clouder.watch.common.ui.BaseAbstractActivity;
import com.clouder.watch.common.utils.SettingsKey;
import com.clouder.watch.common.widget.WatchToast;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Locale;

/**
 * 开机向导语言切换界面
 * 语言包含简体中文和英文两种选择
 * Created by yang_shoulai on 7/22/2015.
 */
public class GuideLanguageActivity extends BaseAbstractActivity implements View.OnClickListener {

    private static final String TAG = "GuideLanguageActivity";

    private Button btnEnglish;

    private Button btnChinese;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityCache.add(this);
        setContentView(R.layout.guide_language);
        initView();
    }

    private void initView() {
        btnEnglish = (Button) findViewById(R.id.btn_language_english);
        btnChinese = (Button) findViewById(R.id.btn_language_chinese);
        btnChinese.setOnClickListener(this);
        btnEnglish.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        Log.d(TAG, "Button with id equals " + v.getId() + " clicked!");
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null || adapter.getAddress() == null) {
            WatchToast.make(this, getString(R.string.please_wait_for_bt), Toast.LENGTH_SHORT).show();
            return;
        }
        int viewId = v.getId();
        if (viewId == R.id.btn_language_english) {
            updateLocale(Locale.US);
        } else if (viewId == R.id.btn_language_chinese) {
            updateLocale(Locale.CHINA);
        }
        //用户点击完成后跳转到开机向导的配对（二维码）界面
        startActivity(new Intent(this, GuidePairCodeActivity.class));
        overridePendingTransition(com.clouder.watch.common.R.anim.base_slide_right_in, com.clouder.watch.common.R.anim.base_slide_left_out);
    }

    /**
     * 切换系统语言
     *
     * @param locale
     */
    private void updateLocale(Locale locale) {
        String amnClassStr = "android.app.ActivityManagerNative";
        try {
            Class<?> amnClass = Class.forName(amnClassStr);
            Method getDefault = amnClass.getMethod("getDefault");
            Object am = getDefault.invoke(null);
            Method getConfiguration = am.getClass().getMethod("getConfiguration");
            Configuration configuration = (Configuration) getConfiguration.invoke(am);
            configuration.setLocale(locale);
            Method updateConfiguration = am.getClass().getMethod("updateConfiguration", Configuration.class);
            updateConfiguration.invoke(am, configuration);
            BackupManager.dataChanged("com.android.providers.settings");
        } catch (ClassNotFoundException e) {
            Log.e(TAG, "ClassNotFoundException", e);
        } catch (NoSuchMethodException e) {
            Log.e(TAG, "NoSuchMethodException", e);
        } catch (InvocationTargetException e) {
            Log.e(TAG, "InvocationTargetException", e);
        } catch (IllegalAccessException e) {
            Log.e(TAG, "IllegalAccessException", e);
        }

    }

}
