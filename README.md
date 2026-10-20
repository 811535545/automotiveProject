# CommercialCarApp - AAOS 智能座舱中间件与应用项目文档

本文档详细介绍了 CommercialCarApp 项目的目录结构、各模块作用注释、项目入口点及核心接口 API 说明。

---

## 📁 1. 项目目录结构与模块说明

项目采用了模块化的 Android 车载架构，划分了 `:app` 入口、`:core` 核心能力层以及 `:feature` 业务功能层。

```
CommercialCarApp/
├── app/                      # [App入口模块] 主 Activity、Cockpit 导航与 Tab 切换容器
├── core/                     # [核心中间件层]
│   ├── aidl/                 # 跨进程 Binder IPC 通信 AIDL 接口定义
│   ├── audio/                # 车载多区音频、PTT 语音按抢占与音量 Ducking 管理
│   ├── common/               # 全局协程 Dispatcher、Logcat 统一日志与 Result 封装
│   ├── data/                 # 车辆数据仓库层 (VehicleRepository)
│   ├── designsystem/         # 车规级 UI 设计系统 (CarTheme, CarButton, Gauge 仪表盘)
│   ├── model/                # 座舱数据模型 (信号状态、档位、电源模式、防分心状态等)
│   ├── power/                # 车辆电源生命周期管理 (STR 内存休眠、关机预备)
│   ├── uxrestrictions/       # 车规防分心安全引擎 (依据车速/档位执行 GB44495 拦截)
│   ├── vehicle/              # 进程内 Vehicle 服务仿真 Daemon (CarCoreDaemon)
│   └── vehicle-service/      # 车辆服务连接管理 (Binder DeathRecipient 死亡重连与属性适配)
└── feature/                  # [业务功能模块层]
    ├── climate/              # HVAC 双区空调与风量控制 UI
    ├── cluster/              # 液晶仪表屏 (Instrument Cluster Display) UI 与 TBT 导航卡片
    ├── dashboard/            # 中控台核心仪表、总线数据模拟注入器 (Injector) UI 与 ViewModel
    ├── media/                # 高精影音播放器 UI (受行车防分心策略管控)
    ├── settings/             # 车辆控制与个性化设置 (驾驶模式切换、ADAS 状态)
    └── voice/                # 智能语音助手 PTT 弹窗 Overlay 交互
```

### 模块详细作用说明

| 模块路径 | 模块名称 | 作用与职责说明 |
| :--- | :--- | :--- |
| `:app` | **应用入口** | 包含 `MainActivity`，负责初始化全局 `CarTheme`、搭建顶栏 Dock Bar 导航，管理 5 大功能页面的切换与 `VoiceAssistantOverlay` 的悬浮置顶。 |
| `:core:aidl` | **IPC 接口** | 定义 `IVehicleCoreManager.aidl` 与 `IVehicleEventListener.aidl`，是车辆服务与客户端 App 通信的 Binder 接口契约。 |
| `:core:audio` | **车载音频** | 管理座舱音频焦点，处理方向盘 PTT (Push-To-Talk) 语音按键事件，控制背景音乐音量 Ducking (压低)。 |
| `:core:common` | **通用工具** | 提供统一的协程调度器 (`CarDispatchers`)、车规日志工具 (`CarLogger`) 与标准结果封装 (`Result`)。 |
| `:core:data` | **数据仓库** | 统一数据出口 `VehicleRepository`，聚合来自 VehicleService、UXRestrictionsEngine、PowerManager 和 AudioManager 的状态。 |
| `:core:designsystem` | **设计系统** | 车规级深色主题色板、大颗粒防盲打控件 (`CarButton` / `CarCard`)、车速仪表盘 (`SpeedGaugeComponent`) 与安全标签 (`SafetyBadge`)。 |
| `:core:model` | **数据模型** | 包含 `CarSignal<T>`（带有效性状态）、`VehicleGear`（档位）、`PowerState`（电源状态）、`UxRestrictionState`（防分心策略）、`ClimateState`（空调）等。 |
| `:core:power` | **电源管理** | 监控车辆电源模式迁移（NORMAL 运行、STR 内存休眠挂起、Garage Mode 车库维护、关机预备），提供监听器注册。 |
| `:core:uxrestrictions`| **防分心引擎** | 实时评估车速与档位，当车速 > 0.5km/h 或挂入 D 档时，触发防分心规则（禁止播放视频、限制列表长度为 5-10 条）。 |
| `:core:vehicle` | **车辆服务 Daemon** | 运行在 Stub 层的单例 `CarCoreDaemon`，模拟车辆硬件 ECU/VHAL 推流，并维护 `RemoteCallbackList` 广播回调。 |
| `:core:vehicle-service`| **服务连接适配** | `VehicleServiceConnectionManager` 绑定 Binder 并设置 `IBinder.DeathRecipient` 死亡重连机制；`VehiclePropertyAdapter` 将 AIDL 回调转为响应式 `StateFlow`。 |
| `:feature:climate` | **空调功能** | 提供双区（主驾/副驾）独立温度调节、7 挡风量调节与 HVAC 状态展示。 |
| `:feature:cluster` | **液晶仪表** | 模拟二级独立显示屏，展示大字号车速、档位、TBT 导航卡片及 ADAS 巡航状态。 |
| `:feature:dashboard` | **中控仪表盘** | 展示座舱中间件状态，内置**总线仿真 Injector** 按钮面板（支持手动注入 P/D 档、车速、STR 休眠与 PTT 语音触发）。 |
| `:feature:media` | **多媒体影音** | 播放器 UI，自动感知防分心状态；行车中强制关闭视频并提示安全警告，仅保留音频播放。 |
| `:feature:settings` | **车辆设置** | 驾驶模式切换（ECO/舒适/运动/自定义）及 ADAS 辅助功能状态展示。 |
| `:feature:voice` | **语音助手** | 悬浮遮罩 UI，响应 PTT 事件弹窗，并展示音频焦点压低动画。 |

