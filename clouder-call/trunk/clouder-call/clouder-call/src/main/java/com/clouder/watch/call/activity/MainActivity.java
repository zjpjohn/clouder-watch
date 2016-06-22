package com.clouder.watch.call.activity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.Photo;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.clouder.watch.call.R;
import com.clouder.watch.call.ui.MyAdapter;
import com.clouder.watch.call.ui.MyViewPager;
import com.clouder.watch.call.ui.RoundImageView;

import java.io.IOException;
import java.io.InputStream;
import java.text.Collator;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Vector;

/**
 * ClassName: MainActivity
 *
 * @author xing_peng
 * @description
 * @Date 2015-9-6
 */
public class MainActivity extends Activity {

    private static String TAG = "MainActivity";

    private List<View> lists = new ArrayList<>();

    private ContentResolver resolver;

    /**
     * contacts *
     */
    private TextView contactsEmptyView;

    private ListView contactsListView;

    private BaseAdapter contactsAdapter;

    private List<Item> contactsListMap = new Vector<>();


    private View contactsView, firstViewPager;

    private ImageView cImageView;

    /**
     * records *
     */
    private TextView recordsEmptyView;

    private ListView recordsListView;

    private BaseAdapter recordsAdapter;

    private List<Item> recordsListMap = new Vector<>();

    private View recordsView;

    private ImageView rImageView;

    /**
     * get Phone fields *
     */
    private static final String[] PHONES_PROJECTION = new String[]{Phone.DISPLAY_NAME, Phone.NUMBER, Photo.PHOTO_ID, Phone.CONTACT_ID};

    /**
     * contacts name *
     */
    private static final int PHONES_DISPLAY_NAME_INDEX = 0;

    /**
     * contacts phone number *
     */
    private static final int PHONES_NUMBER_INDEX = 1;

    /**
     * contacts photo ID *
     */
    private static final int PHONES_PHOTO_ID_INDEX = 2;

    /**
     * contacts ID *
     */
    private static final int PHONES_CONTACT_ID_INDEX = 3;


    private static final int MSG_WHAT_FIND_CONTACT = 1;

    private static final int MSG_WHAT_SORT_CONTACT = 2;

    private static final int MSG_WHAT_FIND_RECORD = 3;

    private static final int MSG_WHAT_SORT_RECORD = 4;

    private static final String EXTRA_NAME = "extra_name";

    private static final String EXTRA_PHOTO = "extra_photo";

    private static final String EXTRA_NUMBER = "extra_number";

    private static final String EXTRA_TIME = "extra_time";


    private SimpleDateFormat sfd = new SimpleDateFormat("yyyy/MM/dd HH:mm");

    private BroadcastReceiver receiver;

