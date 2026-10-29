# 智能座舱 Android 架构师 / 高级工程师 —— 简历项目经历与技术亮点复盘

> 本文档专为 **车载 Android 架构师 / 高级 HMI / 中间件工程师** 岗位面试与简历撰写深度定制。
> 区别于工程代码的 `README.md`，本文档采用 STAR 法则（Situation, Task, Action, Result）提取项目中最具技术含金量与壁垒的核心卖点，可以直接复制到简历中，并包含面试官常问的深度技术追问与标准回答。

---

## 📄 简历项目经历模块（可以直接复制到简历）

### 项目名称：CommercialCarApp — 企业级 AAOS 智能座舱中间件与 HMI 分层架构原型
* **项目角色**：智能座舱 Android 核心架构师 / 主导开发者
* **技术栈**：Kotlin | Jetpack Compose | Coroutines / StateFlow | Binder / AIDL | AAOS VHAL Bridge | CAN DBC Codec | Multi-Zone Audio | Hilt/DI | Compose 3D Canvas

#### 项目描述：
主导开发了一套符合车规级标准的 **Android Automotive OS (AAOS) 企业级智能座舱中间件与 HMI 分层架构原型**。项目涵盖 30 个 Gradle 子模块，完整实现了应用层（HMI）、中间件数据门面（Repository）、原生 VHAL 双模桥接层以及独立进程通信（`:vehicle` Daemon）。引入了底层 CAN 报文打包解包、多音区音频焦点仲裁、迟滞区间防分心引擎、信号超时降级看门狗及 3D SR 感知渲染画卷等车规级高稳健性架构。

#### 核心贡献与技术亮点（STAR 法则）：
1. **多进程 Binder IPC 与服务自愈架构**：设计并实现了跨进程车辆通信服务 (`:vehicle` 独立进程)，基于 `AIDL` + `RemoteCallbackList` + `DeathRecipient` 机制实现进程隔离。引入带抖动的 **指数退避重连算法 (Exponential Backoff)**，实测服务进程被杀死后在 1~3 秒内实现无感断线自愈与状态快照回推。
2. **车规级 VHAL 双通道桥接抽象 (Dual-Channel VHAL Bridge)**：针对不同部署环境，设计了 `VehiclePropertyProvider` 抽象层。通过 Kotlin 反射机制动态感知原生 AAOS 系统 `android.car.CarPropertyManager`（支持 `PERF_VEHICLE_SPEED` / `GEAR_SELECTION`），在普通安卓设备上平滑降级至 Mock Binder 进程，实现零编译期依赖与多端适配。
3. **CAN / DBC 总线信号编解码与校验**：基于 ISO 11898-1 标准实现了 `CanMessageCodec` DBC 协议编解码引擎。对 CAN ID `0x120` 车速/档位/电源信号进行二进制 Pack/Unpack，内置 0~15 滚动计数器 (Rolling Counter) 与 **ISO SAE J1850 CRC8 校验算法**，实现总线级防篡改与正确性验证。
4. **多音区音频焦点仲裁引擎 (Multi-Zone Audio Arbitrator)**：遵循 AAOS 规范开发了 `MultiZoneAudioManager`，管理主驾 (Driver Zone 0)、副驾 (Passenger Zone 1)、后排 (Rear Zone 2) 物理音区。实现了主驾导航播报（`USAGE_NAVIGATION`）时主驾媒体音量自动 Ducking 降至 30% 而副驾保持 100% 的物理音区隔离策略，以及 PTT 语音按键的全全音区强压音。
5. **车规级信号降级看门狗与迟滞防分心引擎**：
   - **信号看门狗 (Signal Timeout Watchdog)**：设计了 5000ms 信号超时降级机制，总线信号中断时自动将 `CarSignal` 降级为 `UNAVAILABLE`，UI 自动显示 `-- KM/H` 警告色，拒绝界面僵死旧值。
   - **迟滞防分心引擎 (Anti-Flicker Hysteresis)**：设计了车速 `> 0.5 km/h` 触发、必须降速至 `< 0.2 km/h` 且挂 P 档方可解除的 Hysteresis 迟滞逻辑，彻底解决临界车速引起的 UI 闪烁。
6. **座舱多分辨率自适应布局 (Car Adaptive Layout)**：基于 Compose 开发了 `CarAdaptiveLayout` 响应式容器，动态计算 `screenWidthDp` 与宽高比。自动在 **贯穿式超宽屏 (21:9 三列并行)**、**标准横屏 (16:9 左右分栏)** 和 **低分辨率小屏 (单列流动)** 之间平滑切换。
7. **3D 智驾 SR 感知与数字孪生画卷**：基于 Compose Canvas 与 3D 透视投影算法（Perspective Projection），开发了 `AdasSrVisualizer` 与 `Vehicle3dVisualizer`，零文件体积实现 NOA 发光领航廊道、后退流动车道栅格、自车光环以及前车/卡车/行人 3D 空间感知投影。
8. **Android 15 AppFunctions AI 控车解析引擎**：接入 `CarAppFunctionsHandler`，将温度调节、车门锁止、驾驶模式切换等控车能力映射为系统级 AppFunctions 意图，支持 AI 语音 Agent 无 UI 执行控车逻辑。

---

## 🎯 面试深水区高频问题与标准回答策略 (Interview Q&A Strategy)

### 问题 1：你在项目里是如何处理 Binder 跨进程死亡与断线恢复的？
> **面试官意图**：考察对 Android Binder 底层机制、进程生命周期及防死锁处理的理解。

