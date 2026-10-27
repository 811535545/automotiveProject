# CommercialCarApp — AAOS 智能座舱仿真架构

> 本文档对应**仓库根目录**的实际工程（`E:\gitHub\automotiveProject`），Gradle 的 `rootProject.name` 仍为 `CommercialCarApp`；`CommercialCarApp/` 子目录是无源码的迁移残留（仅构建/IDE 缓存），不是源码工程。
>
> **定位**：可运行的座舱 HMI + 分层架构原型。车辆信号域默认由独立 `:vehicle` 进程通过 AIDL/Binder 提供，已实测服务进程被杀后 3 秒自动重连；在 AAOS 镜像上会尝试通过反射读取/订阅原生 `CarPropertyManager` 的车速与档位（只读，权限不足自动回退 Mock）。另有 CAN/DBC 编解码、多音区音频仲裁、多屏分发、3D 车身数字孪生与 AppFunctions 控车演示。

---

## 1. 项目定位与边界

### 1.1 这个项目做了什么

- 用 Kotlin + Jetpack Compose 实现一套横屏座舱 HMI，包含 10 个 Tab：中控、仪表、导航、空调、影音、电话、互联、能量、健康、设置。
- 用 `app / feature / core` 三层模块化组织代码，能力下沉到 `core`，页面隔离在 `feature`。
- 用 `StateFlow` 做单向数据流，用 `VehicleRepository` 作为统一数据门面。
- 用 AIDL + 独立 `:vehicle` 进程 + `bindService` + `RemoteCallbackList` + `DeathRecipient` 实现车辆服务的跨进程链路，并支持服务进程崩溃后的自动重连与状态快照回推。
- 客户端模块只依赖 AIDL 契约，服务端实现放在 `:core:vehicle`，由 `VehicleCoreService` 在 `:vehicle` 进程中托管。
- **【最新扩充】6 大车规级高级特性与架构支撑**：
  1. **可选 AAOS `android.car` & Mock VHAL 双通道 (`VehiclePropertyProvider`)**：默认 Mock Binder；在带 `android.car` 的 AAOS 镜像上尝试读取/订阅 `PERF_VEHICLE_SPEED` 与 `GEAR_SELECTION`，只读桥接，权限不足或无 CarService 时整体回退 Mock。
  2. **ISO 11898-1 CAN/DBC 信号协议编解码器 (`CanMessageCodec`)**：模拟网关与 VHAL 底层交互，带 Rolling Counter 循环计数器与 ISO SAE J1850 **CRC8 校验码计算与验证**。
  3. **符合 AAOS 标准的 3 音区音频焦点仲裁 (`MultiZoneAudioManager`)**：支持主驾 (Driver Zone 0)、副驾 (Passenger Zone 1)、后排 (Rear Zone 2) 物理音区隔离，导航自动 Ducking 降音与 PTT 全车抢占。
  4. **座舱多屏分发与仪表屏 Display 联动 (`CockpitDisplayManager`)**：模拟 `DisplayAreaPolicy` 跨屏分发，支持将导航 Turn-By-Turn (TBT) 指引卡片实时投射至仪表屏。
  5. **依赖注入容器 (`VehicleServiceContainer`) 与完整单元测试**：替换静态硬编码单例，提供 Service Locator / DI Container 架构，并通过 100% 通过的 JVM 单元测试。
  6. **2.5D/3D 车身数字孪生渲染器 (`Vehicle3dVisualizer`) 与 Android 15 AppFunctions AI 控车 Handler (`CarAppFunctionsHandler`)**：实时绘制车身、灯光、门锁、胎压与高速风阻线条，并暴露系统级 AI Agent 控车意图。
- 补齐 4 个扩展域的仿真闭环：数字钥匙、T-BOX/车云/OTA、ADAS 感知、DMS/多账户 Profile。
- 补齐 4 个座舱核心域的仿真闭环：导航（路线/逐段引导/仪表联动）、电话（蓝牙/拨号/来电/计时）、能量（SOC/充电/预约）、健康（胎压/自检/DTC 扫描）。

### 1.2 这个项目明确没有做