---

## 🚪 2. 项目入口与核心调用链 (Project Entry Point)

### 2.1 主入口文件
* **路径**: `app/src/main/java/com/oem/car/app/MainActivity.kt`
* **入口 Activity**: `MainActivity`
* **界面结构**:
  * 顶部导航栏 (Automotive Dock Bar): 64dp 高度，提供中控仪表、液晶仪表、空调 HVAC、多媒体影音、车辆设置 5 大 Tab 切换。
  * 悬浮覆盖层 (Overlay): `VoiceAssistantOverlay()` 全局置顶，用于响应语音 PTT 事件。

### 2.2 核心数据流架构 (Data Flow Architecture)

```
[UI 层: Dashboard / Climate / Media / Cluster]
        │
        ▼ (Collect StateFlow)
[ViewModel 层: DashboardViewModel]
        │
        ▼ (Expose Flows / Invoke Commands)
[仓库层: VehicleRepositoryImpl]
        ├──> [CarUxRestrictionsEngine] ──> (根据速度/档位评估防分心)
        ├──> [CarPowerLifecycleManager] ──> (处理电源模式转变)
        ├──> [CarAudioManagerDelegate] ──> (处理 PTT 与 Audio Ducking)
        └──> [VehicleServiceConnectionManager]
                     │
                     ▼ (Binder IPC / DeathRecipient)
         [VehiclePropertyAdapter] <──> [CarCoreDaemon (AIDL Stub)]
```

---

## 🔌 3. 核心接口文档 (API Reference)

### 3.1 IPC Binder AIDL 接口

#### 3.1.1 `IVehicleCoreManager.aidl`
车辆核心服务管理接口（服务端实现）。

```aidl
package com.oem.car.middleware;

import com.oem.car.middleware.IVehicleEventListener;

interface IVehicleCoreManager {
    // 注册车辆事件监听器
    void registerListener(IVehicleEventListener listener);
    
    // 注销车辆事件监听器
    void unregisterListener(IVehicleEventListener listener);
    
    // 仿真注入：注入车速 (km/h)
    void injectMockSpeed(float speed);
    
    // 仿真注入：注入档位代码 (1=P, 2=R, 4=N, 8=D)
    void injectMockGear(int gear);
    
    // 仿真注入：注入电源状态 (0=RUNNING, 1=STR_SLEEP, 2=GARAGE_MODE, 3=SHUTDOWN_PREPARE)
    void injectMockPower(int state);
    
    // 仿真注入：触发方向盘 PTT 语音按键
    void triggerPttVoice();
}
```

#### 3.1.2 `IVehicleEventListener.aidl`
车辆事件异步回调接口（客户端实现，单向异步 `oneway`）。

```aidl
package com.oem.car.middleware;

oneway interface IVehicleEventListener {
    // 车速变化回调
    void onSpeedChanged(float speed);
    
    // 档位变化回调
    void onGearChanged(int gear);
    
    // 电源模式变化回调
    void onPowerStateChanged(int state);
    
    // 行车安全防分心拦截状态变化回调
    void onDistractionStateChanged(boolean isRestricted);
}
```

---

### 3.2 车辆数据仓库接口 (`VehicleRepository.kt`)

**路径**: `core/data/src/main/java/com/oem/car/core/data/VehicleRepository.kt`

```kotlin
interface VehicleRepository {
    // ---- 状态流 (StateFlows) ----
    val isConnected: StateFlow<Boolean>                  // 车辆服务连接状态
    val speedSignal: StateFlow<CarSignal<Float>>        // 车速信号 (km/h)
    val gearSignal: StateFlow<CarSignal<VehicleGear>>   // 档位信号 (P/R/N/D)
    val powerSignal: StateFlow<CarSignal<PowerState>>   // 电源状态
    val uxRestrictionState: StateFlow<UxRestrictionState> // 防分心策略状态
    val audioZoneState: StateFlow<AudioZoneState>       // 音频焦点与 PTT 状态
    val climateState: StateFlow<ClimateState>           // 双区空调状态
    val driveMode: StateFlow<DriveMode>                 // 驾驶模式 (ECO/COMFORT/SPORT)

    // ---- 接口方法 (Methods) ----
    fun initialize()                                    // 初始化仓库，建立车辆服务连接与信号监听
    fun injectSpeed(speed: Float)                       // 注入/模拟车速变化
    fun injectGear(gear: VehicleGear)                   // 注入/模拟档位变化
    fun injectPower(powerState: PowerState)             // 注入/模拟电源模式迁移
    fun triggerPtt()                                    // 触发/结束 PTT 语音会话
    fun updateClimate(transform: (ClimateState) -> ClimateState) // 更新空调状态
    fun setDriveMode(mode: DriveMode)                   // 切换驾驶模式
}
```