    private Handler uiHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            int what = msg.what;
            switch (what) {
                case MSG_WHAT_FIND_CONTACT:
                    if (contactsListView.getVisibility() != View.VISIBLE) {
                        contactsListView.setVisibility(View.VISIBLE);
                        contactsEmptyView.setVisibility(View.GONE);
                    }
                    String contactName = msg.getData().getString(EXTRA_NAME);
                    String phoneNumber = msg.getData().getString(EXTRA_NUMBER);
                    Bitmap contactPhoto = msg.getData().getParcelable(EXTRA_PHOTO);
                    String time = msg.getData().getString(EXTRA_TIME);
                    contactsListMap.add(new Item(contactName, phoneNumber, contactPhoto, time));
                    contactsAdapter.notifyDataSetChanged();
                    break;
                case MSG_WHAT_SORT_CONTACT:
                    if (contactsListMap.isEmpty()) {
                        contactsEmptyView.setVisibility(View.VISIBLE);
//                        contactsListView.setVisibility(View.GONE);
                        contactsListView.setEmptyView(contactsListView);
                    } else {
                        contactsEmptyView.setVisibility(View.GONE);
//                        contactsListView.setVisibility(View.VISIBLE);
                        Collections.sort(contactsListMap, cmp);
                        contactsAdapter.notifyDataSetChanged();
                    }
                    break;
                case MSG_WHAT_FIND_RECORD:
                    if (recordsListView.getVisibility() != View.VISIBLE) {
                        recordsListView.setVisibility(View.VISIBLE);
                        recordsEmptyView.setVisibility(View.GONE);
                    }
                    String name = msg.getData().getString(EXTRA_NAME);
                    String number = msg.getData().getString(EXTRA_NUMBER);
                    Bitmap photo = msg.getData().getParcelable(EXTRA_PHOTO);
                    String datetime = msg.getData().getString(EXTRA_TIME);
                    recordsListMap.add(0, new Item(name, number, photo, datetime));
                    recordsAdapter.notifyDataSetChanged();
                    break;

                case MSG_WHAT_SORT_RECORD:
                    if (recordsListMap.isEmpty()) {
                        recordsEmptyView.setVisibility(View.VISIBLE);
                        recordsListView.setVisibility(View.GONE);
                    } else {
                        recordsEmptyView.setVisibility(View.GONE);
                        recordsListView.setVisibility(View.VISIBLE);
                    }
                    break;
            }


        }
    };

    private Messenger serverMessenger = null;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "onServiceConnected");
            serverMessenger = new Messenger(service);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "onServiceDisconnected");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Intent callListenerServiceIntent = new Intent();
        callListenerServiceIntent.setComponent(new ComponentName("com.clouder.watch.mobile", "com.clouder.watch.mobile.CallMessageListenerService"));
        bindService(callListenerServiceIntent, serviceConnection, BIND_AUTO_CREATE);
        contactsView = getLayoutInflater().inflate(R.layout.contacts_layout, null);
        recordsView = getLayoutInflater().inflate(R.layout.records_layout, null);
        firstViewPager = getLayoutInflater().inflate(R.layout.viewpager_none, null);
        lists.add(firstViewPager);
        lists.add(contactsView);
        lists.add(recordsView);

        MyAdapter myAdapter = new MyAdapter(lists);
        MyViewPager viewPager = (MyViewPager) findViewById(R.id.viewPager);
        viewPager.setAdapter(myAdapter);
        viewPager.setCurrentItem(1);
        viewPager.setOnPageChangeListener(new MyViewPager.OnPageChangeListener() {
            @Override
            public void onPageSelected(int arg0) {
                switch (arg0) {
                    case 0:
                        rImageView.setVisibility(View.INVISIBLE);
                        cImageView.setVisibility(View.INVISIBLE);
                        finish();
                        overridePendingTransition(android.R.anim.accelerate_decelerate_interpolator, android.R.anim.slide_out_right);
                        break;
                    case 1:
                        cImageView.setImageResource(R.drawable.circle_2);
                        rImageView.setImageResource(R.drawable.circle_1);
                        break;
                    case 2:
                        rImageView.setImageResource(R.drawable.circle_2);
                        cImageView.setImageResource(R.drawable.circle_1);
                        break;
                    default:
                        break;
                }
            }

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });
        //contacts view
        contactsEmptyView = (TextView) contactsView.findViewById(R.id.empty);
        contactsEmptyView.setVisibility(View.GONE);
        contactsListView = (ListView) contactsView.findViewById(R.id.contacts);