- 车辆服务虽有独立 `:vehicle` 进程，但仍与 App 在**同一 APK、同一 UID** 内，`exported=false`，没有权限校验/AIDL 版本管理。
- AAOS 原生通道只读桥接车速与档位，需要 `CAR_SPEED` / `CAR_POWERTRAIN` 系统权限；本仓库只在普通 Android 设备验证了 Mock 回退，AAOS 原生路径尚未附实机/镜像验证日志。
- 没有 CarSystemUI、Launcher、真实多显示屏物理硬隔离。
- 没有真实数字钥匙协议（CCC 3.0 / UWB / BLE / NFC / SE）、真实 T-BOX 云通道、真实 OTA、真实 ADAS 感知与 3D SR 渲染。
- 没有真实地图引擎、蓝牙 HFP/PBAP 链路、BMS/充电桩协议、UDS/DoIP 诊断链路。

因此：**它适合做架构演示、AAOS 学习、面试项目；不能直接作为量产座舱交付。**

> 📄 **同级目录重点架构复盘与面试文档**：
> - 📄 [ARCHITECTURE_PORTFOLIO.md](ARCHITECTURE_PORTFOLIO.md)：**智能座舱 Android 架构师 / 高级工程师简历项目经历 (STAR 法则) 与技术面试深水区复盘**
> - 📄 [MIDDLEWARE_DATA_FLOW.md](MIDDLEWARE_DATA_FLOW.md)：**车载中间件全景数据流向图与 8 层内部流水线透视 (从物理电信号到屏幕像素重绘)**

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
| VHAL 桥接 | 抽象 `VehiclePropertyProvider`（Mock Binder 进程 ↔ 可选 AAOS `CarPropertyManager` 车速/档位只读桥接） |
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
│   ├── vehicle-service/              # VehiclePropertyProvider + AaosCarPropertyBridge (Mock/AAOS 双通道)
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
| `:core:model` | `VehiclePropertyIds.kt` / `AaosSignalMapper.kt` | 定义 AAOS 官方属性 ID（车速 `0x11600207`、档位 `0x11400400`、电池 `0x11600309` 等），并负责 m/s→km/h、AAOS 档位码→项目档位码、CarPropertyValue 状态映射。 |
| `:core:model` | `CanFrame.kt` & `CanMessageCodec` | ISO 11898-1 CAN 总线数据帧模型，包含 CAN ID `0x120` DBC 信号编解码器，计算并验证 Rolling Counter 和 ISO SAE J1850 CRC8 校验码。 |
| `:core:vehicle-service` | `VehiclePropertyProvider.kt` / `AaosCarPropertyBridge.kt` | 提供 `MockBinderPropertyProvider`（双进程 Binder）与 `AaosCarPropertyProvider`（反射连接 `CarPropertyManager`，读取并订阅车速/档位；失败整体回退 Mock）。 |
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

### 4.2 CAN 报文打包与车辆属性接收链路（Mock / AAOS）

```text
Dashboard Injector（主进程，仅 Mock 通道生效）
  └── VehicleRepository.injectSpeed / injectGear / injectPower
        └── VehiclePropertyProvider (MockBinder)
              └── IVehicleCoreManager.Proxy.transact(...) ── Binder IPC ──►
                    CarCoreDaemon（:vehicle 进程）
                      ├── CanMessageCodec.encodeVehicleSignal() 打包 8 字节 CAN 报文
                      ├── 计算 CRC8 校验码与 Rolling Counter (0~15)
                      ├── 打印 Log.d(TAG, "CAN=CAN[0x120] DLC=8 Data=...") Hex Dump
                      ├── checkSafetyPolicy()
                      └── dispatch(RemoteCallbackList) 异步通知客户端

AAOS 镜像（可选，只读）
  CarPropertyManager.registerCallback(PERF_VEHICLE_SPEED / GEAR_SELECTION)
    └── AaosCarPropertyBridge 解包 CarPropertyValue
          └── AaosSignalMapper：m/s→km/h、AAOS 档位码→项目档位码
                └── VehiclePropertyAdapter → StateFlow → Compose
```

> 原生 AAOS 通道是只读的，Dashboard 的注入按钮只对 Mock 通道生效；没有 `CAR_SPEED` / `CAR_POWERTRAIN` 权限时会整体回退 Mock，不会出现两个数据源混用。

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

### 4.5 座舱中间件全景数据流水线 (从“物理线缆”到“屏幕像素点”)

