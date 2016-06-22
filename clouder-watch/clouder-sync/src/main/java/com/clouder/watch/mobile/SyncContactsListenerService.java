/**
 * **************************************************************************
 * <p/>
 * HOPERUN PROPRIETARY INFORMATION
 * <p/>
 * The information contained herein is proprietary to HopeRun
 * and shall not be reproduced or disclosed in whole or in part
 * or used for any design or manufacture
 * without direct written authorization from HopeRun.
 * <p/>
 * Copyright (c) 2014 by HopeRun.  All rights reserved.
 * <p/>
 * ***************************************************************************
 */
package com.clouder.watch.mobile;

import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.Photo;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;
import android.util.Log;

import com.cms.android.common.ConnectionResult;
import com.cms.android.common.api.MobvoiApiClient;
import com.cms.android.common.api.MobvoiApiClient.ConnectionCallbacks;
import com.cms.android.common.api.MobvoiApiClient.OnConnectionFailedListener;
import com.cms.android.common.api.ResultCallback;
import com.cms.android.common.api.Status;
import com.cms.android.wearable.MessageApi;
import com.cms.android.wearable.MessageEvent;
import com.cms.android.wearable.Node;
import com.cms.android.wearable.NodeApi;
import com.cms.android.wearable.Wearable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ClassName: SynContactsService
 *
 * @author xing_peng
 * @description
 * @Date 2015-8-18
 */
