# CommercialCarApp — AAOS 智能座舱仿真架构

> 本文档对应**仓库根目录**的实际工程（`E:\gitHub\automotiveProject`），Gradle 的 `rootProject.name` 仍为 `CommercialCarApp`；`CommercialCarApp/` 子目录是无源码的迁移残留（仅构建/IDE 缓存），不是源码工程。
>
> **定位**：可运行的座舱 HMI + 分层架构原型。车辆信号域已拆成独立 `:vehicle` 进程，通过 AIDL/Binder 跨进程通信，并实测服务进程被杀后 3 秒自动重连；其余业务域（数字钥匙、T-BOX、OTA、ADAS、DMS、导航等）仍在主进程内仿真。不连接真车、不连接云端，也不代表量产合规实现。

---

## 1. 项目定位与边界

### 1.1 这个项目做了什么

- 用 Kotlin + Jetpack Compose 实现一套横屏座舱 HMI，包含 10 个 Tab：中控、仪表、导航、空调、影音、电话、互联、能量、健康、设置。
- 用 `app / feature / core` 三层模块化组织代码，能力下沉到 `core`，页面隔离在 `feature`。
- 用 `StateFlow` 做单向数据流，用 `VehicleRepository` 作为统一数据门面。
- 用 AIDL + 独立 `:vehicle` 进程 + `bindService` + `RemoteCallbackList` + `DeathRecipient` 实现车辆服务的跨进程链路，并支持服务进程崩溃后的自动重连与状态快照回推。
- 客户端模块只依赖 AIDL 契约，服务端实现放在 `:core:vehicle`，由 `VehicleCoreService` 在 `:vehicle` 进程中托管。
- 补齐 4 个扩展域的仿真闭环：数字钥匙、T-BOX/车云/OTA、ADAS 感知、DMS/多账户 Profile。
- 补齐 4 个座舱核心域的仿真闭环：导航（路线/逐段引导/仪表联动）、电话（蓝牙/拨号/来电/计时）、能量（SOC/充电/预约）、健康（胎压/自检/DTC 扫描）。

### 1.2 这个项目明确没有做

- 没有接入 `android.car`、CarService、VHAL 或真实车辆总线。
- 车辆服务虽有独立 `:vehicle` 进程，但仍与 App 在**同一 APK、同一 UID** 内，`exported=false`，没有权限校验/AIDL 版本管理，也不是 AAOS 标准 `CarService`/VHAL。
- 没有 CarSystemUI、Launcher、多显示屏、副驾/后排 RSE。
- 没有真实数字钥匙协议（CCC 3.0 / UWB / BLE / NFC / SE）、真实 T-BOX 云通道、真实 OTA、真实 ADAS 感知与 3D SR 渲染。
- 没有真实地图引擎、蓝牙 HFP/PBAP 链路、BMS/充电桩协议、UDS/DoIP 诊断链路；导航、电话、能量、健康四个页面均为**主进程内状态仿真**。
- 没有完整测试体系（目前仅有安全策略 JVM 单测）、CI、签名发布、功能安全/信息安全流程。

因此：**它适合做架构演示、AAOS 学习、面试项目；不能直接作为量产座舱交付。**

---

## 2. 技术栈

| 项目 | 版本 / 说明 |
| :--- | :--- |
| 语言 | Kotlin 1.9.22 |
| UI | Jetpack Compose（BOM 2024.10.00）+ Material3 |
| 构建 | Gradle 8.4 + AGP 8.2.2 |
| JDK | 17 |
| Android | compileSdk 34 / minSdk 28 / targetSdk 34 |
| 架构 | 多模块 + Repository + StateFlow + 单向数据流 |
| IPC | AIDL + `bindService` + `RemoteCallbackList`（仅车辆域，主进程 ↔ `:vehicle`） |
| 并发 | kotlinx-coroutines-core 1.7.3 |
| 屏幕 | 单 Activity、横屏、10 个 Tab |

---

## 3. 目录结构与模块职责

```text
automotiveProject/
├── app/                              # 应用入口：MainActivity + 顶部 Tab 栏 + 10 Tab + 语音 Overlay
│   └── src/main/res/                 # 启动图标（mipmap 各密度 ic_launcher/ic_launcher_round PNG）
├── core/
│   ├── aidl/                         # IVehicleCoreManager / IVehicleEventListener
│   ├── audio/                        # PTT 音频焦点与 Ducking 仿真
│   ├── common/                       # 日志、Dispatchers、Result
│   ├── data/                         # VehicleRepository 接口与实现（统一门面）
│   ├── designsystem/                 # CarTheme、CarButton、CarCard、仪表盘组件
│   ├── model/                        # 车辆信号、档位、电源、座舱与扩展域数据模型
│   ├── power/                        # 电源状态机（STR / Garage / 关机预备）
│   ├── uxrestrictions/               # 行车防分心策略（演示阈值）
│   ├── vehicle/                      # CarCoreDaemon（AIDL Stub）+ VehicleCoreService（:vehicle 进程）
│   ├── vehicle-service/              # 客户端 bindService/死亡监听/AIDL 回调转 StateFlow（只依赖 AIDL）
│   ├── digitalkey/                   # 数字钥匙仿真（区域/技术/门锁/启动授权）
│   ├── telematics/                   # T-BOX/车云/OTA 仿真
│   ├── adas/                         # ADAS/感知目标仿真
│   ├── account/                      # DMS/多账户/个性化仿真
│   ├── navigation/                   # 【新增】导航路线与逐段引导仿真
│   ├── phone/                        # 【新增】蓝牙电话与通话状态机仿真
│   ├── energy/                       # 【新增】电池/充电/预约仿真
│   └── health/                       # 【新增】健康自检/胎压/DTC 仿真
├── feature/
    ├── dashboard/                    # 中控仪表 + 总线注入 Injector
    ├── cluster/                      # 液晶仪表（当前是同一 Activity 的 Tab）
    ├── navigation/                   # 【新增】导航：仿真地图 + 路线卡片
    ├── climate/                      # 双区空调 UI
    ├── media/                        # 影音 UI（受防分心策略控制）
    ├── phone/                        # 【新增】电话：蓝牙/联系人/通话 UI
    ├── connected/                    # 智能互联：数字钥匙/T-BOX/ADAS/账户
    ├── energy/                        # 【新增】能量：SOC/充电/预约 UI
    ├── health/                        # 【新增】健康：胎压/自检/DTC UI
    ├── settings/                      # 驾驶模式 + ADAS 静态展示
    └── voice/                         # 语音助手 PTT 浮层
└── scripts/
    └── verify_ipc.sh                  # 跨进程验证：只杀 :vehicle，断言 3 秒重连与 BinderProxy
```

