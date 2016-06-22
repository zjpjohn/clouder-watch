package com.clouder.watch.common.sync;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

/**
 * Created by yang_shoulai on 9/6/2015.
 */
public class SyncServiceHelper {

    private static final String TAG = "SyncServiceHelper";
    /**
     * package name of clouder watch sync service run on watch device
     */
    public static final String CLOUDER_SYNC_SERVICE_PKG = "com.clouder.watch.mobile";

    /**
     * service name of clouder watch sync service
     */
    public static final String CLOUDER_SYNC_SERVICE = "com.clouder.watch.mobile.SyncService";

    /**
     * status for send message success
     */
    public static final int STATUS_SUCCESS = 200;

    /**
     * status for send message failed as cms service disconnected
     */
    public static final int STATUS_FAILED_CMS_SERVICE_DISCONNECTED = 100;

    /**
     * status for send message failed as disconnect to sync service
     */
    public static final int STATUS_FAILED_SYNC_SERVICE_DISCONNECT = 300;

    /**
     * status for send message failed as remote exception occurs
     */
    public static final int STATUS_FAILED_REMOTE_EXCEPTION = 400;

    /**
     * status for send message failed as unknown error
     */
    public static final int STATUS_FAILED_UNKNOWN_ERROR = 500;


    public static final int MSG_TYPE_REGISTER_LISTENER = 1;

    public static final int MSG_TYPE_UNREGISTER_LISTENER = 2;

    public static final int MSG_TYPE_SEND_MESSAGE = 3;

    public static final String KEY_SYNC_MESSAGE_IN_DATA = "key_sync_message_in_data";

    public static final String KEY_SYNC_MESSAGE_PATH = "key_sync_message_path";


    public interface ISyncServiceCallback {

        void onBindSuccess();

        void onSendSuccess(SyncMessage syncMessage);

        void onSendFailed(SyncMessage syncMessage, int reason);

    }

    private IMessageListener messageListener;

    private Context bindContext;

    private ISyncServiceCallback mCallback;

    public SyncServiceHelper(Context context, ISyncServiceCallback callback) {
        bindContext = context;
        mCallback = callback;
    }

    private Messenger syncMessenger;

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(bindContext.getClass().getSimpleName(), "Connected with sync service!");
            syncMessenger = new Messenger(service);
            if (mCallback != null) {
                mCallback.onBindSuccess();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.w(bindContext.getClass().getSimpleName(), "Disconnected with sync service!");
        }
    };

    private Handler mHandler = new IncomingHandler();

    private Messenger mMessenger = new Messenger(mHandler);

    private Messenger msgListener = new Messenger(new MessageListener());


    /**
     * client should invoke this method first
     */
    public void bind() {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName(CLOUDER_SYNC_SERVICE_PKG, CLOUDER_SYNC_SERVICE));
        try {
            bindContext.bindService(intent, connection, Context.BIND_AUTO_CREATE);
        } catch (Exception e) {
            Log.e(bindContext.getClass().getSimpleName(), "Exception", e);
        }

    }

    /**
     * unbind the client with sync service
     */
    public void unbind() {
        unRegisterListener();
        if (bindContext != null && syncMessenger != null) {
            bindContext.unbindService(connection);
        }
    }

    /**
     * send message to sync service
     * then sync serice can send the message to handle device using cms service
     *
     * @param msgObj the msg data
     */
    public final void send(SyncMessage msgObj) {
        if (syncMessenger != null && msgObj != null) {
            Message message = Message.obtain();
            message.getData().putByteArray(KEY_SYNC_MESSAGE_IN_DATA, msgObj.toBytes());
            message.what = MSG_TYPE_SEND_MESSAGE;
            message.getData().putString(KEY_SYNC_MESSAGE_PATH, msgObj.getPath());
            message.replyTo = mMessenger;
            try {
                syncMessenger.send(message);
            } catch (RemoteException e) {
                Log.e(TAG, "RemoteException", e);
                if (mCallback != null) {
                    mCallback.onSendFailed(msgObj, STATUS_FAILED_REMOTE_EXCEPTION);
                }
            }
        } else {
            if (mCallback != null) {
                Log.e(TAG, "Disconnected with cms service!");
                mCallback.onSendFailed(msgObj, STATUS_FAILED_SYNC_SERVICE_DISCONNECT);
            }
        }
    }

    /**
     * judge whether client has connected with sync service
     *
     * @return
     */
    public boolean isBind() {
        return syncMessenger != null;
    }


    public void setMessageListener(String path, IMessageListener listener) {
        if (messageListener != null) {
            unRegisterListener();
        }
        messageListener = listener;
        if (syncMessenger != null && messageListener != null) {
            Message message = Message.obtain();
            message.getData().putString(KEY_SYNC_MESSAGE_PATH, path);
            message.replyTo = msgListener;
            message.what = MSG_TYPE_REGISTER_LISTENER;
            try {
                syncMessenger.send(message);
            } catch (RemoteException e) {
                Log.e(bindContext.getClass().getSimpleName(), "RemoteException", e);
            }
        }

    }

    private void unRegisterListener() {
        if (syncMessenger != null && messageListener != null) {
            Message message = Message.obtain();
            message.what = MSG_TYPE_UNREGISTER_LISTENER;
            message.replyTo = msgListener;
            try {
                syncMessenger.send(message);
            } catch (RemoteException e) {
                Log.e(bindContext.getClass().getSimpleName(), "RemoteException", e);
            }
        }
    }


    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            int status = msg.what;
            String path = msg.getData().getString(KEY_SYNC_MESSAGE_PATH);
            byte[] bytes = msg.getData().getByteArray(KEY_SYNC_MESSAGE_IN_DATA);
            SyncMessage message = SyncMessageParser.parse(path, bytes);
            if (message != null && mCallback != null) {
                if (status == STATUS_SUCCESS) {
                    mCallback.onSendSuccess(message);
                } else {
                    if (status == STATUS_FAILED_CMS_SERVICE_DISCONNECTED) {
                        mCallback.onSendFailed(message, STATUS_FAILED_CMS_SERVICE_DISCONNECTED);
                    } else {
                        mCallback.onSendFailed(message, STATUS_FAILED_UNKNOWN_ERROR);
                    }
                }

            }

        }
    }

    class MessageListener extends Handler {
        @Override
        public void handleMessage(Message msg) {
            String path = msg.getData().getString(KEY_SYNC_MESSAGE_PATH);
            byte[] bytes = msg.getData().getByteArray(KEY_SYNC_MESSAGE_IN_DATA);
            SyncMessage message = SyncMessageParser.parse(path, bytes);
            if (messageListener != null) {
                messageListener.onMessageReceived(path, message);
            }
        }
    }
}
