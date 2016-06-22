package com.clouder.watch.common.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;

import com.clouder.watch.common.R;


/**
 * Created by yang_shoulai on 6/26/2015.
 */
public abstract class SwipeTopActivity extends BaseAbstractActivity {

    protected SwipeTopLayout layout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        layout = (SwipeTopLayout) LayoutInflater.from(this).inflate(
                R.layout.swipe_top_layout, null);
        layout.attachToActivity(this);
    }


    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, R.anim.base_slide_top_out);
    }
}
