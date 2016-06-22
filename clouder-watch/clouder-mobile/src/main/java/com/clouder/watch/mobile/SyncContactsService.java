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
import android.bluetooth.BluetoothAdapter;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;
import android.util.Log;

import com.cms.android.common.ConnectionResult;
import com.cms.android.common.api.MobvoiApiClient;
import com.cms.android.common.api.MobvoiApiClient.ConnectionCallbacks;
import com.cms.android.common.api.MobvoiApiClient.OnConnectionFailedListener;
import com.cms.android.common.api.ResultCallback;
import com.cms.android.common.api.Status;
import com.cms.android.wearable.DataApi;
import com.cms.android.wearable.DataEventBuffer;
import com.cms.android.wearable.MessageApi;
import com.cms.android.wearable.MessageEvent;
import com.cms.android.wearable.Node;
import com.cms.android.wearable.NodeApi;
import com.cms.android.wearable.NodeApi.GetConnectedNodesResult;
import com.cms.android.wearable.Wearable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

/**
 * ClassName: SynContactsService
 *
 * @author xing_peng
 * @description
 * @Date 2015-8-18
 */
public class SyncContactsService extends Service implements OnConnectionFailedListener, ConnectionCallbacks,
        MessageApi.MessageListener, NodeApi.NodeListener, DataApi.DataListener {
    private static final String TAG = "SyncContactsService";
    private MobvoiApiClient mobvoiApiClient;
    private ContentResolver resolver;
    public static final String SYN_CONTACTS_URL = "/call/contacts";
    public static final String RESET_CONTACTS_URL = "/call/contacts/reset";
    public static final String CONTACT_ADD = "/call/contacts/add";
    public static final String CONTACT_DELETE = "/call/contacts/delete";
    private static final int EXECUTION_TIME = 5000;
    private static final int CONTACT_CHANGE = 1;
    private ContactContentObservers contactobserver;
    private Handler mainHandler = new Handler();
    private List<ContactItem> oldIdList = new Vector<>();
    private List<byte[]> failIdList = new Vector<>();
    private List<ContactItem> newList;

    @Override
    public void onCreate() {
        mobvoiApiClient = new MobvoiApiClient.Builder(this).addApi(Wearable.API).addConnectionCallbacks(this).addOnConnectionFailedListener(this).build();
        mobvoiApiClient.connect();
        new Thread(new Runnable() {
            @Override
            public void run() {
                oldIdList = queryAllContacts();
            }
        }).start();
        contactobserver = new ContactContentObservers(SyncContactsService.this, mHandler);
        this.getContentResolver().registerContentObserver(RawContacts.CONTENT_URI, true, contactobserver);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand...........");
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        Log.d(TAG, "onMessageReceived path : " + messageEvent.getPath());
        if (messageEvent.getPath().equals("SYNC_FALSE")) {
            Log.d(TAG, "该手机联系人已同步，不需要重复同步");

        }
        if (messageEvent.getPath().equals("SYNC_TRUE")) {
            Log.d(TAG, "开始同步联系人");
            new Thread(new Runnable() {
                public void run() {
                    try {
                        Thread.sleep(EXECUTION_TIME);
                        sendPhoneContacts();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }).start();

        }
    }

    @Override
    public void onConnected(Bundle arg0) {
        Log.e(TAG, "mobile：CMS onConnected");
        Log.d(TAG, "添加NodeListener");
        Wearable.NodeApi.addListener(mobvoiApiClient, this).setResultCallback(new ResultCallback<Status>() {
            @Override
            public void onResult(Status status) {
                if (status.isSuccess()) {
                    Log.e(TAG, "添加NodeListener成功");
                } else {
                    Log.e(TAG, "添加NodeListener失败! status = " + status);
                }
            }
        });
        Log.d(TAG, "添加MessageListener");
        Wearable.MessageApi.addListener(mobvoiApiClient, this).setResultCallback(new ResultCallback<Status>() {
            @Override
            public void onResult(Status status) {
                if (status.isSuccess()) {
                    Log.e(TAG, "添加MessageListener成功!");
                } else {
                    Log.e(TAG, "添加MessageListener失败! status = " + status);
                }
            }
        });
        Log.d(TAG, "添加DataListener");
        Wearable.DataApi.addListener(mobvoiApiClient, this).setResultCallback(new ResultCallback<Status>() {
            @Override
            public void onResult(Status status) {
                if (status.isSuccess()) {
                    Log.e(TAG, "添加DataListener成功!");
                } else {
                    Log.e(TAG, "添加DataListener失败! status = " + status);
                }
            }
        });

        NodeApi.GetConnectedNodesResult result = Wearable.NodeApi.getConnectedNodes(mobvoiApiClient).await();
        if (result != null) {
            if (result.getStatus().isSuccess()) {
                List<Node> nodes = result.getNodes();
                if (nodes != null && !nodes.isEmpty()) {
//                    Log.d(TAG, "在onConnected时同步联系人......");
//                    TimerTask task = new TimerTask() {
//                        public void run() {
//                            new Thread(new Runnable() {
//                                @Override
//                                public void run() {
//                                    sendMessageRequest("SYNC_CONTACTS_REQUEST");
//                                }
//                            }).start();
//                        }
//                    };
//                    Timer timer = new Timer();
//                    timer.schedule(task, 10000);
//                    new Thread(new Runnable() {
//                        public void run() {
//                            try {
//                                Thread.sleep(EXECUTION_TIME);
//                                sendPhoneContacts();
//                            } catch (InterruptedException e) {
//                                e.printStackTrace();
//                            }
//                        }
//                    }).start();
                } else {
                    Log.e(TAG, "(nodes = null || nodes.isEmpty()");
                }
            } else {
                Log.e(TAG, "result.getStatus()为false");
            }
        } else {
            Log.e(TAG, "result为null");
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Wearable.MessageApi.removeListener(mobvoiApiClient, this);
        Wearable.NodeApi.removeListener(mobvoiApiClient, this);
        Wearable.DataApi.removeListener(mobvoiApiClient, this);
        mobvoiApiClient.disconnect();
    }

    @Override
    public void onConnectionSuspended(int arg0) {
        Log.e(TAG, "mobile：CMS onConnectionSuspended cause:" + arg0);
        Log.e(TAG, "Mobvoi api client connection was suspended! mobvoi api client will try reconnect! ");
        reconnect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult arg0) {
        Log.e(TAG, "mobile：CMS onConnectionFailed" + (arg0 == null ? "" : arg0.toString()));
        if (arg0 != null && arg0.getErrorCode() == 9) {
            reconnect();
        }
    }

    private void sendMessages(final String url, final byte[] content) {
        GetConnectedNodesResult result = Wearable.NodeApi.getConnectedNodes(mobvoiApiClient).await();
        if (result != null) {
            List<Node> nodes = result.getNodes();
            if (nodes != null && !nodes.isEmpty()) {
                for (Node node : nodes) {
                    Log.d(TAG, "nodes.size:" + nodes.size());
                    Log.d(TAG, "nodeId : + " + node.getId() + " | displayName : " + node.getDisplayName());
                    MessageApi.SendMessageResult sendMessageResult = Wearable.MessageApi.sendMessage(mobvoiApiClient, node.getId(), url, content).await();
                    boolean isSuccess = sendMessageResult.getStatus().isSuccess();
                    Log.d(TAG, "消息是否发送成功：" + isSuccess);
                    if (isSuccess && failIdList != null) {
                        for (int i = 0; i < failIdList.size(); i++) {
                            if (Arrays.equals(content, failIdList.get(i))) {
                                failIdList.remove(i);
                            }

                        }
                    }
                    if (!isSuccess) {
                        Log.d(TAG, "重新发送该联系人");
                        failIdList.add(content);
                        continue;
                    }
                }
            } else {
                Log.e(TAG, "no nodes");
            }
        } else {
            Log.e(TAG, "no nodes");
        }
    }

    private void sendMessageRequest(final String url) {
        GetConnectedNodesResult result = Wearable.NodeApi.getConnectedNodes(mobvoiApiClient).await();
        if (result != null) {
            List<Node> nodes = result.getNodes();
            if (nodes != null && !nodes.isEmpty()) {
                for (Node node : nodes) {
                    Log.d(TAG, "nodes.size:" + nodes.size());
                    Log.d(TAG, "nodeId : + " + node.getId() + " | displayName : " + node.getDisplayName());
                    MessageApi.SendMessageResult sendMessageResult = Wearable.MessageApi.sendMessage(mobvoiApiClient, node.getId(), url, BluetoothAdapter.getDefaultAdapter().getAddress().getBytes()).await();
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

    private void sendMessage(final String url, final byte[] content) {
        GetConnectedNodesResult result = Wearable.NodeApi.getConnectedNodes(mobvoiApiClient).await();
        if (result != null) {
            List<Node> nodes = result.getNodes();
            if (nodes != null && !nodes.isEmpty()) {
                for (Node node : nodes) {
                    Log.d(TAG, "nodes.size:" + nodes.size());
                    Log.d(TAG, "nodeId : + " + node.getId() + " | displayName : " + node.getDisplayName());
                    MessageApi.SendMessageResult sendMessageResult = Wearable.MessageApi.sendMessage(mobvoiApiClient, node.getId(), url, content).await();
                    boolean isSuccess = sendMessageResult.getStatus().isSuccess();
                    Log.d(TAG, "消息是否发送成功：" + isSuccess);
                    if (!isSuccess) {
                        //TODO
                        Log.d(TAG, "重新发送该联系人");
                    }
                }
            } else {
                Log.e(TAG, "no nodes");
            }
        } else {
            Log.e(TAG, "no nodes");
        }
    }

    private void sendContacts(String path, List<Contact> contacts) {
        Log.d(TAG, "Send Contacts, Size = " + contacts.size());
        for (Contact contact : contacts) {
            Log.d(TAG, "发送联系人：" + ",name:" + contact.getContactName() + ",num:" + contact.getPhoneNumber());
            sendMessages(path, contact.toByteArray());
        }
    }

    private List<Contact> convert(List<ContactItem> contactItems) {
        List<Contact> contactList = new ArrayList<>();
        for (ContactItem item : contactItems) {
            Contact contact = new Contact();
            contact.setContactName(item.name);
            contact.setPhoneNumber(item.number);
            Bitmap photo = getPhoto(item.number);
            if (photo != null) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                photo.compress(Bitmap.CompressFormat.PNG, 100, baos);
                contact.setPhoto(baos.toByteArray());
            }
            contactList.add(contact);
        }
        return contactList;
    }

    private void sendPhoneContacts() {
        sendMessage(RESET_CONTACTS_URL, new byte[]{0});
        new Thread(new Runnable() {
            @Override
            public void run() {
                List<ContactItem> contactItems = queryAllContacts();
                List<Contact> contactList = convert(contactItems);
                sendContacts(SYN_CONTACTS_URL, contactList);
            }
        }).start();
    }

    private List<ContactItem> queryAllContacts() {
        List<ContactItem> contactItems = new ArrayList<>();
        ContentResolver resolver = getContentResolver();
        Cursor cursor = resolver.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                //获取contactId
                String contactId = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));
                String displayName = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                Log.d(TAG, "contact id = " + contactId + ", display name = " + displayName);
                //根据contactId获取version
                Cursor rawsCursor = getContentResolver().query(ContactsContract.RawContacts.CONTENT_URI,
                        new String[]{RawContacts._ID, ContactsContract.RawContacts.VERSION, ContactsContract.RawContacts.CONTACT_ID}, ContactsContract.RawContacts.CONTACT_ID + " =  ? and " + ContactsContract.RawContacts.DELETED + " = ?", new String[]{contactId, "0"}, null);
                if (rawsCursor != null) {
                    while (rawsCursor.moveToNext()) {
                        String rawVersion = rawsCursor.getString(rawsCursor.getColumnIndex(ContactsContract.RawContacts.VERSION));
                        String rawContactId = rawsCursor.getString(rawsCursor.getColumnIndex(ContactsContract.RawContacts._ID));
                        Log.d(TAG, "raw version = " + rawVersion + ", raw contact id = " + rawContactId);
                        Cursor dataCursor = resolver.query(
                                Data.CONTENT_URI, //查询data表
                                new String[]{Phone.NUMBER}, Data.RAW_CONTACT_ID + " = ? and " + Data.MIMETYPE + " = ?", new String[]{rawContactId, "vnd.android.cursor.item/phone_v2"}, null);
                        if (dataCursor != null) {
                            while (dataCursor.moveToNext()) {
                                String number = dataCursor.getString(dataCursor.getColumnIndex(Phone.NUMBER));
                                ContactItem item = new ContactItem(contactId, rawVersion, displayName, number);
                                Log.d(TAG, String.format("----Add Contact Item----, contact id = [%s], raw contact id = [%s], name = [%s], number = [%s],version = [%s]", contactId, rawContactId, displayName, number, rawVersion));
                                contactItems.add(item);
                            }
                            dataCursor.close();
                        }
                    }
                    rawsCursor.close();
                }
            }
            cursor.close();
        }
        return contactItems;
    }

    private static class ContactItem {
        public String id;
        public String version;
        public String name;
        public String number;

        public ContactItem(String id, String version, String name, String number) {
            this.id = id;
            this.version = version;
            this.name = name;
            this.number = number;
        }
    }

    private void onContactsChange() {
        Log.d(TAG, "联系人库改变");
        Log.d(TAG, "OLD List");
        showContacts(oldIdList);
        new Thread(new Runnable() {
            @Override
            public void run() {
                newList = queryAllContacts();
                Log.d(TAG, "NEW List");
                showContacts(newList);
                List<ContactItem> addList = queryNewContacts(oldIdList, newList);
                sendContacts(CONTACT_ADD, convert(addList));
                List<ContactItem> deleteList = queryDeleteContacts(oldIdList, newList);
                sendContacts(CONTACT_DELETE, convert(deleteList));
                List<ContactItem> needDelete = new ArrayList<>();
                List<ContactItem> needAdd = new ArrayList<>();
                queryUpdateContacts(oldIdList, newList, needDelete, needAdd);
                Log.d(TAG, "NEED DELETE LIST");
                showContacts(needDelete);
                if (!needDelete.isEmpty()) {
                    sendContacts(CONTACT_DELETE, convert(needDelete));
                }
                Log.d(TAG, "NEED ADD LIST");
                showContacts(needAdd);
                if (!needAdd.isEmpty()) {
                    sendContacts(CONTACT_ADD, convert(needAdd));
                }
                oldIdList = newList;
            }
        }).start();
    }

    private void showContacts(List<ContactItem> list) {
        for (ContactItem item : list) {
            Log.d(TAG, "Contact, id = " + item.id + ", version = " + item.version + ", name = " + item.name + ", number = " + item.number);
        }
    }

    //新增
    public List<ContactItem> queryNewContacts(List<ContactItem> old, List<ContactItem> newList) {
        List<ContactItem> list = new ArrayList<>();
        for (int i = 0; i < newList.size(); i++) {
            boolean add = true;
            for (int j = 0; j < old.size(); j++) {
                if (newList.get(i).id.equals(old.get(j).id)) {
                    add = false;
                    break;
                }
            }
            if (add) {
                list.add(newList.get(i));
            }
        }
        return list;
    }

    //删除
    public List<ContactItem> queryDeleteContacts(List<ContactItem> old, List<ContactItem> newList) {
        List<ContactItem> list = new ArrayList<>();
        for (int i = 0; i < old.size(); i++) {
            boolean delete = true;
            for (int j = 0; j < newList.size(); j++) {
                if (newList.get(j).id.equals(old.get(i).id)) {
                    delete = false;
                    break;
                }
            }
            if (delete) {
                list.add(old.get(i));
            }
        }
        return list;
    }

    //修改
    public void queryUpdateContacts(List<ContactItem> old, List<ContactItem> newList, List<ContactItem> needDelete, List<ContactItem> needAdd) {
        for (int i = 0; i < newList.size(); i++) {
            boolean update = false;
            ContactItem newItem = newList.get(i);
            for (int j = 0; j < old.size(); j++) {
                ContactItem oldItem = old.get(j);
                if (newItem.id.equals(oldItem.id) && !newItem.version.equals(oldItem.version)) {
                    update = true;
                    break;
                }
            }
            if (update) {
                if (!contactExistById(newItem.id, needDelete)) {
                    for (int m = 0; m < old.size(); m++) {
                        if (old.get(m).id.equals(newItem.id)) {
                            needDelete.add(old.get(m));
                        }
                    }
                }
                if (!contactExistById(newItem.id, needAdd)) {
                    for (int n = 0; n < newList.size(); n++) {
                        if (newList.get(n).id.equals(newItem.id)) {
                            needAdd.add(newList.get(n));
                        }
                    }
                }
                Log.d(TAG, "修改联系人：" + ",name:" + newItem.name + ",number:" + newItem.number);
            }
        }
    }

    private boolean contactExistById(String id, List<ContactItem> items) {
        for (ContactItem item : items) {
            if (item.id.equals(id)) {
                return true;
            }
        }
        return false;
    }

    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case CONTACT_CHANGE:
                    new Thread(new Runnable() {
                        public void run() {
                            onContactsChange();
                        }
                    }).start();
                    break;
                default:
                    break;
            }
        }
    };

    /**
     * get head photo
     */
    private Bitmap getPhoto(String phoneNumber) {
        Log.d(TAG, "获取联系人 num : " + phoneNumber);
        Bitmap bitmap = null;
        resolver = getContentResolver();
        long contactId = 0;
        Uri uri = Uri.parse("content://com.android.contacts/" + "data/phones/filter/" + phoneNumber);
        Cursor cursor = resolver.query(uri, null, null, null, null);
        if (cursor.moveToFirst()) {
            contactId = cursor.getLong(cursor.getColumnIndex("contact_id"));
            Uri contentUri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId);
            InputStream input = ContactsContract.Contacts.openContactPhotoInputStream(resolver, contentUri);
            if (input != null) {
                bitmap = BitmapFactory.decodeStream(input);
            }
            cursor.close();
        }

        return bitmap;
    }

    @Override
    public void onPeerConnected(Node node) {
        Log.e(TAG, "node " + node.getId() + " onPeerConnected,");
        Log.e(TAG, "在onPeerConnected中同步联系人......");

        TimerTask task = new TimerTask() {
            public void run() {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        sendMessageRequest("SYNC_CONTACTS_REQUEST");
                    }
                }).start();
            }
        };
        Timer timer = new Timer();
        timer.schedule(task, 20000);
//        new Thread(new Runnable() {
//            public void run() {
//                try {
//                    Thread.sleep(EXECUTION_TIME);
//                    sendPhoneContacts();
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            }
//        }).start();
    }

    @Override
    public void onPeerDisconnected(Node node) {
        Log.e(TAG, "node " + node.getId() + " onPeerDisconnected");
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEventBuffer) {

    }

    static class Contact {
        private byte[] photo;
        private String contactName;

        private String phoneNumber;

        public Contact() {
        }

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
    }


    /**
     * 重连
     */
    public void reconnect() {
        Wearable.MessageApi.removeListener(mobvoiApiClient, this);
        Wearable.NodeApi.removeListener(mobvoiApiClient, this);
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

    /**
     * 判断号码是否为空
     */
    public static boolean isNumeric(String str) {
        for (int i = str.length(); --i >= 0; ) {
            if (!Character.isDigit(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }
}