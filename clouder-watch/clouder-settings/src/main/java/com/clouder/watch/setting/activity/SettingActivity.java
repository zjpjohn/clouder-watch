package com.clouder.watch.setting.activity;

import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.ContentObserver;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.wearable.view.WearableListView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.clouder.watch.common.ui.SwipeRightActivity;
import com.clouder.watch.common.utils.SettingsKey;
import com.clouder.watch.common.utils.SystemSettingsUtils;
import com.clouder.watch.common.widget.WatchDialog;
import com.clouder.watch.common.widget.WatchToast;
import com.clouder.watch.setting.R;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by yang_shoulai on 7/15/2015.
 */
public class SettingActivity extends SwipeRightActivity {

    private static final String TAG = "SettingActivity";

    private LayoutInflater inflater;

    private WearableListView listView;

    private WearableListView.Adapter adapter;

    private Handler mHandler = new Handler();

    private ProgressDialog resetProgressDialog = null;

    private List<String> list = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        getResolution();
        list.add(getString(R.string.settings_item_brightness_adjust_title));                    // 0  亮度调整
        list.add(getString(R.string.settings_item_wake_title));                    // 1  表盘长亮
        list.add(getString(R.string.settings_item_power_title));                    // 2  省电选项
        list.add(getString(R.string.settings_item_bluetooth_adjust_title));                    // 3  蓝牙连接
        list.add(getString(R.string.settings_item_watch_face_title));                    // 4  更换表盘
        list.add(getString(R.string.settings_item_shutdown_title));                        // 5  关机
        list.add(getString(R.string.settings_item_restart_title));                        // 6  重启
        list.add(getString(R.string.settings_item_reset_title));                    // 7  重置
        list.add(getString(R.string.wifi_settings));                   // 8  wifi
        list.add(getString(R.string.settings_item_screen_locker_title));                    // 9  屏幕锁
        list.add(getString(R.string.hot_word_search));                                  // 10 热词搜索
        list.add(getString(R.string.system_update));                                    // 11 系统升级

