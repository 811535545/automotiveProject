/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package com.oem.car.middleware;
public interface IVehicleEventListener extends android.os.IInterface
{
  /** Default implementation for IVehicleEventListener. */
  public static class Default implements com.oem.car.middleware.IVehicleEventListener
  {
    @Override public void onSpeedChanged(float speed) throws android.os.RemoteException
    {
    }
    @Override public void onGearChanged(int gear) throws android.os.RemoteException
    {
    }
    @Override public void onPowerStateChanged(int state) throws android.os.RemoteException
    {
    }
    @Override public void onDistractionStateChanged(boolean isRestricted) throws android.os.RemoteException
    {
    }
    @Override
    public android.os.IBinder asBinder() {
      return null;
    }
  }
  /** Local-side IPC implementation stub class. */
  public static abstract class Stub extends android.os.Binder implements com.oem.car.middleware.IVehicleEventListener
  {
    /** Construct the stub at attach it to the interface. */
    public Stub()
    {
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an com.oem.car.middleware.IVehicleEventListener interface,
     * generating a proxy if needed.
     */
    public static com.oem.car.middleware.IVehicleEventListener asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof com.oem.car.middleware.IVehicleEventListener))) {
        return ((com.oem.car.middleware.IVehicleEventListener)iin);
      }
      return new com.oem.car.middleware.IVehicleEventListener.Stub.Proxy(obj);
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
        case TRANSACTION_onSpeedChanged:
        {
          float _arg0;
          _arg0 = data.readFloat();
          this.onSpeedChanged(_arg0);
          break;
        }
        case TRANSACTION_onGearChanged:
        {
          int _arg0;
          _arg0 = data.readInt();
          this.onGearChanged(_arg0);
          break;
        }
        case TRANSACTION_onPowerStateChanged:
        {
          int _arg0;
          _arg0 = data.readInt();
          this.onPowerStateChanged(_arg0);
          break;
        }
        case TRANSACTION_onDistractionStateChanged:
        {
          boolean _arg0;
          _arg0 = (0!=data.readInt());
          this.onDistractionStateChanged(_arg0);
          break;
        }
        default:
        {
          return super.onTransact(code, data, reply, flags);
        }
      }
      return true;
    }
    private static class Proxy implements com.oem.car.middleware.IVehicleEventListener
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
      @Override public void onSpeedChanged(float speed) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeFloat(speed);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onSpeedChanged, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void onGearChanged(int gear) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(gear);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onGearChanged, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void onPowerStateChanged(int state) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(state);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onPowerStateChanged, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void onDistractionStateChanged(boolean isRestricted) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(((isRestricted)?(1):(0)));
          boolean _status = mRemote.transact(Stub.TRANSACTION_onDistractionStateChanged, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
    }
    static final int TRANSACTION_onSpeedChanged = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
    static final int TRANSACTION_onGearChanged = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
    static final int TRANSACTION_onPowerStateChanged = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
    static final int TRANSACTION_onDistractionStateChanged = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
  }
  public static final java.lang.String DESCRIPTOR = "com.oem.car.middleware.IVehicleEventListener";
  public void onSpeedChanged(float speed) throws android.os.RemoteException;
  public void onGearChanged(int gear) throws android.os.RemoteException;
  public void onPowerStateChanged(int state) throws android.os.RemoteException;
  public void onDistractionStateChanged(boolean isRestricted) throws android.os.RemoteException;
}