> 本文档专为解决“看懂了中间件抽象名词，但不知道数据具体怎么流转、字节怎么变成屏幕像素”的痛点而设计。详细拆解从物理线缆电信号、Linux 内核 SocketCAN、C++ VHAL、Java CarService、中间件路由到 Compose UI 像素渲染的全链路。
>
> **实现边界**：以下是典型 AAOS 目标链路，不代表本仓库已全部实现。本项目已落地第 6~8 层的 Java 适配、安全策略与 Compose 渲染；第 1~5 层属于概念链路或本地模拟（没有真实 CAN/VHAL，原生属性通道尚未在 AAOS 镜像实测）。各场景的真实覆盖情况见文末“本项目实现情况”。

#### 1. 主流水线：从“物理线缆电信号”到“屏幕像素点” (End-to-End Signal Pipeline)

这是汽车中 90% 车辆信号（车速、档位、门锁、胎压、水温）运行的完整全生命周期流向：

```text
┌─────────────────────────────────────────────────────────────────────────────────────────────┐
│ [层级 1: 物理线缆层] 车身传感器 / 发动机 ECU                                                │
│   └── 通过双绞线 (CAN_H 3.5V / CAN_L 1.5V) 广播差分电压波形                                 │
└───────────────────────────┬─────────────────────────────────────────────────────────────────┘
                            │ 物理电信号
                            ▼
┌─────────────────────────────────────────────────────────────────────────────────────────────┐
│ [层级 2: 车机硬件芯片层] CAN 收发器 (TJA1051) + CAN 控制器 (RK3588 内置 CAN IP)             │
│   └── 收发器将电压差还原为逻辑 0/1；控制器完成 CAN 帧 CRC15 校验并打包为 8 字节 Payload:    │
│       [0x17, 0x70, 0x08, 0x00, 0x00, 0x00, 0x05, 0x9A]                                      │
│       注: 末字节 0x9A 为应用层/DBC CRC8，CAN 帧本身由硬件 CRC15 保护                        │
└───────────────────────────┬─────────────────────────────────────────────────────────────────┘
                            │ 物理数据帧 (struct can_frame)
                            ▼
┌─────────────────────────────────────────────────────────────────────────────────────────────┐
│ [层级 3: Linux 内核驱动层] SocketCAN 内核网卡驱动 (can0)                                    │
│   └── 内核将 CAN 数据暴露为 Linux 虚拟网络套接字 (can0)，可通过 `candump can0` 抓包         │
└───────────────────────────┬─────────────────────────────────────────────────────────────────┘
                            │ Linux Socket/file descriptor
                            ▼
┌─────────────────────────────────────────────────────────────────────────────────────────────┐
│ [层级 4: C++ Native VHAL 层] VehicleHal / PropertyHalService (HAL 硬件抽象)                 │
│   1. 根据 DBC 协议解析 Byte 0~1: 0x1770 = 6000 ──► 6000 / 100 = 60.0 km/h                   │
│   2. 校验 Byte 7 应用层 CRC8 码 (0x9A): 校验正确                                            │
│   3. 封装为 C++ 结构体 VehiclePropValue { prop: PERF_VEHICLE_SPEED, value: 60.0f }          │
└───────────────────────────┬─────────────────────────────────────────────────────────────────┘
                            │ Binder IPC 跨进程调用 (ICarProperty)
                            ▼
┌─────────────────────────────────────────────────────────────────────────────────────────────┐
│ [层级 5: Java Framework 层] CarService 进程 (com.android.car) 中的 CarPropertyService       │
│   1. 进行权限校验: checkCallingPermission("android.car.permission.CAR_SPEED")               │
│   2. 通过 Binder 触发回调代理 ICarPropertyEventListener.onPropertyEvent()                   │
└───────────────────────────┬─────────────────────────────────────────────────────────────────┘
                            │ Binder IPC (ICarPropertyEventListener)
                            ▼
┌─────────────────────────────────────────────────────────────────────────────────────────────┐
│ [层级 6: 车载中间件适配层] AaosCarPropertyBridge & VehiclePropertyAdapter                   │
│   1. 解包 CarPropertyValue ──► 封装为 CarSignal<Float>(value=60.0f, status=VALID, ...)      │
│   2. 更新内部响应式数据流: _speedSignal.value = newSignal                                   │
│   3. 【信号降级看门狗】：若 > 5000ms 未收到更新，看门狗自动将 status 改为 UNAVAILABLE       │
└───────────────────────────┬─────────────────────────────────────────────────────────────────┘
                            │ StateFlow / Coroutine Flow
                            ▼
┌─────────────────────────────────────────────────────────────────────────────────────────────┐
│ [层级 7: 状态评估与安全策略中间件] DrivingSafetyPolicy & CarUxRestrictionsEngine            │
│   1. 评估车速 speed = 60.0 km/h > 0.5 km/h ──► 判定 restriction = true                      │
│   2. 更新防分心状态: noVideo = true, maxListLength = 10                                     │
└───────────────────────────┬─────────────────────────────────────────────────────────────────┘
                            │ Compose collectAsState()
                            ▼
┌─────────────────────────────────────────────────────────────────────────────────────────────┐
│ [层级 8: 声明式 UI 渲染层] Jetpack Compose Recomposition                                    │
│   1. SpeedGaugeComponent: speedSignal 改变 ──► 重新绘制屏幕数字 "60"                        │
│   2. Vehicle3dVisualizer: speed > 0 ──► Canvas 启动后退风阻线条流动动画                     │
│   3. 影音卡片: noVideo == true ──► 触发红色安全拦截遮罩 "行车视频安全拦截生效"              │
└─────────────────────────────────────────────────────────────────────────────────────────────┘
```

