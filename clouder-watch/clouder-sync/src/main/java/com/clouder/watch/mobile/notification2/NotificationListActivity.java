package com.clouder.watch.mobile.notification2;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.clouder.watch.mobile.R;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by yang_shoulai on 12/8/2015.
 */
public class NotificationListActivity extends Activity {

    private static final String TAG = "NotificationList";

    private ListView listView;

    private View emptyView;
    private ServiceConnection serviceConnection;

    private List<Notification> notificationList;

    private NotificationListHandler handler = new NotificationListHandler();

    private Messenger serviceMessenger;

    private Messenger notificationListMessenger = new Messenger(handler);

    private BaseAdapter adapter;

    private int downY, moveY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_notification);
        serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                serviceMessenger = new Messenger(service);
                registerListener();
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {

            }
        };
        Intent notificationListenerService = new Intent(this, NotificationListenerService.class);
        bindService(notificationListenerService, serviceConnection, Context.BIND_AUTO_CREATE);
        listView = (ListView) findViewById(R.id.listView);
        emptyView = findViewById(R.id.empty);
        adapter = new BaseAdapter() {
            @Override
            public int getCount() {
                return notificationList == null ? 0 : notificationList.size();
            }

            @Override
            public Object getItem(int position) {
                return notificationList.get(position);
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                ViewHolder holder;
                Notification notification = notificationList.get(position);
                if (convertView == null) {
                    holder = new ViewHolder();
                    convertView = getLayoutInflater().inflate(R.layout.item_notification_list, null);
                    holder.icon = (ImageView) convertView.findViewById(R.id.icon);
                    holder.title = (TextView) convertView.findViewById(R.id.title);
                    holder.content = (TextView) convertView.findViewById(R.id.content);
                    holder.date = (TextView) convertView.findViewById(R.id.timeTop);
                    convertView.setTag(holder);
                } else {
                    holder = (ViewHolder) convertView.getTag();
                }
                if (notification.getIcon() != null) {
                    holder.icon.setImageBitmap(notification.getIcon());
                }
                holder.title.setText(notification.getTitle());
                holder.content.setText(notification.getContent());
                holder.date.setText(notification.getDate());
                return convertView;
            }

            class ViewHolder {
                public ImageView icon;

                public TextView title;

                public TextView content;

                public TextView date;

            }
        };
        this.listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Notification notification = notificationList.get(position);
                Intent detail = new Intent(NotificationListActivity.this, NotificationDetailActivity.class);
                detail.putExtra(NotificationListenerService.EXTRA_NOTIFICATION, notification);
                startActivity(detail);
            }
        });

        SwipeDismissListViewTouchListener touchListener = new SwipeDismissListViewTouchListener(listView, new SwipeDismissListViewTouchListener.DismissCallbacks() {
            @Override
            public void setFlag(int flag) {
                if (flag == 0) {
                    finish();
                    overridePendingTransition(0,R.anim.activity_close);
                }
            }

            @Override
            public boolean canDismiss(int position) {
                return true;
            }

            @Override
            public void onDismiss(ListView listView, int[] reverseSortedPositions) {
                removeByPositions(reverseSortedPositions);
                adapter.notifyDataSetChanged();
            }
        });
        listView.setOnTouchListener(touchListener);
        listView.setOnScrollListener(touchListener.makeScrollListener());
        listView.setEmptyView(emptyView);
        swipeToFinishListener();
    }

    private void removeByPositions(int[] positions) {
        if (positions != null && positions.length > 0) {
            List<String> uuids = new ArrayList<>();
            for (int position : positions) {
                if (position >= 0 && position < notificationList.size()) {
                    uuids.add(notificationList.get(position).getUuid());
                }
            }
            if (!uuids.isEmpty()) {
                Iterator<Notification> iterator = notificationList.iterator();
                while (iterator.hasNext()) {
                    Notification notification = iterator.next();
                    for (String uuid : uuids) {
                        if (notification.getUuid().equals(uuid)) {
                            iterator.remove();
                            deleteNotification(uuid);
                            break;
                        }
                    }
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unRegisterListener();
        if (serviceConnection != null) {
            unbindService(serviceConnection);
        }
    }

    private void registerListener() {
        if (serviceMessenger != null) {
            Message msg = Message.obtain();
            msg.what = NotificationListenerService.MSG_WHAT_REGISTER_LISTENER;
            msg.replyTo = notificationListMessenger;
            try {
                serviceMessenger.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    private void unRegisterListener() {
        if (serviceMessenger != null) {
            Message msg = Message.obtain();
            msg.what = NotificationListenerService.MSG_WHAT_UNREGISTER_LISTENER;
            msg.replyTo = notificationListMessenger;
            try {
                serviceMessenger.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    private void deleteNotification(String uuid) {
        if (serviceMessenger != null) {
            Message msg = Message.obtain();
            msg.what = NotificationListenerService.MSG_WHAT_NOTIFICATION_DELETE;
            msg.getData().putString(NotificationListenerService.EXTRA_UUID, uuid);
            try {
                serviceMessenger.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 获取得到左右的通知消息列表
     *
     * @param notifications
     */
    private void onGetNotificationList(List<Notification> notifications) {
//        if (notifications == null || notifications.isEmpty()) {
//            notificationList = new ArrayList<>();
//
//            Notification notification1 = new Notification();
//            notification1.setTitle("习大大找你聊天");
//            notification1.setContent("Hi，晚上有空吗？");
//            notification1.setDate("12/3");
//            notification1.setTime("12:30");
//            notification1.setIcon(null);
//            notificationList.add(notification1);
//            notificationList.add(notification1);
//            notificationList.add(notification1);
//            notificationList.add(notification1);
//            notificationList.add(notification1);
//            notificationList.add(notification1);
//            notificationList.add(notification1);
//            notificationList.add(notification1);
//            notificationList.add(notification1);
//        } else {
//            notificationList = notifications;
//        }
//        notifications = notificationList;
        notificationList = notifications;
        adapter.notifyDataSetChanged();
    }


    /**
     * 通知新增
     *
     * @param notification
     */
    private void onNotificationAdd(Notification notification) {
        Log.d(TAG, "onNotificationAdd size = " + (notificationList == null ? 0 : notificationList.size()));
        if (!existByUuid(notification)) {
            if (notificationList == null) {
                notificationList = new ArrayList<>();
            }
            notificationList.add(0, notification);
            adapter.notifyDataSetChanged();
        }
    }


    /**
     * 通知删除
     *
     * @param notification
     */
    private void onNotificationDelete(Notification notification) {
        if (notificationList != null && notification != null) {
            Iterator<Notification> iterator = notificationList.iterator();
            boolean refresh = false;
            while (iterator.hasNext()) {
                Notification noti = iterator.next();
                if (noti.getUuid().equals(notification.getUuid())) {
                    iterator.remove();
                    refresh = true;
                }
            }
            if (refresh) {
                adapter.notifyDataSetChanged();
            }
        }
    }


    /**
     * 通知更新
     *
     * @param notification
     */
    private void onNotificationUpdate(Notification notification) {
        if (notificationList != null && notification != null) {
            Iterator<Notification> iterator = notificationList.iterator();
            while (iterator.hasNext()) {
                Notification noti = iterator.next();
                if (noti.getId().equals(notification.getId())) {
                    noti.setContent(notification.getContent());
                }
            }
            adapter.notifyDataSetChanged();
        }
    }

    private void swipeToFinishListener() {
        emptyView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        downY = (int) event.getRawY();
                        Log.d(TAG, "MotionEvent.ACTION_DOWN : " + downY);
                        break;
                    case MotionEvent.ACTION_UP:
                        //下滑超过40dp 退出当前页面
                        if ((moveY - downY) > 40) {
                            finish();
                            overridePendingTransition(0,R.anim.activity_close);
                        }
                        break;
                    case MotionEvent.ACTION_MOVE:
                        moveY = (int) event.getRawY();
                        Log.d(TAG, "MotionEvent.ACTION_MOVE : " + moveY);
                        break;
                    default:
                        break;

                }
                return true;
            }
        });

    }


    private boolean existByUuid(Notification notification) {
        if (notificationList == null || notification == null) {
            return false;
        }
        for (Notification noti : notificationList) {
            if (noti.getUuid().equals(notification.getUuid())) {
                return true;
            }
        }
        return false;
    }

    public class NotificationListHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {
            int what = msg.what;
            Log.d(TAG, "On notification list change, MSG_WHAT = " + what);
            if (what == NotificationListenerService.MSG_WHAT_GET_NOTIFICATION_LIST) {
                List<Notification> notifications = msg.getData().getParcelableArrayList(NotificationListenerService.EXTRA_NOTIFICATION_LIST);
                onGetNotificationList(notifications);
            } else if (what == NotificationListenerService.MSG_WHAT_NOTIFICATION_ADD) {
                Notification notification = msg.getData().getParcelable(NotificationListenerService.EXTRA_NOTIFICATION);
                onNotificationAdd(notification);
            } else if (what == NotificationListenerService.MSG_WHAT_NOTIFICATION_DELETE) {
                Notification notification = msg.getData().getParcelable(NotificationListenerService.EXTRA_NOTIFICATION);
                onNotificationDelete(notification);
            } else if (what == NotificationListenerService.MSG_WHAT_NOTIFICATION_UPDATE) {
                Notification notification = msg.getData().getParcelable(NotificationListenerService.EXTRA_NOTIFICATION);
                onNotificationUpdate(notification);
            }
        }


    }
}
