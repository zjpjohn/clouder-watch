package com.clouder.watch.common.sync;

/**
 * 数据同步的数据
 * 调用SyncService发送数据的内容
 * Created by yang_shoulai on 8/17/2015.
 */
public class SyncMessageWrapper {

    /**
     * 发送数据至手机的哪个应用
     */
    private String packageName;

    /**
     * 消息path,唯一标识一个消息
     */
    private String path;

    /**
     * 发送消息的具体内容
     */
    private SyncMessage content;

    public SyncMessageWrapper() {

    }

    public SyncMessageWrapper(String pkg, String path, SyncMessage message) {
        this.packageName = pkg;
        this.path = path;
        this.content = message;
    }

    /**
     * 返回需要发送的消息的字节数组
     *
     * @return
     */
    public byte[] getContentBytes() {
        return content == null ? null : content.toBytes();
    }



    public String getPackageName() {
        return packageName;
    }


    public String getPath() {
        return path;
    }


    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public SyncMessage getContent() {
        return content;
    }

    public void setContent(SyncMessage content) {
        this.content = content;
    }
}
