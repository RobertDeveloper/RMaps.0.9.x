/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: C:\\Projects\\EclipseWorkspaceE\\RMaps\\src\\com\\robert\\maps\\trackwriter\\ITrackWriterCallback.aidl
 */
package com.robert.maps.trackwriter;
public interface ITrackWriterCallback extends android.os.IInterface
{
/** Local-side IPC implementation stub class. */
public static abstract class Stub extends android.os.Binder implements com.robert.maps.trackwriter.ITrackWriterCallback
{
private static final java.lang.String DESCRIPTOR = "com.robert.maps.trackwriter.ITrackWriterCallback";
/** Construct the stub at attach it to the interface. */
public Stub()
{
this.attachInterface(this, DESCRIPTOR);
}
/**
 * Cast an IBinder object into an com.robert.maps.trackwriter.ITrackWriterCallback interface,
 * generating a proxy if needed.
 */
public static com.robert.maps.trackwriter.ITrackWriterCallback asInterface(android.os.IBinder obj)
{
if ((obj==null)) {
return null;
}
android.os.IInterface iin = (android.os.IInterface)obj.queryLocalInterface(DESCRIPTOR);
if (((iin!=null)&&(iin instanceof com.robert.maps.trackwriter.ITrackWriterCallback))) {
return ((com.robert.maps.trackwriter.ITrackWriterCallback)iin);
}
return new com.robert.maps.trackwriter.ITrackWriterCallback.Stub.Proxy(obj);
}
public android.os.IBinder asBinder()
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
case TRANSACTION_newPointWrited:
{
data.enforceInterface(DESCRIPTOR);
double _arg0;
_arg0 = data.readDouble();
double _arg1;
_arg1 = data.readDouble();
this.newPointWrited(_arg0, _arg1);
return true;
}
}
return super.onTransact(code, data, reply, flags);
}
private static class Proxy implements com.robert.maps.trackwriter.ITrackWriterCallback
{
private android.os.IBinder mRemote;
Proxy(android.os.IBinder remote)
{
mRemote = remote;
}
public android.os.IBinder asBinder()
{
return mRemote;
}
public java.lang.String getInterfaceDescriptor()
{
return DESCRIPTOR;
}
/**
     * Called when the service has a new value for you.
     */
public void newPointWrited(double lat, double lon) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeDouble(lat);
_data.writeDouble(lon);
mRemote.transact(Stub.TRANSACTION_newPointWrited, _data, null, android.os.IBinder.FLAG_ONEWAY);
}
finally {
_data.recycle();
}
}
}
static final int TRANSACTION_newPointWrited = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
}
/**
     * Called when the service has a new value for you.
     */
public void newPointWrited(double lat, double lon) throws android.os.RemoteException;
}
