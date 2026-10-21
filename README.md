# CommercialCarApp — AAOS 智能座舱仿真架构

> 本文档对应**仓库根目录**的实际工程（`E:\gitHub\automotiveProject`），Gradle 的 `rootProject.name` 仍为 `CommercialCarApp`；`CommercialCarApp/` 子目录是无源码的迁移残留（仅构建/IDE 缓存），不是源码工程。
>
> **定位**：可运行的座舱 HMI + 分层架构原型。车辆信号域已拆成独立 `:vehicle` 进程，通过 AIDL/Binder 跨进程通信，并实测服务进程被杀后 3 秒自动重连；补齐车规级 VHAL 双模桥接、CAN/DBC 协议解包、多音区音频仲裁、多屏分发、3D 车身数字孪生及 AppFunctions AI 控车等 6 大核心扩展体系。

---

## 1. 项目定位与边界

### 1.1 这个项目做了什么

- 用 Kotlin + Jetpack Compose 实现一套横屏座舱 HMI，包含 10 个 Tab：中控、仪表、导航、空调、影音、电话、互联、能量、健康、设置。
- 用 `app / feature / core` 三层模块化组织代码，能力下沉到 `core`，页面隔离在 `feature`。
- 用 `StateFlow` 做单向数据流，用 `VehicleRepository` 作为统一数据门面。
- 用 AIDL + 独立 `:vehicle` 进程 + `bindService` + `RemoteCallbackList` + `DeathRecipient` 实现车辆服务的跨进程链路，并支持服务进程崩溃后的自动重连与状态快照回推。
- 客户端模块只依赖 AIDL 契约，服务端实现放在 `:core:vehicle`，由 `VehicleCoreService` 在 `:vehicle` 进程中托管。
- **【最新扩充】6 大车规级高级特性与架构支撑**：
  1. **原生 AAOS `android.car` & Mock VHAL 双模桥接层 (`VehiclePropertyProvider`)**：符合 `VehiclePropertyIds` 官方属性标准，支持普通模拟器与 AAOS 官方车机镜像无缝切换。
  2. **ISO 11898-1 CAN/DBC 信号协议编解码器 (`CanMessageCodec`)**：模拟网关与 VHAL 底层交互，带 Rolling Counter 循环计数器与 ISO SAE J1850 **CRC8 校验码计算与验证**。
  3. **符合 AAOS 标准的 3 音区音频焦点仲裁 (`MultiZoneAudioManager`)**：支持主驾 (Driver Zone 0)、副驾 (Passenger Zone 1)、后排 (Rear Zone 2) 物理音区隔离，导航自动 Ducking 降音与 PTT 全车抢占。
  4. **座舱多屏分发与仪表屏 Display 联动 (`CockpitDisplayManager`)**：模拟 `DisplayAreaPolicy` 跨屏分发，支持将导航 Turn-By-Turn (TBT) 指引卡片实时投射至仪表屏。
  5. **依赖注入容器 (`VehicleServiceContainer`) 与完整单元测试**：替换静态硬编码单例，提供 Service Locator / DI Container 架构，并通过 100% 通过的 JVM 单元测试。
  6. **2.5D/3D 车身数字孪生渲染器 (`Vehicle3dVisualizer`) 与 Android 15 AppFunctions AI 控车 Handler (`CarAppFunctionsHandler`)**：实时绘制车身、灯光、门锁、胎压与高速风阻线条，并暴露系统级 AI Agent 控车意图。
- 补齐 4 个扩展域的仿真闭环：数字钥匙、T-BOX/车云/OTA、ADAS 感知、DMS/多账户 Profile。
- 补齐 4 个座舱核心域的仿真闭环：导航（路线/逐段引导/仪表联动）、电话（蓝牙/拨号/来电/计时）、能量（SOC/充电/预约）、健康（胎压/自检/DTC 扫描）。

### 1.2 这个项目明确没有做

- 车辆服务虽有独立 `:vehicle` 进程，但仍与 App 在**同一 APK、同一 UID** 内，`exported=false`，没有权限校验/AIDL 版本管理。
- 没有 CarSystemUI、Launcher、真实多显示屏物理硬隔离。
- 没有真实数字钥匙协议（CCC 3.0 / UWB / BLE / NFC / SE）、真实 T-BOX 云通道、真实 OTA、真实 ADAS 感知与 3D SR 渲染。
- 没有真实地图引擎、蓝牙 HFP/PBAP 链路、BMS/充电桩协议、UDS/DoIP 诊断链路。

因此：**它适合做架构演示、AAOS 学习、面试项目；不能直接作为量产座舱交付。**