public class SyncContactsListenerService extends Service implements OnConnectionFailedListener, ConnectionCallbacks,
        MessageApi.MessageListener, NodeApi.NodeListener {

    private static final String TAG = "ContactsListenerService";

    private MobvoiApiClient mobvoiApiClient;
    private Context mContext;
    private ContentResolver resolver;
    private Map<String, String> map = new HashMap<>();
    private boolean repeat = false;
    private Handler mainHandler = new Handler();
    /**
     * get Phone fields *
     */
    private static final String[] PHONES_PROJECTION = new String[]{Phone.DISPLAY_NAME, Phone.NUMBER, Photo.PHOTO_ID,
            Phone.CONTACT_ID};


    private static final String SYN_CONTACTS_URL = "/call/contacts";
    public static final String RESET_CONTACTS_URL = "/call/contacts/reset";
    public static final String CONTACT_ADD = "/call/contacts/add";
    public static final String CONTACT_DELETE = "/call/contacts/delete";

    @Override
    public void onCreate() {
        mContext = this;
        resolver = mContext.getContentResolver();
        mobvoiApiClient = new MobvoiApiClient.Builder(this).addApi(Wearable.API).addConnectionCallbacks(this).addOnConnectionFailedListener(this).build();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        Log.d(TAG, "onMessageReceived path : " + messageEvent.getPath());
        if (messageEvent.getPath().equals(SYN_CONTACTS_URL)) {
            Contact contact = new Contact(messageEvent.getData());
            Log.d(TAG, "准备插入的联系人：" + contact.getContactName() + "," + contact.getPhoneNumber());
            if (contact.getPhoto() == null) {
                Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.img_call_list_head);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
                contact.setPhoto(baos.toByteArray());
            }
            savePhoneContact(contact);
        } else if (messageEvent.getPath().equals(RESET_CONTACTS_URL)) {
            deletePhoneContacts();
        } else if (messageEvent.getPath().equals(CONTACT_ADD)) {
            Contact contact = new Contact(messageEvent.getData());
            savePhoneContact(contact);
            sendBroadcastAdd(contact);
        } else if (messageEvent.getPath().equals(CONTACT_DELETE)) {
            Contact contact = new Contact(messageEvent.getData());
            deleteContact(contact);
            sendBroadcastDelete(contact);
        } else if (messageEvent.getPath().equals("SYNC_CONTACTS_REQUEST")) {
            SharedPreferences sp = getSharedPreferences("request", MODE_PRIVATE);
            String nodId = sp.getString("nodeId", null);
            String phoneNodeId = new String(messageEvent.getData());
            Log.d(TAG, "nodeIdFromSharedPreferences:" + nodId + " , nodeIdFromPhone :" + phoneNodeId);
            if (phoneNodeId.equals(nodId)) {
                Log.d(TAG, "发送消息通知手机不需要同步联系人");
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        sendMessageRequest("SYNC_FALSE");
                    }
                }).start();
            }
            if (nodId == null || !phoneNodeId.equals(nodId)) {
                Log.d(TAG, "发送消息通知手机可以同步联系人");
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        sendMessageRequest("SYNC_TRUE");
                    }
                }).start();
            }
            SharedPreferences sharedPreferences = getSharedPreferences("request", MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            Log.d(TAG, "将手机端传过来的nodeId存入SharedPreferences：" + phoneNodeId);
            editor.putString("nodeId", phoneNodeId);
            editor.commit();
        }

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand...........");
        if (!mobvoiApiClient.isConnected()) {
            mobvoiApiClient.connect();
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Wearable.MessageApi.removeListener(mobvoiApiClient, this);
        Wearable.NodeApi.removeListener(mobvoiApiClient, this);
        map.clear();
        mobvoiApiClient.disconnect();

    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.e(TAG, "手机端：CMS服务已连接");
        Wearable.MessageApi.addListener(mobvoiApiClient, this);
        Wearable.NodeApi.addListener(mobvoiApiClient, SyncContactsListenerService.this).setResultCallback(new ResultCallback<Status>() {
            @Override
            public void onResult(Status status) {
                if (status.isSuccess()) {
                    Log.d(TAG, "add node listener success!");

                } else {
                    Log.e(TAG, "add node listener failed!");
                }
            }
        });
    }

    @Override
    public void onConnectionSuspended(int result) {
        Log.e(TAG, "CMS ConnectionSuspended cause:" + result);
        Log.e(TAG, "Mobvoi api client connection was suspended! mobvoi api client will try reconnect! ");
        reconnect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.e(TAG, "CMS ConnectionFailed " + result);
        if (result != null && result.getErrorCode() == 9) {
            reconnect();
        }
    }

    @Override
    public void onPeerConnected(Node node) {
        Log.d(TAG, "onPeerConnected: nodeId = " + node.getId() + " , displayName = " + node.getDisplayName());
    }

    @Override
    public void onPeerDisconnected(Node node) {
        Log.d(TAG, "onPeerDisconnected: nodeId = " + node.getId() + " , displayName = " + node.getDisplayName());
    }

    /**
     * 重连
     */
    public void reconnect() {
        Wearable.MessageApi.removeListener(mobvoiApiClient, this);
        mobvoiApiClient.disconnect();
        Log.e(TAG, "Mobvoi api client will try connect again " + 5
                + " seconds later! thread =" + Thread.currentThread().getName());

        mainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mobvoiApiClient.connect();
            }
        }, 5000);
    }

    private void savePhoneContact(Contact contact) {
        String phoneNumber = contact.getPhoneNumber();
        phoneNumber = phoneNumber.replace(" ", "");
        phoneNumber = phoneNumber.replace("-", "");
        repeat = false;
        if (map.size() > 0) {
            for (Map.Entry<String, String> entry : map.entrySet()) {
                String num = entry.getKey().toString();
                String name = entry.getValue().toString();
//                Log.d(TAG, "遍历：" + num + "," + name);
                if (name.equals(contact.getContactName()) && num.equals(phoneNumber)) {
                    Log.d(TAG, "该联系人已存在，不用重复同步！" + "姓名：" + contact.getContactName() + ",号码：" + contact.getPhoneNumber());
                    repeat = true;
                }
            }
        }
        if (repeat == false) {
            ContentValues values = new ContentValues();
            // 首先向RawContacts.CONTENT_URI执行一个空值插入，目的是获取系统返回的rawContactId
            Uri rawContactUri = mContext.getContentResolver().insert(RawContacts.CONTENT_URI, values);
            long rawContactId = ContentUris.parseId(rawContactUri);
            Log.d(TAG, "savePhoneContact rawContactId = " + rawContactId);
            // 往data表入姓名数据
            values.clear();
            values.put(Data.RAW_CONTACT_ID, rawContactId);
            values.put(Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE);
            values.put(StructuredName.GIVEN_NAME, contact.getContactName());
            resolver.insert(Data.CONTENT_URI, values);
            // 往data表入电话数据
            values.clear();
            values.put(ContactsContract.Contacts.Data.RAW_CONTACT_ID, rawContactId);
            values.put(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE);
            values.put(Phone.NUMBER, phoneNumber);
            values.put(Phone.TYPE, Phone.TYPE_MOBILE);
            resolver.insert(Data.CONTENT_URI, values);
            // 添加头像
            values.clear();
            values.put(ContactsContract.Contacts.Data.RAW_CONTACT_ID, rawContactId);
            values.put(Data.MIMETYPE, Photo.CONTENT_ITEM_TYPE);
            values.put(Photo.PHOTO, contact.getPhoto());
            resolver.insert(Data.CONTENT_URI, values);

            map.put(phoneNumber, contact.getContactName());
            Log.d(TAG, "插入联系人: " + "姓名：" + contact.getContactName() + ",号码：" + contact.getPhoneNumber());
            sendBroadcastAdd(contact);
        }
    }

    private void deletePhoneContacts() {
        resolver.delete(
                RawContacts.CONTENT_URI.buildUpon()
                        .appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true").build(), null, null);
        resolver.delete(CallLog.Calls.CONTENT_URI, null, null);
        map.clear();
    }

//    private void checkContacts() {
//        resolver = mContext.getContentResolver();
//        // get phone contacts
//        Cursor phoneCursor = resolver.query(Phone.CONTENT_URI, PHONES_PROJECTION, null, null, null);
//        if (phoneCursor != null) {
//            while (phoneCursor.moveToNext()) {
//
//                // get the contact phone number
//                String phoneNumber = phoneCursor.getString(1);
//                // When the cell phone number is empty or null field Skip the
//                // current loop
//                if (TextUtils.isEmpty(phoneNumber))
//                    continue;
//                phoneNumber = phoneNumber.replace(" ", "");
//                phoneNumber = phoneNumber.replace("-", "");
//                // get the contact name
//                String contactName = phoneCursor.getString(0);
//                map.put(phoneNumber, contactName);
//                Log.d(TAG, "查看手表中已有的联系人：" + "name :" + contactName + ", num :" + phoneNumber);
//            }
//            phoneCursor.close();
//        }
//    }

    private void deleteContact(Contact contact) {
        Log.d(TAG, "deleteContact " + contact.getContactName());
        String phoneNumber = contact.getPhoneNumber();
        phoneNumber = phoneNumber.replace(" ", "");
        phoneNumber = phoneNumber.replace("-", "");
        Long id = getContactId(phoneNumber);
        //删除data表中数据
        String where = Data.CONTACT_ID + " =?";
        String[] whereparams = new String[]{id.toString()};
        resolver.delete(Data.CONTENT_URI, where, whereparams);
        //删除rawContact表中数据
        where = RawContacts.CONTACT_ID + " =?";
        whereparams = new String[]{id.toString()};
        resolver.delete(RawContacts.CONTENT_URI, where, whereparams);
        Log.d(TAG, "Map移除删除的联系人：" + map.get(phoneNumber));
        map.remove(phoneNumber);
    }

    private void sendBroadcastAdd(Contact contact) {
        String phoneNumber = contact.getPhoneNumber();
        phoneNumber = phoneNumber.replace(" ", "");
        phoneNumber = phoneNumber.replace("-", "");
        Intent add = new Intent();
        add.putExtra("extra_state", 1);
        add.putExtra("extra_phone_number", phoneNumber);
        add.putExtra("extra_name", contact.getContactName());
        byte[] bytes = contact.getPhoto();
        if (bytes != null) {
            add.putExtra("extra_photo", BitmapFactory.decodeByteArray(contact.getPhoto(), 0, bytes.length));
        }
        add.setAction("com.clouder.watch.ACTION_CONTACTS_CHANGE");
        sendBroadcast(add);
        Log.d(TAG, "send broadcast : " + "add");
    }

    private void sendBroadcastDelete(Contact contact) {
        String phoneNumber = contact.getPhoneNumber();
        phoneNumber = phoneNumber.replace(" ", "");
        phoneNumber = phoneNumber.replace("-", "");
        Intent delete = new Intent();
        delete.putExtra("extra_state", 2);
        delete.putExtra("extra_phone_number", phoneNumber);
        delete.putExtra("extra_name", contact.getContactName());
        byte[] bytes = contact.getPhoto();
        if (bytes != null) {
            delete.putExtra("extra_photo", BitmapFactory.decodeByteArray(contact.getPhoto(), 0, bytes.length));
        }
        delete.setAction("com.clouder.watch.ACTION_CONTACTS_CHANGE");
        sendBroadcast(delete);
        Log.d(TAG, "send broadcast : " + "delete");
    }

    private void sendMessageRequest(final String url) {
        NodeApi.GetConnectedNodesResult result = Wearable.NodeApi.getConnectedNodes(mobvoiApiClient).await();
        if (result != null) {
            List<Node> nodes = result.getNodes();
            if (nodes != null && !nodes.isEmpty()) {
                for (Node node : nodes) {
                    Log.d(TAG, "nodes.size:" + nodes.size());
                    Log.d(TAG, "nodeId : + " + node.getId() + " | displayName : " + node.getDisplayName());
                    MessageApi.SendMessageResult sendMessageResult = Wearable.MessageApi.sendMessage(mobvoiApiClient, node.getId(), url, "aaa".getBytes()).await();
                    boolean isSuccess = sendMessageResult.getStatus().isSuccess();
                    Log.d(TAG, "请求消息是否发送成功：" + isSuccess);
                }
            } else {
                Log.e(TAG, "no nodes");
            }
        } else {
            Log.e(TAG, "no nodes");
        }
    }

    private long getContactId(String phoneNumber) {
        resolver = mContext.getContentResolver();
        long contactId = 0;
        Log.d(TAG, "getContactId phoneNumber = " + phoneNumber);
        Uri uri = Uri.parse("content://com.android.contacts/" + "data/phones/filter/" + phoneNumber);
        Cursor cursor = resolver.query(uri, null, null, null, null);
        Log.d(TAG, "cursor.isFirst() = " + cursor.isFirst());
        if (cursor.moveToFirst()) {
            contactId = cursor.getLong(cursor.getColumnIndex(Data.CONTACT_ID));
            Log.d(TAG, "RAW_CONTACT_ID = " + contactId);
            Log.d(TAG, "CONTACT_ID = " + cursor.getLong(cursor.getColumnIndex(Data.CONTACT_ID)));
        }
        cursor.close();
        return contactId;
    }

    static class Contact {

        private byte[] photo;

        private String contactName;

        private String phoneNumber;


        public Contact(byte[] data) {
            fromByteArray(data);
        }

        public byte[] getPhoto() {
            return photo;
        }

        public void setPhoto(byte[] photo) {
            this.photo = photo;
        }

        public String getContactName() {
            return contactName;
        }

        public void setContactName(String contactName) {
            this.contactName = contactName;
        }

        public String getPhoneNumber() {
            return phoneNumber;
        }

        public void setPhoneNumber(String phoneNumber) {
            this.phoneNumber = phoneNumber;
        }

        public byte[] toByteArray() {

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(bos);

            byte[] mData = new byte[]{};
            try {
                writeFields(dos);
                mData = bos.toByteArray();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    bos.flush();
                    bos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
            return mData;
        }

        private void writeFields(DataOutput dataOutput) throws IOException {
            dataOutput.writeUTF(this.contactName);
            dataOutput.writeUTF(this.phoneNumber);
            if (this.photo != null) {
                dataOutput.writeInt(this.photo.length);
                dataOutput.write(this.photo);
            } else {
                dataOutput.writeInt(0);
            }

        }

        public void fromByteArray(byte[] data) {

            ByteArrayInputStream bis = new ByteArrayInputStream(data);
            DataInputStream dis = new DataInputStream(bis);

            try {
                readFields(dis);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    bis.close();
                    dis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }

        public void readFields(DataInput dataInput) throws IOException {
            this.contactName = dataInput.readUTF();
            this.phoneNumber = dataInput.readUTF();
            int count = dataInput.readInt();
            if (count > 0) {
                this.photo = new byte[count];
                dataInput.readFully(photo);
            }
        }

        @Override
        public String toString() {
            return "Contact{" +
                    "photo=" + (photo == null) +
                    ", contactName='" + contactName + '\'' +
                    ", phoneNumber='" + phoneNumber + '\'' +
                    '}';
        }
    }
}
