package com.cms.android.wearable.service.impl;

import android.os.RemoteException;
import android.util.Log;

import com.cms.android.wearable.service.codec.IParser;
import com.cms.android.wearable.service.codec.TransportData;
import com.cms.android.wearable.service.codec.TransportParser;
import com.cms.android.wearable.service.common.LogTool;

import java.io.IOException;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.PriorityBlockingQueue;

/**
 * Created by yang_shoulai on 11/6/2015.
 */
public class MessageQueueConsumer extends Thread {

    private static final String TAG = "MessageQueueConsumer";

    private static final int MESSAGE_FAILED_PRIORITY_OFFSET = 24 * 60 * 60 * 1000;

    private static final int DATA_FAILED_PRIORITY_OFFSET = 6 * 60 * 60 * 1000;

    private IRFCommService rfcommClientService;

    private PriorityBlockingQueue<QueuePriorityTask> messageQueue;

    private Map<String, QueuePriorityTask> taskMap;

    private Map<String, IParser> cacheInfoMap;

    private boolean canceled = false;

    public MessageQueueConsumer(IRFCommService service, PriorityBlockingQueue<QueuePriorityTask> queue, Map<String,
            QueuePriorityTask> taskMap, Map<String, IParser> cacheInfoMap) {
        setName(TAG);
        this.rfcommClientService = service;
        this.messageQueue = queue;
        this.taskMap = taskMap;
        this.cacheInfoMap = cacheInfoMap;
    }


    @Override
    public void run() {
        LogTool.d(TAG, "MessageQueueConsumer Start Working!");
        while (!canceled) {
            try {
                QueuePriorityTask task = messageQueue.take();
                LogTool.d(TAG, "取出一个task，剩余" + messageQueue.size() + "个task");
                String id = task.getData().getId();
                task.setTime(new Date().getTime());
                taskMap.put(id, task);
                TransportData data = task.getData();
                byte[] bytes = TransportParser.dataPack(data);
                if (!rfcommClientService.write(bytes)) {
                    LogTool.e(TAG, String.format("传输子包(UUID = %s)发送失败,重新发送", id));
                    // reset before repeat
                    resetCachePriority(task);
                    int repeat = task.getRepeat();
                    task.setRepeat(++repeat);
                    putQueue(task);
                    break;
                }
            } catch (InterruptedException e) {
                LogTool.e(TAG, "Caught InterruptedException", e);
                break;
            } catch (IOException e) {
                LogTool.e(TAG, "Caught IOException", e);
                break;
            } catch (RemoteException e) {
                LogTool.e(TAG, "Caught RemoteException", e);
                break;
            }
        }
        if (!canceled) {
            cancel();
            LogTool.d(TAG, "MessageQueueConsumer Stop!");
        }
    }

    public void cancel() {
        this.canceled = true;
    }

    public boolean isCanceled() {
        return canceled;
    }

    private void resetCachePriority(QueuePriorityTask task) {
        TransportData transportData = task.getData();
        String uuid = transportData.getUuid();
        IParser parser = cacheInfoMap.get(uuid);
        if (parser != null) {
            int offset = parser.getType() == IParser.TYPE_MESSAGE ? MESSAGE_FAILED_PRIORITY_OFFSET
                    : DATA_FAILED_PRIORITY_OFFSET;
            task.setPriority(task.getRepeat() == 0 ? (task.getPriority() - offset) : task.getPriority());
        } else {
            Log.d(TAG, "Can not find parser in cacheInfoMap");
        }

    }

    /**
     * put task in queue
     *
     * @param task
     */
    private void putQueue(QueuePriorityTask task) {
        this.messageQueue.offer(task);
    }
}