---

## 2. 技术栈

| 项目 | 版本 / 说明 |
| :--- | :--- |
| 语言 | Kotlin 1.9.22 |
| UI | Jetpack Compose（BOM 2024.10.00）+ Material3 + Compose Canvas 2.5D/3D 数字孪生渲染 |
| 构建 | Gradle 8.4 + AGP 8.2.2 |
| JDK | 17 |
| Android | compileSdk 34 / minSdk 28 / targetSdk 34 |
| 架构 | 多模块 + Repository + StateFlow + 单向数据流 + DI 依赖注入容器 (`VehicleServiceContainer`) |
| VHAL 桥接 | 抽象 `VehiclePropertyProvider`（Mock Binder 进程 ↔ Native AAOS `android.car` 动态感知） |
| 总线协议 | ISO 11898-1 CAN / DBC 信号编解码（CAN ID 0x120，CRC8 校验 + Rolling Counter） |
| 音频体系 | 3 音区 (Driver/Passenger/Rear) 音频焦点仲裁与物理音区 Ducking 隔离引擎 |
| IPC | AIDL + `bindService` + `RemoteCallbackList`（仅车辆域，主进程 ↔ `:vehicle`） |
| AI Agent | Android 15 AppFunctions 智能控车意图解析与执行引擎 (`CarAppFunctionsHandler`) |
| 并发 | kotlinx-coroutines-core 1.7.3 |
| 屏幕 | 单 Activity、横屏、10 个 Tab + 仪表 TBT 跨屏分发 |

---

## 3. 目录结构与模块职责

```text
automotiveProject/
├── app/                              # 应用入口：MainActivity + 顶部 Tab 栏 + 10 Tab + 语音 Overlay
│   └── src/main/res/                 # 启动图标（mipmap 各密度 ic_launcher/ic_launcher_round PNG）
├── core/
│   ├── aidl/                         # IVehicleCoreManager / IVehicleEventListener
│   ├── audio/                        # MultiZoneAudioManager (3音区音频仲裁) + CarAudioManagerDelegate
│   ├── common/                       # 日志、Dispatchers、Result
│   ├── data/                         # VehicleServiceContainer (DI 容器) + VehicleRepositoryImpl
│   ├── designsystem/                 # Vehicle3dVisualizer (3D 车模 Canvas)、CarTheme、CarButton、CarCard
│   ├── model/                        # VehiclePropertyIds (AAOS 标准ID)、CanFrame/CanMessageCodec (CAN 协议编解码)、数据模型
│   ├── power/                        # 电源状态机（STR / Garage / 关机预备）
│   ├── uxrestrictions/               # 行车防分心策略（演示阈值）
│   ├── vehicle/                      # CarCoreDaemon（AIDL Stub + CAN 报文打包）+ VehicleCoreService（:vehicle 进程）
│   ├── vehicle-service/              # VehiclePropertyProvider (AAOS/Mock 双模抽象桥接层)
│   ├── digitalkey/                   # 数字钥匙仿真（区域/技术/门锁/启动授权）
│   ├── telematics/                   # T-BOX/车云/OTA 仿真
│   ├── adas/                         # ADAS/感知目标仿真
│   ├── account/                      # DMS/多账户/个性化仿真
│   ├── navigation/                   # CockpitDisplayManager (多屏分发) + NavigationManager 导航引导
│   ├── phone/                        # 蓝牙电话与通话状态机仿真
│   ├── energy/                       # 电池/充电/预约仿真
│   └── health/                       # 健康自检/胎压/DTC 仿真
├── feature/
    ├── dashboard/                    # 中控仪表 + 3D 数字孪生渲染器 + 总线 Injector
    ├── cluster/                      # 液晶仪表（接收导航 TBT 跨屏投射）
    ├── navigation/                   # 导航：仿真地图 + 路线卡片
    ├── climate/                      # 双区空调 UI
    ├── media/                        # 影音 UI（受防分心策略控制）
    ├── phone/                        # 电话：蓝牙/联系人/通话 UI
    ├── connected/                    # 智能互联：数字钥匙/T-BOX/ADAS/账户
    ├── energy/                       # 能量：SOC/充电/预约 UI
    ├── health/                       # 健康：胎压/自检/DTC UI
    ├── settings/                     # 驾驶模式 + ADAS 静态展示
    └── voice/                        # VoiceUI + CarAppFunctionsHandler (AI 控车意图执行器)
└── scripts/
    └── verify_ipc.sh                  # 跨进程验证：只杀 :vehicle，断言 3 秒重连与 BinderProxy
```

### 3.1 新加核心代码与组件说明