        list.add(getString(R.string.settings_item_about_title));                        // 12 关于
        list.add(getString(R.string.settings_item_dev_ops_title));                  // 13 开发者选项
        this.inflater = LayoutInflater.from(this);
        listView = (WearableListView) findViewById(R.id.list_view);
        adapter = new SettingsAdapter();
        listView.setAdapter(adapter);
        listView.scrollToPosition(1);
        //注册系统亮度监听,包括亮度模式和亮度值
        getContentResolver().registerContentObserver(Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS), true, brightnessObserver);
        getContentResolver().registerContentObserver(Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS_MODE), true, brightnessObserver);

    }

    private class SettingsAdapter extends WearableListView.Adapter {

        @Override
        public WearableListView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

            return new WearableListView.ViewHolder(inflater.inflate(R.layout.item_activity_setting_layout, null));
        }

        @Override
        public void onBindViewHolder(WearableListView.ViewHolder holder, int position) {
            TextView title = (TextView) holder.itemView.findViewById(R.id.settings_title);
            final TextView btn = (TextView) holder.itemView.findViewById(R.id.settings_btn);
            title.setText(list.get(position));
            if (position == 0) {
                //亮度调节
                holder.itemView.setOnClickListener(null);
                btn.setVisibility(View.VISIBLE);
                btn.setBackgroundResource(R.drawable.btn_settings_on);
                if (SystemSettingsUtils.isAutomaticBrightness(SettingActivity.this)) {
                    btn.setText(getString(R.string.settings_item_brightness_adjust_state_auto));
                } else {
                    int level = SystemSettingsUtils.judgeBrightnessLevel(SettingActivity.this, SystemSettingsUtils.getLightness(SettingActivity.this));
                    btn.setText(String.valueOf(level));
                }
                btn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(SettingActivity.this, BrightnessActivity.class);
                        startActivity(intent);
                    }
                });
            } else if (position == 1) {
                //表盘长亮
                btn.setVisibility(View.VISIBLE);

                holder.itemView.setOnClickListener(null);
                if (SystemSettingsUtils.isAlwaysWakeOn(SettingActivity.this)) {
                    btn.setText(R.string.state_on);
                    btn.setBackgroundResource(R.drawable.btn_settings_on);
                } else {
                    btn.setText(R.string.state_off);
                    btn.setBackgroundResource(R.drawable.btn_settings_off);
                }
                btn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (SystemSettingsUtils.isAlwaysWakeOn(SettingActivity.this)) {
                            btn.setText(R.string.state_off);
                            btn.setBackgroundResource(R.drawable.btn_settings_off);
                            SystemSettingsUtils.setScreenAlwaysWakeOn(SettingActivity.this, false);
                            Settings.System.putInt(getContentResolver(), SettingsKey.WAKE_ON, 0);
                        } else {
                            btn.setText(R.string.state_on);
                            btn.setBackgroundResource(R.drawable.btn_settings_on);
                            SystemSettingsUtils.setScreenAlwaysWakeOn(SettingActivity.this, true);
                            Settings.System.putInt(getContentResolver(), SettingsKey.WAKE_ON, 1);
                        }
                    }
                });
            } else {
                btn.setVisibility(View.INVISIBLE);
                btn.setOnClickListener(null);
                if (position == 2) {
                    //省电选项
                    holder.itemView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            startActivity(new Intent(SettingActivity.this, PowerSaveActivity.class));
                        }
                    });

                } else if (position == 3) {
                    //蓝牙连接
                    holder.itemView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            startActivity(new Intent(SettingActivity.this, BluetoothPairActivity.class));
                        }
                    });
                } else if (position == 4) {
                    //更换表盘
                    holder.itemView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            startActivity(new Intent(SettingActivity.this, WatchFaceActivity.class));
                        }
                    });
                } else if (position == 5) {
                    //关机
                    holder.itemView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            WatchDialog dialog = new WatchDialog(SettingActivity.this, getString(R.string.settings_item_shutdown_dialog_title),
                                    getString(R.string.settings_item_shutdown_dialog_message));
                            dialog.setCallbackListener(new WatchDialog.ICallbackListener() {
                                @Override
                                public void onNegativeClick(WatchDialog dialog) {
                                    dialog.dismiss();
                                }

                                @Override
                                public void onPositiveClick(WatchDialog dialog) {
                                    dialog.dismiss();
                                    SystemSettingsUtils.shutdown();
                                }
                            });
                            dialog.show();
                        }
                    });

                } else if (position == 6) {
                    //重启
                    holder.itemView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            WatchDialog dialog = new WatchDialog(SettingActivity.this, getString(R.string.settings_item_restart_dialog_title),
                                    getString(R.string.settings_item_restart_dialog_message));
                            dialog.setCallbackListener(new WatchDialog.ICallbackListener() {
                                @Override
                                public void onNegativeClick(WatchDialog dialog) {
                                    dialog.dismiss();
                                }

                                @Override
                                public void onPositiveClick(WatchDialog dialog) {
                                    dialog.dismiss();
                                    SystemSettingsUtils.reboot(SettingActivity.this, "User want to restart this device.");
                                }
                            });
                            dialog.show();
                        }
                    });

                } else if (position == 7) {
                    //重置
                    holder.itemView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            WatchDialog dialog = new WatchDialog(SettingActivity.this, getString(R.string.settings_item_reset_dialog_title),
                                    getString(R.string.settings_item_reset_dialog_message));
                            dialog.setCallbackListener(new WatchDialog.ICallbackListener() {
                                @Override
                                public void onNegativeClick(WatchDialog dialog) {
                                    dialog.dismiss();
                                }

                                @Override
                                public void onPositiveClick(WatchDialog dialog) {
                                    dialog.dismiss();
                                    if (resetProgressDialog == null) {
                                        resetProgressDialog = new ProgressDialog(SettingActivity.this);
                                        resetProgressDialog.setIndeterminate(true);
                                        resetProgressDialog.setCancelable(false);
                                        resetProgressDialog.setTitle(getString(R.string.setting_reset_device_progress_dialog_title));
                                        resetProgressDialog.setMessage(getString(R.string.setting_reset_device_progress_dialog_message));
                                    }
                                    removeDataInSystemSettings();
                                    try {
                                        //TODO 反射实现调用系统重置的API，未测试，在源码下编译时需要改写，参考com.android.settings.MasterClearConfirm
                                        Method getService = SettingActivity.this.getClass().getMethod("getSystemService", String.class);
                                        final Object pdbManager = getService.invoke(SettingActivity.this, "persistent_data_block");
                                        if (pdbManager != null) {
                                            Method getOemUnlockEnabled = pdbManager.getClass().getMethod("getOemUnlockEnabled");
                                            Boolean enable = (Boolean) getOemUnlockEnabled.invoke(pdbManager);
                                            Log.d(TAG, "getOemUnlockEnabled  = " + enable);
                                            if (!enable) {
                                                resetProgressDialog.show();
                                                new AsyncTask<Void, Void, Void>() {
                                                    @Override
                                                    protected Void doInBackground(Void... params) {
                                                        try {
                                                            Method wipe = pdbManager.getClass().getMethod("wipe");
                                                            wipe.invoke(pdbManager);
                                                        } catch (IllegalAccessException e) {
                                                            Log.e(TAG, "IllegalAccessException", e);
                                                        } catch (InvocationTargetException e) {
                                                            Log.e(TAG, "InvocationTargetException", e);
                                                        } catch (NoSuchMethodException e) {
                                                            Log.e(TAG, "NoSuchMethodException", e);
                                                        }
                                                        return null;
                                                    }

                                                    @Override
                                                    protected void onPostExecute(Void aVoid) {
                                                        resetProgressDialog.hide();
                                                        doMasterClear();
                                                    }
                                                }.execute();
                                            } else {
                                                doMasterClear();
                                            }
                                        } else {
                                            doMasterClear();
                                        }
                                    } catch (NoSuchMethodException e) {
                                        Log.e(TAG, "NoSuchMethodException", e);
                                    } catch (InvocationTargetException e) {
                                        Log.e(TAG, "InvocationTargetException", e);
                                    } catch (IllegalAccessException e) {
                                        Log.e(TAG, "IllegalAccessException", e);
                                    }
                                }
                            });
                            dialog.show();
                        }
                    });
                } else if (position == 8) {
                    //WI-FI
                    holder.itemView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            startActivity(new Intent(SettingActivity.this, WifiManagerActivity.class));

                        }
                    });
                } else if (position == 9) {
                    //屏幕锁
                    holder.itemView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            startActivity(new Intent(SettingActivity.this, ScreenLockerActivity.class));
                        }
                    });
                } else if (position == 12) {
                    //关于
                    holder.itemView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            startActivity(new Intent(SettingActivity.this, AboutActivity.class));
                        }
                    });
                } else if (position == 13) {
                    //开发者选项
                    holder.itemView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            startActivity(new Intent(SettingActivity.this, DevOpsActivity.class));
                        }
                    });
                } else if (position == 10) {
                    //热词搜索


                } else if (position == 11) {
                    //系统更新
                    holder.itemView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            try {
                                Intent intent = new Intent();
                                ComponentName componentName = new ComponentName("com.hoperun.watch", "com.hoperun.watch.activity.MainActivity");
                                intent.setComponent(componentName);
                                startActivity(intent);
                            } catch (Exception e) {
                                WatchToast.make(SettingActivity.this, getString(R.string.can_not_open_update_page), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
            }
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public int getItemCount() {
            return list.size();
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        /*取消系统亮度值监听*/
        getContentResolver().unregisterContentObserver(brightnessObserver);
    }


    /**
     * 监听系统亮度的值是否发生改变
     */
    private ContentObserver brightnessObserver = new ContentObserver(mHandler) {
        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }
        }
    };


    private void doMasterClear() {
        Log.d(TAG, "doMasterClear");
        /*Intent intent = new Intent("com.android.internal.os.storage.FORMAT_AND_FACTORY_RESET");
        intent.putExtra("android.intent.extra.REASON", "MasterClearConfirm");
        intent.setComponent(new ComponentName("android", "com.android.internal.os.storage.ExternalStorageFormatter"));
        startService(intent);*/

        Intent intent = new Intent("android.intent.action.MASTER_CLEAR");
        intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        intent.putExtra("android.intent.extra.REASON", "MasterClearConfirm");
        sendBroadcast(intent);
    }

    /**
     * 获取屏幕分辨率
     */
    private void getResolution() {
        Display display = getWindowManager().getDefaultDisplay();
        DisplayMetrics displayMetrics = new DisplayMetrics();
        display.getMetrics(displayMetrics);
        float density = displayMetrics.density; //得到密度
        float width = displayMetrics.widthPixels;//得到宽度
        float height = displayMetrics.heightPixels;//得到高度
        Log.d(TAG, "屏幕密度：" + density + ", 屏幕高度：" + height + ", 屏幕宽度" + width);
    }


    /**
     * 清除设置信息
     */
    private void removeDataInSystemSettings() {
        ContentResolver resolver = getContentResolver();
        Settings.System.putInt(resolver, SettingsKey.DEVICE_PAIRED, 0);
        Settings.System.putString(resolver, SettingsKey.DEVICE_PAIRED_BT_ADDRESS, "");
        Settings.System.putString(resolver, SettingsKey.DEVICE_PAIRED_BT_NAME, "");
        Settings.System.putString(resolver, SettingsKey.WATCH_FACE_PKG, "");
        Settings.System.putString(resolver, SettingsKey.WATCH_FACE_SERVICE_NAME, "");
        Settings.System.putInt(resolver, SettingsKey.BRIGHTNESS_LEVEL, -1);
        Settings.System.putInt(resolver, SettingsKey.WAKE_ON, 0);
        Settings.System.putInt(resolver, SettingsKey.AWAKEN_ON_HANDS_UP, 0);
        Settings.System.putInt(resolver, SettingsKey.AWAKEN_ON_CHARGING, 0);
        Settings.System.putInt(resolver, SettingsKey.AWAKEN_VOICE_SEARCH_ON_HOT_WORD, 0);
        Settings.System.putInt(resolver, SettingsKey.AWAKEN_VOICE_SEARCH_ON_GESTURE, 0);
        Settings.System.putInt(resolver, SettingsKey.SCREEN_LOCKER_ENABLE, 0);
        Settings.System.putString(resolver, SettingsKey.SCREEN_LOCKER_PWD, "");
        Settings.System.putInt(resolver, SettingsKey.NOTIFICATION_PUSH_ENABLE, 0);
        Settings.System.putInt(resolver, SettingsKey.NOTIFICATION_SHOCK_ENABLE, 0);
        Settings.System.putString(resolver, SettingsKey.NOTIFICATION_BLACK_LIST, "");
        Settings.System.putInt(resolver, SettingsKey.BLUETOOTH_DEBUG_KEY, 0);
        Settings.System.putString(resolver, SettingsKey.PAIRED_DEVICE_PHONENUMBER, "");
    }
}
