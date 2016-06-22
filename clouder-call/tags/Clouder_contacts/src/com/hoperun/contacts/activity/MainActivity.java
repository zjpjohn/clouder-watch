package com.hoperun.contacts.activity;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.hoperun.contacts.R;
import com.hoperun.contacts.vo.Contact;

public class MainActivity extends Activity {

	private static String TAG = "Contacts";

	private Context mContext;
	private ContentResolver resolver;

	private TextView emptyView;
	private ListView listView;
	private SimpleAdapter adapter;

	private List<Contact> contacts = new ArrayList<>();
	private List<Map<String, Object>> listMap = new ArrayList<>();

	/** 获取库Phon表字段 **/
	private static final String[] PHONES_PROJECTION = new String[] { Phone.DISPLAY_NAME, Phone.NUMBER, Photo.PHOTO_ID,
			Phone.CONTACT_ID };

	/** 联系人显示名称 **/
	private static final int PHONES_DISPLAY_NAME_INDEX = 0;

	/** 电话号码 **/
	private static final int PHONES_NUMBER_INDEX = 1;

	/** 头像ID **/
	private static final int PHONES_PHOTO_ID_INDEX = 2;

	/** 联系人的ID **/
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
				
				adapter.setViewBinder(new ListViewBinder());
				listView.setAdapter(adapter);
				listView.setOnItemClickListener(new OnItemClickListener() {
					@Override
					public void onItemClick(AdapterView<?> arg0, View view, int position, long id) {
						@SuppressWarnings("unchecked")
						Map<String, Object> map = (Map<String, Object>) listView.getItemAtPosition(position);
						Log.d("testList", "name :" + String.valueOf(map.get("name")));
						Log.d("testList", "num :" + String.valueOf(map.get("num")));

						ComponentName componetName = new ComponentName("com.hoperun.records",
								"com.hoperun.records.activity.IncomingActivity");

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
	 * 获取通讯录列表
	 */
	private void getPhoneContacts() {

		resolver = mContext.getContentResolver();

		// 获取手机联系人
		Cursor phoneCursor = resolver.query(Phone.CONTENT_URI, PHONES_PROJECTION, null, null, null);

		if (phoneCursor != null) {
			while (phoneCursor.moveToNext()) {

				// 得到手机号码
				String phoneNumber = phoneCursor.getString(PHONES_NUMBER_INDEX);
				// 当手机号码为空的或者为空字段 跳过当前循环
				if (TextUtils.isEmpty(phoneNumber))
					continue;

				// 得到联系人名称
				String contactName = phoneCursor.getString(PHONES_DISPLAY_NAME_INDEX);
				Log.d(TAG, "name :" + contactName + ", num :" + phoneNumber);

				// 得到联系人ID
				Long contactid = phoneCursor.getLong(PHONES_CONTACT_ID_INDEX);

				// 得到联系人头像ID
				Long photoid = phoneCursor.getLong(PHONES_PHOTO_ID_INDEX);

				// 得到联系人头像Bitamp
				Bitmap contactPhoto = null;

				// photoid 大于0 表示联系人有头像 如果没有给此人设置头像则给他一个默认的
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
	 * 保存联系人
	 */
	@SuppressWarnings("unused")
	private void setPhoneContacts() {
		ContentValues values = new ContentValues();
		// 首先向RawContacts.CONTENT_URI执行一个空值插入，目的是获取系统返回的rawContactId
		Uri rawContactUri = mContext.getContentResolver().insert(RawContacts.CONTENT_URI, values);
		long rawContactId = ContentUris.parseId(rawContactUri);

		// 往data表入姓名数据
		values.clear();
		values.put(Data.RAW_CONTACT_ID, rawContactId);
		values.put(Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE);
		values.put(StructuredName.GIVEN_NAME, "中国电信");
		resolver.insert(android.provider.ContactsContract.Data.CONTENT_URI, values);

		// 往data表入电话数据
		values.clear();
		values.put(android.provider.ContactsContract.Contacts.Data.RAW_CONTACT_ID, rawContactId);
		values.put(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE);
		values.put(Phone.NUMBER, "10000");
		values.put(Phone.TYPE, Phone.TYPE_MOBILE);
		resolver.insert(android.provider.ContactsContract.Data.CONTENT_URI, values);

		// 添加头像
		values.clear();
		values.put(android.provider.ContactsContract.Contacts.Data.RAW_CONTACT_ID, rawContactId);
		values.put(Data.MIMETYPE, Photo.CONTENT_ITEM_TYPE);
		Bitmap headBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		headBitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
		values.put(Photo.PHOTO, baos.toByteArray());
		resolver.insert(android.provider.ContactsContract.Data.CONTENT_URI, values);
	}
}