### 3.1 模块职责与成熟度

| 模块 | 职责 | 成熟度 |
| :--- | :--- | :--- |
| `:app` | `MainActivity`、10 个 Tab、`VoiceAssistantOverlay` 全局浮层 | 可用 |
| `:core:aidl` | 车辆服务契约，`oneway` 异步回调 | 完整（车辆域契约） |
| `:core:vehicle` | `CarCoreDaemon` + `VehicleCoreService`，运行在 `:vehicle` 进程 | 仿真（跨进程服务端） |
| `:core:vehicle-service` | `bindService`、死亡监听、注入代理、AIDL 回调转 `StateFlow` | 仿真（跨进程客户端，只依赖 AIDL） |
| `:core:data` | 聚合所有域状态，暴露统一 `VehicleRepository` | 可用 |
| `:core:uxrestrictions` | 按车速/档位计算防分心状态 | 演示规则 |
| `:core:power` | 电源状态迁移与监听器 | 骨架 |
| `:core:audio` | PTT 抢焦点、音乐 Ducking | 骨架 |
| `:core:digitalkey` | 靠近/远离、技术切换、门锁、启动授权 | 仿真闭环 |
| `:core:telematics` | 云连接、哨兵、远程命令、OTA 进度 | 仿真闭环 |
| `:core:adas` | NOA、车道保持、巡航速度、感知目标 | 仿真闭环 |
| `:core:account` | 人脸验证、座椅记忆、偏好温度、应用 Profile | 仿真闭环 |
| `:core:navigation` | 目的地预设、逐段引导、剩余里程/ETA/限速/车道提示 | 仿真闭环 |
| `:core:phone` | 蓝牙连接、拨号/来电状态机、通话计时、静音 | 仿真闭环 |
| `:core:energy` | SOC/续航联动、充电限值、充电进度、预约与预热 | 仿真闭环 |
| `:core:health` | 胎压、系统自检、DTC 扫描/清除 | 仿真闭环 |
| `:core:model` | `CarSignal<T>`、业务数据类、`DrivingSafetyPolicy` 共享规则 | 完整 |
| `:core:designsystem` | 车规大触控组件与深色主题 | 可用 |
| `:core:common` | 日志、调度器、Result | 可用 |
| `:feature:*` | 各业务页面 | Dashboard/Connected/导航/电话/能量/健康/空调/影音/设置可交互；仪表以展示为主；语音为 PTT 浮层 |

---

## 4. 架构与数据流

### 4.1 依赖关系

```text
app
 ├── feature:dashboard / cluster / navigation / climate / media / phone
 │   / connected / energy / health / settings / voice
 │     ├── core:data
 │     │     ├── core:vehicle-service ──► core:aidl
 │     │     ├── core:uxrestrictions / power / audio
 │     │     ├── core:digitalkey / telematics / adas / account
 │     │     ├── core:navigation / phone / energy / health
 │     │     └── core:common / model
 │     └── core:designsystem / common / model
 ├── core:vehicle ──► core:aidl / core:model   // 仅用于把服务实现打进同一 APK，由 :vehicle 进程加载
 ├── core:common / model
 └── core:designsystem ──► core:model

运行时进程边界：
主进程 com.oem.car.app（UI / Repository / 客户端）
  └── bindService ──► :vehicle 进程 com.oem.car.app:vehicle（VehicleCoreService + CarCoreDaemon）
```

补充：`:app` 也直接声明了 `:core:data` 依赖（与各 feature 相同，上图未重复展开）。`:app` 依赖 `:core:vehicle` 只是因为服务类必须打进同一个 APK；各 feature 不依赖服务端实现。

`:core:vehicle-service` 只依赖 `:core:aidl`，不再编译期依赖 `:core:vehicle`；客户端看不到 `CarCoreDaemon` 类。

### 4.2 启动链路

```text
MainActivity.onCreate
  └── VehicleRepositoryImpl.initialize(applicationContext)   // AtomicBoolean 保证只执行一次
        ├── VehicleServiceConnectionManager.connect()
        │     └── bindService(ComponentName(pkg, VehicleCoreService))
        │           └── AMS 发现 android:process=":vehicle"
        │                 ├── fork 出 :vehicle 进程
        │                 ├── VehicleCoreService.onCreate() → CarCoreDaemon()
        │                 └── onBind() 返回 daemon（Binder 服务端）
        │           └── 主进程 onServiceConnected(binder)
        │                 ├── linkToDeath(...)
        │                 ├── Stub.asInterface(binder).registerListener(...)
        │                 └── 服务端 syncStateTo() 回推状态快照
        └── 常驻协程
              ├── speedSignal → CarUxRestrictionsEngine.evaluate()
              ├── gearSignal  → CarUxRestrictionsEngine.evaluate()
              └── powerSignal → CarPowerLifecycleManager.onPowerStateChanged()
```

### 4.3 车辆信号链路（跨进程）

```text
Dashboard Injector（主进程）
  └── VehicleRepository.injectSpeed / injectGear / injectPower
        └── VehicleServiceConnectionManager.withService { manager.injectMockXxx(...) }
              └── IVehicleCoreManager.Proxy.transact(...) ── Binder IPC ──►
                    CarCoreDaemon.injectMockSpeed / injectMockGear / injectMockPower（:vehicle 进程）
                      ├── 更新内部 StateFlow
                      ├── checkSafetyPolicy()          // 仅速度/档位注入触发，规则来自 DrivingSafetyPolicy
                      └── dispatch(RemoteCallbackList)
                            └── oneway onSpeedChanged / onGearChanged / ...
                                  └── 客户端 Binder 线程
                                        └── VehiclePropertyAdapter 更新 StateFlow
                                              └── VehicleRepository 暴露
                                                    └── Compose collectAsState()
```

真实日志中服务端会打印 `callerPid=<主进程> myPid=<:vehicle 进程> thread=binder:<pid>_N`，可直接证明调用跨进程；客户端打印 `binder=android.os.BinderProxy localInterface=null`。

