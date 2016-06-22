package com.clouder.watch.common.utils;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by yang_shoulai on 7/21/2015.
 */
public class WifiHelper {

    private static final String TAG = "WifiHelper";

    // 定义几种加密方式，一种是WEP，一种是WPA，还有没有密码的情况
    public enum WifiCipherType {
        WIFICIPHER_WEP, WIFICIPHER_WPA, WIFICIPHER_NOPASS, WIFICIPHER_INVALID
    }


    private WifiManager wifiManager;

    private WifiManager.WifiLock lock;

    public WifiHelper(Context context) {
        this.wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        this.lock = wifiManager.createWifiLock("clouder setting wifi lock");
    }

    public boolean isOn() {
        Log.d(TAG, "wifiManager.isWifiEnabled()");
        boolean wifiOn = this.wifiManager.isWifiEnabled();
        Log.d(TAG, "wifiManager.isWifiEnabled() wifiOn ? " + wifiOn);
        return wifiOn;
    }

    public void open() {
        this.wifiManager.setWifiEnabled(true);
    }

    public void close() {
        this.wifiManager.setWifiEnabled(false);
    }

    public int getState() {
        Log.d(TAG, "wifiManager.getWifiState() begin");
        int state = this.wifiManager.getWifiState();
        Log.d(TAG, "wifiManager.getWifiState() return, state = " + state);
        return state;
    }

    public WifiInfo getConnectionInfo() {
        Log.d(TAG, "wifiManager.getConnectionInfo() begin");
        return this.wifiManager.getConnectionInfo();
    }

    public void lock() {
        if (!lock.isHeld()) {
            lock.acquire();
        }
    }

    public void unlock() {
        if (lock.isHeld()) {
            lock.release();
        }
    }


    // 查看以前是否也配置过这个网络
    private WifiConfiguration isExsits(String SSID) {
        List<WifiConfiguration> existingConfigs = wifiManager.getConfiguredNetworks();
        if (existingConfigs != null) {
            for (WifiConfiguration existingConfig : existingConfigs) {
                if (existingConfig.SSID.equals("\"" + SSID + "\"")) {
                    return existingConfig;
                }
            }
        }
        return null;
    }

    private WifiConfiguration createWifiInfo(String SSID, String Password, WifiCipherType Type) {
        Log.d(TAG, "createWifiInfo begin");
        WifiConfiguration config = new WifiConfiguration();
        config.allowedAuthAlgorithms.clear();
        config.allowedGroupCiphers.clear();
        config.allowedKeyManagement.clear();
        config.allowedPairwiseCiphers.clear();
        config.allowedProtocols.clear();
        config.SSID = "\"" + SSID + "\"";
        if (Type == WifiCipherType.WIFICIPHER_NOPASS) {
            config.wepKeys[0] = "";
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            config.wepTxKeyIndex = 0;
        }else if (Type == WifiCipherType.WIFICIPHER_WEP) {
            config.preSharedKey = "\"" + Password + "\"";
            config.hiddenSSID = true;
            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            config.wepTxKeyIndex = 0;
        }else if (Type == WifiCipherType.WIFICIPHER_WPA) {
            config.preSharedKey = "\"" + Password + "\"";
            config.hiddenSSID = true;
            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
            // 此处需要修改否则不能自动重联
            // config.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
            config.status = WifiConfiguration.Status.ENABLED;
        } else {
            config = null;
        }
        Log.d(TAG, "createWifiInfo end, config = " + config);
        return config;
    }

    // 提供一个外部接口，传入要连接的无线网
    public boolean connect(String ssid, String password, WifiCipherType type) {
        Log.d(TAG, "connect");
        WifiConfiguration wifiConfig = createWifiInfo(ssid, password, type);
        if (wifiConfig == null) {
            return false;
        }
        WifiConfiguration tempConfig = isExsits(ssid);
        if (tempConfig != null) {
            wifiManager.removeNetwork(tempConfig.networkId);
        }
        int netID = wifiManager.addNetwork(wifiConfig);
        Log.d(TAG, "enableNetwork");
        return wifiManager.enableNetwork(netID, true);
    }


    public List<WifiConfiguration> getConfiguredNetworks() {
        List<WifiConfiguration> list = this.wifiManager.getConfiguredNetworks();
        List<WifiConfiguration> results = new ArrayList<>();
        if (list != null && !list.isEmpty()) {
            for (WifiConfiguration configuration : list) {
                if (configuration.status != WifiConfiguration.Status.DISABLED) {
                    results.add(configuration);
                }
            }

        }
        return results;
    }


    public void connect(int networkId) {
        this.wifiManager.enableNetwork(networkId, true);
    }

    public void disconnect(int netId) {
        Log.d(TAG, "disableNetwork");
        //this.wifiManager.disableNetwork(netId);
        Log.d(TAG, "disconnect");
        this.wifiManager.disconnect();
        Log.d(TAG, "disconnect ");
    }

}
