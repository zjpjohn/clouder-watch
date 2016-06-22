package com.clouder.watch.common.ui;

import android.os.Bundle;
import android.view.LayoutInflater;

import com.clouder.watch.common.R;


/**
 * Created by yang_shoulai on 6/26/2015.
 */
public abstract class SwipeRightActivity extends BaseAbstractActivity {

    protected SwipeRightLayout layout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        layout = (SwipeRightLayout) LayoutInflater.from(this).inflate(R.layout.swipe_right_layout, null);
        layout.attachToActivity(this);
    }


}