#### 2. SOA (SOME/IP) 服务化通信数据流向：面向服务 RPC 与事件订阅

传统 CAN 是面向信号的硬编码广播，而现代 SOA 中间件是面向服务 (Service-Oriented) 的动态发现与订阅：

```text
[智驾域控制器 (AdasDomainController)]
     │ 1. 启动并提供服务: ServiceID = 0x2001, InstanceID = 0x0001
     │ 2. SOME/IP-SD (Service Discovery) 在 UDP 224.0.0.1:30490 组播 OfferService 报文
     ▼
[座舱中间件 SOA 注册表 (SoaServiceRegistry)]
     │ 1. 收到 SOME/IP-SD 组播 ──► 在注册表中标记 Service 0x2001 isDiscovered = true
     │ 2. 发送 SOME/IP-SD SubscribeEventgroup 报文，订阅 3D 感知目标事件组 (EventGroup 0x0001)
     ▼
[网络 UDP/TCP Socket 接收线程]
     │ 1. 收到二进制 SOME/IP 报文包: [Header|ServiceID|MessageID|Length|RequestID|Payload...]
     │ 2. 反序列化 (Unmarshalling) 解出 3D 目标:
     │    - Target 1: Sedan (x = 25m, y = 0m, speed = 78km/h)
     │    - Target 2: Truck (x = 12m, y = -3.5m, speed = 70km/h)
     ▼
[仪表 3D 感知画卷 (AdasSrVisualizer)]
     │ 1. 传入 AdasPerceptionState(targets = [Target1, Target2])
     │ 2. 透视公式计算 3D 转换: TargetX = CenterX + (y / 10m) * Width, TargetY = HorizonY + (x / 60m)
     │ 3. Canvas 实时重绘: 在画面中画出前车青色 3D 框与卡车橙色 3D 框
```

#### 3. 车载多音区音频抢占与 Ducking 降音数据流

当驾驶员在听歌时，导航突然发出“前方 200 米右转”提示，音频中间件的数据与控制流向如下：

```text
[导航中间件 (NavigationManager)]
     │ 1. 准备播报导航语音 ──► 创建 AudioAttributes(USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
     │ 2. 调用 MultiZoneAudioManager.requestAudioFocus(AudioZone.DRIVER_ZONE_0, CarAudioUsage.NAVIGATION)
     ▼
[多音区音频中间件 (MultiZoneAudioManager)]
     │ 1. 检查音区归属: 确认播报请求来自 DRIVER_ZONE_0 (主驾音区)
     │ 2. 执行音区仲裁逻辑 (Zone Focus Arbitration):
     │    ├── 主驾音区 (DRIVER_ZONE_0): 媒体音量 VolumeMultiplier 降至 0.3f (Ducking 降音 70%)
     │    └── 副驾音区 (PASSENGER_ZONE_1): 媒体音量 VolumeMultiplier 维持 1.0f (副驾听歌不降音，物理音区隔离)
     ▼
[车载 HAL 音频混音器 / AudioFlinger / DSP 芯片]
     │ 1. 主驾通道 PCM 音频采样点乘以 0.3 (声音变小)
     │ 2. 混音器将 1.0 倍的导航语音 PCM 数据叠加到主驾通道
     │ 3. 送入物理 DSP 芯片输出至主驾扬声器 (喇叭 0)
```