---

### 3.3 车辆服务连接管理 (`VehicleServiceConnectionManager.kt`)

**路径**: `core/vehicle-service/src/main/java/com/oem/car/core/vehicleservice/VehicleServiceConnectionManager.kt`

* **主要职责**:
  * 连接并绑定 `IVehicleCoreManager` 服务的 Binder 代理。
  * 注册 `IBinder.DeathRecipient` 死亡监听。当 Binder 死亡（进程崩溃/服务异常）时，自动触发日志记录并在 3 秒后执行自动重连（`scheduleReconnect()`）。
* **公共方法**:
  * `fun connect()`: 发起服务连接与 Binder linkToDeath。
  * `fun disconnect()`: 解绑服务与取消注册回调。
  * `fun injectSpeed(speed: Float)` / `injectGear(gear: VehicleGear)` / `injectPower(powerState: PowerState)` / `triggerPtt()`: 代理发送测试指令。

---

### 3.4 车规防分心安全引擎 (`CarUxRestrictionsEngine.kt`)

**路径**: `core/uxrestrictions/src/main/java/com/oem/car/core/uxrestrictions/CarUxRestrictionsEngine.kt`

* **主要职责**: 依据 GB 44495 及 NHTSA 车规标准评估行驶安全。
* **核心评估逻辑**:
  ```kotlin
  fun evaluate(speed: Float, gear: VehicleGear) {
      val isDriving = gear == VehicleGear.DRIVE || speed > 0.5f
      val isHighSpeed = speed > 15.0f

      val newState = UxRestrictionState(
          isRestricted = isDriving,
          noVideo = isDriving,       // 行驶状态禁止视频播放
          noKeyboard = isDriving,    // 行驶状态禁止使用全键盘
          maxListLength = if (isHighSpeed) 5 else if (isDriving) 10 else Int.MAX_VALUE
      )
      // 状态变更更新 Flow
  }
  ```

---

### 3.5 电源生命周期管理器 (`CarPowerLifecycleManager.kt`)

**路径**: `core/power/src/main/java/com/oem/car/core/power/CarPowerLifecycleManager.kt`

* **核心监听接口**:
  ```kotlin
  interface PowerStateListener {
      fun onPrepareShutdown() {} // 关机预备
      fun onSuspendToRam() {}    // STR 内存休眠挂起
      fun onWakeUp() {}          // 系统唤醒恢复
  }
  ```
* **核心公开方法**:
  * `fun registerListener(listener: PowerStateListener)`
  * `fun unregisterListener(listener: PowerStateListener)`
  * `fun onPowerStateChanged(newState: PowerState)`: 处理电源状态迁移并通知各监听器。

---

### 3.6 音频管理与 PTT 代理 (`CarAudioManagerDelegate.kt`)

**路径**: `core/audio/src/main/java/com/oem/car/core/audio/CarAudioManagerDelegate.kt`

* **核心公开方法**:
  * `fun triggerPttEvent()`: 请求临时音频焦点，设置 `isPttActive = true`，并触发媒体音量 Ducking。
  * `fun releasePttEvent()`: 释放 PTT 会话，恢复背景媒体音量。

---

## 📐 4. 关键数据模型定义 (Data Models)

位于 `core/model/src/main/java/com/oem/car/core/model/`:

1. **`CarSignal<T>`**: 包装带有效性状态的车载信号。
   ```kotlin
   data class CarSignal<T>(
       val value: T,
       val status: SignalStatus = SignalStatus.VALID,
       val timestamp: Long = System.currentTimeMillis()
   )
   ```
2. **`VehicleGear`**: 档位枚举。
   * `PARK (1)`: P (驻车)
   * `REVERSE (2)`: R (倒车)
   * `NEUTRAL (4)`: N (空档)
   * `DRIVE (8)`: D (行车)
3. **`PowerState`**: 车辆电源状态枚举。
   * `RUNNING (0)`: NORMAL 运行中
   * `STR_SLEEP (1)`: STR 内存休眠挂起
   * `GARAGE_MODE (2)`: Garage Mode 车库维护模式
   * `SHUTDOWN_PREPARE (3)`: 关机预备模式
4. **`UxRestrictionState`**: 防分心管制状态。
   * `isRestricted: Boolean`: 是否处于防分心限制中
   * `noVideo: Boolean`: 是否禁止视频播放
   * `noKeyboard: Boolean`: 是否禁止软键盘输入
   * `maxListLength: Int`: 允许显示的最大列表条数 (5/10/Int.MAX_VALUE)
