package com.clouder.records.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.clouder.records.R;
import com.clouder.records.view.SildingFinishLayout;
import com.clouder.records.vo.Record;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends Activity {

    private static String TAG = "MainActivity";

    private Context mContext;
    private ContentResolver resolver;

    private TextView emptyView;
    private ListView listView;
    private SimpleAdapter adapter;

    private List<Record> records = new ArrayList<>();
    private List<Map<String, Object>> listMap = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mContext = this;
        adapter = new SimpleAdapter(this, listMap, R.layout.list_records, new String[] { "name", "date", "img" },
                new int[] { R.id.name, R.id.date, R.id.img });

        Intent serviceIntent = new Intent();
        serviceIntent.setAction("com.clouder.records.service.MessageListenerService");
        serviceIntent.setPackage(getPackageName());
        startService(serviceIntent);

        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.activity_main_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                emptyView = (TextView) findViewById(R.id.empty);
                listView = (ListView) findViewById(R.id.records);

                adapter.setViewBinder(new ListViewBinder());
                listView.setAdapter(adapter);
                listView.setOnItemClickListener(new OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> arg0, View view, int position, long id) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> map = (Map<String, Object>) listView.getItemAtPosition(position);
                        Log.d("testList", "name :" + String.valueOf(map.get("name")));
                        Log.d("testList", "date :" + String.valueOf(map.get("date")));

                        ComponentName componetName = new ComponentName(getPackageName(),
                                "com.clouder.records.activity.IncomingActivity");
                        Intent startIntent = new Intent();
                        startIntent.setComponent(componetName);

                        startIntent.putExtra("command", "dialCall");
                        startIntent.putExtra("name", map.get("name") == null ? map.get("num").toString() : map.get("name")
                                .toString());
                        startIntent.putExtra("number", map.get("num").toString());

                        Bitmap img = (Bitmap) map.get("img");
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        img.compress(Bitmap.CompressFormat.PNG, 100, baos);
                        startIntent.putExtra("img", baos.toByteArray());

                        startActivity(startIntent);
                    }
                });

                SildingFinishLayout mSildingFinishLayout = (SildingFinishLayout) findViewById(R.id.main);
                mSildingFinishLayout
                        .setOnSildingFinishListener(new SildingFinishLayout.OnSildingFinishListener() {

                            @Override
                            public void onSildingFinish() {
                                MainActivity.this.finish();
                            }
                        });
                mSildingFinishLayout.setListTouchView(listView);
                mSildingFinishLayout.setTouchView(mSildingFinishLayout);

                init();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        records.clear();
        listMap.clear();

        if (listView != null) {
            init();
        }
    }

    private void init() {
        getPhoneRecords();
        if (null != records && records.size() > 0) {
            for (int i = records.size() - 1; i >= 0; i--) {
                Record record = records.get(i);
                Map<String, Object> map = new HashMap<>();
                map.put("name", record.getName());
                map.put("date", record.getDate());
                map.put("img", record.getContactPhoto());
                map.put("num", record.getPhoneNumber());
                listMap.add(map);
            }
        }
        adapter.notifyDataSetChanged();
    }

    private class ListViewBinder implements SimpleAdapter.ViewBinder {
        @Override
        public boolean setViewValue(View view, Object data, String textRepresentation) {
            if ((view instanceof ImageView) && (data instanceof Bitmap)) {
                ImageView imageView = (ImageView) view;
                Bitmap bmp = (Bitmap) data;
                imageView.setImageBitmap(bmp);
                return true;
            }
            return false;
        }
    }

    /**
     * get call log
     */
    @SuppressLint("SimpleDateFormat")
    private void getPhoneRecords() {
        resolver = mContext.getContentResolver();

        // get call log
        Cursor phoneCursor = resolver.query(CallLog.Calls.CONTENT_URI, null, null, null, null);

        if (phoneCursor != null) {
            while (phoneCursor.moveToNext()) {
                // contact phoneNumber
                String phoneNumber = phoneCursor.getString(phoneCursor.getColumnIndex(CallLog.Calls.NUMBER));

                // contact name
                // String name =
                // phoneCursor.getString(phoneCursor.getColumnIndexOrThrow(CallLog.Calls.CACHED_NAME));
                String name = getContactName(phoneNumber);

                SimpleDateFormat sfd = new SimpleDateFormat("yyyy/MM/dd HH:mm");
                Date date = new Date(Long.parseLong(phoneCursor.getString(phoneCursor
                        .getColumnIndexOrThrow(CallLog.Calls.DATE))));
                // call time
                String time = sfd.format(date);

                // duration time:s
                String duration = phoneCursor.getString(phoneCursor.getColumnIndexOrThrow(CallLog.Calls.DURATION));

                // call type
                String type = phoneCursor.getString(phoneCursor.getColumnIndex(CallLog.Calls.TYPE));

                Long contactId = getContactId(phoneNumber);

                Record record = new Record();
                record.setName(name == null ? phoneNumber : name);
                record.setDate(time);
                record.setPhoneNumber(phoneNumber);
                record.setDuration(duration);
                record.setType(type);
                record.setContactPhoto(getPhoto(contactId));
                record.setRecordId(String.valueOf(contactId));

                records.add(record);
                emptyView.setVisibility(View.INVISIBLE);
            }
        }

        if (records.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
        }
    }

    @SuppressWarnings("unused")
    private void setPhoneRecords(String name, String num) {
        resolver = mContext.getContentResolver();
        ContentValues values = new ContentValues();
        values.put(CallLog.Calls.CACHED_NAME, name);
        values.put(CallLog.Calls.NUMBER, num);
        values.put(CallLog.Calls.DATE, new Date().getTime());
        values.put(CallLog.Calls.TYPE, 0);// incoming:1,dial:2,not answer:3
        resolver.insert(CallLog.Calls.CONTENT_URI, values);
    }

    /**
     * get head photo
     */
    private Bitmap getPhoto(Long contactId) {

        Bitmap bitmap = null;
        resolver = mContext.getContentResolver();

        Uri uri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId);
        InputStream input = ContactsContract.Contacts.openContactPhotoInputStream(resolver, uri);
        if (input != null) {
            bitmap = BitmapFactory.decodeStream(input);
        } else {
            bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.img_call_list_head);
        }

        return bitmap;
    }

    /**
     * get contact id
     */
    private long getContactId(String phoneNumber) {
        long contactId = 0;
        Uri uri = Uri.parse("content://com.android.contacts/" + "data/phones/filter/" + phoneNumber);
        Cursor cursor = resolver.query(uri, null, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            contactId = cursor.getLong(cursor.getColumnIndex("contact_id"));
        }
        return contactId;
    }

    /**
     * get contact name
     */
    private String getContactName(String phoneNumber) {
        String contactName = phoneNumber;
        Cursor cursorOriginal = mContext.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                new String[]{ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME},
                ContactsContract.CommonDataKinds.Phone.NUMBER + "='" + phoneNumber + "'", null, null);

        if (null != cursorOriginal && cursorOriginal.moveToFirst()) {
            contactName = cursorOriginal.getString(cursorOriginal
                    .getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
        }
        return contactName;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(0, R.anim.base_slide_right_out);
    }
}

