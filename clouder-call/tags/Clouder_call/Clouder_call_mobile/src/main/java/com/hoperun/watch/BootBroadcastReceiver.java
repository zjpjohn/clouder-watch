package com.hoperun.watch;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.hoperun.watch.service.CallListenerService;

/**
 * Created by xing_peng on 2015/7/22.
 */
public class BootBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent service = new Intent(context, CallListenerService.class);//定义一个意图
        context.startService(service);//开启服务
    }

}
