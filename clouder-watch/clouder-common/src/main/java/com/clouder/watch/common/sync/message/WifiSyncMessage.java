package com.clouder.watch.common.sync.message;

import com.clouder.watch.common.sync.SyncMessage;
import com.clouder.watch.common.sync.SyncMessagePathConfig;
import com.google.gson.Gson;

/**
 * 将手机端的wifi连接信息同步至手表端
 * 包含wifi名称、密码等
 * Created by yang_shoulai on 8/17/2015.
 */
public class WifiSyncMessage extends SyncMessage {

    private String bssid;

    private String ssid;

    private String password;

    private String macAddress;

    private String ipAddress;

    private int type; //密码加密方式

    public WifiSyncMessage() {
        super(null, SyncMessagePathConfig.WIFI_INFO);
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getSsid() {
        return ssid;
    }

    public void setSsid(String ssid) {
        this.ssid = ssid;
    }

    public String getBssid() {
        return bssid;
    }

    public void setBssid(String bssid) {
        this.bssid = bssid;
    }

}
