# 车载中间件全景数据流向与内部流水线透视 (Automotive Middleware Data Flow Pipelines)

> 本文档专为解决 **“看懂了中间件抽象名词，但不知道数据具体怎么流转、字节怎么变成屏幕像素”** 的痛点而设计。
> 详细拆解了从**物理线缆电信号**、到 **Linux 内核 SocketCAN**、**C++ VHAL**、**Java CarService**、**中间件路由** 以及 **Compose UI 像素渲染** 的全链路数据流向图与流水线。

---

## 1. 🔍 主流水线：从“物理线缆电信号”到“屏幕像素点” (End-to-End Signal Pipeline)

这是汽车中 **90% 车辆信号（车速、档位、门锁、胎压、水温）** 运行的完整全生命周期流向：

```text
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│ [层级 1: 物理线缆层] 车身传感器 / 发动机 ECU                                             │
│   └── 通过双绞线 (CAN_H 3.5V / CAN_L 1.5V) 广播差分电压波形                              │
└───────────────────────────┬─────────────────────────────────────────────────────────────┘
                            │ 物理电信号
                            ▼
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│ [层级 2: 车机硬件芯片层] CAN 收发器 (TJA1051) + CAN 控制器 (RK3588 内置 CAN IP)           │
│   └── 收发器将电压差还原为逻辑 0/1 高低电平；控制器完成 CRC8 校验并打包为 8 字节 Payload:     │
│       [0x17, 0x70, 0x08, 0x00, 0x00, 0x00, 0x05, 0x9A]                                   │
└───────────────────────────┬─────────────────────────────────────────────────────────────┘
                            │ 物理数据帧 (struct can_frame)
                            ▼
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│ [层级 3: Linux 内核驱动层] SocketCAN 内核网卡驱动 (can0)                                │
│   └── 内核将 CAN 数据暴露为 Linux 虚拟网络套接字 (can0)，可通过 `candump can0` 抓包        │
└───────────────────────────┬─────────────────────────────────────────────────────────────┘
                            │ Linux Socket/file descriptor
                            ▼
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│ [层级 4: C++ Native VHAL 层] VehicleHal / PropertyHalService (HAL 硬件抽象)              │
│   1. 根据 DBC 协议解析 Byte 0~1: 0x1770 = 6000 ──► 6000 / 100 = 60.0 km/h                │
│   2. 校验 Byte 7 CRC8 码 (0x9A): 校验正确                                               │
│   3. 封装为 C++ 结构体 VehiclePropValue { prop: PERF_VEHICLE_SPEED, value: 60.0f }      │
└───────────────────────────┬─────────────────────────────────────────────────────────────┘
                            │ Binder IPC 跨进程调用 (ICarProperty)
                            ▼
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│ [层级 5: Java Framework 层] SystemServer 进程中的 CarPropertyService                    │
│   1. 进行权限校验: checkCallingPermission("android.car.permission.CAR_SPEED")           │
│   2. 通过 Binder 触发回调代理 ICarPropertyEventListener.onPropertyEvent()                │
└───────────────────────────┬─────────────────────────────────────────────────────────────┘
                            │ Binder IPC (ICarPropertyEventListener)
                            ▼
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│ [层级 6: 车载中间件适配层] AaosCarPropertyBridge & VehiclePropertyAdapter                │
│   1. 解包 CarPropertyValue ──► 封装为 CarSignal<Float>(value=60.0f, status=VALID, ...)   │
│   2. 更新内部响应式数据流: _speedSignal.value = newSignal                               │
│   3. 【信号降级看门狗】：若 > 5000ms 未收到更新，看门狗自动将 status 改为 UNAVAILABLE      │
└───────────────────────────┬─────────────────────────────────────────────────────────────┘
                            │ StateFlow / Coroutine Flow
                            ▼
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│ [层级 7: 状态评估与安全策略中间件] DrivingSafetyPolicy & CarUxRestrictionsEngine          │
│   1. 评估车速 speed = 60.0 km/h > 0.5 km/h ──► 判定 restriction = true                    │
│   2. 更新防分心状态: noVideo = true, maxListLength = 10                                 │
└───────────────────────────┬─────────────────────────────────────────────────────────────┘
                            │ Compose collectAsState()
                            ▼
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│ [层级 8: 声明式 UI 渲染层] Jetpack Compose Recomposition                                 │
│   1. SpeedGaugeComponent: speedSignal 改变 ──► 重新绘制屏幕数字 "60"                     │
│   2. Vehicle3dVisualizer: speed > 0 ──► Canvas 启动后退风阻线条流动动画                   │
│   3. 影音卡片: noVideo == true ──► 触发红色安全拦截遮罩 "🛑 行车视频安全拦截生效"         │
└───────────────────────────┬─────────────────────────────────────────────────────────────┘
```

---

## 2. 🔍 SOA (SOME/IP) 服务化通信数据流向：面向服务 RPC 与事件订阅

传统 CAN 是**面向信号的硬编码广播**，而现代 SOA 中间件是**面向服务 (Service-Oriented) 的动态发现与订阅**：

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
     │ 3. Canvas 实时重绘: 在画面中画出前车青色 3D 框与卡车橙色 3D 框！
```

---

## 3. 🔍 车载多音区音频抢占与 Ducking 降音数据流

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

---

## 4. 🔍 电源状态机与 STR (Suspend-to-RAM) 休眠/唤醒数据流

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
[毫秒级自愈恢复 (Instant Recovery)]
     │ ├── VehiclePropertyProvider: 自动重新建立与 VHAL / :vehicle 进程的 Binder 通信
     │ ├── 客户端: 发送 registerListener，接收服务端推回的状态快照
     │ └── UI 界面: 毫秒级恢复上一次的座舱仪表与 3D 车模，无需等待冷启动！
```

---

## 📊 总结：数据流向全景一览表

| 场景 | 数据起点 | 中间件处理过程 | 数据终点 |
| :--- | :--- | :--- | :--- |
| **车速更新** | 轮速传感器电信号 | CAN收发器 ➔ SocketCAN ➔ VHAL(CRC8校验) ➔ CarPropertyService ➔ Adapter(看门狗) | SpeedGaugeComponent 绘制数字与风阻线条 |
| **行车防分心** | 车速 > 0.5km/h | DrivingSafetyPolicy(迟滞算法) ➔ CarUxRestrictionsEngine 动态生成 restriction | 视频播放器弹出红色安全拦截遮罩 |
| **导航降音** | 导航指引触发 | MultiZoneAudioManager 识别 Driver Zone 0 ➔ 动态将主驾媒体音量 Ducking 至 0.3 | 混音器 DSP 将主驾音乐变小，导航声音清晰输出 |
| **3D 智驾 SR** | 毫米波雷达/摄像头 | SOME/IP-SD 发现服务 ➔ Socket 接收解包 3D 目标坐标 (X, Y) ➔ 透视投影计算 | AdasSrVisualizer Canvas 画出前车/卡车 3D 立体框 |
| **STR 休眠唤醒**| 12V 电压/车门开合 | CarPowerLifecycleManager 广播 Suspend ➔ 刷新缓存/断开Socket ➔ 唤醒重连 Binder | 毫秒级无感恢复座舱上次显示状态 |
