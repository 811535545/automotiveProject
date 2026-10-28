package com.oem.car.core.data

import android.content.Context
import com.oem.car.core.model.*
import kotlinx.coroutines.flow.StateFlow

/**
 * ============================================================================
 * 车载核心统一数据门面接口 (Vehicle Repository Facade Interface)
 * ============================================================================
 *
 * 【核心职责】
 * 1. 作为整个座舱 App 的单一数据源入口 (Single Source of Truth)；
 * 2. 聚合车辆底层信号（车速、档位、电源、防分心）、音频、空调、数字钥匙、T-BOX/OTA、
 *    ADAS 感知、DMS 账户、导航、蓝牙电话、能量管理与健康诊断等 10 多个领域子系统；
 * 3. 向上层 Feature UI 模块暴露响应式 `StateFlow` 数据流，向下层中间件与 VHAL 转发控制命令。
 */
interface VehicleRepository {

    /** 车载服务连接状态：true 表示已成功连上 :vehicle 进程或原生 AAOS 车机服务 */
    val isConnected: StateFlow<Boolean>

    /** 车速信号流（含数值、状态、时戳与降级看门狗状态） */
    val speedSignal: StateFlow<CarSignal<Float>>

    /** 档位信号流（P/R/N/D） */
    val gearSignal: StateFlow<CarSignal<VehicleGear>>

    /** 车辆电源状态信号流（RUNNING / STR_SLEEP / GARAGE_MODE / SHUTDOWN_PREPARE） */
    val powerSignal: StateFlow<CarSignal<PowerState>>

    /** 行车防分心策略状态（控制视频拦截、键盘禁用与列表最大长度限制） */
    val uxRestrictionState: StateFlow<UxRestrictionState>

    /** 多音区音频焦点与 PTT 语音状态 */
    val audioZoneState: StateFlow<AudioZoneState>

    /** 双区空调状态（主副驾温度、风量、AC 开关等） */
    val climateState: StateFlow<ClimateState>

    /** 驾驶模式状态（ECO / COMFORT / SPORT / CUSTOM） */
    val driveMode: StateFlow<DriveMode>

    /** CCC 3.0 数字钥匙状态（UWB/BLE/NFC、距离、门锁、启动授权） */
    val digitalKeyState: StateFlow<DigitalKeyState>

    /** T-BOX 与云端 OTA 升级状态 */
    val telematicsState: StateFlow<TelematicsState>

    /** ADAS 智驾感知与 SR 3D 渲染状态（NOA、巡航速度、周边感知目标列表） */
    val adasState: StateFlow<AdasPerceptionState>

    /** DMS 人脸识别与多账户驾驶员 Profile 偏好状态 */
    val userProfile: StateFlow<UserProfile>

    /** 导航与逐段引导路线状态 */
    val navigationState: StateFlow<NavigationState>

    /** 蓝牙电话与通话状态机 */
    val phoneState: StateFlow<PhoneState>

    /** 能量管理与 SOC 充电状态 */
    val energyState: StateFlow<EnergyState>

    /** 车辆健康诊断与 DTC 故障码状态 */
    val vehicleHealthState: StateFlow<VehicleHealthState>

    /** 导航目的地预设列表（公司、家、充电站等） */
    val navigationPresets: List<NavigationDestination>

    /**
     * 初始化仓库，绑定 Context 并启动底层车辆服务连接与信号看门狗。
     * 保证全局只执行一次。
     */
    fun initialize(context: Context)

    /** 注入模拟车速（客户端测试入口） */
    fun injectSpeed(speed: Float)

    /** 注入模拟档位 */
    fun injectGear(gear: VehicleGear)

    /** 注入模拟电源状态 */
    fun injectPower(powerState: PowerState)

    /** 触发方向盘 PTT 语音按键事件（抢占音频焦点 & Ducking 降音） */
    fun triggerPtt()

    /** 释放方向盘 PTT 语音按键事件 */
    fun releasePtt()

    /** 更新空调状态变换函数 */
    fun updateClimate(transform: (ClimateState) -> ClimateState)

    /** 设置驾驶模式 */
    fun setDriveMode(mode: DriveMode)

    /** 数字钥匙：模拟物理靠近车辆 */
    fun moveKeyCloser()

    /** 数字钥匙：模拟物理远离车辆 */
    fun moveKeyAway()

    /** 数字钥匙：循环切换通信技术 (UWB ➔ BLE ➔ NFC) */
    fun cycleKeyTech()

    /** 数字钥匙：切换全车车门锁止状态 */
    fun toggleDoorLock()

    /** T-BOX：切换哨兵模式 */
    fun toggleSentryMode()

    /** T-BOX：发送远程控制指令 */
    fun sendRemoteCommand(command: String)

    /** T-BOX：启动 OTA 差分升级流程 */
    fun startOtaUpdate()

    /** ADAS：切换 NOA 领航辅助状态 */
    fun toggleNoa()

    /** ADAS：切换车道保持状态 */
    fun toggleLaneKeep()

    /** ADAS：调整巡航设定车速 (delta: +5 / -5) */
    fun adjustCruiseSpeed(delta: Float)

    /** DMS：验证驾驶员人脸 Face ID */
    fun verifyDriverFace()

    /** DMS：循环切换座椅记忆位置 */
    fun cycleSeatPosition()

    /** DMS：调整驾驶员偏好温度 */
    fun adjustPreferredTemp(delta: Float)

    /** DMS：应用驾驶员个性化 Profile 到座舱（如将空调设为偏好温度） */
    fun applyUserProfile()

    /** 导航：开始导航到指定目的地 */
    fun startNavigation(destination: NavigationDestination)

    /** 导航：推进下一段导航指引 */
    fun advanceNavigation()

    /** 导航：停止并退出导航 */
    fun stopNavigation()

    /** 电话：切换蓝牙连接状态 */
    fun togglePhoneBluetooth()

    /** 电话：向指定联系人拨打电话 */
    fun dialContact(contact: Contact)

    /** 电话：模拟收到外部来电 */
    fun simulateIncomingCall(contact: Contact)

    /** 电话：接听来电 */
    fun answerCall()

    /** 电话：挂断电话 */
    fun hangUpCall()

    /** 电话：切换通话麦克风静音状态 */
    fun toggleCallMute()

    /** 能量：开始或停止充电 */
    fun toggleCharging()

    /** 能量：调整充电限值 (±5%) */
    fun adjustChargeLimit(delta: Int)

    /** 能量：切换预约充电 */
    fun toggleScheduledCharging()

    /** 能量：切换电池预热 */
    fun togglePreconditioning()

    /** 健康：扫描全车系统诊断与 DTC 故障码 */
    fun scanVehicleHealth()

    /** 健康：清空已存储的 DTC 故障码 */
    fun clearDtcs()
}
