package com.clouder.watch.common.ui;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;


/**
 * Created by yang_shoulai on 6/26/2015.
 */
public abstract class BaseAbstractActivity extends Activity {

    private static final String TAG = "BaseAbstractActivity";

    @Override
    protected void onRestart() {
        super.onRestart();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }


    /**
     * 复写该方法是为了解决当时板子安装应用以后会经常出现的Activity无法获取的bug
     * 由查看日志可知，是因为在系统获取Intent ComponentName时将包名（全类名）中的“.”置换为
     * “/”,所以在此处将强制ComponentName中将“/”置换为“.”
     *
     * @param intent
     */
    @Override
    public void startActivity(Intent intent) {
        ComponentName componentName = intent.getComponent();
        if (componentName != null) {
            String pkg = componentName.getPackageName();
            String clz = componentName.getClassName();
            Log.d(TAG, String.format("Start Activity[%s, %s]", pkg, clz));
            if (clz.contains("/")) {
                Log.d(TAG, String.format("Replace class all \"/\" with \".\""));
                String newClz = clz.replace("/", ".");
                componentName = new ComponentName(pkg, newClz);
                intent.setComponent(componentName);
                intent.setAction(intent.getAction());
            }
        }
        super.startActivity(intent);
    }

}