`linkToDeath` 现在监听的是真实的 `:vehicle` 进程。杀掉该进程会触发 `Vehicle service binder died` → `scheduleReconnect in 3000ms` → 重新 `bindService`；主进程 PID 不变，服务端状态重置后通过注册快照回推。

已实测的一个边界：`injectMockXxx` 先调用 `checkSafetyPolicy()` 再 `dispatch` 对应的速度/档位回调，因此防分心状态回调可能早于本次速度/档位回调到达客户端；UI 最终状态一致，但依赖回调顺序的代码需要额外注意。

### 4.4 扩展域链路

```text
ConnectedScreen
  ├── repository.digitalKeyState / moveKeyCloser / moveKeyAway / cycleKeyTech / toggleDoorLock
  │     └── DigitalKeyManager
  ├── repository.telematicsState / toggleSentryMode / sendRemoteCommand / startOtaUpdate
  │     └── TelematicsManager（协程模拟 OTA 阶段与进度）
  ├── repository.adasState / toggleNoa / toggleLaneKeep / adjustCruiseSpeed
  │     └── AdasManager
  └── repository.userProfile / verifyDriverFace / cycleSeatPosition / adjustPreferredTemp / applyUserProfile
        └── AccountManager
              └── applyUserProfile()（人脸验证通过后）会把偏好温度写入 climateState（跨域联动示例）
```

### 4.5 导航/电话/能量/健康链路

```text
NavigationScreen
  ├── repository.navigationPresets / startNavigation(destination)
  ├── repository.advanceNavigation() / stopNavigation()
  │     └── NavigationManager：逐段指令、剩余里程、ETA、限速、车道提示
  └── ClusterScreen 读取 repository.navigationState
        └── 仪表 TBT 卡片与导航页共享同一路线状态（跨 Tab 联动）

PhoneScreen
  ├── repository.phoneState / togglePhoneBluetooth / dialContact / simulateIncomingCall
  └── repository.answerCall / hangUpCall / toggleCallMute
        └── PhoneManager：DIALING → ACTIVE 状态机 + 每秒计时 + 静音

EnergyScreen
  └── repository.energyState / toggleCharging / adjustChargeLimit
        / toggleScheduledCharging / togglePreconditioning
        └── EnergyManager：每 800 ms SOC +1%，到充电限值自动停止

HealthScreen
  └── repository.vehicleHealthState / scanVehicleHealth / clearDtcs
        └── VehicleHealthManager：扫描 1.5 s 后返回 P0456 / B1A00
```

### 4.6 PTT 链路

```text
Dashboard "方向盘 PTT"
  └── repository.triggerPtt()
        ├── CarAudioManagerDelegate.triggerPttEvent()      // 主进程本地音频状态：isPttActive=true
        └── VehicleServiceConnectionManager.triggerPtt()
              └── IVehicleCoreManager.Proxy.triggerPttVoice() ── Binder ──►
                    CarCoreDaemon._voiceTriggerEvent           // 仅更新时间戳，当前没有消费者
VoiceAssistantOverlay（监听主进程 isPttActive 状态，本身不触发 PTT）"完成对话"
  └── repository.releasePtt()
        └── CarAudioManagerDelegate.releasePttEvent()      // isPttActive=false，浮层关闭
```

注意：PTT 只有“主进程 → 服务端”的单向调用，没有服务端回调；语音浮层的显示/关闭完全由主进程 `isPttActive` 驱动。

### 4.7 跨进程实测：进程边界与死亡重连

服务端和客户端在同一个 APK 内，但运行在两个进程：

```text
主进程 com.oem.car.app (pid=6080)
  VehicleServiceConnectionManager
    └── bindService(ComponentName(pkg, VehicleCoreService))
          │  AMS 按 android:process=":vehicle" fork 新进程
          ▼
  :vehicle 进程 com.oem.car.app:vehicle (pid=6694)
    VehicleCoreService.onCreate() → CarCoreDaemon()
    VehicleCoreService.onBind()   → 返回 daemon（Binder 服务端）
          │
          ▼
  主进程 onServiceConnected(binder)
    ├── binder.queryLocalInterface(...) == null   // 拿到的是 BinderProxy
    ├── binder.linkToDeath(...)                   // 服务进程死亡监听
    └── IVehicleCoreManager.Stub.asInterface(binder).registerListener(...)
          └── 服务端 syncStateTo() 立即回推 speed/gear/power/restricted 快照
```

一次实测（`scripts/verify_ipc.sh`，`am crash` 只杀 `:vehicle`）：

```text
# 杀之前
main pid=6080, vehicle pid=6112

# 杀掉 :vehicle 后，主进程不变
W VehicleServiceConn: Service disconnected, waiting for system rebind
E VehicleServiceConn: Vehicle service binder died
W VehicleServiceConn: scheduleReconnect in 3000ms

# 约 3.3s 后 AMS 重新拉起 :vehicle
I VehicleCoreService: onCreate pid=6694
I CarCoreDaemon: registerListener registered=true callerPid=6080 myPid=6694 thread=binder:6694_4
I CarCoreDaemon: push snapshot speed=0.0 gear=1 power=0 restricted=false
I VehicleServiceConn: Connected pid=6080 binder=android.os.BinderProxy localInterface=null bindCostMs=270
```

可确认的四件事：

1. `callerPid=6080` 与 `myPid=6694` 不同，AIDL 方法确实跨进程执行。
2. `binder=android.os.BinderProxy`、`localInterface=null`，客户端拿到的不是同进程对象。
3. `linkToDeath` 真实触发，固定 3 秒后重新 `bindService`；主进程 PID 不变。
4. 新服务进程是空状态，靠 `registerListener` 后的快照回推恢复 UI，而不是依赖旧进程内存。

