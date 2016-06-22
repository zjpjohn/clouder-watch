package com.clouder.watch.mobile.sync.handler;

import android.app.AlertDialog;
import android.app.Service;
import android.content.DialogInterface;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.os.Vibrator;
import android.view.WindowManager;

import com.clouder.watch.common.sync.IHandler;
import com.clouder.watch.common.sync.IMessageListener;
import com.clouder.watch.common.sync.SyncMessage;
import com.clouder.watch.common.sync.SyncMessagePathConfig;
import com.clouder.watch.common.sync.message.SearchPhoneSyncMessage;
import com.clouder.watch.mobile.SyncService;

import java.io.IOException;

/**
 * Created by yang_shoulai on 9/6/2015.
 */
public class SearchPhoneHandler implements IHandler<SearchPhoneSyncMessage>, IMessageListener {


    private SyncService syncService;

    private AlertDialog dialog;

    private Vibrator vibrator;

    private MediaPlayer mp;


    public SearchPhoneHandler(SyncService service) {
        syncService = service;

        vibrator = (Vibrator) syncService.getSystemService(Service.VIBRATOR_SERVICE);

        try {

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    public void handle(String path, final SearchPhoneSyncMessage message) {
        syncService.mHandler.post(new Runnable() {
            @Override
            public void run() {
                boolean start = message.isStartSearch();
                if (!start) {
                    if (dialog != null && dialog.isShowing()) {
                        dialog.dismiss();
                        vibrator.cancel();
                        if (mp != null) {
                            mp.stop();
                        }
                    }
                } else {
                    if (dialog == null) {
                        final SearchPhoneSyncMessage msg = new SearchPhoneSyncMessage(SyncMessagePathConfig.SEARCH_PHONE);
                        msg.setStartSearch(false);
                        AlertDialog.Builder builder = new AlertDialog.Builder(syncService);
                        builder.setTitle("提示");
                        builder.setMessage("已找到手机");
                        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                syncService.sendMessage(msg);
                                vibrator.cancel();
                                if (mp != null) {
                                    mp.stop();
                                }
                            }
                        });
                        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                syncService.sendMessage(msg);
                                vibrator.cancel();
                                if (mp != null) {
                                    mp.stop();
                                }
                            }
                        });
                        builder.setCancelable(false);
                        dialog = builder.create();
                        dialog.getWindow().setType((WindowManager.LayoutParams.TYPE_SYSTEM_ALERT));
                    }
                    if (!dialog.isShowing()) {
                        dialog.show();
                        vibrator.vibrate(new long[]{100, 400, 100, 400}, 1);
                        try {
                            mp = new MediaPlayer();
                            mp.setDataSource(syncService, RingtoneManager
                                    .getDefaultUri(RingtoneManager.TYPE_RINGTONE));
                            mp.setLooping(true);
                            mp.prepare();
                            mp.start();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                    }

                }
            }
        });

    }

    @Override
    public void onMessageReceived(String path, SyncMessage message) {
        handle(path, (SearchPhoneSyncMessage) message);
    }
}
