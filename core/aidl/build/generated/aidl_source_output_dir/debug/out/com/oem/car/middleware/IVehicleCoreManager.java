/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package com.oem.car.middleware;
public interface IVehicleCoreManager extends android.os.IInterface
{
  /** Default implementation for IVehicleCoreManager. */
  public static class Default implements com.oem.car.middleware.IVehicleCoreManager
  {
    @Override public void registerListener(com.oem.car.middleware.IVehicleEventListener listener) throws android.os.RemoteException
    {
    }
    @Override public void unregisterListener(com.oem.car.middleware.IVehicleEventListener listener) throws android.os.RemoteException
    {
    }
    @Override public void injectMockSpeed(float speed) throws android.os.RemoteException
    {
    }
    @Override public void injectMockGear(int gear) throws android.os.RemoteException
    {
    }
    @Override public void injectMockPower(int state) throws android.os.RemoteException
    {
    }
    @Override public void triggerPttVoice() throws android.os.RemoteException
    {
    }
    @Override
    public android.os.IBinder asBinder() {
      return null;
    }
  }
  /** Local-side IPC implementation stub class. */
  public static abstract class Stub extends android.os.Binder implements com.oem.car.middleware.IVehicleCoreManager
  {
    /** Construct the stub at attach it to the interface. */
    public Stub()
    {
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an com.oem.car.middleware.IVehicleCoreManager interface,
     * generating a proxy if needed.
     */
    public static com.oem.car.middleware.IVehicleCoreManager asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof com.oem.car.middleware.IVehicleCoreManager))) {
        return ((com.oem.car.middleware.IVehicleCoreManager)iin);
      }
      return new com.oem.car.middleware.IVehicleCoreManager.Stub.Proxy(obj);
    }
    @Override public android.os.IBinder asBinder()
    {
      return this;
    }
    @Override public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) throws android.os.RemoteException
    {
      java.lang.String descriptor = DESCRIPTOR;
      if (code >= android.os.IBinder.FIRST_CALL_TRANSACTION && code <= android.os.IBinder.LAST_CALL_TRANSACTION) {
        data.enforceInterface(descriptor);
      }
      switch (code)
      {
        case INTERFACE_TRANSACTION:
        {
          reply.writeString(descriptor);
          return true;
        }
      }
      switch (code)
      {
        case TRANSACTION_registerListener:
        {
          com.oem.car.middleware.IVehicleEventListener _arg0;
          _arg0 = com.oem.car.middleware.IVehicleEventListener.Stub.asInterface(data.readStrongBinder());
          this.registerListener(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_unregisterListener:
        {
          com.oem.car.middleware.IVehicleEventListener _arg0;
          _arg0 = com.oem.car.middleware.IVehicleEventListener.Stub.asInterface(data.readStrongBinder());
          this.unregisterListener(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_injectMockSpeed:
        {
          float _arg0;
          _arg0 = data.readFloat();
          this.injectMockSpeed(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_injectMockGear:
        {
          int _arg0;
          _arg0 = data.readInt();
          this.injectMockGear(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_injectMockPower:
        {
          int _arg0;
          _arg0 = data.readInt();
          this.injectMockPower(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_triggerPttVoice:
        {
          this.triggerPttVoice();
          reply.writeNoException();
          break;
        }
        default:
        {
          return super.onTransact(code, data, reply, flags);
        }
      }
      return true;
    }
    private static class Proxy implements com.oem.car.middleware.IVehicleCoreManager
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
      @Override public void registerListener(com.oem.car.middleware.IVehicleEventListener listener) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeStrongInterface(listener);
          boolean _status = mRemote.transact(Stub.TRANSACTION_registerListener, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void unregisterListener(com.oem.car.middleware.IVehicleEventListener listener) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeStrongInterface(listener);
          boolean _status = mRemote.transact(Stub.TRANSACTION_unregisterListener, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void injectMockSpeed(float speed) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeFloat(speed);
          boolean _status = mRemote.transact(Stub.TRANSACTION_injectMockSpeed, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void injectMockGear(int gear) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(gear);
          boolean _status = mRemote.transact(Stub.TRANSACTION_injectMockGear, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void injectMockPower(int state) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(state);
          boolean _status = mRemote.transact(Stub.TRANSACTION_injectMockPower, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void triggerPttVoice() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_triggerPttVoice, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
    }
    static final int TRANSACTION_registerListener = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
    static final int TRANSACTION_unregisterListener = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
    static final int TRANSACTION_injectMockSpeed = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
    static final int TRANSACTION_injectMockGear = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
    static final int TRANSACTION_injectMockPower = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
    static final int TRANSACTION_triggerPttVoice = (android.os.IBinder.FIRST_CALL_TRANSACTION + 5);
  }
  public static final java.lang.String DESCRIPTOR = "com.oem.car.middleware.IVehicleCoreManager";
  public void registerListener(com.oem.car.middleware.IVehicleEventListener listener) throws android.os.RemoteException;
  public void unregisterListener(com.oem.car.middleware.IVehicleEventListener listener) throws android.os.RemoteException;
  public void injectMockSpeed(float speed) throws android.os.RemoteException;
  public void injectMockGear(int gear) throws android.os.RemoteException;
  public void injectMockPower(int state) throws android.os.RemoteException;
  public void triggerPttVoice() throws android.os.RemoteException;
}
