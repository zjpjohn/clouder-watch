package com.cms.android.wearable.service.impl;

import com.cms.android.wearable.service.codec.TransportData;

import java.util.Date;

/**
 * Created by yang_shoulai on 11/14/2015.
 */
public class QueuePriorityTask implements Comparable<QueuePriorityTask> {

    private TransportData data;

    private long priority;

    private long time;

    private int repeat;

    public QueuePriorityTask(long priority, TransportData data) {
        super();
        this.priority = priority;
        this.data = data;
        this.time = new Date().getTime();
        this.repeat = 0;
    }

    public void setPriority(long priority) {
        this.priority = priority;
    }

    public long getPriority() {
        return priority;
    }

    public TransportData getData() {
        return data;
    }

    @Override
    public int compareTo(QueuePriorityTask another) {
        // 数字小，优先级高
        return this.priority > another.priority ? 1 : this.priority < another.priority ? -1 : 0;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public int getRepeat() {
        return repeat;
    }

    public void setRepeat(int repeat) {
        this.repeat = repeat;
    }

}