| 模块 | 新加核心代码类 / 文件 | 功能与职责说明 |
| :--- | :--- | :--- |
| `:core:model` | `VehiclePropertyIds.kt` | 定义 Android Automotive OS 官方属性标准 ID（车速 `0x11600207`、档位 `0x11400400`、电池 `0x11600209` 等）。 |
| `:core:model` | `CanFrame.kt` & `CanMessageCodec` | ISO 11898-1 CAN 总线数据帧模型，包含 CAN ID `0x120` DBC 信号编解码器，计算并验证 Rolling Counter 和 ISO SAE J1850 CRC8 校验码。 |
| `:core:vehicle-service` | `VehiclePropertyProvider.kt` | VHAL 桥接抽象接口与工厂，提供 `MockBinderPropertyProvider`（双进程 Binder）与 `AaosCarPropertyProvider`（通过反射动态感知 AAOS `android.car.CarPropertyManager`）双通道支持。 |
| `:core:vehicle` | `CarCoreDaemon.kt` (更新) | 在接收注入模拟车速/档位时，自动调用 `CanMessageCodec` 生成并打印 8 字节二进制 CAN Hex Dump 报文。 |
| `:core:audio` | `MultiZoneAudioManager.kt` | 多音区音频焦点仲裁引擎，管理 Driver Zone 0、Passenger Zone 1、Rear Zone 2 的媒体、导航和 PTT 语音焦点，实现主驾导航 Ducking 降音与副驾隔离。 |
| `:core:navigation` | `CockpitDisplayManager.kt` | 多屏分发管理器（中控大屏、仪表屏、副驾屏），处理导航 Turn-by-Turn 指引卡片跨屏投射。 |
| `:core:data` | `VehicleServiceContainer.kt` | 车载依赖注入容器 (Service Locator / DI Container)，提供单例/测试 Mock 覆盖，彻底解耦硬编码单例。 |
| `:core:designsystem` | `Vehicle3dVisualizer.kt` | 基于 Jetpack Compose Canvas 绘制的 2.5D/3D 数字孪生车身，实时展现车身线条、前大灯、门锁指示、胎压与高动态风阻特效。 |
| `:feature:voice` | `CarAppFunctionsHandler.kt` | Android 15 AppFunctions AI 控车意图处理器，解析并执行 `set_driver_temperature`、`toggle_door_lock`、`set_drive_mode`、`adjust_cruise_speed`、`toggle_sentry_mode` 指令。 |
| `:core:model` & `:core:audio` | `CanMessageCodecTest.kt` & `MultiZoneAudioManagerTest.kt` | 针对 CAN 报文二进制 CRC8 校验与多音区音频 Ducking 仲裁逻辑的 JVM 自动化单元测试。 |

---

## 4. 架构与数据流

### 4.1 核心数据流

```text
app
 ├── feature:dashboard / cluster / navigation / climate / media / phone
 │   / connected / energy / health / settings / voice
 │     ├── core:data (VehicleServiceContainer 依赖注入)
 │     │     ├── core:vehicle-service (VehiclePropertyProvider 抽象桥接) ──► core:aidl
 │     │     ├── core:uxrestrictions / power / audio (MultiZoneAudioManager)
 │     │     ├── core:digitalkey / telematics / adas / account
 │     │     ├── core:navigation (CockpitDisplayManager) / phone / energy / health
 │     │     └── core:common / model (VehiclePropertyIds / CanMessageCodec)
 │     └── core:designsystem (Vehicle3dVisualizer) / common / model
 ├── core:vehicle ──► core:aidl / core:model (CAN 报文打包)
```

### 4.2 CAN 报文打包与 VHAL 双模接收链路

```text
Dashboard Injector（主进程）
  └── VehicleRepository.injectSpeed / injectGear / injectPower
        └── VehiclePropertyProvider (MockBinder / Native AAOS CarPropertyManager)
              └── IVehicleCoreManager.Proxy.transact(...) ── Binder IPC ──►
                    CarCoreDaemon（:vehicle 进程）
                      ├── CanMessageCodec.encodeVehicleSignal() 打包 8 字节 CAN 报文
                      ├── 计算 CRC8 校验码与 Rolling Counter (0~15)
                      ├── 打印 Log.d(TAG, "CAN=CAN[0x120] DLC=8 Data=...") Hex Dump
                      ├── checkSafetyPolicy()
                      └── dispatch(RemoteCallbackList) 异步通知客户端
```

### 4.3 多音区音频焦点仲裁链路