运行方式见 [9.4 跨进程验证](#94-跨进程验证)。

> 注意：`:vehicle` 是本项目自定义的私有进程名，不是 AAOS 标准进程。AAOS 量产架构是 App → CarService（`com.android.car`）→ VHAL（`android.hardware.automotive.vehicle@V1-emulator-service`）→ CAN/以太网；本项目没有接入这条系统链路。

---

## 5. 页面与演示路径

| Tab | 可演示内容 |
| :--- | :--- |
| 中控仪表 | 车速/档位/电源状态；Injector 注入 P 档、D 档 80km/h、STR 休眠、唤醒、PTT；防分心视频拦截 |
| 液晶仪表 | 大字号车速、档位；**动态 TBT 导航卡片**（与导航页共享路线状态）；ADAS 文案 |
| **导航** | 仿真地图与路线；公司/家/充电站三个目的地；逐段“下一段”推进；剩余里程、ETA、限速、车道提示；结束后仪表同步恢复 |
| 空调 HVAC | 主/副驾温度 ±0.5°C、风量 1–7 挡（本地状态） |
| 多媒体影音 | 播放/暂停（“下一首”为占位按钮）；行车状态自动切换为“视频防分心保护” |
| **电话** | 蓝牙连接/断开；联系人拨号（1.2 s 自动接通）；模拟来电接听；通话计时；静音；挂断 |
| **智能互联** | 数字钥匙靠近/远离/切换技术/门锁；T-BOX 远程命令、哨兵、OTA 进度；ADAS NOA/车道/巡航；DMS 人脸、座椅、偏好温度、应用 Profile |
| **能量** | SOC 环形仪表；续航/温度/能耗；充电限值 ±5%；开始/停止充电（SOC 实时上升）；预约充电；电池预热 |
| **健康** | 四轮胎压（右后偏低告警）；12V/里程/保养里程；制动/ESP/SRS/ADAS 自检；DTC 扫描（P0456、B1A00）与清除 |
| 车辆设置 | 驾驶模式切换；ADAS 能力静态文案展示（仅 UI 文案，非真实能力） |

**建议演示顺序**

1. 中控仪表点击“D档 (80 km/h)”，观察防分心状态变红，切到多媒体页看到视频被拦截。
2. 点击“P档 (0 km/h)”恢复安全状态。
3. 导航页选择“导航到 公司”，点“下一段”推进引导，再切到仪表页看 TBT 卡片同步。
4. 电话页向“张先生”拨号，观察 DIALING → 通话中 → 计时；测试静音与挂断。
5. 能量页开始充电，观察 SOC 与续航上升，点击停止充电。
6. 健康页扫描故障码，查看 P0456 / B1A00，再清除。
7. 智能互联页把数字钥匙从远处逐步“靠近”，观察门锁与启动授权变化。
8. 点击“OTA 升级”，观察 CHECKING → DOWNLOADING → VERIFYING → APPLYING → SUCCESS。
9. DMS 点击“人脸识别”后点击“应用个性化到座舱”，切到空调页确认主驾温度被改为偏好温度。
10. 点击“方向盘 PTT”弹出语音浮层，点击“完成对话”关闭。
11. 运行 `./scripts/verify_ipc.sh -s <设备序列号>`，观察只杀 `:vehicle` 后主进程 PID 不变、3 秒后服务进程重建并重连。

---

## 6. 核心接口

### 6.1 AIDL

`core/aidl/src/main/aidl/com/oem/car/middleware/IVehicleCoreManager.aidl`

```aidl
void registerListener(IVehicleEventListener listener);
void unregisterListener(IVehicleEventListener listener);
void injectMockSpeed(float speed);
void injectMockGear(int gear);
void injectMockPower(int state);
void triggerPttVoice();
```

`core/aidl/src/main/aidl/com/oem/car/middleware/IVehicleEventListener.aidl`

```aidl
oneway interface IVehicleEventListener {
    void onSpeedChanged(float speed);
    void onGearChanged(int gear);
    void onPowerStateChanged(int state);
    void onDistractionStateChanged(boolean isRestricted);
}
```

进程与线程：

- 客户端 `:app` 主进程持有 `IVehicleCoreManager.Proxy`；服务端 `CarCoreDaemon` 在 `:vehicle` 进程的 Binder 线程池执行（`binder:<pid>_N`）。
- 回调接口是 `oneway`：服务端发出后不等待客户端处理完；客户端回调同样运行在客户端 Binder 线程，不阻塞主线程。
- `registerListener` 是同步调用，返回前会执行 `syncStateTo()` 回推快照；快照回调因 `oneway` 异步到达。

### 6.2 VehicleRepository（统一门面）

状态与数据：

```kotlin
val isConnected: StateFlow<Boolean>
val speedSignal: StateFlow<CarSignal<Float>>
val gearSignal: StateFlow<CarSignal<VehicleGear>>
val powerSignal: StateFlow<CarSignal<PowerState>>
val uxRestrictionState: StateFlow<UxRestrictionState>
val audioZoneState: StateFlow<AudioZoneState>
val climateState: StateFlow<ClimateState>
val driveMode: StateFlow<DriveMode>
val digitalKeyState: StateFlow<DigitalKeyState>
val telematicsState: StateFlow<TelematicsState>
val adasState: StateFlow<AdasPerceptionState>
val userProfile: StateFlow<UserProfile>
val navigationState: StateFlow<NavigationState>
val navigationPresets: List<NavigationDestination>
val phoneState: StateFlow<PhoneState>
val energyState: StateFlow<EnergyState>
val vehicleHealthState: StateFlow<VehicleHealthState>
```

命令：

```kotlin
fun initialize(context: Context)
fun injectSpeed(speed: Float)
fun injectGear(gear: VehicleGear)
fun injectPower(powerState: PowerState)
fun triggerPtt()
fun releasePtt()
fun updateClimate(transform: (ClimateState) -> ClimateState)
fun setDriveMode(mode: DriveMode)
fun moveKeyCloser()
fun moveKeyAway()
fun cycleKeyTech()
fun toggleDoorLock()
fun toggleSentryMode()
fun sendRemoteCommand(command: String)
fun startOtaUpdate()
fun toggleNoa()
fun toggleLaneKeep()
fun adjustCruiseSpeed(delta: Float)
fun verifyDriverFace()
fun cycleSeatPosition()
fun adjustPreferredTemp(delta: Float)
fun applyUserProfile()
fun startNavigation(destination: NavigationDestination)
fun advanceNavigation()
fun stopNavigation()
fun togglePhoneBluetooth()
fun dialContact(contact: Contact)
fun simulateIncomingCall(contact: Contact)
fun answerCall()
fun hangUpCall()
fun toggleCallMute()
fun toggleCharging()
fun adjustChargeLimit(delta: Int)
fun toggleScheduledCharging()
fun togglePreconditioning()
fun scanVehicleHealth()
fun clearDtcs()
```

### 6.3 扩展域 Manager

**DigitalKeyManager**

- 模拟距离序列：`FAR(12m) → APPROACHING(8m) → WELCOME(4m) → UNLOCK(1.2m) → CABIN_INSIDE(0.5m)`（区域显示文案见 `ProximityZone`）。
- 进入 `UNLOCK` 自动解锁，离开到 `APPROACHING/FAR` 自动落锁。
- 只有 `CABIN_INSIDE` 且 SE 鉴权通过才允许启动。
- `cycleTech()` 在 `UWB → BLE → NFC` 之间切换。

**TelematicsManager**

- 维护云连接、信号强度、哨兵模式、最近远程命令。
- `startOtaUpdate()` 用协程模拟 OTA 全流程；同一时间只允许一个 OTA Job。

**AdasManager**

- `toggleNoa()`、`toggleLaneKeep()`、`adjustCruiseSpeed(delta)`。
- 巡航速度限制在 30–130 km/h。
- 感知目标来自 `AdasPerceptionState.targets` 静态数据。

**AccountManager**

- 初始 `isFaceIdVerified=false`，`verifyFace()` 置为已验证。
- `cycleSeatPosition()` 在 1–4 之间循环。
- `adjustPreferredTemp(delta)` 限制在 16–30°C。
- `VehicleRepositoryImpl.applyUserProfile()` 在人脸验证通过后会把偏好温度同步到主驾空调温度。

**NavigationManager**

- 3 个目的地预设（公司 18.6 km / 家 12.4 km / 望京充电站 6.8 km）。
- `startRoute()` 初始化路线，并按目的地设定限速（望京充电站 40 km/h，其余 80 km/h）；`advance()` 推进 4 段指令，更新剩余里程、ETA、车道提示与进度，第 4 段后置为 `ARRIVED`。
- 路线状态在导航页与仪表页之间共享。

**PhoneManager**

- `callJob` 保证同一时间只有一路通话。
- `dial()` 进入 `DIALING`，1.2 s 后自动 `ACTIVE` 并每秒计时。
- `simulateIncomingCall(contact)` 进入 `RINGING`，`answerCall()` 接听；断开蓝牙会结束通话。
- 通话中可静音；挂断后状态归零。

**EnergyManager**

- 开始充电后每 800 ms SOC +1%、续航 +6 km，达到充电限值自动停止。
- 充电限值限制在 50–100%；预约充电、电池预热为独立开关。

**VehicleHealthManager**

- `scan()` 模拟 1.5 s 诊断，返回 P0456（蒸发排放微小泄漏）与 B1A00（右前轮胎压偏低）。
- `clearDtcs()` 取消扫描并清空故障码。

### 6.4 关键数据模型

- `CarSignal<T>`：值 + `SignalStatus` + 时间戳 + `areaId`。
- `VehicleGear`：P=1 / R=2 / N=4 / D=8。
- 行车防分心规则：`DrivingSafetyPolicy.isDriving()`（D 档或 >0.5 km/h）与 `maxListLength()`（高速 >15 km/h 时 5 条，否则行车 10 条，驻车不限）。
- `PowerState`：RUNNING / STR_SLEEP / GARAGE_MODE / SHUTDOWN_PREPARE。
- `UxRestrictionState`：`isRestricted`、`noVideo`、`noKeyboard`、`maxListLength`。
- 座舱基础域：`AudioZoneState`（PTT/Ducking）、`ClimateState`（双区温度/风量）、`DriveMode`（ECO/COMFORT/SPORT/CUSTOM）。
- 扩展域：`DigitalKeyState`、`TelematicsState`、`OtaState`、`AdasPerceptionState`、`PerceptionTarget`、`UserProfile`。
- 座舱核心域：`NavigationState` / `NavigationDestination` / `RouteStatus`、`PhoneState` / `Contact` / `CallStatus`、`EnergyState`、`VehicleHealthState` / `TirePressure` / `SystemHealth` / `DtcCode` / `HealthSeverity`。

---

## 7. 与商业量产项目的差距

> 本项目补齐的是**扩展域的第一层仿真闭环**。距离量产座舱仍有四层差距。

### 7.1 车辆接入层（最关键）

| 商业量产必备 | 当前状态 |
| :--- | :--- |
| CarService / VHAL / `CarPropertyManager` | 无 `android.car` 引用 |
| 独立车辆服务进程 + `bindService` | 已实现：同 APK 内 `:vehicle` 进程 + AIDL，含死亡重连与快照回推 |
| 跨进程权限与调用方校验 | 无：同 APK/同 UID，`exported=false`，未声明自定义权限 |
| 属性读写、订阅、权限、areaId、超时、限流 | 只有 `injectMock*` 和内存 StateFlow |
| AIDL 版本管理 / 多客户端 | 无：单一客户端、无版本字段 |
| 真实 ECU / CAN / Ethernet 信号 | 无 |

### 7.2 系统 HMI 底座

| 商业量产必备 | 当前状态 |
| :--- | :--- |
| CarSystemUI（状态栏、Dock、QS、音量、通知） | 无 |
| CarLauncher（桌面、卡片、应用网格） | 无 |
| 多屏 / OccupantZone / Presentation / 仪表独立显示 | 单 Activity 单屏，仪表是 Tab |
| 多音区音频策略、焦点、路由、提示音 | 只有 PTT Ducking 布尔状态 |
| 电源/热管理/STR/唤醒锁 | 只有电源状态机骨架 |
| OTA（SOTA/FOTA、A/B、回滚） | 只有本地 OTA 进度动画 |
| 安全启动、SELinux、Keystore、SecOC | 无 |
| UDS/DTC/DoIP 诊断与日志抓取 | 无 |

### 7.3 座舱业务域

| 功能域 | 当前状态 | 量产要求 |
| :--- | :--- | :--- |
| 数字钥匙 | 区域/技术/门锁仿真 | CCC 3.0、UWB/BLE/NFC、SE/HSM、PKI、防中继 |
| T-BOX / 云控 | 本地状态 + 假 OTA | 4G/5G、GNSS、MQTT/CoAP/TLS、eCall、哨兵回传 |
| ADAS / SR | 本地 NOA/车道/巡航状态 | 感知融合、3D SR 渲染、AR-HUD、功能安全 |
| DMS / Profile | 本地人脸与偏好仿真 | 摄像头人脸识别、座椅/后视镜联动、隐私合规 |
| 空调 | UI 本地状态 | HVAC 服务、风门/压缩机/多区控制、实车标定 |
| 媒体 | UI 播放占位 | MediaSession、音源管理、收音机、USB/流媒体 |
| 导航 | 路线/逐段引导/仪表联动仿真 | 地图引擎、离线/在线地图、实时路况、EV 充电路径、AR-HUD |
| 语音 | 浮层占位 | 唤醒词、ASR、NLU、TTS、技能生态 |
| 电话/蓝牙 | 通话状态机与计时仿真 | HFP/PBAP、通讯录同步、eCall、多麦克风降噪 |
| 投屏 | 无 | CarPlay / HiCar / Android Auto |
| 相机 | 无 | AVM、DVR、哨兵 |
| 能量管理 | SOC/充电/预约仿真 | 真实 BMS、GB/T 27930、热管理、V2L/V2G |
| 健康诊断 | 胎压/自检/DTC 仿真 | UDS/DoIP、冻结帧、远程诊断、预测性维护 |
| 账户 | 本地 Profile | 账号体系、云端同步、多用户隔离 |

### 7.4 量产工程体系

| 能力 | 当前状态 |
| :--- | :--- |
| 单元/集成/UI/HIL 测试 | 已有安全策略 JVM 单测（`DrivingSafetyPolicyTest` 8 例、`CarUxRestrictionsEngineTest` 5 例）；无集成/UI/HIL 测试 |
| CI/CD、签名、发布、灰度 | 无 |
| 性能/内存/启动/稳定性基线 | 无 |
| 日志、埋点、远程诊断 | 仅 Logcat 本地日志（CarLogger）；无埋点、远程诊断 |
| ISO 26262 / ISO 21434 / GB 44495 / GB 44496 等流程 | 无 |

> 说明：行车防分心规则在本项目中是**演示阈值**，参考 NHTSA Driver Distraction Guidelines；GB 44495 是整车信息安全要求，不应作为驾驶员分心判定依据。旧版 README 与代码中的相关引用已修正。

### 7.5 商业化功能缺口清单（量化）

> 计数口径：按“可独立交付/验收的功能条目”统计，不按代码行数或页面数。同一能力在不同域重复出现时只计一次主责域。
> **当前缺口：功能层 121 项 + 工程体系 11 项 = 132 项。** 完成“独立 `:vehicle` 进程 + `bindService`”后，平台与系统层缺口由 28 项降为 27 项；补上安全策略 JVM 单测后，工程体系缺口由 12 项降为 11 项。导航、电话、能量、健康仍为 L1 仿真闭环，每个域还缺 5–8 项量产能力。

#### 7.5.1 平台与系统层（27 项）

| 能力域 | 已实现 | 缺失数 | 主要缺口 |
| :--- | :--- | ---: | :--- |
| 车辆接入与总线 | 同 APK 内 `:vehicle` 进程 + AIDL/bindService + 死亡重连 + mock 注入 | 4 | CarService/VHAL/CarPropertyManager 接入；CAN/Ethernet/ECU 实信号；属性订阅/areaId/超时/降级；SOME-IP/DoIP/TSN 网络管理 |
| 系统 HMI | 单 Activity + Compose Tab | 5 | CarSystemUI；CarLauncher；多屏/OccupantZone/Presentation；通知/QS/音量/状态栏；多窗口/分屏/画中画 |
| 音频系统 | PTT 抢焦点 + 音乐 Ducking | 4 | 多音区 AudioPolicy；焦点栈/路由/提示音；主动降噪/回声消除；独立音区播放与耳机输出 |
| 电源与热管理 | 电源状态机骨架 | 4 | 量产电源状态机；STR/休眠唤醒策略；电池/座舱热管理；12V 亏电保护 |
| OTA 与安全 | 本地 OTA 进度动画 | 6 | SOTA/FOTA A/B 与回滚；差分升级/灰度/云管；安全启动/SELinux/Keystore；SecOC/HSM/PKI；签名与完整性校验；失败恢复/看门狗 |
| 诊断与日志 | 仅 Logcat 本地日志；健康页 DTC 仿真 | 4 | UDS/DoIP 诊断栈；DTC 冻结帧/快照；日志抓取/远程诊断；故障预测与健康管理 |

#### 7.5.2 座舱业务层（94 项）

| 功能域 | 已实现（仿真） | 缺失数 | 主要缺口 |
| :--- | :--- | ---: | :--- |
| 导航 | 路线/逐段引导/剩余里程/ETA/限速/仪表联动 | 7 | 地图引擎与渲染；在线/离线地图与更新；POI/搜索/多途经点；实时路况与动态重算；EV 充电路径与能耗联动；AR-HUD/多屏流转；语音播报与车道级引导 |
| 语音 | PTT 浮层占位 | 7 | 唤醒词/离线唤醒；ASR；NLU/对话管理；TTS；技能生态；多音区声源定位；可见即可说/连续对话 |
| 媒体 | 播放/暂停 + 防分心拦截 | 7 | MediaSession/音源管理；USB/本地媒体；在线流媒体；收音机/DAB；音效/EQ/声场；多音区独立音源；DRM/版权 |
| 电话 | 蓝牙连接/拨号/来电/计时/静音/挂断 | 6 | HFP/PBAP；通讯录与通话记录同步；多麦克风降噪/回声消除；eCall/BCall；隐私模式；多设备连接管理 |
| 空调 | 双区温度 + 风量 | 7 | HVAC 服务与执行器；多区/后排控制；风门/压缩机/热泵；座椅加热通风按摩；空气质量/香氛/净化；远程预约/预空调；能耗优化 |
| 能量 | SOC/续航/充电/限值/预约/预热 | 6 | 真实 BMS 与电池安全；GB/T 27930、OCPP 充电协议；预约充电与电价策略；电池热管理；V2L/V2G；能耗预测与续航联动 |
| 健康诊断 | 胎压/自检/DTC 扫描清除 | 5 | 真实 UDS/DoIP；冻结帧/快照；远程诊断上报；预测性维护；保养预约与服务闭环 |
| 数字钥匙 | 区域/技术/门锁/启动授权 | 6 | CCC 3.0；UWB/BLE/NFC 真实链路；SE/HSM + PKI；防中继/安全测距；钥匙分享/权限；手机/手表生态 |
| T-BOX/车云 | 云状态/哨兵/远程命令/OTA 进度 | 7 | 4G/5G；GNSS；MQTT/CoAP/TLS；远程控车闭环；eCall/BCall；哨兵视频回传；云管平台与账号 |
| ADAS/SR | NOA/车道保持/巡航/感知目标展示 | 8 | 感知融合；3D SR；AR-HUD；高速/城区 NOA；APA/RPA 泊车；AEB/BSD/DOW 等主动安全；功能安全；DMS 联动 |
| DMS/账户 | 人脸验证/座椅/温度/Profile | 5 | 真实摄像头 DMS；疲劳/分心检测；座椅/后视镜/方向盘联动；多用户隔离/云同步；隐私合规 |
| 互联投屏 | 无 | 4 | CarPlay；HiCar；Android Auto；无线投屏/反向控制 |
| 影像 | 无 | 4 | AVM 360；DVR 行车记录仪；哨兵影像；倒车影像 |
| 场景/生态 | 无 | 5 | 场景引擎；应用商店；小程序/快应用；智能家居；消息/日程 |
| 设置 | 驾驶模式 + ADAS 静态展示 | 5 | 显示/声音/网络/蓝牙/Wi-Fi；语言/时间/单位；账号/隐私权限；恢复出厂/诊断入口；多车型配置 |
| 仪表 | 车速/档位/TBT/ADAS 文案 | 5 | 独立屏渲染；全量报警灯；能耗/行程曲线；主题/布局切换；故障文字提示 |

#### 7.5.3 工程体系（11 项）

| 能力 | 当前状态 |
| :--- | :--- |
| 单元测试 / 集成与 UI 测试 / HIL 台架 | 安全策略 JVM 单测已补（2 个测试类、13 个用例）；集成/UI/HIL 无（2 项） |
| CI/CD / 签名发布与灰度 / 性能与启动基线 / 稳定性长稳 | 均无（4 项） |
| 日志埋点与远程可观测 / 多车型配置与国际化 | 仅 Logcat 本地日志；无埋点/远程可观测，也无多车型配置/国际化（2 项） |
| ISO 26262 功能安全 / ISO 21434 与 GB 44495/44496 信息安全 / 隐私合规 | 无（3 项） |

#### 7.5.4 结论

- **能演示的**：10 个 Tab 的交互/展示闭环、跨域状态联动（导航↔仪表、DMS↔空调）、真实双进程 AIDL/Binder 链路（BinderProxy、死亡重连、快照回推）。
- **不能交付的**：任何真实车辆/云端/蓝牙/地图/BMS/诊断链路，以及全部量产工程与合规体系。
- 若以“可量产交付”为 100%，本项目当前更接近**功能原型 + 架构样板**，而不是按完成度折算的百分比；缺口应按上表逐项立项补齐。

### 7.6 通信方式分层（目标量产架构）

> 上表的量产能力并非都依赖 HTTP。当前仓库没有网络通信：车辆域是**同 APK 双进程的 Binder/AIDL**，其余业务域仍在主进程内仿真；第 1 节也写明无网络依赖。

| 能力 | 主要通信方式 | 是否 HTTP |
| :--- | :--- | :--- |
| 当前项目（车辆域跨进程） | 主进程 ↔ `:vehicle` 的 Binder/AIDL 方法调用、`oneway` 回调、StateFlow；其余业务域主进程内；无网络通信 | 否 |
| 真实车辆接入 / VHAL | Binder/AIDL + CAN/CAN-FD/LIN/Automotive Ethernet（SOME/IP、DDS） | 否 |
| 数字钥匙 CCC 3.0 | UWB 测距 + BLE + NFC + 安全芯片 | 否（云端开通可能用 HTTPS） |
| 电话 | Bluetooth HFP / PBAP | 否 |
| 投屏 | USB / Wi-Fi + CarPlay/Android Auto 私有协议 | 否 |
| 导航 | GNSS + 离线地图；在线瓦片/算路走 HTTPS | 部分 |
| 媒体 | 本地/USB/蓝牙 A2DP；在线流媒体走 HTTPS | 部分 |
| 语音 | 端侧唤醒 + 云端 ASR/NLU，常用 WebSocket/gRPC | 部分 |
| T-BOX 云通道 | 云端 MQTT/HTTPS；车端 CAN/以太网 | 部分 |
| OTA | 云端 HTTPS 下载 + 车端 UDS/DoIP 刷写 | 部分 |
| ADAS / SR | 车载以太网（SOME/IP、DDS）+ CAN | 否 |
| DMS / Profile | 摄像头/红外端侧推理；Profile 云同步可用 HTTPS | 部分 |
| 能量管理 | CAN、PLC（ISO 15118）、GB/T 27930、充电桩 OCPP（WebSocket） | 部分 |
| 健康诊断 | UDS（ISO 14229）/ DoIP（ISO 13400） | 否 |

> 因此，座舱通信应按**“Binder + 车载总线 + 短距无线 + 云协议”**四层理解；HTTP(S) 只是云侧接入方式之一，且常由 MQTT、WebSocket、gRPC 替代。评估一项能力时，应先判断它属于哪条链路，再讨论具体协议。

---

## 8. 已知限制

1. **跨进程只覆盖车辆域**：主进程 ↔ `:vehicle` 已走 AIDL/Binder，并实测进程死亡重连；数字钥匙、T-BOX、导航、电话、能量、健康等仍为主进程内 Manager，不是独立服务。
2. **同一 APK/UID，不是独立部署**：`:vehicle` 与主进程同包同 UID，`exported=false`；没有自定义权限、调用方校验、AIDL 版本管理，也没有前台服务/`BIND_IMPORTANT` 等保活策略。
3. **服务端状态只在内存**：`:vehicle` 重建后 speed/gear/power 回到默认值，客户端靠注册快照恢复；没有持久化与状态恢复。
4. **PTT 没有跨进程回传**：`triggerPttVoice()` 只更新服务端 `_voiceTriggerEvent`，目前无消费者；语音浮层由主进程 `CarAudioManagerDelegate.isPttActive` 驱动。
5. **重连策略固定**：`RECONNECT_DELAY_MS=3000`，没有指数退避、最大重试、电量/网络感知；`am crash`/`kill -9` 场景已实测，系统级低内存回收未覆盖。
6. **服务端并发与广播收尾待加固**：`checkSafetyPolicy()` 的读-改-写不是原子操作；`dispatch()` 的 `finishBroadcast()` 未放在 `finally`，回调抛出 `Error` 时会跳过收尾。
7. **Feature 直接依赖实现**：多数页面默认注入 `VehicleRepositoryImpl` 单例，没有 DI；测试与替换成本高。
8. **仪表不是独立屏**：Cluster 是同一 Activity 的一个 Tab，不是多显示设备。
9. **测试覆盖仍很薄**：已有安全策略 JVM 单测（13 个用例），但没有 AIDL 跨进程集成测试、UI 测试、HIL，也未接入 CI；`scripts/verify_ipc.sh` 仍是手工执行。
10. **仓库卫生**：历史构建产物已从 Git 索引移除，并已在 `.gitignore` 全局忽略 `build/`；`CommercialCarApp/` 仍是未跟踪的迁移残留（无源码，仅构建/IDE 缓存），不建议打开。
11. **四个新域是仿真**：导航没有地图引擎/定位，电话没有真实 HFP/PBAP，能量没有 BMS/充电桩协议，健康没有 UDS/DoIP；页面说明中也标注了“仿真模式”。
12. **Tab 栏支持横向滚动**：顶部 Tab 栏已启用 `horizontalScroll`；当前模拟器宽度可完整显示 10 个 Tab，但在窄屏或更大字体/显示密度下可能超出宽度，此时可横向滑动访问右侧 Tab。
13. **跨 Tab 联动靠内存状态**：导航路线、通话、充电、故障码均只存在于主进程内，App 重启即丢失。
14. **健康演示数据不一致**：胎压卡显示右后 2.1 bar 偏低，而扫描出的 DTC `B1A00` 文案写“右前轮胎压偏低”；两处均为静态演示数据，未与真实传感器绑定。

---

## 9. 构建与运行

### 9.1 环境要求

- JDK 17
- Android SDK Platform 34
- Gradle 8.4（仓库自带 wrapper）

### 9.2 构建

```bash
# Windows Git Bash
export ANDROID_HOME=D:/SDK
./gradlew :app:assembleDebug

# 安全策略 JVM 单测（不需要模拟器）
./gradlew :core:model:testDebugUnitTest :core:uxrestrictions:testDebugUnitTest
```

产物：

```text
app/build/outputs/apk/debug/app-debug.apk
```

说明：`local.properties` 未入库，构建时通过 `ANDROID_HOME` 或 IDE 的 SDK 配置提供路径。

### 9.3 运行

- 安装到 Android 模拟器或真机即可，无需连接车辆。应用包名为 `com.oem.car.app`，桌面显示名为 `AAOS Middleware`。
- 本项目验证所用的 AAOS 模拟器镜像（android-automotive x86_64）驾驶用户为 User 10；命令行安装需显式指定用户，否则默认用户下可能看不到应用：
  ```bash
  adb install -r --user 10 app/build/outputs/apk/debug/app-debug.apk
  ```
- 业务能力均为本地仿真；车辆域会在应用内额外启动一个 `:vehicle` 进程。没有网络、蓝牙、UWB、摄像头依赖。

### 9.4 跨进程验证

前置：应用已安装并能启动（脚本未检测到主进程时会自动拉起 `MainActivity`）；`adb` 在 PATH 中，Windows 下使用 Git Bash 运行。

```bash
# 默认 am crash，只杀 :vehicle，无需 root
./scripts/verify_ipc.sh -s emulator-5554

# userdebug/eng 镜像：adb root 后 kill -9
./scripts/verify_ipc.sh -s emulator-5554 --mode root

# 保留现有 logcat，脚本用 IPCVerify 标记截取本次日志
./scripts/verify_ipc.sh -s emulator-5554 --no-clear
```

脚本断言：主进程 PID 不变、`:vehicle` PID 变化、`scheduleReconnect in 3000ms`、`binder=android.os.BinderProxy`、`localInterface=null`、`registerListener callerPid=<main> myPid=<new>`、快照回推与 `oneway` 回调。

手工验证：

```bash
adb shell ps -A | grep com.oem.car.app
adb logcat -c
adb logcat -s VehicleServiceConn VehicleCoreService CarCoreDaemon
adb shell am crash --user 10 "$(adb shell pidof com.oem.car.app:vehicle | tr -d '\r')"
```

不要用 `am force-stop`、`am kill`、`killall` 或 IDE 的 Stop/Terminate：它们会杀掉整个应用（主进程 + `:vehicle`），无法验证只重启服务进程。

---

## 10. 后续演进路线（建议）

1. **车辆接入深化**：独立 `:vehicle` 进程 + `bindService` + AIDL + 崩溃重连已完成；下一步用 `CarPropertyManager`/VHAL 适配层替换 `CarCoreDaemon`，补权限/调用方校验、AIDL 版本、多客户端与状态持久化，保留 mock 作为 debug 实现。
2. **自动化验证**：安全策略 JVM 单测已完成（13 个用例）；下一步把单测与 `scripts/verify_ipc.sh` 接入 CI，再补 AIDL 跨进程集成测试、启动/重连长稳测试。
3. **系统 HMI**：补 CarSystemUI、CarLauncher、多屏/仪表独立显示、OccupantZone。
4. **平台能力**：多音区音频策略、电源/热管理、诊断日志、安全底座、真实 OTA。
5. **业务量产化**：数字钥匙 CCC 3.0、T-BOX 云通道、ADAS/SR、DMS/Profile、真实导航与媒体/语音/电话/投屏、能量管理、健康诊断。
6. **工程体系**：fake VHAL、单测/集成/HIL、CI/CD、性能基线、合规流程。

## 11. kotlin关键字使用规则
-场景	                                        适合 object   适合 class + 宿主
-无状态工具、常量、日志（CarLogger）	            ✅
-客户端进程内的信号缓存（VehiclePropertyAdapter）	✅
-纯进程内、无生命周期的仿真数据源	                ✅
-需要被 Service 托管、有销毁/重建语义		                      ✅
-需要注入 VHAL/Context、要写单测替换		                      ✅
-要防止调用方绕过进程边界		                                  ✅
