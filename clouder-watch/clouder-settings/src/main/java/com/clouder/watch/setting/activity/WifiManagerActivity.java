package com.clouder.watch.setting.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.wearable.view.WearableListView;
import android.support.wearable.view.WearableListView.Adapter;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.clouder.watch.common.sync.IMessageListener;
import com.clouder.watch.common.sync.SyncMessage;
import com.clouder.watch.common.sync.SyncMessagePathConfig;
import com.clouder.watch.common.sync.SyncServiceHelper;
import com.clouder.watch.common.sync.message.WifiSyncMessage;
import com.clouder.watch.common.ui.SwipeRightActivity;
import com.clouder.watch.common.utils.SystemSettingsUtils;
import com.clouder.watch.common.utils.WifiHelper;
import com.clouder.watch.common.widget.WatchToast;
import com.clouder.watch.setting.R;

import java.util.Iterator;
import java.util.List;

/**
 * Created by yang_shoulai on 9/9/2015.
 */
public class WifiManagerActivity extends SwipeRightActivity {

    private static final String TAG = "WifiManagerActivity";

    private WifiHelper wifiHelper;

    private List<WifiConfiguration> configuredNetworks;

    private static final int VIEW_TYPE_HEAD = 1;

    private static final int VIEW_TYPE_ADD = 2;

    private static final int VIEW_TYPE_ITEM = 3;

    private WearableListView listView;

    private TextView airPlaneTip;

    private TextView header;

    private Adapter wearableListAdapter;

