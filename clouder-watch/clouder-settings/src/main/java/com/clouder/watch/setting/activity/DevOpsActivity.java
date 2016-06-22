package com.clouder.watch.setting.activity;

import android.content.Intent;
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
import com.clouder.watch.setting.utils.SystemPropertiesUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by yang_shoulai on 7/23/2015.
 */
public class DevOpsActivity extends SwipeRightActivity {

    private static final String TAG = "DevOpsActivity";

    public static final String KEY_POINTER_LOCATION = "pointer_location";

    /**
     * Controls overdraw debugging.
     * <p/>
     * Possible values:
     * "false", to disable overdraw debugging
     * "show", to show overdraw areas on screen
     * "count", to display an overdraw counter
     */
    public static final String DEBUG_OVERDRAW_PROPERTY = "debug.hwui.overdraw";

    /**
     * When set to true, apps will draw debugging information about their layouts.
     *
     * @hide
     */
    public static final String DEBUG_LAYOUT_PROPERTY = "debug.layout";

    /**
     * Show touch positions on screen?
     * 0 = no
     * 1 = yes
     *
     * @hide
     */
    public static final String SHOW_TOUCHES = "show_touches";

    /**
     * 强制使用GPU渲染
     */
    private static final String HARDWARE_UI_PROPERTY = "persist.sys.ui.hw";

    /**
     * bluetooth HCI snoop log configuration
     *
     * @hide
     */
    public static final String BLUETOOTH_HCI_LOG = "bluetooth_hci_log";

    /**
     * 蓝牙调试
     */
    public static final String EXTRA_BLUETOOTH_DEBUG_STATE = "com.clouder.watch.setting.extra_bluetooth_debug_state";
    public static final String ACTION_BLUETOOTH_DEBUG = "com.clouder.watch.setting.action.bluetooth_debug";
    public static final String CATEGORY_BLUETOOTH_DEBUG = "com.clouder.watch.setting.category.bluetooth_debug";