#### 4. 电源状态机与 STR (Suspend-to-RAM) 休眠/唤醒数据流

当车主锁车离开，车机进入休眠以及重新解锁唤醒时的数据流向：

```text
[整车电源控制器 (PowerECU)]
     │ 发送电源迁移指令 CAN 报文: PowerState -> STR_SLEEP (1)
     ▼
[电源状态机中间件 (CarPowerLifecycleManager)]
     │ 1. 更新内部 powerState = STR_SLEEP
     │ 2. 向所有注册中间件广播 onSuspendToRam() 事件
     ▼
[各类中间件休眠处理 (Suspend Handlers)]
     │ ├── 信号中间件: 暂停高频 VHAL 定时器，保存最后有效状态快照至 NVM/MMKV 闪存
     │ ├── 网络中间件: 断开非必要云端 MQTT/WebSocket 套接字
     │ └── 渲染中间件: 暂停 Compose 3D Canvas 动画重绘，释放 GPU 渲染句柄
     ▼
[整车 SOC 芯片进入 STR 深度挂起 (RAM 保持供电，CPU/GPU 停电)]
     │
     │ ─── (车主拉车门解锁，电源 ECU 瞬间高电平唤醒开发板) ───
     ▼
[电源状态机中间件 (CarPowerLifecycleManager)]
     │ 1. 收到物理中断唤醒，更新 powerState = RUNNING (0)
     │ 2. 向所有中间件广播 onWakeUp() 事件
     ▼
[快速自愈恢复 (Warm Recovery)]
     │ ├── VehiclePropertyProvider: 自动重新建立与 VHAL / :vehicle 进程的 Binder 通信
     │ ├── 客户端: 发送 registerListener，接收服务端推回的状态快照
     │ └── UI 界面: 恢复上一次的座舱仪表与 3D 车模，无需冷启动
```

#### 总结：数据流向全景一览表

| 场景 | 数据起点 | 中间件处理过程 | 数据终点 |
| :--- | :--- | :--- | :--- |
| 车速更新 | 轮速传感器电信号 | CAN 收发器 → SocketCAN → VHAL（DBC 解析 + CRC 校验）→ CarPropertyService → Adapter（看门狗） | SpeedGaugeComponent 绘制数字与风阻线条 |
| 行车防分心 | 车速 > 0.5km/h | DrivingSafetyPolicy（迟滞算法）→ CarUxRestrictionsEngine 动态生成 restriction | 视频播放器弹出红色安全拦截遮罩 |
| 导航降音 | 导航指引触发 | MultiZoneAudioManager 识别 Driver Zone 0 → 动态将主驾媒体音量 Ducking 至 0.3 | 混音器 DSP 将主驾音乐变小，导航声音清晰输出 |
| 3D 智驾 SR | 毫米波雷达/摄像头 | SOME/IP-SD 发现服务 → Socket 接收解包 3D 目标坐标 (X, Y) → 透视投影计算 | AdasSrVisualizer Canvas 画出前车/卡车 3D 立体框 |
| STR 休眠唤醒 | 12V 电压/车门开合 | CarPowerLifecycleManager 广播 Suspend → 刷新缓存/断开 Socket → 唤醒重连 Binder | 恢复座舱上次显示状态 |

#### 本项目实现情况（对照上图）

- **车速更新**：`AaosCarPropertyBridge`、`VehiclePropertyAdapter`（含 5 秒超时降级）和仪表渲染已实现；CAN/VHAL 未接真实硬件，AAOS 原生属性路径尚未实测。
- **行车防分心**：`DrivingSafetyPolicy` 迟滞判定与限制状态已实现，完全在本地计算。
- **导航降音**：`MultiZoneAudioManager` 的音区音量倍率仲裁已实现；没有 AudioFlinger/DSP 的真实音频输出链路。
- **3D 智驾 SR**：`AdasSrVisualizer` 的透视绘制已实现，数据来自 `core:adas` 的本地仿真；SOME/IP 服务发现与 Socket 接收未实现。
- **STR 休眠唤醒**：电源状态机与监听广播为模拟；真实 STR、NVM 持久化、MQTT/WebSocket 断开未实现；Binder 死亡重连已实测，固定 3 秒，不是毫秒级。