//        contactsListView.setVisibility(View.VISIBLE);
        cImageView = (ImageView) findViewById(R.id.circle_contact);
        contactsListView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View view, int position, long id) {
                if (Settings.Global.getInt(getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 0) == 1) {
                    Log.d(TAG, "isAirPlaneOn");
                    Toast.makeText(MainActivity.this, R.string.notify, Toast.LENGTH_SHORT).show();
                } else {
                    Item item = contactsListMap.get(position);
                    dialPhoneCall(item.number);
                }
            }
        });

        //records view
        recordsEmptyView = (TextView) recordsView.findViewById(R.id.empty);
        recordsEmptyView.setVisibility(View.GONE);
        recordsListView = (ListView) recordsView.findViewById(R.id.records);
        recordsListView.setVisibility(View.VISIBLE);
        rImageView = (ImageView) findViewById(R.id.circle_record);
        recordsListView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View view, int position, long id) {
                if (Settings.Global.getInt(getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 0) == 1) {
                    Log.d(TAG, "isAirPlaneOn");
                    Toast.makeText(MainActivity.this, R.string.notify, Toast.LENGTH_SHORT).show();
                } else {
                    Item item = recordsListMap.get(position);
                    dialPhoneCall(item.number);
                }

            }
        });
        contactsAdapter = new BaseAdapter() {
            @Override
            public int getCount() {
                return contactsListMap == null ? 0 : contactsListMap.size();
            }

            @Override
            public Object getItem(int position) {
                return contactsListMap.get(position);
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
                    convertView = getLayoutInflater().inflate(R.layout.list_contacts, null);
                    viewHolder.roundImageView = (RoundImageView) convertView.findViewById(R.id.img);
                    viewHolder.tvName = (TextView) convertView.findViewById(R.id.name);
                    viewHolder.tvNumber = (TextView) convertView.findViewById(R.id.num);
                    convertView.setTag(viewHolder);
                } else {
                    viewHolder = (ViewHolder) convertView.getTag();
                }
                Item item = contactsListMap.get(position);
                if (item.head == null) {
                    viewHolder.roundImageView.setImageResource(R.drawable.img_call_list_head);
                } else {
                    viewHolder.roundImageView.setImageBitmap(item.head);
                }
                viewHolder.tvName.setText(item.name);
                viewHolder.tvNumber.setText(item.number);
                return convertView;
            }
        };

        contactsListView.setAdapter(contactsAdapter);

        recordsAdapter = new BaseAdapter() {
            @Override
            public int getCount() {
                return recordsListMap == null ? 0 : recordsListMap.size();
            }

            @Override
            public Object getItem(int position) {
                return recordsListMap.get(position);
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
                    convertView = getLayoutInflater().inflate(R.layout.list_records, null);
                    viewHolder.roundImageView = (RoundImageView) convertView.findViewById(R.id.img);
                    viewHolder.tvName = (TextView) convertView.findViewById(R.id.name);
                    viewHolder.tvNumber = (TextView) convertView.findViewById(R.id.date);
                    convertView.setTag(viewHolder);
                } else {
                    viewHolder = (ViewHolder) convertView.getTag();
                }
                Item item = recordsListMap.get(position);
                if (item.head == null) {
                    viewHolder.roundImageView.setImageResource(R.drawable.img_call_list_head);
                } else {
                    viewHolder.roundImageView.setImageBitmap(item.head);
                }
                viewHolder.tvName.setText(item.name);
                viewHolder.tvNumber.setText(item.date);
                return convertView;
            }
        };
        recordsListView.setAdapter(recordsAdapter);

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

            }
        };
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("com.clouder.watch.ACTION_CALL_RECORDS_CHANGE");
        intentFilter.addAction("com.clouder.watch.ACTION_CONTACTS_CHANGE");
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if ("com.clouder.watch.ACTION_CALL_RECORDS_CHANGE".equals(action)) {
                    String name = intent.getStringExtra("name");
                    String number = intent.getStringExtra("number");
                    long time = intent.getLongExtra("time", new Date().getTime());
                    Message msg = Message.obtain();
                    msg.what = MSG_WHAT_FIND_RECORD;
                    msg.getData().putString(EXTRA_NAME, name);
                    msg.getData().putString(EXTRA_NUMBER, number);
                    msg.getData().putString(EXTRA_TIME, sfd.format(new Date(time)));
                    msg.getData().putParcelable(EXTRA_PHOTO, getPhoto(getContactId(number)));
                    uiHandler.sendMessage(msg);
                } else if ("com.clouder.watch.ACTION_CONTACTS_CHANGE".equals(action)) {
                    String number = intent.getStringExtra("extra_phone_number");
                    int state = intent.getIntExtra("extra_state", -1);
                    Item item = contactExist(number);
                    if (state == 1) {
                        //新增
                        if (item == null) {
                            String name = intent.getStringExtra("extra_name");
                            Bitmap head = intent.getParcelableExtra("extra_photo");
                            String time = intent.getParcelableExtra("extra_time");
                            item = new Item(name, number, head, time);
                            contactsListMap.add(item);
                            contactsAdapter.notifyDataSetChanged();
                        }
                    } else if (state == 2) {
                        //删除
                        if (item != null) {
                            contactsListMap.remove(item);
                            contactsAdapter.notifyDataSetChanged();
                        }
                    }

                }

            }
        };
        registerReceiver(receiver, intentFilter);

        loadContacts();
        loadCallRecords();
    }


    private Item contactExist(String phoneNumber) {
        if (contactsListMap.isEmpty()) {
            return null;
        }
        for (Item item : contactsListMap) {
            if (item.number.equals(phoneNumber)) {
                return item;
            }
        }
        return null;
    }


    private void loadCallRecords() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                resolver = getContentResolver();
                Cursor phoneCursor = resolver.query(CallLog.Calls.CONTENT_URI, null, null, null, null);
                if (phoneCursor != null) {
                    while (phoneCursor.moveToNext()) {
                        String phoneNumber = phoneCursor.getString(phoneCursor.getColumnIndex(CallLog.Calls.NUMBER));
                        String name = getContactName(phoneNumber);
                        Date date = new Date(Long.parseLong(phoneCursor.getString(phoneCursor.getColumnIndexOrThrow(CallLog.Calls.DATE))));
                        String time = sfd.format(date);
                        Long contactId = getContactId(phoneNumber);
                        Message msg = Message.obtain();
                        msg.what = MSG_WHAT_FIND_RECORD;
                        msg.getData().putString(EXTRA_NAME, name == null ? phoneNumber : name);
                        msg.getData().putString(EXTRA_NUMBER, phoneNumber);
                        msg.getData().putParcelable(EXTRA_PHOTO, getPhoto(contactId));
                        msg.getData().putString(EXTRA_TIME, time);
                        uiHandler.sendMessage(msg);
                    }
                    phoneCursor.close();
                }
                uiHandler.sendEmptyMessage(MSG_WHAT_SORT_RECORD);
            }
        }).start();
    }

    private void loadContacts() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                resolver = getContentResolver();
                long begin = System.currentTimeMillis();
                Log.d(TAG, "开始读取数据库中的联系人: start = " + begin);
                Cursor phoneCursor = resolver.query(Phone.CONTENT_URI, PHONES_PROJECTION, null, null, PHONES_PROJECTION[0] + " ASC");
                if (phoneCursor != null) {
                    while (phoneCursor.moveToNext()) {
                        String phoneNumber = phoneCursor.getString(PHONES_NUMBER_INDEX);
                        if (TextUtils.isEmpty(phoneNumber))
                            continue;
                        String contactName = phoneCursor.getString(PHONES_DISPLAY_NAME_INDEX);
                        Long contactId = phoneCursor.getLong(PHONES_CONTACT_ID_INDEX);
                        Long photoId = phoneCursor.getLong(PHONES_PHOTO_ID_INDEX);
                        Bitmap contactPhoto;
                        if (photoId > 0) {
                            Uri uri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId);
                            InputStream input = ContactsContract.Contacts.openContactPhotoInputStream(resolver, uri);
                            contactPhoto = BitmapFactory.decodeStream(input);
                            try {
                                if (input != null) {
                                    input.close();
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        } else {
                            contactPhoto = BitmapFactory.decodeResource(getResources(), R.drawable.img_call_list_head);
                        }
                        Message msg = Message.obtain();
                        msg.what = MSG_WHAT_FIND_CONTACT;
                        msg.getData().putString(EXTRA_NAME, contactName);
                        msg.getData().putString(EXTRA_NUMBER, phoneNumber);
                        msg.getData().putParcelable(EXTRA_PHOTO, contactPhoto);
                        msg.getData().putString(EXTRA_TIME, "");
                        uiHandler.sendMessage(msg);
                    }
                    phoneCursor.close();
                    long end = System.currentTimeMillis();
                    Log.d(TAG, "读取联系人数据完毕: end = " + end + ", cost = " + (end - begin));
                }
                uiHandler.sendEmptyMessage(MSG_WHAT_SORT_CONTACT);
            }
        }).start();
    }

    public class ViewHolder {
        public RoundImageView roundImageView;
        public TextView tvName;
        public TextView tvNumber;
    }

    public class Item {

        public String name;

        public String number;

        public Bitmap head;

        public String date;

        public Item(String name, String number, Bitmap head, String date) {
            this.name = name;
            this.number = number;
            this.head = head;
            this.date = date;
        }

    }


    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(serviceConnection);
        unregisterReceiver(receiver);
    }

    /**
     * get head photo
     */
    private Bitmap getPhoto(Long contactId) {
        Bitmap bitmap = null;
        resolver = getContentResolver();
        InputStream input = null;
        try {
            Uri uri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId);
            input = ContactsContract.Contacts.openContactPhotoInputStream(resolver, uri);
            if (input != null) {
                bitmap = BitmapFactory.decodeStream(input);
            } else {
                bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.img_call_list_head);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return bitmap;
    }

    /**
     * get contact id
     */
    private long getContactId(String phoneNumber) {
        long contactId = 0;
        Cursor cursor = null;
        try {
            Uri uri = Uri.parse("content://com.android.contacts/" + "data/phones/filter/" + phoneNumber);
            cursor = resolver.query(uri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                contactId = cursor.getLong(cursor.getColumnIndex("contact_id"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return contactId;
    }

    /**
     * get contact name
     */
    private String getContactName(String phoneNumber) {
        String contactName = phoneNumber;
        Cursor cursorOriginal = null;
        try {
            cursorOriginal = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    new String[]{ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME},
                    ContactsContract.CommonDataKinds.Phone.NUMBER + "='" + phoneNumber + "'", null, null);
            if (null != cursorOriginal && cursorOriginal.moveToFirst()) {
                contactName = cursorOriginal.getString(cursorOriginal
                        .getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursorOriginal != null) {
                cursorOriginal.close();
            }

        }
        return contactName;
    }

    Comparator<Item> cmp = new Comparator<Item>() {
        public int compare(Item o1, Item o2) {
            return Collator.getInstance(Locale.CHINESE).compare(o1.name, o2.name);
        }
    };


    /**
     * dialPhoneCall
     */
    public void dialPhoneCall(String phoneNumber) {
        Log.d(TAG, "dialPhoneCall PhoneNumber = " + phoneNumber);
        if (serverMessenger != null) {
            Message msg = Message.obtain();
            msg.what = 3;
            msg.getData().putString("phoneNumber", phoneNumber);
            try {
                serverMessenger.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }
}
