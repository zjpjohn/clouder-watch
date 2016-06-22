package com.hoperun.watch.service;

import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.util.Log;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.WearableListenerService;
import com.hoperun.watch.activity.IncomingCallActivity;

/**
 * Created by xing_peng on 2015/7/13.
 */
public class MessageListenerService extends WearableListenerService {

    private static String TAG = "MessageListenerService";

    private static String INCOMING_CALL =  "/call/incoming";
    private static String DIAL_CALL =  "/call/dial";
    private static String REJECT_CALL = "/call/reject";
    private static String ACCEPT_CALL = "/call/accept";
    private static String TERMINATE_CALL = "/call/terminate";

    public static String ACCEPT_ACTION = "com.hoperun.watch.AcceptCall";
    public static String TERMINATE_ACTION = "com.hoperun.watch.TerminateCall";
    public static String REJECT_ACTION ="com.hoperun.watch.RejectCall";

    public static void handleIncomingCall(Context context, String node, String path, byte[] data) {
        if (!inZenMode(context)) {

            if (DIAL_CALL.equals(path)) {
                // 去电
                String number = new String(data);
                Log.d(TAG, "DIAL number = " + number);

                Intent startIntent = new Intent(context, IncomingCallActivity.class);
                startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startIntent.putExtra("number", number);
                startIntent.putExtra("node", node);
                startIntent.putExtra("command", "dialCall");
                context.startActivity(startIntent);

            } else if (INCOMING_CALL.equals(path)) {
                // 来电
                String number = new String(data);
                Log.d(TAG, "INCOMING number = " + number);

                Intent startIntent = new Intent(context, IncomingCallActivity.class);
                startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startIntent.putExtra("number", number);
                startIntent.putExtra("node", node);
                startIntent.putExtra("command", "incomingCall");
                context.startActivity(startIntent);

            } else if (ACCEPT_CALL.equals(path)) {
                // 接通
                Log.d(TAG, "ACCEPT_CALL");

                Intent updateIntent = new Intent();
                updateIntent.setAction(ACCEPT_ACTION);
                context.sendBroadcast(updateIntent);

            } else  if (TERMINATE_CALL.equals(path)) {
                // 挂断
                Log.d(TAG, "TERMINATE_CALL");

                Intent updateIntent = new Intent();
                updateIntent.setAction(TERMINATE_ACTION);
                context.sendBroadcast(updateIntent);

            } else  if (REJECT_CALL.equals(path)) {
                // 拒接
                Log.d(TAG, "REJECT_CALL");

                Intent rejectIntent = new Intent();
                rejectIntent.setAction(REJECT_ACTION);
                context.sendBroadcast(rejectIntent);
            }
        }
    }

    @Override
    public void onCreate() {

    }

    @Override
    public void onPeerConnected(Node node) {
        Log.d(TAG, "Node Connected : " + node.getId());
    }

    @Override
    public void onPeerDisconnected(Node node) {
        Log.d(TAG, "Node Disconnected : " + node.getId());
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        if (messageEvent.getPath().startsWith("/call")) {
            Log.d(TAG, "onMessageReceived path : " + messageEvent.getPath());
            handleIncomingCall(getApplicationContext(), messageEvent.getSourceNodeId(), messageEvent.getPath(), messageEvent.getData());
        }
    }

    // 检查是否飞行模式
    private static boolean inZenMode(Context paramContext)
    {
        boolean bool = false;
        try
        {
            if (Settings.Global.getInt(paramContext.getContentResolver(), "in_zen_mode") != 0)
                bool = true;
        }
        catch (Settings.SettingNotFoundException localSettingNotFoundException)
        {
            Log.e(TAG, "Setting not found");
        }
        Log.d(TAG, "inZenMode : " + bool);
        return bool;
    }
}
