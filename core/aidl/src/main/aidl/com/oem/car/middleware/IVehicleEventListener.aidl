package com.oem.car.middleware;

oneway interface IVehicleEventListener {
    void onSpeedChanged(float speed);
    void onGearChanged(int gear);
    void onPowerStateChanged(int state);
    void onDistractionStateChanged(boolean isRestricted);
}
