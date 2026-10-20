# CommercialCarApp — AAOS 智能座舱仿真架构

> 本文档对应**仓库根目录**的实际工程（`E:\gitHub\automotiveProject`），不是 `CommercialCarApp/` 子目录。
>
> **定位**：可运行的座舱 HMI + 分层架构原型。车辆信号、Binder 服务、数字钥匙、T-BOX、OTA、ADAS、DMS 均为**进程内仿真**，不连接真车、不连接云端，也不代表量产合规实现。

---

## 1. 项目定位与边界

### 1.1 这个项目做了什么

- 用 Kotlin + Jetpack Compose 实现一套横屏座舱 HMI，包含 6 个页面：中控仪表、液晶仪表、空调、多媒体、智能互联、车辆设置。
- 用 `app / feature / core` 三层模块化组织代码，能力下沉到 `core`，页面隔离在 `feature`。
- 用 `StateFlow` 做单向数据流，用 `VehicleRepository` 作为统一数据门面。
- 用 AIDL + `RemoteCallbackList` + `DeathRecipient` 演示车辆服务的接口形态（当前为同进程模拟）。
- 补齐 4 个扩展域的仿真闭环：数字钥匙、T-BOX/车云/OTA、ADAS 感知、DMS/多账户 Profile。

### 1.2 这个项目明确没有做

- 没有接入 `android.car`、CarService、VHAL 或真实车辆总线。
- 没有独立服务进程，`CarCoreDaemon` 与 App 在同一个进程。
- 没有 CarSystemUI、Launcher、多显示屏、副驾/后排 RSE。
- 没有真实数字钥匙协议（CCC 3.0 / UWB / BLE / NFC / SE）、真实 T-BOX 云通道、真实 OTA、真实 ADAS 感知与 3D SR 渲染。
- 没有测试体系、CI、签名发布、功能安全/信息安全流程。

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
| 并发 | kotlinx-coroutines-core 1.7.3 |
| 屏幕 | 单 Activity、横屏、6 个 Tab |

---

## 3. 目录结构与模块职责

```text
automotiveProject/
├── app/                              # 应用入口：MainActivity + DockBar + 6 Tab + 语音 Overlay
├── core/
│   ├── aidl/                         # IVehicleCoreManager / IVehicleEventListener
│   ├── audio/                        # PTT 音频焦点与 Ducking 仿真
│   ├── common/                       # 日志、Dispatchers、Result
│   ├── data/                         # VehicleRepository 接口与实现（统一门面）
│   ├── designsystem/                 # CarTheme、CarButton、CarCard、仪表盘组件
│   ├── model/                        # 车辆信号、档位、电源、扩展域数据模型
│   ├── power/                        # 电源状态机（STR / Garage / 关机预备）
│   ├── uxrestrictions/               # 行车防分心策略（演示阈值）
│   ├── vehicle/                      # CarCoreDaemon：AIDL Stub 仿真服务端
│   ├── vehicle-service/              # 连接管理 + AIDL 回调转 StateFlow
│   ├── digitalkey/                   # 【新增】数字钥匙仿真（区域/技术/门锁/启动授权）
│   ├── telematics/                   # 【新增】T-BOX/车云/OTA 仿真
│   ├── adas/                         # 【新增】ADAS/感知目标仿真
│   └── account/                      # 【新增】DMS/多账户/个性化仿真
└── feature/
    ├── dashboard/                    # 中控仪表 + 总线注入 Injector
    ├── cluster/                      # 液晶仪表（当前是同一 Activity 的 Tab）
    ├── climate/                      # 双区空调 UI
    ├── media/                        # 影音 UI（受防分心策略控制）
    ├── connected/                    # 【新增】智能互联：数字钥匙/T-BOX/ADAS/账户
    ├── settings/                     # 驾驶模式 + ADAS 静态展示
    └── voice/                        # 语音助手 PTT 浮层
```

### 3.1 模块职责与成熟度

