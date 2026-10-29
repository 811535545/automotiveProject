# 智能座舱软件开发与架构规范 (Smart Cockpit Development Specifications)

> **版本**：v1.0.0
> **适用范围**：Android Automotive OS (AAOS) / 智能座舱中间件与应用层开发
> **制定目的**：规范座舱软件架构设计、存储安全、总线通信与行车安全策略，保障车机系统的高可用性、强抗断电性与 60fps 丝滑体验。

---

## 目录
1. [一、 整体分层与组件化架构规范](#一-整体分层与组件化架构规范)
2. [二、 车载持久化与缓存分层规范（含禁令）](#二-车载持久化与缓存分层规范含禁令)
3. [三、 高频 CAN 总线通信与 IO 规范](#三-高频-can-总线通信与-io-规范)
4. [四、 行车安全防分心 (UX Restrictions) 规范](#四-行车安全防分心-ux-restrictions-规范)
5. [五、 多音区音频焦点与 PTT 规范](#五-多音区音频焦点与-ptt-规范)
6. [六、 车载电源生命周期与进程隔离规范](#六-车载电源生命周期与进程隔离规范)
7. [七、 异常防崩溃与抗断电保护规范](#七-异常防崩溃与抗断电保护规范)

---

## 一、 整体分层与组件化架构规范

### 1.1 架构分层原则 (Clean Architecture)
系统必须严格遵循 **Clean Architecture** 架构与 **单向数据流 (Unidirectional Data Flow, UDF)**，严禁跨层调用：

* **UI 视图层 (`feature:*`)**：基于 Jetpack Compose 与 Material 3 Automotive 设计系统，只关注视图渲染与用户意图 (User Intents) 派发。
* **表现层 (`ViewModel`)**：管理 UI 状态，将底层状态转换为 UI StateFlow，使用 `viewModelScope` 协程处理异步任务。
* **领域/数据层 (`core:data`)**：以 `VehicleRepository` 作为**单一数据源 (Single Source of Truth, SSOT)**，聚合底层 10+ 个子系统，向下管理 VHAL/Binder 通道。
* **中间件与 HAL 层 (`core:vehicle-service`)**：封装 `CarPropertyManager` 与 AIDL IPC，对接 C++ VHAL 或 SocketCAN 物理总线。

### 1.2 状态管理与并发
* 强制使用 `StateFlow` 或 `SharedFlow` 暴露状态，禁止使用非线程安全的 `LiveData`。
* 数据下发遵循“意图下发 ➔ 校验 ➔ VHAL 响应 ➔ StateFlow 推送”的闭环，确保 UI 与车辆真实物理状态强一致。

---

## 二、 车载持久化与缓存分层规范（含禁令）

> [!CAUTION]
> **【红线禁令 1】严禁在座舱项目中使用原生 `SharedPreferences`！**
> `SharedPreferences` 在车载强行断电（下车拔钥匙/STR 强关）时，写入中的 XML 会变成 0 字节损坏，且 `apply()` 会通过 `QueuedWork` 阻塞主线程引发 ANR。

### 2.1 存储分层标准

| 存储层级 | 推荐技术栈 | 典型数据内容 | 规约与注意事项 |
| :--- | :--- | :--- | :--- |
| **第一级：纯内存缓存** | **RAM + StateFlow / C++ 结构体** | 车速、档位、胎压、转速、智驾感知目标 | **防闪存 (eMMC/UFS) 擦写报废**，纳秒级读写，禁止写盘 |
| **第二级：内存映射 KV** | **MMKV (`mmap`)** | 座椅记忆位置、空调偏好温度、播放进度、主题 | **全项目指定轻量 KV 替代方案**，基于 `mmap` 零损坏风险 |
| **第三级：结构化数据库** | **SQLite + Room (WAL 模式)** | OBD-II / DTC 故障码历史、导航 POI、充电日志 | 必须开启 **WAL (Write-Ahead Logging)** 模式，读写互不阻塞 |
| **第四级：文件系统分区** | **f2fs / Device Encrypted (DE)** | 哨兵视频、OTA 升级包、系统 Crash Logs | 敏感数据（数字钥匙证书）需配合 SE 加密芯片与 KeyStore |

---

## 三、 高频 CAN 总线通信与 IO 规范

> [!IMPORTANT]
> **【红线禁令 2】严禁在 CAN 总线与 VHAL 数据接收端使用 BIO (阻塞式 IO)！**
> 面对每秒 500 ~ 2000 条的高频 CAN 报文，BIO 一条一条阻塞读取会导致内核 Socket 缓冲区溢出（丢包）、延迟剧增与线程池崩溃。

### 3.1 传输与解包机制
1. **NIO 多路复用**：VHAL / SocketCAN 必须采用 **NIO (`O_NONBLOCK` + `epoll`)** 机制，非阻塞批量读取原始数据。
2. **无锁环形队列 (RingBuffer)**：IO 线程读取后立刻存入无锁环形队列，耗时控制在 1 微秒以内，防止高频冲刷丢包。
3. **应用层背压与降频 (Backpressure)**：
   * CAN 总线频率最高可达 2000Hz，而屏幕刷新率仅 60Hz。
   * 必须在 Repository / ViewModel 层使用 `StateFlow` 的 `.sample(16ms)` 或 `conflate()` 进行降频收敛，将 CPU 消耗从 90% 降至 1% 以内。

---

## 四、 行车安全防分心 (UX Restrictions) 规范

### 4.1 核心原则
根据车规级 UXRestrictions 安全策略，当车辆处于移动状态（`speed > 0 km/h` 或非 `P` 档）时，系统必须强制实施行车防分心拦截：

1. **视频与娱乐拦截**：中控/副驾屏禁止播放长视频，自动切为“行车安全提示遮罩”。
2. **文本输入限制**：禁用软键盘复杂输入，仅保留语音输入（PTT）。
3. **列表项截断**：长列表（如歌单、通讯录）强制截断为最多显示 6~10 项，禁止行驶中频繁刷页。

```kotlin
// 伪代码规范示例：在 Repository 统一拦截
if (currentSpeed > 0f && isVideoPlaybackRequested) {
    CarLogger.w("UXRestrictions", "🛑 行车状态拦截视频播放!")
    return SafetyResult.RESTRICTED
}
```

---

## 五、 多音区音频焦点与 PTT 规范

### 5.1 音频焦点 (Audio Focus) 管理
1. **多音区独立路由**：主驾、副驾、后排（Audio Zones）音频通道隔离，互不干涉。
2. **PTT (Push-To-Talk) 抢占机制**：
   * 当驾驶员按下方向盘 PTT 按键时，必须请求 `AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK` 焦点。
   * 正在播放的媒体（如音乐、电台）必须自动降低音量（Ducking 降音 30%~50%），以确保语音识别的降噪体验。

---

## 六、 车载电源生命周期与进程隔离规范

### 6.1 车载电源状态机 (Power States)

| 电源状态 | 系统行为与规范 |
| :--- | :--- |
| **`RUNNING` (正常运行)** | 所有服务与 UI 正常响应。 |
| **`STR_SLEEP` (快速休眠)** | 挂起至 RAM (Suspend-to-RAM)，**3 秒内必须保存状态并断开蓝牙/网络**，停止所有后台 Timer 与高频轮询。 |
| **`GARAGE_MODE` (车库模式)** | 允许后台继续静默下载 OTA 升级包或上传车辆健康日志。 |
| **`SHUTDOWN_PREPARE` (准备关机)** | 强制刷盘持久化（MMKV 自动落盘），释放所有 Binder 引用，防止野指针泄漏。 |

---

## 七、 异常防崩溃与抗断电保护规范

1. **Binder 异常防御**：所有跨进程 AIDL / `CarPropertyManager` 调用必须包裹 `try-catch`，捕获 `DeadObjectException`（底层服务挂掉）与 `TransactionTooLargeException`（传输数据超过 1MB 限制）。
2. **看门狗超时监控 (Watchdog)**：关键车载信号（如车速、档位）需设定看门狗超时机制（如超过 1000ms 未收到更新，标记为 `SignalStatus.INVALID` 或 `TIMEOUT` 降级状态）。
3. **空指针与初始化防护**：全局静态单例在初始化前调用必须判断防重机制（如 `AtomicBoolean`），防止重复初始化导致资源竞态死锁。

---

> **文档维护**：座舱架构组
> **发布日期**：2025 年 3 月