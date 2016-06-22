package com.clouder.watch.mobile;

import android.app.ActionBar;
import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.StringRes;
import android.view.View;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.TextView;

/**
 * Created by yang_shoulai on 8/27/2015.
 */
public class BaseActivity extends Activity {
    /**
     * 返回按钮
     */
    private ImageButton mBtnBack;

    /**
     * action bar 标题
     */
    private TextView mTvTitle;


    protected ClouderApplication application;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        customActionBar();
        application = (ClouderApplication) getApplication();
    }


    private void customActionBar() {
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
            actionBar.setCustomView(R.layout.activity_title_template);
            mTvTitle = (TextView) actionBar.getCustomView().findViewById(R.id.header_title);
            mBtnBack = (ImageButton) actionBar.getCustomView().findViewById(R.id.header_btn_back);
            mBtnBack.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                }
            });
        }
    }

    /**
     * 设置Action Bar标题
     *
     * @param titleId
     */
    public void setActionBarTitle(@StringRes int titleId) {
        mTvTitle.setText(titleId);
    }

    /**
     * 设置Action Bar标题
     *
     * @param title
     */
    public void setActionBarTitle(String title) {
        mTvTitle.setText(title);
    }

    /**
     * 隐藏Action Bar
     */
    public void hideActionBar() {
        getActionBar().hide();
    }

    /**
     * 展示Action Bar
     */
    public void showActionBar() {

    }

    /**
     * 显示Action Bar返回按钮
     */
    public void showActionBarBack() {
        this.mBtnBack.setVisibility(View.VISIBLE);
    }

    /**
     * 隐藏Action Bar返回按钮
     */
    public void hideActionBarBack() {
        this.mBtnBack.setVisibility(View.GONE);
    }

    /**
     * 设置Action Bar返回按钮点击事件处理
     *
     * @param listener
     */
    public void setOnActionBarClick(View.OnClickListener listener) {
        this.mBtnBack.setOnClickListener(listener);
    }
}
