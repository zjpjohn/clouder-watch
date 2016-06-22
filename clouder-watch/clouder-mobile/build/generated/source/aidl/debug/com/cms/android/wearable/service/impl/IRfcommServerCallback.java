/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: D:\\SVN\\Clouder_Watch\\PJ_SRC\\clouder-watch\\clouder-mobile\\src\\main\\aidl\\com\\cms\\android\\wearable\\service\\impl\\IRfcommServerCallback.aidl
 */
package com.cms.android.wearable.service.impl;
public interface IRfcommServerCallback extends android.os.IInterface
{
/** Local-side IPC implementation stub class. */
public static abstract class Stub extends android.os.Binder implements com.cms.android.wearable.service.impl.IRfcommServerCallback
{
private static final java.lang.String DESCRIPTOR = "com.cms.android.wearable.service.impl.IRfcommServerCallback";
/** Construct the stub at attach it to the interface. */
public Stub()
{
this.attachInterface(this, DESCRIPTOR);
}
/**
 * Cast an IBinder object into an com.cms.android.wearable.service.impl.IRfcommServerCallback interface,
 * generating a proxy if needed.
 */
public static com.cms.android.wearable.service.impl.IRfcommServerCallback asInterface(android.os.IBinder obj)
{
if ((obj==null)) {
return null;
}
android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
if (((iin!=null)&&(iin instanceof com.cms.android.wearable.service.impl.IRfcommServerCallback))) {
return ((com.cms.android.wearable.service.impl.IRfcommServerCallback)iin);
}
return new com.cms.android.wearable.service.impl.IRfcommServerCallback.Stub.Proxy(obj);
}
@Override public android.os.IBinder asBinder()
{
return this;
}
@Override public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) throws android.os.RemoteException
{
switch (code)
{
case INTERFACE_TRANSACTION:
{
reply.writeString(DESCRIPTOR);
return true;
}
case TRANSACTION_onRFCOMMSocketReady:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
boolean _result = this.onRFCOMMSocketReady(_arg0);
reply.writeNoException();
reply.writeInt(((_result)?(1):(0)));
return true;
}
case TRANSACTION_onRFCOMMSocketConnected:
{
data.enforceInterface(DESCRIPTOR);
this.onRFCOMMSocketConnected();
reply.writeNoException();
return true;
}
case TRANSACTION_onRFCOMMSocketDisconnected:
{
data.enforceInterface(DESCRIPTOR);
int _arg0;
_arg0 = data.readInt();
java.lang.String _arg1;
_arg1 = data.readString();
this.onRFCOMMSocketDisconnected(_arg0, _arg1);
reply.writeNoException();
return true;
}
case TRANSACTION_onDataReceived:
{
data.enforceInterface(DESCRIPTOR);
byte[] _arg0;
_arg0 = data.createByteArray();
this.onDataReceived(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_onDataSent:
{
data.enforceInterface(DESCRIPTOR);
byte[] _arg0;
_arg0 = data.createByteArray();
this.onDataSent(_arg0);
reply.writeNoException();
return true;
}
}
return super.onTransact(code, data, reply, flags);
}
private static class Proxy implements com.cms.android.wearable.service.impl.IRfcommServerCallback
{
private android.os.IBinder mRemote;
Proxy(android.os.IBinder remote)
{
mRemote = remote;
}
@Override public android.os.IBinder asBinder()
{
return mRemote;
}
public java.lang.String getInterfaceDescriptor()
{
return DESCRIPTOR;
}
@Override public boolean onRFCOMMSocketReady(java.lang.String address) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
boolean _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(address);
mRemote.transact(Stub.TRANSACTION_onRFCOMMSocketReady, _data, _reply, 0);
_reply.readException();
_result = (0!=_reply.readInt());
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public void onRFCOMMSocketConnected() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_onRFCOMMSocketConnected, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void onRFCOMMSocketDisconnected(int cause, java.lang.String address) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(cause);
_data.writeString(address);
mRemote.transact(Stub.TRANSACTION_onRFCOMMSocketDisconnected, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void onDataReceived(byte[] bytes) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeByteArray(bytes);
mRemote.transact(Stub.TRANSACTION_onDataReceived, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void onDataSent(byte[] bytes) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeByteArray(bytes);
mRemote.transact(Stub.TRANSACTION_onDataSent, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
}
static final int TRANSACTION_onRFCOMMSocketReady = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
static final int TRANSACTION_onRFCOMMSocketConnected = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
static final int TRANSACTION_onRFCOMMSocketDisconnected = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
static final int TRANSACTION_onDataReceived = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
static final int TRANSACTION_onDataSent = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
}
public boolean onRFCOMMSocketReady(java.lang.String address) throws android.os.RemoteException;
public void onRFCOMMSocketConnected() throws android.os.RemoteException;
public void onRFCOMMSocketDisconnected(int cause, java.lang.String address) throws android.os.RemoteException;
public void onDataReceived(byte[] bytes) throws android.os.RemoteException;
public void onDataSent(byte[] bytes) throws android.os.RemoteException;
}