    private WearableListView gridView;
    private WearableListView.Adapter adapter;
    private LayoutInflater inflater;
    private TextView header;
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
        setContentView(R.layout.activity_dev_ops);
        inflater = LayoutInflater.from(this);
        initItems();
        this.gridView = (WearableListView) findViewById(R.id.list_view);
        header = (TextView) findViewById(R.id.header);
        adapter = new DevOpsListAdapter();
        this.gridView.setAdapter(adapter);
        this.gridView.scrollToPosition(1);
    }

    private void initItems() {
        //0 蓝牙窥探记录
        boolean btHciEnable = Settings.Secure.getInt(getContentResolver(), BLUETOOTH_HCI_LOG, 0) == 1;
        list.add(new Item(getString(R.string.activity_dev_ops_item_bt_rec), btHciEnable));
        //1 ADB调试
        boolean enableAdb = Settings.Global.getInt(getContentResolver(), Settings.Global.ADB_ENABLED, 0) == 1;
        list.add(new Item(getString(R.string.activity_dev_ops_item_adb_debug), enableAdb));
        //2 通过蓝牙调试
        boolean enableBtDebug = Settings.System.getInt(getContentResolver(), SettingsKey.BLUETOOTH_DEBUG_KEY, 0) == 1;
        list.add(new Item(getString(R.string.activity_dev_ops_item_bt_debug), enableBtDebug));
        //3 允许模仿位置
        boolean enableAllowMock = Settings.Secure.getInt(getContentResolver(), Settings.Secure.ALLOW_MOCK_LOCATION, 0) == 1;
        list.add(new Item(getString(R.string.activity_dev_ops_position_debug), enableAllowMock));
        //4 调试布局
        boolean layoutDebugEnable = !SystemPropertiesUtils.getString(DEBUG_LAYOUT_PROPERTY, "false").equals("false");
        list.add(new Item(getString(R.string.activity_dev_ops_layout_debug), layoutDebugEnable));
        //5 调试overdraw
        boolean overdrawEnable = !SystemPropertiesUtils.getString(DEBUG_OVERDRAW_PROPERTY, "false").equals("false");
        list.add(new Item(getString(R.string.activity_dev_ops_overdraw_debug), overdrawEnable));
        //6 调试GPU性能
        boolean hardwareUiEnable = !SystemPropertiesUtils.getString(HARDWARE_UI_PROPERTY, "false").equals("false");
        list.add(new Item(getString(R.string.activity_dev_ops_gpu_debug), hardwareUiEnable));
        //7 指针位置
        boolean pointerLocationEnable = Settings.System.getInt(getContentResolver(), KEY_POINTER_LOCATION, 0) == 1;
        list.add(new Item(getString(R.string.activity_dev_ops_pointer_position), pointerLocationEnable));
        //8 展示手势
        boolean showTouchesEnable = Settings.System.getInt(getContentResolver(), SHOW_TOUCHES, 0) == 1;
        list.add(new Item(getString(R.string.activity_dev_ops_gesture), showTouchesEnable));
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
    }


    private class DevOpsListAdapter extends WearableListView.Adapter {


        @Override
        public WearableListView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new WearableListView.ViewHolder(inflater.inflate(R.layout.item_activity_power_save_layout, null));
        }

        @Override
        public void onBindViewHolder(WearableListView.ViewHolder holder, int position) {
            TextView title = (TextView) holder.itemView.findViewById(R.id.item_title);
            final TextView btn = (TextView) holder.itemView.findViewById(R.id.item_btn);
            title.setText(list.get(position).title);
            if (list.get(position).enable) {
                btn.setBackgroundResource(R.drawable.btn_settings_on);
                btn.setText(R.string.state_on);
            } else {
                btn.setBackgroundResource(R.drawable.btn_settings_off);
                btn.setText(R.string.state_off);
            }
            btn.setOnClickListener(getOnclicListener(position));
        }

        @Override
        public int getItemCount() {
            return list.size();
        }
    }

    //蓝牙窥探记录
    private View.OnClickListener getOnclicListener(int position) {
        View.OnClickListener listener = null;
        if (position == 0) {
            //0 蓝牙窥探记录
            listener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    boolean enable = Settings.Secure.getInt(getContentResolver(), BLUETOOTH_HCI_LOG, 0) == 1;
                    Settings.Secure.putInt(getContentResolver(), BLUETOOTH_HCI_LOG, enable ? 0 : 1);
                    list.get(0).enable = !enable;
                    adapter.notifyDataSetChanged();
                }
            };
        } else if (position == 1) {
            //1 ADB调试
            listener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    boolean enableAdb = Settings.Global.getInt(getContentResolver(), Settings.Global.ADB_ENABLED, 0) == 1;
                    Settings.Global.putInt(getContentResolver(), Settings.Global.ADB_ENABLED, enableAdb ? 0 : 1);
                    list.get(1).enable = !enableAdb;
                    adapter.notifyDataSetChanged();
                }
            };

        } else if (position == 2) {
            //2 通过蓝牙调试
            listener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    boolean enableBtDebug = Settings.System.getInt(getContentResolver(), SettingsKey.BLUETOOTH_DEBUG_KEY, 0) == 1;
                    Settings.System.putInt(getContentResolver(), SettingsKey.BLUETOOTH_DEBUG_KEY, enableBtDebug ? 0 : 1);
                    sendBtDebugBroadcast(!enableBtDebug);
                    list.get(2).enable = !enableBtDebug;
                    adapter.notifyDataSetChanged();
                }
            };

        } else if (position == 3) {
            //3 允许模仿位置
            listener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    boolean enableMockLocation = Settings.Secure.getInt(getContentResolver(), Settings.Secure.ALLOW_MOCK_LOCATION, 0) == 1;
                    Settings.Secure.putInt(getContentResolver(), Settings.Secure.ALLOW_MOCK_LOCATION, enableMockLocation ? 0 : 1);
                    list.get(3).enable = !enableMockLocation;
                    adapter.notifyDataSetChanged();
                }
            };


        } else if (position == 4) {
            //4 调试布局
            listener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String value = SystemPropertiesUtils.getString(DEBUG_LAYOUT_PROPERTY, "false");
                    SystemPropertiesUtils.set(DEBUG_LAYOUT_PROPERTY, "false".equals(value) ? "true" : "false");
                    list.get(4).enable = "false".equals(value);
                    adapter.notifyDataSetChanged();
                }
            };


        } else if (position == 5) {
            //5 调试overdraw
            listener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String value = SystemPropertiesUtils.getString(DEBUG_OVERDRAW_PROPERTY, "false");
                    SystemPropertiesUtils.set(DEBUG_OVERDRAW_PROPERTY, "false".equals(value) ? "show" : "false");
                    list.get(5).enable = "false".equals(value);
                    adapter.notifyDataSetChanged();
                }
            };

        } else if (position == 6) {
            //6 调试GPU性能
            listener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String value = SystemPropertiesUtils.getString(HARDWARE_UI_PROPERTY, "false");
                    SystemPropertiesUtils.set(HARDWARE_UI_PROPERTY, "false".equals(value) ? "true" : "false");
                    list.get(6).enable = "false".equals(value);
                    adapter.notifyDataSetChanged();
                }
            };


        } else if (position == 7) {
            //7 指针位置
            listener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    boolean enable = Settings.System.getInt(getContentResolver(), KEY_POINTER_LOCATION, 0) == 1;
                    Settings.System.putInt(getContentResolver(), KEY_POINTER_LOCATION, enable ? 0 : 1);
                    list.get(7).enable = !enable;
                    adapter.notifyDataSetChanged();
                }
            };

        } else if (position == 8) {
            //8 展示手势
            listener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    boolean selected = Settings.System.getInt(getContentResolver(), SHOW_TOUCHES, 0) == 1;
                    Settings.System.putInt(getContentResolver(), SHOW_TOUCHES, selected ? 0 : 1);
                    list.get(8).enable = !selected;
                    adapter.notifyDataSetChanged();
                }
            };
        }
        return listener;
    }


    /**
     * 发送蓝牙调试开关的广播
     */
    private void sendBtDebugBroadcast(boolean state) {
        Intent intent = new Intent();
        intent.setAction(ACTION_BLUETOOTH_DEBUG);
        intent.addCategory(CATEGORY_BLUETOOTH_DEBUG);
        intent.putExtra(EXTRA_BLUETOOTH_DEBUG_STATE, state);
        sendBroadcast(intent);
    }


}
