package com.clouder.watch.setting.activity;

import android.os.Bundle;
import android.provider.Settings;
import android.support.wearable.view.WearableListView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.clouder.watch.common.ui.SwipeRightActivity;
import com.clouder.watch.common.utils.SettingsKey;
import com.clouder.watch.setting.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by yang_shoulai on 7/15/2015.
 */
public class PowerSaveActivity extends SwipeRightActivity {

    private LayoutInflater inflater;

    private WearableListView listView;

    private TextView header;

    private WearableListView.Adapter adapter;

    private List<Item> list = new ArrayList<>();


    private static class Item {
        public String title;
        public boolean enable;

        public Item(String title, boolean enable) {
            this.title = title;
            this.enable = enable;
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_power_save);
        inflater = LayoutInflater.from(this);
        listView = (WearableListView) findViewById(R.id.list_view);
        header = (TextView) findViewById(R.id.header);
        initItems();
        adapter = new WearableListView.Adapter() {
            @Override
            public WearableListView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                return new WearableListView.ViewHolder(inflater.inflate(R.layout.item_activity_power_save_layout, null));
            }

            @Override
            public void onBindViewHolder(WearableListView.ViewHolder holder, int position) {
                TextView title = (TextView) holder.itemView.findViewById(R.id.item_title);
                TextView btn = (TextView) holder.itemView.findViewById(R.id.item_btn);
                title.setText(list.get(position).title);
                if (list.get(position).enable) {
                    btn.setText(getString(R.string.power_save_wake_on_hands_up_on));
                    btn.setBackgroundResource(R.drawable.btn_settings_on);
                } else {
                    btn.setText(getString(R.string.power_save_wake_on_hands_up_off));
                    btn.setBackgroundResource(R.drawable.btn_settings_off);
                }
                if (position == 0) {
                    btn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            boolean enable = Settings.System.getInt(getContentResolver(), SettingsKey.AWAKEN_ON_HANDS_UP, 0) == 1;
                            Settings.System.putInt(getContentResolver(), SettingsKey.AWAKEN_ON_HANDS_UP, !enable ? 1 : 0);
                            list.get(0).enable = !enable;
                            adapter.notifyDataSetChanged();
                        }
                    });
                } else if (position == 1) {
                    btn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            boolean enable = Settings.System.getInt(getContentResolver(), SettingsKey.AWAKEN_ON_CHARGING, 0) == 1;
                            Settings.System.putInt(getContentResolver(), SettingsKey.AWAKEN_ON_CHARGING, !enable ? 1 : 0);
                            list.get(1).enable = !enable;
                            adapter.notifyDataSetChanged();
                        }
                    });
                } else if (position == 2) {
                    btn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            boolean enable = Settings.System.getInt(getContentResolver(), SettingsKey.AWAKEN_VOICE_SEARCH_ON_HOT_WORD, 0) == 1;
                            Settings.System.putInt(getContentResolver(), SettingsKey.AWAKEN_VOICE_SEARCH_ON_HOT_WORD, !enable ? 1 : 0);
                            list.get(2).enable = !enable;
                            adapter.notifyDataSetChanged();
                        }
                    });
                } else if (position == 3) {
                    btn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            boolean enable = Settings.System.getInt(getContentResolver(), SettingsKey.AWAKEN_VOICE_SEARCH_ON_GESTURE, 0) == 1;
                            Settings.System.putInt(getContentResolver(), SettingsKey.AWAKEN_VOICE_SEARCH_ON_GESTURE, !enable ? 1 : 0);
                            list.get(3).enable = !enable;
                            adapter.notifyDataSetChanged();
                        }
                    });
                } else {
                    btn.setOnClickListener(null);
                }

            }

            @Override
            public int getItemCount() {
                return list.size();
            }
        };
        listView.setAdapter(adapter);
        listView.scrollToPosition(1);
    }

    private void initItems() {
        //0 抬手唤醒屏幕开关
        list.add(new Item(getString(R.string.power_save_wake_on_hands_up),
                Settings.System.getInt(getContentResolver(), SettingsKey.AWAKEN_ON_HANDS_UP, 0) == 1));

        //1 充电时屏幕长亮开关
        list.add(new Item(getString(R.string.power_save_wake_on_charging),
                Settings.System.getInt(getContentResolver(), SettingsKey.AWAKEN_ON_CHARGING, 0) == 1));

        //2 热词唤醒语音搜索开关
        list.add(new Item(getString(R.string.power_save_hot_word_voice),
                Settings.System.getInt(getContentResolver(), SettingsKey.AWAKEN_VOICE_SEARCH_ON_HOT_WORD, 0) == 1));

        //3 手势唤醒语音搜索开关
        list.add(new Item(getString(R.string.power_save_gesture_voice),
                Settings.System.getInt(getContentResolver(), SettingsKey.AWAKEN_VOICE_SEARCH_ON_GESTURE, 0) == 1));
    }


}
