package com.clouder.watch.mobile.handler;

import android.content.Context;
import android.provider.Settings;
import android.util.Log;

import com.clouder.watch.common.sync.IHandler;
import com.clouder.watch.common.sync.IMessageListener;
import com.clouder.watch.common.sync.SyncMessage;
import com.clouder.watch.common.sync.message.NotificationBlackListSyncMessage;
import com.clouder.watch.common.utils.SettingsKey;

import java.util.List;

/**
 * 通知设置黑名单同步处理
 * Created by yang_shoulai on 8/17/2015.
 */
public class NotificationBlackListHandler implements IHandler<NotificationBlackListSyncMessage>, IMessageListener {

    private static final String TAG = "BlackListHandler";


    private Context context;

    public NotificationBlackListHandler(Context context) {
        this.context = context;
    }


    private String getDetail(List<String> list) {
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < list.size(); i++) {
            buffer.append(list.get(i));
            if (i != list.size() - 1) {
                buffer.append(",");
            }
        }
        return buffer.toString();
    }


    @Override
    public void handle(String path, NotificationBlackListSyncMessage message) {
        List<String> list = message.getList();
        if (list == null || list.isEmpty()) {
            Log.i(TAG, "Notification black list to be saved is empty! method will return.");
            return;
        }
        String str = getDetail(list);
        Log.i(TAG, String.format("Receive notification black list app from handle device with size [%s] contains [%s]", list, str));
        Settings.System.putString(context.getContentResolver(), SettingsKey.NOTIFICATION_BLACK_LIST, str);
    }

    @Override
    public void onMessageReceived(String path, SyncMessage message) {
        handle(path, (NotificationBlackListSyncMessage) message);
    }
}