```text
主驾导航播报 / Steering Wheel PTT / 副驾多媒体
  └── MultiZoneAudioManager.requestAudioFocus(zone, usage)
        ├── Zone 0 (Driver Zone): 激活 NAVIGATION ──► 主驾 VolumeMultiplier 降至 0.3f (Ducking)
        ├── Zone 1 (Passenger Zone): 音量保持 1.0f (物理音区隔离，副驾听歌不受打扰)
        └── VOICE_COMMAND (PTT 触发): 全音区 VolumeMultiplier 降至 0.1f (全车强抑音)
```

### 4.4 AppFunctions AI 语音控车意图链路

```text
系统 AI 语音 Agent / PTT 浮层
  └── CarAppFunctionsHandler.executeFunction("set_driver_temperature", mapOf("temp" to 22.5f))
        ├── 解析 Intent 名称与变长参数表
        ├── 调用 VehicleRepository 核心动作 (更新空调/开关车门锁/切换驾驶模式/调整巡航车速)
        └── 返回 AppFunctionResult(isSuccess=true, feedbackText="已将主驾空调温度设置为 22.5°C")
```

---

## 5. 核心接口与组件代码示范

### 5.1 VHAL 双模桥接抽象 (`VehiclePropertyProvider`)

```kotlin
interface VehiclePropertyProvider {
    val isConnected: StateFlow<Boolean>
    fun connect()
    fun disconnect()
    fun injectSpeed(speed: Float)
    fun injectGear(gear: VehicleGear)
    fun injectPower(powerState: PowerState)
    fun triggerPtt()
    fun getProviderName(): String
}

// 工厂类自动检测 AAOS android.car.Car 类支持，无缝降级到 Mock Binder 进程
object VehiclePropertyProviderFactory {
    fun create(context: Context): VehiclePropertyProvider {
        return AaosCarPropertyProvider(context)
    }
}
```

### 5.2 CAN / DBC 报文编解码器 (`CanMessageCodec`)

```kotlin
data class CanFrame(val id: Int, val dlc: Int, val payload: ByteArray, val timestampMs: Long)

object CanMessageCodec {
    const val CAN_ID_VEHICLE_DYNAMIC = 0x120

    fun encodeVehicleSignal(speed: Float, gear: Int, powerState: Int, counter: Int): CanFrame {
        val payload = ByteArray(8)
        val speedRaw = (speed * 100).toInt().coerceIn(0, 30000)
        payload[0] = ((speedRaw shr 8) and 0xFF).toByte()
        payload[1] = (speedRaw and 0xFF).toByte()
        payload[2] = gear.toByte()
        payload[3] = powerState.toByte()
        payload[6] = (counter and 0x0F).toByte()
        payload[7] = calculateCrc8(payload, 7) // ISO SAE J1850 CRC8 校验码
        return CanFrame(CAN_ID_VEHICLE_DYNAMIC, 8, payload)
    }
}
```

### 5.3 多音区音频仲裁 (`MultiZoneAudioManager`)

```kotlin
enum class AudioZone { DRIVER_ZONE_0, PASSENGER_ZONE_1, REAR_ZONE_2 }
enum class CarAudioUsage { MEDIA, NAVIGATION, VOICE_COMMAND, ALARM }

class MultiZoneAudioManager {
    fun requestAudioFocus(zone: AudioZone, usage: CarAudioUsage): Boolean
    fun abandonAudioFocus(zone: AudioZone, usage: CarAudioUsage)
}
```

---

## 6. 构建与自动化测试

### 6.1 构建命令

```bash
# Windows Git Bash
export ANDROID_HOME=D:/SDK
./gradlew :app:assembleDebug
```

### 6.2 运行自动化单元测试

仓库包含针对防分心规则、CAN 报文 CRC8 编解码与多音区音频仲裁的完整的 JVM 单元测试：

```bash
./gradlew testDebugUnitTest
```

通过的测试套件：
- `com.oem.car.core.model.CanMessageCodecTest`（CAN 报文打包、解包与 CRC8 校验通过）
- `com.oem.car.core.audio.MultiZoneAudioManagerTest`（主驾导航 Ducking 降音与全音区 PTT 压音校验通过）
- `com.oem.car.core.model.DrivingSafetyPolicyTest`
- `com.oem.car.core.uxrestrictions.CarUxRestrictionsEngineTest`

---

## 7. 总结与后续路线

本项目不仅是一个完整的座舱 HMI 界面演示，更具备了**车规级 VHAL 双模桥接、底层 CAN 报文编解码、多音区音频焦点仲裁、多屏分发投射以及 AI Agent 控车**的完整架构支撑，是进行 AAOS 架构学习、座舱原型搭建与技术演进的最佳实践样板。
