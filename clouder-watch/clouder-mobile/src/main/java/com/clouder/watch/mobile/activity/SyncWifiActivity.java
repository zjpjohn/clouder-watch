package com.clouder.watch.mobile.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.clouder.watch.common.sync.SyncMessage;
import com.clouder.watch.common.sync.message.WifiSyncMessage;
import com.clouder.watch.common.utils.WifiHelper;
import com.clouder.watch.mobile.R;
import com.clouder.watch.mobile.SyncActivity;
import com.clouder.watch.mobile.utils.ToastUtils;

import java.util.List;


/**
 * Created by yang_shoulai on 9/9/2015.
 */
public class SyncWifiActivity extends SyncActivity implements View.OnClickListener, AdapterView.OnItemClickListener {

    private String TAG = "SyncWifiActivity";

    private ListView listView;

    private BaseAdapter adapter;

    private RadioGroup radioGroup;

    private Button btnSend;

    private WifiHelper wifiHelper;

    private List<WifiConfiguration> wifiConfigurations;

    private EditText psdEditText;

    private String ssid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sync_wifi);
        listView = (ListView) findViewById(R.id.listView);
        radioGroup = (RadioGroup) findViewById(R.id.radioGroup);
        btnSend = (Button) findViewById(R.id.btnSend);
        psdEditText = (EditText) findViewById(R.id.password);

        btnSend.setOnClickListener(this);

        wifiHelper = new WifiHelper(this);

        adapter = new BaseAdapter() {
            @Override
            public int getCount() {
                return wifiConfigurations == null ? 0 : wifiConfigurations.size();
            }

            @Override
            public Object getItem(int position) {
                return wifiConfigurations.get(position);
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                ViewHolder holder;
                if (convertView == null) {
                    holder = new ViewHolder();
                    convertView = getLayoutInflater().inflate(android.R.layout.simple_list_item_2, null);
                    holder.ssid = (TextView) convertView.findViewById(android.R.id.text1);
                    holder.bssid = (TextView) convertView.findViewById(android.R.id.text2);
                    convertView.setTag(holder);
                } else {
                    holder = (ViewHolder) convertView.getTag();
                }
                WifiConfiguration configuration = wifiConfigurations.get(position);
                holder.ssid.setText(configuration.SSID);
                holder.bssid.setText(configuration.BSSID);
                if (ssid != null && ssid.equals(configuration.SSID)) {
                    convertView.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
                } else {
                    convertView.setBackgroundColor(getResources().getColor(android.R.color.background_light));
                }
                return convertView;
            }

            class ViewHolder {
                public TextView ssid;

                public TextView bssid;
            }
        };

        listView.setAdapter(adapter);
        listView.setOnItemClickListener(this);
        if (wifiHelper.isOn()) {
            wifiConfigurations = wifiHelper.getConfiguredNetworks();
            adapter.notifyDataSetChanged();
            onWifiConfigurationsChange();
        } else {
            wifiHelper.open();
        }

    }

    private void onWifiConfigurationsChange() {
        if (wifiConfigurations == null || wifiConfigurations.isEmpty()) {
            ToastUtils.show(SyncWifiActivity.this, "No Configured Wifi NetWorks Found!");
            this.radioGroup.setVisibility(View.GONE);
            this.btnSend.setVisibility(View.GONE);
            this.psdEditText.setVisibility(View.GONE);
        } else {
            this.radioGroup.setVisibility(View.VISIBLE);
            this.btnSend.setVisibility(View.VISIBLE);
            this.psdEditText.setVisibility(View.VISIBLE);
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        registerReceiver(receiver, filter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
    }

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(action)) {
                //Wifi状态改变，包括打开，正在打开，关闭，正在关闭
                if (WifiManager.WIFI_STATE_DISABLING == wifiHelper.getState()) {
                    ToastUtils.show(SyncWifiActivity.this, "正在关闭Wifi");
                } else if (WifiManager.WIFI_STATE_DISABLED == wifiHelper.getState()) {
                    wifiConfigurations = wifiHelper.getConfiguredNetworks();
                    adapter.notifyDataSetChanged();
                    onWifiConfigurationsChange();
                    ssid = null;
                } else if (WifiManager.WIFI_STATE_ENABLING == wifiHelper.getState()) {
                    //textView.setText("正在打开");
                    ToastUtils.show(SyncWifiActivity.this, "正在打开Wifi");

                } else if (WifiManager.WIFI_STATE_ENABLED == wifiHelper.getState()) {
                    wifiConfigurations = wifiHelper.getConfiguredNetworks();
                    adapter.notifyDataSetChanged();
                }
            }
        }
    };

    @Override
    public void onSendSuccess(SyncMessage syncMessage) {
        super.onSendSuccess(syncMessage);

    }

    @Override
    public void onSendFailed(SyncMessage syncMessage, int errorCode) {
        super.onSendFailed(syncMessage, errorCode);
        //ToastUtils.show(this, "发送消息失败！");
    }


    @Override
    public void onConnectSyncServiceFailed() {
        super.onConnectSyncServiceFailed();
        Log.w(TAG, "Connect with cms service failed!");
        ToastUtils.show(this, "连接CMS服务失败！");
    }

    @Override
    public void onConnectSyncServiceSuccess() {
        super.onConnectSyncServiceSuccess();
        Log.d(TAG, "Connect with cms service success!");
    }

    @Override
    public void onClick(View v) {
        if (R.id.btnSend == v.getId()) {

            if (ssid == null || ssid.trim().length() == 0) {
                ToastUtils.show(this, "请选择一个Wifi网络");
                return;
            }
            int checkedId = radioGroup.getCheckedRadioButtonId();

            if (checkedId == -1) {
                ToastUtils.show(this, "请选择密码加密方式");
                return;
            }

            String password = psdEditText.getText().toString().trim();
            if (password.length() == 0 && checkedId != R.id.type_no_pass) {
                ToastUtils.show(this, "请填写密码");
                return;
            }

            WifiSyncMessage syncMessage = new WifiSyncMessage();
            syncMessage.setMethod(SyncMessage.Method.Set);
            syncMessage.setSsid(ssid);
            syncMessage.setPassword(password);
            int type = -1;
            if (checkedId == R.id.type_wep) {
                type = 0;
            } else if (checkedId == R.id.type_wpa) {
                type = 1;
            } else if (checkedId == R.id.type_no_pass) {
                type = 2;
            }
            syncMessage.setType(type);
            sendMessage(syncMessage);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (ssid == null) {
            ssid = wifiConfigurations.get(position).SSID;
        } else {
            if (!ssid.equals(wifiConfigurations.get(position).SSID)) {
                ssid = wifiConfigurations.get(position).SSID;
            } else {
                ssid = null;
            }
        }
        adapter.notifyDataSetChanged();
    }
}