    private SyncServiceHelper syncServiceHelper = new SyncServiceHelper(this, new SyncServiceHelper.ISyncServiceCallback() {
        @Override
        public void onBindSuccess() {
            Log.d(TAG, "Bind Sync Service Success!");
            syncServiceHelper.setMessageListener(SyncMessagePathConfig.WIFI_INFO, new IMessageListener() {
                @Override
                public void onMessageReceived(String path, SyncMessage message) {
                    Log.d(TAG, "Receive message form sync service, message = " + message);
                    if (SyncMessagePathConfig.WIFI_INFO.equals(path)) {
                        WifiSyncMessage msg = (WifiSyncMessage) message;
                        onReceiveWifiSyncMessage(msg);
                    }
                }
            });
        }

        @Override
        public void onSendSuccess(SyncMessage syncMessage) {

        }

        @Override
        public void onSendFailed(SyncMessage syncMessage, int reason) {
            Log.w(TAG, "Send Message failed with message = " + syncMessage + ", reason = " + reason);
            String str;
            if (reason == SyncServiceHelper.STATUS_FAILED_REMOTE_EXCEPTION) {
                str = getString(R.string.send_msg_error);
            } else if (reason == SyncServiceHelper.STATUS_FAILED_SYNC_SERVICE_DISCONNECT) {
                str = getString(R.string.can_not_connect_sync_service);
            } else {
                str = getString(R.string.send_msg_failed_unknown_error);
            }
            WatchToast.make(WifiManagerActivity.this, str, Toast.LENGTH_SHORT).show();
        }
    });


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wifi_manager);
        wifiHelper = new WifiHelper(this);
        listView = (WearableListView) findViewById(R.id.list_view);
        header = (TextView) findViewById(R.id.header);
        airPlaneTip = (TextView) findViewById(R.id.tip_airplane);

        boolean airPlaneMode = SystemSettingsUtils.isAirPlaneOn(this);
        if (airPlaneMode) {
            airPlaneTip.setVisibility(View.VISIBLE);
            listView.setVisibility(View.GONE);
        } else {
            airPlaneTip.setVisibility(View.GONE);
            listView.setVisibility(View.VISIBLE);
        }
        wearableListAdapter = new WearableListView.Adapter() {
            @Override
            public WearableListView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                if (viewType == VIEW_TYPE_HEAD) {
                    return new WearableListView.ViewHolder(getLayoutInflater().inflate(R.layout.item_wifi_manage_head, null));
                } else if (viewType == VIEW_TYPE_ADD) {
                    return new WearableListView.ViewHolder(getLayoutInflater().inflate(R.layout.item_wifi_manage_add, null));
                } else if (viewType == VIEW_TYPE_ITEM) {
                    return new WearableListView.ViewHolder(getLayoutInflater().inflate(R.layout.item_activity_wifi_manager, null));
                }
                return null;
            }

            @Override
            public void onBindViewHolder(WearableListView.ViewHolder holder, int position) {
                if (position == 0) {
                    ProgressBar progressBar = (ProgressBar) holder.itemView.findViewById(R.id.progressBar);
                    TextView msg = (TextView) holder.itemView.findViewById(R.id.wifi_msg);
                    TextView btn = (TextView) holder.itemView.findViewById(R.id.settings_btn);
                    progressBar.setVisibility(View.GONE);
                    int wifiState = wifiHelper.getState();
                    if (wifiState == WifiManager.WIFI_STATE_ENABLED) {
                        Log.d(TAG, "wifi On");
                        boolean wifiConnected = false;
                        String connectedWifi = "";
                        if (configuredNetworks != null) {
                            Iterator<WifiConfiguration> it = configuredNetworks.iterator();
                            while (it.hasNext()) {
                                WifiConfiguration wc = it.next();
                                if (wc.status == WifiConfiguration.Status.CURRENT) {
                                    wifiConnected = true;
                                    connectedWifi = wc.SSID;
                                    break;
                                }
                            }
                        }

                        Log.d(TAG, "wifi connected ?  " + wifiConnected + ", connected wifi ssid = " + connectedWifi);
                        btn.setText(R.string.state_on); //以连接
                        btn.setBackgroundResource(R.drawable.btn_settings_on);
                        if (wifiConnected) {
                            msg.setText(getString(R.string.wifi_connected_with) + " " + connectedWifi);
                        } else {
                            msg.setText(R.string.wifi_disconnected);
                        }
                    } else if (wifiState == WifiManager.WIFI_STATE_DISABLED) {
                        Log.d(TAG, "wifi Off");
                        btn.setText(R.string.state_off);
                        btn.setBackgroundResource(R.drawable.btn_settings_off);
                        msg.setText("");
                    } else if (wifiState == WifiManager.WIFI_STATE_DISABLING) {
                        progressBar.setVisibility(View.VISIBLE);
                        msg.setText(R.string.turning_off);
                        btn.setText(R.string.state_off);
                        btn.setBackgroundResource(R.drawable.btn_settings_off);
                    } else if (wifiState == WifiManager.WIFI_STATE_ENABLING) {
                        progressBar.setVisibility(View.VISIBLE);
                        msg.setText(R.string.turning_on);
                        btn.setText(R.string.state_on);
                        btn.setBackgroundResource(R.drawable.btn_settings_on);
                    }
                    holder.itemView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (wifiHelper.getState() == WifiManager.WIFI_STATE_DISABLED) {
                                Log.d(TAG, "open wifi");
                                wifiHelper.open();
                            } else if (wifiHelper.getState() == WifiManager.WIFI_STATE_ENABLED) {
                                Log.d(TAG, "close wifi");
                                wifiHelper.close();
                            } else {
                                WatchToast.make(WifiManagerActivity.this, "Please Wait...", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });

                } else if (position == 1) {
                    holder.itemView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (!wifiHelper.isOn()) {
                                WatchToast.make(WifiManagerActivity.this, getString(R.string.opem_wifi_first), Toast.LENGTH_SHORT).show();
                            } else {
                                WifiSyncMessage wifiSyncMessage = new WifiSyncMessage();
                                wifiSyncMessage.setMethod(SyncMessage.Method.Get);
                                syncServiceHelper.send(wifiSyncMessage);
                            }
                        }
                    });
                } else {
                    TextView wifiName = (TextView) holder.itemView.findViewById(R.id.wifi_ssid);
                    TextView wifiState = (TextView) holder.itemView.findViewById(R.id.wifi_switcher);
                    final WifiConfiguration network = configuredNetworks.get(position - 2);
                    wifiName.setText(network.SSID);
                    WifiInfo wifiInfo = wifiHelper.getConnectionInfo();
                    if (wifiInfo == null || !wifiInfo.getSSID().equals(network.SSID)) {
                        wifiState.setText(getString(R.string.disconnect));
                        wifiState.setBackgroundResource(R.drawable.btn_settings_off);
                        holder.itemView.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                wifiHelper.connect(network.networkId);
                            }
                        });
                    } else {
                        wifiState.setText(getString(R.string.connect));
                        wifiState.setBackgroundResource(R.drawable.btn_settings_on);
                        holder.itemView.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                wifiHelper.disconnect(network.networkId);
                            }
                        });
                    }

                }
            }

            @Override
            public int getItemCount() {
                return configuredNetworks == null ? 2 : configuredNetworks.size() + 2;
            }

            @Override
            public int getItemViewType(int position) {
                if (position == 0) {
                    return VIEW_TYPE_HEAD;
                } else if (position == 1) {
                    return VIEW_TYPE_ADD;
                } else {
                    return VIEW_TYPE_ITEM;
                }
            }
        };
        listView.setAdapter(wearableListAdapter);
        listView.scrollToPosition(1);
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        registerReceiver(receiver, filter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        syncServiceHelper.bind();

    }

    @Override
    protected void onPause() {
        super.onPause();
        syncServiceHelper.unbind();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
    }


    private void onReceiveWifiSyncMessage(WifiSyncMessage msg) {
        Log.d(TAG, "onReceiveWifiSyncMessage msg = " + msg);
        if (msg != null) {
            String ssid = msg.getSsid();
            if (ssid != null) {
                ssid = ssid.replaceAll("\"", "");
            }
            String password = msg.getPassword();
            int type = msg.getType();
            Log.d(TAG, "wifiHelper.connect start");
            boolean success = wifiHelper.connect(ssid, password, WifiHelper.WifiCipherType.values()[type]);
            Log.d(TAG, "wifiHelper.connect return, result = " + success);
            String message;
            if (success) {
                message = getString(R.string.connect_new_wifi);
                Log.d(TAG, "wifiHelper.getConfiguredNetworks");
                configuredNetworks = wifiHelper.getConfiguredNetworks();
                wearableListAdapter.notifyDataSetChanged();
            } else {
                message = getString(R.string.can_not_connect_wifi);
            }
            WatchToast.make(this, message, Toast.LENGTH_SHORT).show();
        }

    }

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "onReceive action = " + action);
            if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(action)) {
                //Wifi状态改变，包括打开，正在打开，关闭，正在关闭
                configuredNetworks = wifiHelper.getConfiguredNetworks();
                wearableListAdapter.notifyDataSetChanged();
            } else if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action)) {
                //Wifi连接状态改变
                if (wearableListAdapter != null) {
                    configuredNetworks = wifiHelper.getConfiguredNetworks();
                    Log.d(TAG, "configuredNetworks size = " + (configuredNetworks == null ? 0 : configuredNetworks.size()));
                    wearableListAdapter.notifyDataSetChanged();
                }
                NetworkInfo networkInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                if (null == networkInfo) {
                    WatchToast.make(WifiManagerActivity.this, "WIFI disconnect", Toast.LENGTH_SHORT).show();
                }
            }
        }
    };

}
