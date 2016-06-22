package com.clouder.watch.mobile.activity;

import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.clouder.watch.common.widget.SwitchButton;
import com.clouder.watch.mobile.BaseActivity;
import com.clouder.watch.mobile.ClouderApplication;
import com.clouder.watch.mobile.R;

import java.util.Iterator;
import java.util.List;

/**
 * 通知管理设置黑名单页面
 * Created by yang_shoulai on 8/6/2015.
 */

public class NotificationBlackListActivity extends BaseActivity {

    private static final String TAG = "BlackListActivity";

    private ListView listView;

    private LayoutInflater inflater;

    private List<String> blackList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_black_list);
        inflater = LayoutInflater.from(this);
        listView = (ListView) findViewById(R.id.listView);
        final List<ResolveInfo> apps = loadAllApps();
        blackList = loadBlackList();
        listView.setAdapter(new BaseAdapter() {
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
                ViewHolder viewHolder;
                if (convertView == null) {
                    viewHolder = new ViewHolder();
                    convertView = inflater.inflate(R.layout.item_notification_black_list, null);
                    viewHolder.icon = (ImageView) convertView.findViewById(R.id.icon);
                    viewHolder.tvName = (TextView) convertView.findViewById(R.id.name);
                    viewHolder.switcher = (SwitchButton) convertView.findViewById(R.id.switcher);
                    convertView.setTag(viewHolder);
                } else {
                    viewHolder = (ViewHolder) convertView.getTag();
                }
                final ResolveInfo info = apps.get(position);
                viewHolder.tvName.setText(info.loadLabel(getPackageManager()));
                viewHolder.icon.setImageDrawable(info.loadIcon(getPackageManager()));
                viewHolder.switcher.setOnStatusChangeListener(null);
                viewHolder.switcher.setChecked(isBlack(info.activityInfo.packageName));
                viewHolder.switcher.setOnStatusChangeListener(new SwitchButton.OnStatusChangeListener() {
                    @Override
                    public void onChange(SwitchButton button, boolean checked) {
                        String pkg = info.activityInfo.packageName;
                        if (checked) {
                            if (!blackList.contains(pkg)) {
                                blackList.add(pkg);
                            }
                            addBlackList(info.activityInfo.packageName);
                        } else {
                            if (blackList.contains(pkg)) {
                                blackList.remove(pkg);
                            }
                            deleteBlackList(info.activityInfo.packageName);
                        }
                    }
                });
                return convertView;
            }

            class ViewHolder {
                public ImageView icon;
                public TextView tvName;
                public SwitchButton switcher;
            }
        });
    }

    /**
     * 取得所有安装的应用
     *
     * @return
     */
    private List<ResolveInfo> loadAllApps() {
        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> list = getPackageManager().queryIntentActivities(mainIntent, 0);
        Iterator<ResolveInfo> iterator = list.iterator();
        while (iterator.hasNext()) {
            ResolveInfo info = iterator.next();
            if (info.activityInfo.packageName.equals(this.getPackageName())) {
                iterator.remove();
                break;
            }
        }
        return list;
    }

    /**
     * 取得所有黑名单应用
     *
     * @return
     */
    private List<String> loadBlackList() {
        return ClouderApplication.getInstance().getNotificationBlackList();
    }

    /**
     * 判断应用是否处于黑名单中
     *
     * @param packageName
     * @return
     */
    private boolean isBlack(String packageName) {
        return blackList.contains(packageName);
    }


    private void addBlackList(String packageName) {
        ClouderApplication.getInstance().addNotificationBlackItem(packageName);

    }

    private void deleteBlackList(String packageName) {
        ClouderApplication.getInstance().removeNotificationBlackItem(packageName);
    }

}