---

## 5. 核心接口与组件代码示范

### 5.1 VehiclePropertyProvider 双通道抽象

工厂固定返回 `AaosCarPropertyProvider`，由它在运行时决定走哪条通道：

- 设备存在 `android.car.Car`，且 `CarPropertyManager` 能读取并订阅车速/档位 → 原生 AAOS 通道（只读）；
- 类不存在、CarService 连不上、属性不可用或权限不足 → Mock Binder 通道（`:vehicle` 进程，可注入）。

原生通道的反射细节封装在 `AaosCarPropertyBridge`，普通设备不会加载这部分代码。完整调用链是：

```text
CarPropertyManager → CarPropertyService → PropertyHalService → VehicleHal → IVehicle → VHAL
```

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

// 工厂返回双通道实现：原生连接失败时整体回退 Mock Binder，不会混用两个数据源
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

仓库包含防分心规则、CAN 报文 CRC8 编解码、多音区音频仲裁与 AAOS 信号映射的 JVM 单元测试：

```bash
./gradlew testDebugUnitTest
```

通过的测试套件：
- `com.oem.car.core.model.CanMessageCodecTest`（CAN 报文打包、解包与 CRC8 校验通过）
- `com.oem.car.core.audio.MultiZoneAudioManagerTest`（主驾导航 Ducking 降音与全音区 PTT 压音校验通过）
- `com.oem.car.core.model.DrivingSafetyPolicyTest`
- `com.oem.car.core.uxrestrictions.CarUxRestrictionsEngineTest`
- `com.oem.car.core.model.AaosSignalMapperTest`（m/s→km/h、AAOS/项目档位码映射、CarPropertyValue 状态映射）

---

## 7. 总结与后续路线

本项目是一个可运行的座舱 HMI 与架构演示：包含 Binder 跨进程车辆信号、Mock/AAOS 双通道属性桥接（原生侧只读车速/档位）、CAN 报文编解码、多音区音频仲裁、多屏分发与 AI 控车演示。它适合 AAOS 学习与面试展示，但原生 AAOS 通道仍需在带相应系统权限的 AAOS 镜像上验证，不能当作量产实现。
如果你未来去面试 OEM（车厂）或 Tier-1 架构师，决定你薪资上限的正是以下这些**“车规级防坑经验”**：
1. 极致的长稳与内存零泄漏（7×24 小时开机）
   •
   场景：车机系统和手机不同，它在 STR（内存挂起休眠）模式下可能连续运行几个月不重启。
   •
   经验：不能有任何 Activity、Context、Binder 代理或 Flow 监听器泄漏；每一个 Binder 接口都必须有 DeathRecipient 死亡重连与状态自动恢复机制。
2. 冷启动“早显”与“早期音频” (Early Display & Early Audio)
   •
   场景：国家法规规定，车主启动汽车后，1.5 秒内必须立刻显示倒车影像 (RVC)。但此时 Android 系统（包括 CarService）甚至都还没完全启动完成！
   •
   经验：如何在 Android 庞大的 Java 系统服务加载完成前，利用 Linux 内核层/轻量 C++ 进程直接抢占屏幕绘制倒车画面、播放开机音效。
3. 极端的电源与电压突变处理 (Power Resilience)
   •
   场景：车主打火瞬间 12V 蓄电池电压骤降、极端寒冷/高温天气、蓄电池亏电保护、突然断电。
   •
   经验：如何保证数据（如仪表里程、导航偏好）在掉电瞬间不损坏，如何在 STR 唤醒的 500ms 内瞬间恢复上次的座舱状态。
4. 总线网络抖动与信号降级 (Signal Degrade & Fault Tolerance)
   •
   场景：当 CAN 总线负载率过高、或者某个传感器硬件故障导致 CAN 报文丢帧/超时。
   •
   经验：App 不能崩溃或显示乱码，必须有信号降级机制（如：传感器超时 500ms 未发数据，界面自动显示“数据暂不可用”或“灰色虚线”，并记录 DTC 故障码）。
5. 严格的信息安全与权限控制 (Cyber Security & SecOC)
   •
   场景：防止恶意 App 伪造 Binder 消息向车机下发“解锁车门”或“关闭大灯”的指令。
   •
   经验：自定义 android.car.permission 权限校验、SELinux 强制安全访问控制、SecOC 报文硬件加密与防重放攻击。
