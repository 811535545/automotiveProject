package com.oem.car.middleware;

import com.oem.car.middleware.IVehicleEventListener;

interface IVehicleCoreManager {
    void registerListener(IVehicleEventListener listener);
    void unregisterListener(IVehicleEventListener listener);
    void injectMockSpeed(float speed);
    void injectMockGear(int gear);
    void injectMockPower(int state);
    void triggerPttVoice();
}
