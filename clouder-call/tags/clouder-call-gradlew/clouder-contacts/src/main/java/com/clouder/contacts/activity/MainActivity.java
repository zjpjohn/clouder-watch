package com.clouder.contacts.activity;

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
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.Photo;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;
import android.support.wearable.view.WatchViewStub;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.clouder.contacts.R;
import com.clouder.contacts.view.SildingFinishLayout;
import com.clouder.contacts.vo.Contact;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends Activity {

    private static String TAG = "MainActivity";

    private Context mContext;
    private ContentResolver resolver;
    private OnTouchListener onTouchListener;

    private TextView emptyView;
    private ListView listView;
    private SimpleAdapter adapter;

    private List<Contact> contacts = new ArrayList<>();
    private List<Map<String, Object>> listMap = new ArrayList<>();

    /** get Phone fields **/
    private static final String[] PHONES_PROJECTION = new String[] { Phone.DISPLAY_NAME, Phone.NUMBER, Photo.PHOTO_ID,
            Phone.CONTACT_ID };

    /** contacts name **/
    private static final int PHONES_DISPLAY_NAME_INDEX = 0;

    /** contacts phone number **/
    private static final int PHONES_NUMBER_INDEX = 1;

    /** contacts photo ID **/
    private static final int PHONES_PHOTO_ID_INDEX = 2;

    /** contacts ID **/
    private static final int PHONES_CONTACT_ID_INDEX = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mContext = this;

        adapter = new SimpleAdapter(this, listMap, R.layout.list_contacts, new String[] { "name",
                "num", "img" }, new int[] { R.id.name, R.id.num, R.id.img });

        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.activity_main_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {

                emptyView = (TextView) findViewById(R.id.empty);
                listView = (ListView) findViewById(R.id.contacts);
                RelativeLayout layout = (RelativeLayout) findViewById(R.id.main);
                layout.setOnTouchListener(onTouchListener);
                listView.setOnTouchListener(onTouchListener);
                emptyView.setOnTouchListener(onTouchListener);

                adapter.setViewBinder(new ListViewBinder());
                listView.setAdapter(adapter);
                listView.setOnItemClickListener(new OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> arg0, View view, int position, long id) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> map = (Map<String, Object>) listView.getItemAtPosition(position);
                        Log.d("testList", "name :" + String.valueOf(map.get("name")));
                        Log.d("testList", "num :" + String.valueOf(map.get("num")));

                        ComponentName componetName = new ComponentName("com.clouder.records",
                                "com.clouder.records.activity.IncomingActivity");

                        Intent startIntent = new Intent();
                        startIntent.setComponent(componetName);
                        startIntent.putExtra("command", "dialCall");
                        startIntent.putExtra("name",
                                map.get("name") == null ? map.get("num").toString() : map.get("name").toString());
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
                            public void onSildingRightFinish() {
                                MainActivity.this.finish();
                            }

                            @Override
                            public void onSildingLeftFinish() {
                                ComponentName componetName = new ComponentName("com.clouder.records",
                                        "com.clouder.records.activity.MainActivity");

                                Intent startIntent = new Intent();
                                startIntent.setComponent(componetName);
                                startActivity(startIntent);
                                overridePendingTransition(R.anim.base_slide_right_in, R.anim.base_slide_remain);
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
        contacts.clear();
        listMap.clear();
        if (listView != null) {
            init();
        }
    }

    private void init() {
        getPhoneContacts();
        if (null != contacts && contacts.size() > 0) {
            for (int i = contacts.size() - 1; i >= 0; i--) {
                Contact contact = contacts.get(i);
                Map<String, Object> map = new HashMap<>();
                map.put("name", contact.getName());
                map.put("img", contact.getContactPhoto());
                map.put("num", contact.getPhoneNumber());
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
     * get contacts list
     */
    private void getPhoneContacts() {

        resolver = mContext.getContentResolver();

        // get phone contacts
        Cursor phoneCursor = resolver.query(Phone.CONTENT_URI, PHONES_PROJECTION, null, null, null);

        if (phoneCursor != null) {
            while (phoneCursor.moveToNext()) {

                // get the contact phone number
                String phoneNumber = phoneCursor.getString(PHONES_NUMBER_INDEX);
                // When the cell phone number is empty or null field Skip the current loop
                if (TextUtils.isEmpty(phoneNumber))
                    continue;

                // get the contact name
                String contactName = phoneCursor.getString(PHONES_DISPLAY_NAME_INDEX);
                Log.d(TAG, "name :" + contactName + ", num :" + phoneNumber);

                // get the contact ID
                Long contactid = phoneCursor.getLong(PHONES_CONTACT_ID_INDEX);

                // get the contact photo ID
                Long photoid = phoneCursor.getLong(PHONES_PHOTO_ID_INDEX);

                // get the contact photo bitamp
                Bitmap contactPhoto = null;

                // photoid > 0 mean the contact has head photo. If didn't give the person set portraits will give him a default
                if (photoid > 0) {
                    Uri uri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactid);
                    InputStream input = ContactsContract.Contacts.openContactPhotoInputStream(resolver, uri);
                    contactPhoto = BitmapFactory.decodeStream(input);
                } else {
                    contactPhoto = BitmapFactory.decodeResource(getResources(), R.drawable.img_call_list_head);
                }

                Contact contact = new Contact();
                contact.setName(contactName);
                contact.setPhoneNumber(phoneNumber);
                contact.setContactPhoto(contactPhoto);
                contact.setContactId(contactid.toString());

                contacts.add(contact);
                emptyView.setVisibility(View.INVISIBLE);
            }
            phoneCursor.close();
        }

        if (contacts.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
        }

    }

    /**
     * Save the contact
     */
    private void setPhoneContacts(String name, String phoneNumber, Bitmap headBitmap) {
        ContentValues values = new ContentValues();

        Uri rawContactUri = mContext.getContentResolver().insert(RawContacts.CONTENT_URI, values);
        long rawContactId = ContentUris.parseId(rawContactUri);

        // inset name in data table
        values.clear();
        values.put(Data.RAW_CONTACT_ID, rawContactId);
        values.put(Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE);
        values.put(StructuredName.GIVEN_NAME, name);
        resolver.insert(android.provider.ContactsContract.Data.CONTENT_URI, values);

        // inset phone number in data table
        values.clear();
        values.put(android.provider.ContactsContract.Contacts.Data.RAW_CONTACT_ID, rawContactId);
        values.put(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE);
        values.put(Phone.NUMBER, phoneNumber);
        values.put(Phone.TYPE, Phone.TYPE_MOBILE);
        resolver.insert(android.provider.ContactsContract.Data.CONTENT_URI, values);

        // save head photo
        values.clear();
        values.put(android.provider.ContactsContract.Contacts.Data.RAW_CONTACT_ID, rawContactId);
        values.put(Data.MIMETYPE, Photo.CONTENT_ITEM_TYPE);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        headBitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
        values.put(Photo.PHOTO, baos.toByteArray());
        resolver.insert(android.provider.ContactsContract.Data.CONTENT_URI, values);
    }

}