**标准回答**：
1. **死亡监听**：客户端通过 `IBinder.linkToDeath()` 挂载 `DeathRecipient` 监听器。当 `:vehicle` 进程崩溃或被 kill 时，`binderDied()` 立即触发。
2. **清理与退避重连**：触发后立刻执行 `clearConnection()` 释放旧 BinderProxy，取消残留回调。重连不采用固定延时，而是采用 **带抖动的指数退避算法 (Exponential Backoff)**（`1s -> 2s -> 4s -> max 10s`），避免服务频繁 crash 时产生连接风暴。
3. **状态快照重推**：重新 `bindService` 成功后，客户端向服务端发送 `registerListener`。服务端在 `RemoteCallbackList` 注册成功后，立刻主动调用 `syncStateTo()` 回推当前最新车速、档位与防分心快照，确保客户端 UI 迅速恢复，不需要等待下一次信号触发。

---

### 问题 2：你们是如何处理 CAN 总线信号解析与校验的？
> **面试官意图**：考察对车载总线协议、DBC 格式及数据安全/防篡改的掌握。

**标准回答**：
1. **DBC 帧打包/解包**：我们设计了 `CanFrame(id, dlc, payload, timestamp)` 与 `CanMessageCodec`。对于 ID `0x120` 报文，Byte 0~1 大端序存储车速（放大 100 倍），Byte 2 存档位，Byte 3 存电源状态。
2. **Rolling Counter**：Byte 6 存储 0~15 递增的循环计数器，用于检测总线是否有丢帧或重放攻击。
3. **ISO SAE J1850 CRC8 校验**：Byte 7 存放基于多项式 `0x1D` 计算的 CRC8 校验码。服务端/客户端在解包时，首先重新计算前 7 字节的 CRC8 值并与 Byte 7 比较。若校验失败，直接判定 `isCrcValid = false`，丢弃该帧并标记信号非法，从源头防止非法指令侵入。

---

### 问题 3：你们的多音区音频焦点与行车防分心策略是如何设计的？
> **面试官意图**：考察对车规级交互安全规范（NHTSA / 车规安全）与 AAOS AudioZone 的理解。

**标准回答**：
1. **多音区物理隔离**：我们将座舱划分为 `DRIVER_ZONE_0`（主驾）、`PASSENGER_ZONE_1`（副驾）和 `REAR_ZONE_2`（后排）。当主驾导航播报（`USAGE_NAVIGATION`）时，`MultiZoneAudioManager` 仅将 `DRIVER_ZONE_0` 的媒体音量降低 (Ducking 至 30%)，副驾 Zone 1 的媒体播放音量维持 100%，实现物理音区互不干扰。当方向盘 PTT 触发时，触发全音区强抑音（音量降至 10%）。
2. **迟滞区间防分心 (Hysteresis Safety Policy)**：防分心引擎 `DrivingSafetyPolicy` 引入了迟滞防抖动算法。挂 D 档或车速大于 `0.5 km/h` 即刻锁定视频播放与复杂键盘；但只有当车速降至 `< 0.2 km/h` 且挂 P 档时，才解除防分心限制，彻底杜绝车速在 0.5km/h 临界点来回抖动导致 UI 频繁闪烁的隐患。

---

### 问题 4：你的信号降级与看门狗超时机制是如何落地的？
> **面试官意图**：考察面对总线丢包、传感器停发、底层卡死等异常场景时的防守型编程能力。

**标准回答**：
1. **信号时戳与状态包装**：所有车辆信号统一封装为 `CarSignal<T>(value, status, timestamp, areaId)`，包含了 `SignalStatus`（`VALID`, `INVALID`, `UNAVAILABLE`, `ERROR`）。
2. **超时降级看门狗**：在 `VehiclePropertyAdapter` 中运行了一个后台看门狗协程，每 2000ms 扫描一次信号时间戳。一旦发现车速信号超过 5000ms 未更新（可能 CAN 总线 Bus-off 或传感器故障），看门狗自动将 `_speedSignal.value` 的状态修改为 `SignalStatus.UNAVAILABLE`。
3. **UI 容错渲染**：UI 组件 `SpeedGaugeComponent` 在发现 `status != VALID` 时，绝不冻结显示旧的车速数字，而是自动切为警告色 `-- KM/H` 并标注 `信号离线/降级`，提醒驾驶员传感器失效。

---

## 📊 技术架构对比总结表（用于面试表达）

| 技术模块 | 传统普通手机开发 | 本项目实现的座舱架构 |
| :--- | :--- | :--- |
| **进程模型** | 单进程 Activity/Fragment | 双进程隔离（主进程 UI ↔ `:vehicle` 模拟 Daemon 进程） |
| **通信机制** | LiveData / EventBus / 本地回调 | AIDL + BinderProxy + `RemoteCallbackList` + `DeathRecipient` |
| **硬件协议** | 无 | ISO 11898-1 CAN DBC 报文打包/解包 + ISO SAE J1850 CRC8 校验 |
| **音轨控制** | 全局单一 AudioFocus | 多音区 (`Driver_Zone_0` / `Passenger_Zone_1`) 物理音区隔离仲裁 |
| **屏幕适配** | 简单的 dp 适配或 XML constraint | 依据 `CarWindowSizeClass` 动态适配 21:9 超宽贯穿屏 / 16:9 标准屏 / 小屏 |
| **异常防护** | 崩溃闪退 / 界面卡死旧值 | 信号看门狗超时降级 (`UNAVAILABLE`) + 指数退避自愈重连 |
