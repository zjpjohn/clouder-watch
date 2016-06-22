package com.clouder.watch.setting.activity;

import android.os.Bundle;
import android.provider.Settings;
import android.view.View;

import com.clouder.watch.common.ui.SwipeRightActivity;
import com.clouder.watch.common.utils.SettingsKey;
import com.clouder.watch.common.utils.SystemSettingsUtils;
import com.clouder.watch.setting.R;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by yang_shoulai on 7/14/2015.
 */
public class BrightnessActivity extends SwipeRightActivity implements View.OnClickListener {

    private View[] views;

    private static Map<Integer, Integer> brightnessMap = new HashMap<>();

    static {
        brightnessMap.put(R.id.level_auto, -1);
        brightnessMap.put(R.id.level_1, 1);
        brightnessMap.put(R.id.level_2, 2);
        brightnessMap.put(R.id.level_3, 3);
        brightnessMap.put(R.id.level_4, 4);
        brightnessMap.put(R.id.level_5, 5);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_brightness);
        initView();
    }


    private void initView() {
        views = new View[6];
        views[0] = findViewById(R.id.level_auto);
        views[1] = findViewById(R.id.level_1);
        views[2] = findViewById(R.id.level_2);
        views[3] = findViewById(R.id.level_3);
        views[4] = findViewById(R.id.level_4);
        views[5] = findViewById(R.id.level_5);
        views[0].setOnClickListener(this);
        views[1].setOnClickListener(this);
        views[2].setOnClickListener(this);
        views[3].setOnClickListener(this);
        views[4].setOnClickListener(this);
        views[5].setOnClickListener(this);
        int level = SystemSettingsUtils.judgeBrightnessLevel(this, SystemSettingsUtils.getLightness(this));
        selectLevel(level);
    }

    @Override
    public void onClick(View v) {
        int level = brightnessMap.get(v.getId()) == null ? 0 : brightnessMap.get(v.getId());
        selectLevel(level);
        writeBrightness(level);

    }

    public void selectLevel(int level) {
        for (View view : views) {
            view.setBackground(null);
        }
        int position = level == -1 ? 0 : level;
        views[position].setBackground(getDrawable(R.drawable.btn_settings_light_auto));

    }

    public void writeBrightness(int level) {
        if (level == -1) {
            SystemSettingsUtils.startAutoBrightness(this);
        } else {
            SystemSettingsUtils.stopAutoBrightness(this);

            SystemSettingsUtils.setBrightness(this, level * 50);
        }
        Settings.System.putInt(getContentResolver(), SettingsKey.BRIGHTNESS_LEVEL, level);
    }
}