| 模块 | 职责 | 成熟度 |
| :--- | :--- | :--- |
| `:app` | `MainActivity`、6 个 Tab、`VoiceAssistantOverlay` 全局浮层 | 可用 |
| `:core:aidl` | 车辆服务契约，`oneway` 异步回调 | 完整（但当前同进程） |
| `:core:vehicle` | `CarCoreDaemon` 模拟服务端，维护 `RemoteCallbackList` | 仿真 |
| `:core:vehicle-service` | 连接、死亡监听、注入代理、AIDL 回调转 `StateFlow` | 仿真 |
| `:core:data` | 聚合所有域状态，暴露统一 `VehicleRepository` | 可用 |
| `:core:uxrestrictions` | 按车速/档位计算防分心状态 | 演示规则 |
| `:core:power` | 电源状态迁移与监听器 | 骨架 |
| `:core:audio` | PTT 抢焦点、音乐 Ducking | 骨架 |
| `:core:digitalkey` | 靠近/远离、技术切换、门锁、启动授权 | 仿真闭环 |
| `:core:telematics` | 云连接、哨兵、远程命令、OTA 进度 | 仿真闭环 |
| `:core:adas` | NOA、车道保持、巡航速度、感知目标 | 仿真闭环 |
| `:core:account` | 人脸验证、座椅记忆、偏好温度、应用 Profile | 仿真闭环 |
| `:core:model` | `CarSignal<T>` 及所有业务数据类 | 完整 |
| `:core:designsystem` | 车规大触控组件与深色主题 | 可用 |
| `:core:common` | 日志、调度器、Result | 可用 |
| `:feature:*` | 各业务页面 | Dashboard/Connected 可交互，其余以展示为主 |

---

## 4. 架构与数据流

### 4.1 依赖关系

```text
app
 ├── feature:dashboard / cluster / climate / media / connected / settings / voice
 │     └── core:data
 │           ├── core:vehicle-service ──► core:vehicle ──► core:aidl
 │           ├── core:uxrestrictions / power / audio
 │           ├── core:digitalkey / telematics / adas / account
 │           └── core:common / model
 └── core:designsystem ──► core:model
```

已知依赖问题（保留现状，未在本轮重构）：`:core:vehicle-service` 为了直接拿 `CarCoreDaemon` 而依赖 `:core:vehicle`，客户端模块编译期依赖服务端实现，真实远程服务场景应只依赖 `:core:aidl`。

### 4.2 启动链路

```text
MainActivity
  └── DashboardScreen (默认 Tab)
        └── DashboardViewModel.init
              └── VehicleRepositoryImpl.initialize()   // AtomicBoolean 保证只执行一次
                    ├── VehicleServiceConnectionManager.connect()
                    │     ├── CarCoreDaemon.asBinder()  // 同进程，非 bindService
                    │     └── registerListener(...)
                    └── 常驻协程
                          ├── speedSignal → CarUxRestrictionsEngine.evaluate()
                          ├── gearSignal  → CarUxRestrictionsEngine.evaluate()
                          └── powerSignal → CarPowerLifecycleManager.onPowerStateChanged()
```

### 4.3 车辆信号链路（仿真）

```text
Dashboard Injector
  └── VehicleRepository.injectSpeed / injectGear / injectPower
        └── VehicleServiceConnectionManager
              └── CarCoreDaemon.injectMockXxx()
                    ├── 更新内部 StateFlow
                    ├── checkSafetyPolicy()
                    └── dispatch(IVehicleEventListener)
                          └── VehiclePropertyAdapter 更新 StateFlow
                                └── VehicleRepository 暴露
                                      └── Compose collectAsState()
```

注意：`CarCoreDaemon.asBinder()` 返回的是同进程对象，`linkToDeath` 不会在真实进程崩溃时触发，因此“3 秒自动重连”目前只是代码演示。

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
              └── applyUserProfile() 会把偏好温度写入 climateState（跨域联动示例）
```

### 4.5 PTT 链路

```text
Dashboard "方向盘 PTT" 或语音浮层
  └── repository.triggerPtt()
        ├── CarAudioManagerDelegate.triggerPttEvent()   // 本地音频状态：isPttActive=true
        └── CarCoreDaemon.triggerPttVoice()             // 仅更新 event Flow，当前没有消费者
