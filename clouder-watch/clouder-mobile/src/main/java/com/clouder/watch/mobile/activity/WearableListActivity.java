package com.clouder.watch.mobile.activity;

import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.clouder.watch.mobile.BaseActivity;
import com.clouder.watch.mobile.R;
import com.clouder.watch.mobile.sync.app.WearableAppParser;
import com.clouder.watch.mobile.utils.StringUtils;
import com.clouder.watch.mobile.utils.ToastUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * Created by yang_shoulai on 9/2/2015.
 */
public class WearableListActivity extends BaseActivity implements View.OnClickListener, OnItemClickListener {

    private static final String TAG = "WearableListActivity";

    public static final String SDCARD_PATH = Environment.getExternalStorageDirectory().getPath() +
            File.separator + "ClouderWatch" + File.separator + "SyncApp";

    private ListView wearableListView;

    private Button btnSearch;

    private BaseAdapter adapter;

    private List<WearableAppParser.WearableApp> apps;

    private WearableAppParser parser;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 0) {
                ToastUtils.show(WearableListActivity.this, "Load Success!");
            } else {
                ToastUtils.show(WearableListActivity.this, "Load Failesl!");
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wearable_app_list);
        parser = new WearableAppParser(this);
        wearableListView = (ListView) findViewById(R.id.wearableListView);
        btnSearch = (Button) findViewById(R.id.btnSearch);
        adapter = new BaseAdapter() {
            @Override
            public int getCount() {
                return apps == null ? 0 : apps.size();
            }

            @Override
            public Object getItem(int position) {
                return apps.get(position);
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
                    convertView = getLayoutInflater().inflate(R.layout.item_wearable_app_list, null);
                    holder.mobilePkg = (TextView) convertView.findViewById(R.id.mobilePkg);
                    holder.wearPkg = (TextView) convertView.findViewById(R.id.wearPkg);
                    holder.wearName = (TextView) convertView.findViewById(R.id.wearName);
                    holder.versionCode = (TextView) convertView.findViewById(R.id.versionCode);
                    convertView.setTag(holder);
                } else {
                    holder = (ViewHolder) convertView.getTag();
                }
                WearableAppParser.WearableApp wearableApp = apps.get(position);
                holder.mobilePkg.setText(wearableApp.mobileAppPackage);
                holder.wearPkg.setText(wearableApp.wearablePackage);
                holder.wearName.setText(StringUtils.isEmpty(wearableApp.wearableAppName) ? "" : wearableApp.wearableAppName + ".apk");
                holder.versionCode.setText(wearableApp.versionCode);
                return convertView;
            }

            class ViewHolder {
                TextView mobilePkg;
                TextView wearPkg;
                TextView wearName;
                TextView versionCode;
            }
        };
        wearableListView.setAdapter(adapter);
        wearableListView.setOnItemClickListener(this);
        btnSearch.setOnClickListener(this);
    }


    @Override
    public void onClick(View v) {
        if (R.id.btnSearch == v.getId()) {
            apps = parser.parse();
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        WearableAppParser.WearableApp app = apps.get(position);
        byte[] bytes = parser.loadWearableApk(app.mobileAppPackage, app.wearableAppName);
        new ApkSaver(SDCARD_PATH, app.wearableAppName + ".apk", bytes, mHandler).start();
    }

    public static class ApkSaver extends Thread {
        private String folderPath;

        private String name;

        private byte[] data;

        private Handler uiHandler;

        public ApkSaver(String folderPath, String name, byte[] data, Handler uiHandler) {
            this.folderPath = folderPath;
            this.name = name;
            this.data = data;
            this.uiHandler = uiHandler;
        }

        @Override
        public void run() {
            super.run();
            if (data == null) {
                Log.e(TAG, "apk byte[] 不存在！");
                uiHandler.sendEmptyMessage(1);
                return;
            }
            if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
                Log.e(TAG, "SD卡不可用！");
                uiHandler.sendEmptyMessage(1);
                return;
            }
            File folder = new File(folderPath);
            if (!folder.exists()) {
                if (!folder.mkdirs()) {
                    Log.w(TAG, "can not make dirs");
                }
            }
            File file = new File(folderPath, name);
            if (file.exists()) {
                if (!file.delete()) {
                    Log.w(TAG, "can not delete file " + file);
                }

            }
            FileOutputStream fos = null;
            try {
                Log.d(TAG, "Save Path : " + file.getAbsolutePath());
                if (!file.createNewFile()) {
                    Log.w(TAG, "can not create file " + file);
                }
                fos = new FileOutputStream(file);
                fos.write(data);
                uiHandler.sendEmptyMessage(0);
            } catch (IOException e) {
                e.printStackTrace();
                uiHandler.sendEmptyMessage(1);
            } finally {
                if (fos != null) {
                    try {
                        fos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
