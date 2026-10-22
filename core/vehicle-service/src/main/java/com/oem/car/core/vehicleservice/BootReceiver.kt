package com.oem.car.core.vehicleservice

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.oem.car.core.common.CarLogger

/**
 * 车载核心中间件开机自启广播接收器 (Automotive Boot & Direct Boot Receiver)。
 *
 * 职责：
 * 在系统开机完成 (BOOT_COMPLETED) 或设备解锁前 (LOCKED_BOOT_COMPLETED - Direct Boot 阶段)，
 * 自动拉起座舱中间件，建立与 :vehicle 进程或原生 AAOS CarPropertyManager 的 Binder 链接，
 * 并启动信号看门狗，确保车辆信号在开机第一时间即可被监控分发。
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        CarLogger.i(TAG, "BootReceiver onReceive action=$action")

        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_LOCKED_BOOT_COMPLETED ||
            action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            CarLogger.i(TAG, "Vehicle system booted! Auto-starting Cockpit Middleware Services...")

            // 1. 自动启动信号超时降级看门狗
            VehiclePropertyAdapter.startSignalTimeoutWatchdog()

            // 2. 自动建立 VHAL / Mock Binder 车辆服务连接
            val provider = VehiclePropertyProviderFactory.create(context.applicationContext)
            provider.connect()
        }
    }

    companion object {
        private const val TAG = "CockpitBootReceiver"
    }
}