VoiceAssistantOverlay "完成对话"
  └── repository.releasePtt()
        └── CarAudioManagerDelegate.releasePttEvent()   // isPttActive=false，浮层关闭
```

---

## 5. 页面与演示路径

| Tab | 可演示内容 |
| :--- | :--- |
| 中控仪表 | 车速/档位/电源状态；Injector 注入 P 档、D 档 80km/h、STR 休眠、唤醒、PTT；防分心视频拦截 |
| 液晶仪表 | 大字号车速、档位、静态 TBT 导航卡片、静态 ADAS 文案 |
| 空调 HVAC | 主/副驾温度 ±0.5°C、风量 1–7 挡（本地状态） |
| 多媒体影音 | 播放/暂停；行车状态自动切换为“视频防分心保护” |
| **智能互联** | 数字钥匙靠近/远离/切换技术/门锁；T-BOX 远程命令、哨兵、OTA 进度；ADAS NOA/车道/巡航；DMS 人脸、座椅、偏好温度、应用 Profile |
| 车辆设置 | 驾驶模式切换；ADAS 能力静态展示 |

**建议演示顺序**

1. 中控仪表点击“D档 (80 km/h)”，观察防分心状态变红，切到多媒体页看到视频被拦截。
2. 点击“P档 (0 km/h)”恢复安全状态。
3. 智能互联页把数字钥匙从远处逐步“靠近”，观察门锁与启动授权变化。
4. 点击“OTA 升级”，观察 CHECKING → DOWNLOADING → VERIFYING → APPLYING → SUCCESS。
5. DMS 点击“人脸识别”后点击“应用个性化到座舱”，切到空调页确认主驾温度被改为偏好温度。
6. 点击“方向盘 PTT”弹出语音浮层，点击“完成对话”关闭。

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

### 6.2 VehicleRepository（统一门面）

状态流：

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
```

命令：

```kotlin
fun initialize()
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
```

### 6.3 扩展域 Manager

**DigitalKeyManager**

- 区域顺序：`FAR(12m) → APPROACHING(8m) → WELCOME(4m) → UNLOCK(1.2m) → CABIN_INSIDE(0.5m)`
- 进入 `UNLOCK` 自动解锁，离开到 `APPROACHING/FAR` 自动落锁。
- 只有 `CABIN_INSIDE` 且 SE 鉴权通过才允许启动。
- `cycleTech()` 在 `UWB → BLE → NFC` 之间切换。

**TelematicsManager**

- 维护云连接、信号强度、哨兵模式、最近远程命令。
- `startOtaUpdate()` 用协程模拟 OTA 全流程；同一时间只允许一个 OTA Job。

**AdasManager**

- `toggleNoa()`、`toggleLaneKeep()`、`adjustCruiseSpeed(delta)`。
- 感知目标来自 `AdasPerceptionState.targets` 静态数据。

**AccountManager**

- 初始 `isFaceIdVerified=false`，`verifyFace()` 置为已验证。
- `cycleSeatPosition()` 在 1–4 之间循环。
- `adjustPreferredTemp(delta)` 限制在 16–30°C。
- `VehicleRepositoryImpl.applyUserProfile()` 会把偏好温度同步到主驾空调温度。

### 6.4 关键数据模型

- `CarSignal<T>`：值 + `SignalStatus` + 时间戳 + `areaId`。
- `VehicleGear`：P=1 / R=2 / N=4 / D=8。
- `PowerState`：RUNNING / STR_SLEEP / GARAGE_MODE / SHUTDOWN_PREPARE。
- `UxRestrictionState`：`isRestricted`、`noVideo`、`noKeyboard`、`maxListLength`。
- 扩展域：`DigitalKeyState`、`TelematicsState`、`OtaState`、`AdasPerceptionState`、`PerceptionTarget`、`UserProfile`。

---

## 7. 与商业量产项目的差距

> 本项目补齐的是**扩展域的第一层仿真闭环**。距离量产座舱仍有四层差距。

### 7.1 车辆接入层（最关键）

