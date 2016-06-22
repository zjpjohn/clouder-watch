package com.clouder.watch.mobile.activity;

import android.os.Bundle;
import android.view.View;

import com.clouder.watch.mobile.BaseActivity;
import com.clouder.watch.mobile.R;
import com.clouder.watch.mobile.utils.ToastUtils;

/**
 * Created by yang_shoulai on 7/27/2015.
 */
public class AboutActivity extends BaseActivity implements View.OnClickListener {

    private View btnCheckUpdate;

    private View btnUserHelp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        setActionBarTitle(R.string.about_title);
        btnCheckUpdate = findViewById(R.id.check_update);
        btnUserHelp = findViewById(R.id.user_helper);
        btnCheckUpdate.setOnClickListener(this);
        btnUserHelp.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();

        if (id == R.id.check_update) {
            ToastUtils.show(this, "功能尚未实现，敬请期待！");
        } else if (id == R.id.user_helper) {
            ToastUtils.show(this, "功能尚未实现，敬请期待！");
        }
    }


}
