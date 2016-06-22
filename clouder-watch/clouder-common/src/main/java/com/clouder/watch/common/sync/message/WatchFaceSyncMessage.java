package com.clouder.watch.common.sync.message;

import com.clouder.watch.common.sync.SyncMessage;
import com.clouder.watch.common.sync.SyncMessagePathConfig;
import com.clouder.watch.common.utils.Constant;
import com.google.gson.Gson;

import java.util.List;

/**
 * 用户手机向手表请求表盘同步
 * 手表在接收到该请求后，调用SyncService将所有的已安装的表盘信息发送至手机
 * Created by yang_shoulai on 8/17/2015.
 */
public class WatchFaceSyncMessage extends SyncMessage {

    private List<WatchFace> watchFaces;

    private boolean needSync = false;

    public WatchFaceSyncMessage(String packageName, String path) {
        super(Constant.CLOUDER_MOBILE_PKG, SyncMessagePathConfig.WATCH_FACES);
    }

    public List<WatchFace> getWatchFaces() {
        return watchFaces;
    }

    public void setWatchFaces(List<WatchFace> watchFaces) {
        this.watchFaces = watchFaces;
    }


    public boolean isNeedSync() {
        return needSync;
    }

    public void setNeedSync(boolean needSync) {
        this.needSync = needSync;
    }

    public static class WatchFace {
        /**
         * 表盘名称
         */
        private String name;

        /**
         * 表盘包名
         */
        private String packageName;

        /**
         * 表盘服务名
         */
        private String serviceName;

        /**
         * 表盘缩略图
         */
        private byte[] thumbnail;

        public String getPackageName() {
            return packageName;
        }

        public void setPackageName(String packageName) {
            this.packageName = packageName;
        }

        public String getServiceName() {
            return serviceName;
        }

        public void setServiceName(String serviceName) {
            this.serviceName = serviceName;
        }

        public byte[] getThumbnail() {
            return thumbnail;
        }

        public void setThumbnail(byte[] thumbnail) {
            this.thumbnail = thumbnail;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
