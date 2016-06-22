package com.clouder.watch.setting.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.wearable.view.WearableListView;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.clouder.watch.common.ui.SwipeRightActivity;
import com.clouder.watch.common.widget.WatchToast;
import com.clouder.watch.setting.R;
import com.clouder.watch.setting.SettingsApplication;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by yang_shoulai on 7/17/2015.
 */
public class AboutActivity extends SwipeRightActivity {

    private static final String TAG = "AboutActivity";

    private WearableListView listView;

    private TextView header;

    private WearableListView.Adapter adapter;

    private List<Item> list = new ArrayList<>();


    private static final int VIEW_TYPE_1 = 1;

    private static final int VIEW_TYPE_2 = 2;

    private static class Item {

        public String title;

        public String value;

        public Item(String title, String value) {
            this.title = title;
            this.value = value;
        }

    }

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_BATTERY_CHANGED.equals(action)) {
                int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                int curPower = (int) (level * 100 / (float) scale);
                if (adapter != null && list.get(4) != null) {
                    list.get(4).value = curPower + "%";
                    adapter.notifyDataSetChanged();
                }
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        initItems();
        listView = (WearableListView) findViewById(R.id.list_view);
        header = (TextView) findViewById(R.id.header);
        adapter = new AboutAdapter();
        listView.setAdapter(adapter);
        listView.scrollToPosition(1);
        Log.d(TAG, "add battery change broadcast receiver!");
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(receiver, filter);
    }

    private void initItems() {
        //0 设备型号
        list.add(new Item(getString(R.string.activity_about_watch_type), "Clouder-Watch"));
        //1 设备名称
        list.add(new Item(getString(R.string.activity_about_watch_name), "Clouder-Watch"));
        //2 安卓版本
        list.add(new Item(getString(R.string.activity_about_android_version), Build.VERSION.RELEASE));
        //3 设备版本
        list.add(new Item(getString(R.string.activity_about_watch_version), SettingsApplication.getInstance().getAppVersionName()));
        //4 电池
        list.add(new Item(getString(R.string.activity_about_watch_battery), "20%"));
        //5 设备序列号
        list.add(new Item(getString(R.string.activity_about_watch_number), Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID)));
//        // 监管信息
//        list.add(new Item(getString(R.string.activity_about_jg_info), ""));
        //6 法律声明
        list.add(new Item(getString(R.string.activity_about_law_info), ""));
    }


    private class AboutAdapter extends WearableListView.Adapter {

        @Override
        public WearableListView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (viewType == VIEW_TYPE_1) {
                return new WearableListView.ViewHolder(getLayoutInflater().inflate(R.layout.item_about_activity, null));
            } else {
                return new WearableListView.ViewHolder(getLayoutInflater().inflate(R.layout.item_about_activity_2, null));
            }
        }

        @Override
        public void onBindViewHolder(WearableListView.ViewHolder holder, final int position) {
            if (position == 6) {
                TextView tvTitle = (TextView) holder.itemView.findViewById(R.id.tv_title);
                tvTitle.setText(list.get(position).title);
                holder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        WatchToast.make(AboutActivity.this, getString(R.string.get_law_info), Toast.LENGTH_SHORT).show();
                    }
                });

            } else {
                TextView tvTitle = (TextView) holder.itemView.findViewById(R.id.tv_title);
                TextView tvValue = (TextView) holder.itemView.findViewById(R.id.tv_value);
                tvTitle.setText(list.get(position).title);
                tvValue.setText(list.get(position).value);
            }

        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        @Override
        public int getItemViewType(int position) {
            if (position == 6) {
                return VIEW_TYPE_2;
            } else {
                return VIEW_TYPE_1;
            }
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
    }


}