| 商业量产必备 | 当前状态 |
| :--- | :--- |
| CarService / VHAL / `CarPropertyManager` | 无 `android.car` 引用 |
| 独立车辆服务进程 + `bindService` | 同进程 `object CarCoreDaemon` |
| 属性读写、订阅、权限、areaId、超时、限流 | 只有 `injectMock*` 和内存 StateFlow |
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
| 导航 | 静态 TBT 文案 | 地图引擎、离线/在线导航、EV 充电路径 |
| 语音 | 浮层占位 | 唤醒词、ASR、NLU、TTS、技能生态 |
| 电话/蓝牙 | 无 | HFP、通讯录、通话 UI、蓝牙连接管理 |
| 投屏 | 无 | CarPlay / HiCar / Android Auto |
| 相机 | 无 | AVM、DVR、哨兵 |
| 能量管理 | 无 | 电池、充电、热管理、续航 |
| 账户 | 本地 Profile | 账号体系、云端同步、多用户隔离 |

### 7.4 量产工程体系

| 能力 | 当前状态 |
| :--- | :--- |
| 单元/集成/UI/HIL 测试 | 无 |
| CI/CD、签名、发布、灰度 | 无 |
| 性能/内存/启动/稳定性基线 | 无 |
| 日志、埋点、远程诊断 | 无 |
| ISO 26262 / ISO 21434 / GB 44495 / GB 44496 等流程 | 无 |

> 说明：行车防分心规则在本项目中是**演示阈值**，参考 NHTSA Driver Distraction Guidelines；GB 44495 是整车信息安全要求，不应作为驾驶员分心判定依据。旧版 README 与代码中的相关引用已修正。

---

## 8. 已知限制

1. **IPC 是仿真**：`CarCoreDaemon.asBinder()` 同进程直连，`DeathRecipient` 不会因进程崩溃触发。
2. **服务端实现渗入客户端**：`:core:vehicle-service` 依赖 `:core:vehicle`，AIDL 抽象不彻底。
3. **只有 Dashboard 触发初始化**：默认 Tab 保证启动时会执行，但架构上其他入口不应依赖这一点。
4. **安全策略双份**：`CarCoreDaemon.checkSafetyPolicy()` 与 `CarUxRestrictionsEngine.evaluate()` 各算一套；`VehiclePropertyAdapter.distractionSignal` 当前无消费者。
5. **Feature 直接依赖实现**：多数页面默认注入 `VehicleRepositoryImpl` 单例，没有 DI；测试与替换成本高。
6. **仪表不是独立屏**：Cluster 是同一 Activity 的一个 Tab，不是多显示设备。
7. **无测试**：仓库没有 `src/test` 或 `src/androidTest`。
8. **仓库卫生**：历史构建产物仍被 git 跟踪；`CommercialCarApp/` 是未跟踪的迁移残留（无源码），不建议打开。

---

## 9. 构建与运行

### 环境要求

- JDK 17
- Android SDK Platform 34
- Gradle 8.4（仓库自带 wrapper）

### 构建

```bash
# Windows Git Bash
export ANDROID_HOME=D:/SDK
./gradlew :app:assembleDebug
```

产物：

```text
app/build/outputs/apk/debug/app-debug.apk
```

说明：`local.properties` 未入库，构建时通过 `ANDROID_HOME` 或 IDE 的 SDK 配置提供路径。

### 运行

- 安装到 Android 模拟器或真机即可，无需连接车辆。
- 所有能力均为本地仿真；没有网络、蓝牙、UWB、摄像头依赖。

---

## 10. 后续演进路线（建议）

1. **真实车辆接入**：拆出独立 `service/vehicle-service` 进程，客户端只依赖 `:core:aidl`；用 `CarPropertyManager`/VHAL 适配层替换 `CarCoreDaemon`，保留 mock 作为 debug 实现。
2. **系统 HMI**：补 CarSystemUI、CarLauncher、多屏/仪表独立显示、OccupantZone。
3. **平台能力**：多音区音频策略、电源/热管理、诊断日志、安全底座、真实 OTA。
4. **业务量产化**：数字钥匙 CCC 3.0、T-BOX 云通道、ADAS/SR、DMS/Profile、导航/媒体/语音/电话/投屏。
5. **工程体系**：fake VHAL、单测/集成/HIL、CI/CD、性能基线、合规流程。
