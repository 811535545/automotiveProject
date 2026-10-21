package com.oem.car.core.data

import android.content.Context
import com.oem.car.core.account.AccountManager
import com.oem.car.core.adas.AdasManager
import com.oem.car.core.audio.CarAudioManagerDelegate
import com.oem.car.core.digitalkey.DigitalKeyManager
import com.oem.car.core.energy.EnergyManager
import com.oem.car.core.health.VehicleHealthManager
import com.oem.car.core.navigation.NavigationManager
import com.oem.car.core.phone.PhoneManager
import com.oem.car.core.power.CarPowerLifecycleManager
import com.oem.car.core.telematics.TelematicsManager
import com.oem.car.core.uxrestrictions.CarUxRestrictionsEngine
import com.oem.car.core.vehicleservice.VehiclePropertyProvider
import com.oem.car.core.vehicleservice.VehiclePropertyProviderFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

/**
 * 车载核心服务依赖注入容器 (Vehicle Service Locator / DI Container)。
 *
 * 提供了可解耦、可测试的依赖注入容器结构，替代硬编码单例。
 */
object VehicleServiceContainer {
    private val scope = CoroutineScope(Dispatchers.Default)

    var propertyProviderOverride: VehiclePropertyProvider? = null

    fun getPropertyProvider(context: Context): VehiclePropertyProvider {
        return propertyProviderOverride ?: VehiclePropertyProviderFactory.create(context)
    }

    val uxEngine: CarUxRestrictionsEngine by lazy { CarUxRestrictionsEngine() }
    val powerManager: CarPowerLifecycleManager by lazy { CarPowerLifecycleManager() }
    val audioDelegate: CarAudioManagerDelegate by lazy { CarAudioManagerDelegate() }
    val digitalKeyManager: DigitalKeyManager by lazy { DigitalKeyManager() }
    val telematicsManager: TelematicsManager by lazy { TelematicsManager(scope) }
    val adasManager: AdasManager by lazy { AdasManager() }
    val accountManager: AccountManager by lazy { AccountManager() }
    val navigationManager: NavigationManager by lazy { NavigationManager() }
    val phoneManager: PhoneManager by lazy { PhoneManager(scope) }
    val energyManager: EnergyManager by lazy { EnergyManager(scope) }
    val healthManager: VehicleHealthManager by lazy { VehicleHealthManager(scope) }
}
