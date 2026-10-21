package com.oem.car.middleware.vehicle

import android.os.Binder
import android.os.Process
import android.os.RemoteCallbackList
import android.os.RemoteException
import android.util.Log
import com.oem.car.core.model.DrivingSafetyPolicy
import com.oem.car.middleware.IVehicleCoreManager
import com.oem.car.middleware.IVehicleEventListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ============================================================================
 * Binder 服务端对象（运行在 :vehicle 进程）
 * ============================================================================
 *
 * 【它是什么】
 * 我们只是用 :vehicle 在 App 内模拟了一个“服务端进程”，演示 Binder 边界，
 * 客户端通过 Binder，调用 :vehicle 进程里的 Binder 服务对象（CarCoreDaemon）
 *
 * CarCoreDaemon 继承 IVehicleCoreManager.Stub。
 * Stub 是 AIDL 编译生成的"服务端基类"，它本身就是一个 Binder。
 * 客户端拿到 BinderProxy 后调用 IVehicleCoreManager.Proxy 的方法，
 * 参数会被打包成 Parcel，经 Binder 驱动送到这里，由 Stub.onTransact() 解包，
 * 再调用本类重写的方法。客户端从头到尾都不持有 CarCoreDaemon 对象。
 *
 *
 * 【完整链路（和 VehicleCoreService 一起看）】
 * 1. 客户端 bindService -> AMS 拉起 :vehicle 进程；
 * 2. VehicleCoreService.onCreate() 创建本对象；
 * 3. VehicleCoreService.onBind() 把本对象作为 IBinder 返回；
 * 4. 客户端拿到 BinderProxy，Stub.asInterface() 得到 IVehicleCoreManager.Proxy；
 *
 *
 * 5. 客户端 registerListener(listener)：
 *      本类在 Binder 线程执行 registerListener()
 *      -> callbacks.register(listener)  // 保存客户端回调的 Binder 代理
 *      -> syncStateTo(listener)         // 立即推一份当前状态快照
 * 6. 客户端 injectMockSpeed(60f)：
 *      本类在 Binder 线程执行 injectMockSpeed()
 *      -> 更新 _speed
 *      -> checkSafetyPolicy()           // 重新计算防分心状态
 *      -> dispatch { onSpeedChanged }   // 广播给所有客户端
 * 7. 客户端收到回调（oneway，异步）：
 *      客户端里的 IVehicleEventListener.Stub 在客户端 Binder 线程执行
 *      -> VehiclePropertyAdapter 更新 StateFlow -> Compose UI 重组
 *
 * 【角色是双向的】
 * - 主进程调 registerListener/injectMock* 时：主进程是客户端，本类是服务端；
 * - 本类回调 onSpeedChanged 时：本类是客户端，主进程里的
 *   IVehicleEventListener.Stub 才是服务端。
 * 所以"服务端"不是固定的机器，而是每个 Binder 接口各自的实现方。
 *
 * 【为什么用 RemoteCallbackList，而不是普通 List】
 * 1. 它保存的是客户端回调的 Binder 代理，不是普通对象；
 * 2. 客户端进程死亡时，Binder 会通知它自动移除回调，避免服务端持有死对象、内存泄漏；
 * 3. register/unregister/beginBroadcast 都是线程安全的，适配 Binder 线程池并发；
 * 4. beginBroadcast() 返回当前回调数量，遍历期间列表变化也不会越界。
 *
 * 【线程模型】
 * AIDL 方法不在主线程，而是在 :vehicle 进程的 Binder 线程池（binder:pid_N）执行，
 * 且可能有多个 Binder 线程并发进入。因此：
 * - _speed 等 MutableStateFlow 和 RemoteCallbackList 本身是线程安全的；
 * - 但 checkSafetyPolicy() 的"读两个状态 -> 计算 -> 写回"不是原子操作；
 * - 当前注入入口来自 App UI 单线程顺序调用，实际不会并发；
 *   如果未来允许多个客户端并发注入，需要把注入串行化或加锁。
 *
 *
 *
 * 【状态与进程死亡】
 * _speed/_gear/_powerState 只存在本进程内存里。服务进程被杀死后状态丢失，
 * 客户端重连时 registerListener() 会重新推送默认快照，因此 UI 会回到默认值。
 * 这是 mock 服务的已知限制；真实车机应把状态放到 VehiclePropertyService 等
 * 系统/持久层，或由真实车身信号驱动。
 *
 *
 */
class CarCoreDaemon : IVehicleCoreManager.Stub() {

    /**
     * 已注册的客户端回调列表。
     *
     * 泛型是 AIDL 回调接口 IVehicleEventListener；实际保存的是客户端回调
     * Stub 在服务端的 Binder 代理。客户端进程死亡后会自动移除，不用手动清理。
     */
    private val callbacks = RemoteCallbackList<IVehicleEventListener>()

    /**
     * 当前模拟车速，单位 km/h。
     * 这是服务端的状态源（source of truth），客户端拿到的只是回调副本。
     */
    private val _speed = MutableStateFlow(0f)

    /** 车速的只读视图。目前没有跨进程消费者，客户端使用自己的 VehiclePropertyAdapter。 */
    val speedFlow = _speed.asStateFlow()

    /** 当前模拟档位：1=P，8=D（与服务端 AIDL 约定一致）。 */
    private val _gear = MutableStateFlow(1) // 1=P, 8=D

    /** 档位的只读视图。目前没有跨进程消费者。 */
    val gearFlow = _gear.asStateFlow()

    /** 防分心策略结果：true 表示当前应限制复杂交互。 */
    private val _isRestricted = MutableStateFlow(false)

    /** 防分心状态的只读视图。目前没有跨进程消费者。 */
    val isRestrictedFlow = _isRestricted.asStateFlow()

    /** 当前模拟电源状态：0=RUNNING，1=STR_SLEEP，2=GARAGE_MODE。 */
    private val _powerState = MutableStateFlow(0) // 0=RUNNING, 1=STR_SLEEP, 2=GARAGE_MODE

    /** 电源状态的只读视图。目前没有跨进程消费者。 */
    val powerStateFlow = _powerState.asStateFlow()

    /**
     * 最近一次 PTT 语音触发的时间戳。
     *
     * 注意：目前只有 triggerPttVoice() 写入，没有任何读取方，也没有通过 AIDL
     * 回调给客户端。这是跨进程改造后遗留的一个缺口，详见 triggerPttVoice()。
     */
    private val _voiceTriggerEvent = MutableStateFlow(0L)

    /** PTT 时间戳的只读视图。目前没有消费者。 */
    val voiceTriggerEvent = _voiceTriggerEvent.asStateFlow()

    /**
     * AIDL 方法：客户端注册事件监听。
     *
     * 调用方：主进程的 IVehicleCoreManager.Proxy。
     * 执行线程：:vehicle 进程的 Binder 线程池，不是主线程。
     *
     * 步骤：
     * 1. callbacks.register(listener)：保存客户端回调的 Binder 代理；
     *    返回 false 表示该回调已经注册过，或列表已被 release() kill；
     * 2. syncStateTo(listener)：立即推送当前状态快照。
     *
     * 为什么注册后要立刻推快照？
     * 车辆信号是"电平式"的：客户端关心的是当前值，而不是从注册那一刻起的变化量。
     * 如果只等下一次 inject，晚启动的客户端会一直显示旧值/默认值。
     * 服务进程重启后，客户端重新注册也靠这一步恢复状态。
     *
     * 日志里的 callerPid 是调用方进程 pid，myPid 是当前服务进程 pid；
     * 两者不同且线程名是 binder:xxx_N，就是"真的跨进程"的直接证据。
     */
    override fun registerListener(listener: IVehicleEventListener) {
        val registered = callbacks.register(listener)
        Log.i(
            TAG,
            "registerListener registered=$registered callerPid=${Binder.getCallingPid()} " +
                "myPid=${Process.myPid()} thread=${Thread.currentThread().name}"
        )
        // 注册成功与否都推一次快照：重复注册的客户端也能拿到最新状态。
        syncStateTo(listener)
    }

    /**
     * 向单个新注册的客户端推送一份完整状态快照。
     *
     * 这里直接调用 listener 的方法；listener 在服务端是客户端回调的 Binder 代理。
     * 因为 IVehicleEventListener 是 oneway 接口，这些调用会立即返回，
     * 不会阻塞当前 Binder 线程等待客户端处理完毕。
     *
     * 注意：oneway 只保证"发送后不等待结果"，不保证客户端在 registerListener
     * 返回前处理完；所以客户端可能先收到快照回调、后打印 Connected 日志，
     * 这是正常现象。
     */
    private fun syncStateTo(listener: IVehicleEventListener) {
        Log.i(
            TAG,
            "push snapshot speed=${_speed.value} gear=${_gear.value} " +
                "power=${_powerState.value} restricted=${_isRestricted.value}"
        )
        try {
            // 顺序：速度 -> 档位 -> 电源 -> 防分心。
            // 这些状态是同一时刻的读值；并发注入时可能不是严格一致的快照。
            listener.onSpeedChanged(_speed.value)
            listener.onGearChanged(_gear.value)
            listener.onPowerStateChanged(_powerState.value)
            listener.onDistractionStateChanged(_isRestricted.value)
        } catch (e: RemoteException) {
            // 客户端在推送过程中死亡时会走到这里；RemoteCallbackList 随后会自动移除它。
        }
    }

    /**
     * 释放服务端资源，由 VehicleCoreService.onDestroy() 调用。
     *
     * callbacks.kill() 会清空所有注册的回调，并让后续 register() 返回 false。
     * kill 之后该 RemoteCallbackList 不能复用；但此时整个 Service 实例都要销毁了，
     * 所以不需要考虑复用。
     *
     * 注意：进程被 kill -9/crash 时不会执行到这里，见 VehicleCoreService.onDestroy 注释。
     */
    fun release() {
        Log.i(TAG, "release myPid=${Process.myPid()}")
        callbacks.kill()
    }

    /**
     * AIDL 方法：客户端主动注销监听。
     *
     * 返回 false 表示该回调不在列表中（可能已注销，或列表已 kill）。
     * 即使客户端不调用，客户端进程死亡时 RemoteCallbackList 也会自动移除，
     * 所以这里是"尽力而为"的清理，不是唯一保障。
     */
    override fun unregisterListener(listener: IVehicleEventListener) {
        val unregistered = callbacks.unregister(listener)
        Log.i(
            TAG,
            "unregisterListener unregistered=$unregistered callerPid=${Binder.getCallingPid()}"
        )
    }

    /**
     * AIDL 方法：注入模拟车速（客户端调试入口）。
     *
     * 这是同步 AIDL 调用，客户端线程会阻塞到本方法返回；
     * 因此方法内不要做耗时操作，也不要在 Binder 线程里做 UI 操作。
     *
     * 执行顺序：
     * 1. 更新服务端状态 _speed；
     * 2. 重新计算防分心状态（可能额外广播一次 onDistractionStateChanged）；
     * 3. 广播 onSpeedChanged 给所有客户端。
     *
     * 注意：因为第 2 步在第 3 步之前，客户端可能先收到"防分心"回调，
     * 再收到"速度"回调。如果业务要求严格顺序，需要调整两个 dispatch 的先后。
     */
    private var rollingCounter = 0

    override fun injectMockSpeed(speed: Float) {
        rollingCounter = (rollingCounter + 1) % 16
        val frame = com.oem.car.core.model.CanMessageCodec.encodeVehicleSignal(
            speed = speed,
            gear = _gear.value,
            powerState = _powerState.value,
            counter = rollingCounter
        )
        Log.d(TAG, "injectMockSpeed speed=$speed callerPid=${Binder.getCallingPid()} CAN=${frame.toHexDump()}")
        _speed.value = speed
        checkSafetyPolicy()
        dispatch { it.onSpeedChanged(speed) }
    }

    /**
     * AIDL 方法：注入模拟档位。
     *
     * 执行顺序与 injectMockSpeed 相同：先更新状态，再算防分心，最后广播档位。
     * 因此客户端可能先收到 onDistractionStateChanged，再收到 onGearChanged。
     */
    override fun injectMockGear(gear: Int) {
        Log.d(TAG, "injectMockGear gear=$gear callerPid=${Binder.getCallingPid()}")
        _gear.value = gear
        checkSafetyPolicy()
        dispatch { it.onGearChanged(gear) }
    }

    /**
     * AIDL 方法：注入模拟电源状态。
     *
     * 电源状态不参与防分心规则，所以这里不调用 checkSafetyPolicy()，
     * 只更新状态并广播。
     */
    override fun injectMockPower(state: Int) {
        Log.d(TAG, "injectMockPower state=$state callerPid=${Binder.getCallingPid()}")
        _powerState.value = state
        dispatch { it.onPowerStateChanged(state) }
    }

    /**
     * AIDL 方法：模拟 PTT 语音按键触发。
     *
     * 当前实现只更新服务端本地的 _voiceTriggerEvent 时间戳，没有 dispatch；
     * IVehicleEventListener 里也没有对应的 onVoiceTriggered() 回调。
     * 因此在跨进程架构下，客户端调用它不会收到任何跨进程事件。
     *
     * 如果 PTT 需要作为跨进程能力演示，需要两步：
     * 1. 在 IVehicleEventListener.aidl 增加 onVoiceTriggered()；
     * 2. 在这里 dispatch { it.onVoiceTriggered(...) }。
     */
    override fun triggerPttVoice() {
        Log.d(TAG, "triggerPttVoice callerPid=${Binder.getCallingPid()}")
        _voiceTriggerEvent.value = System.currentTimeMillis()
    }

    /**
     * 车规防分心策略：D 档（8）或车速 > 0.5km/h 时判定为受限制。
     *
     * 只在结果发生变化时广播（边沿触发），避免每个速度 tick 都通知客户端。
     *
     * 注意："读 _gear/_speed -> 计算 -> 写 _isRestricted" 不是原子操作。
     * 当前客户端从 UI 单线程顺序调用注入方法，不会并发；
     * 如果未来多客户端并发注入，可能出现两个线程交错读写，需要加锁或串行化。
     */
    private fun checkSafetyPolicy() {
        // 车规防分心：D 档或速度 > 0.5km/h 触发拦截；规则与客户端共用 DrivingSafetyPolicy
        val restricted = DrivingSafetyPolicy.isDriving(_gear.value, _speed.value)
        if (_isRestricted.value != restricted) {
            _isRestricted.value = restricted
            dispatch { it.onDistractionStateChanged(restricted) }
        }
    }

    /**
     * 向所有已注册客户端广播一次事件。
     *
     * RemoteCallbackList 的固定用法：
     * - beginBroadcast() 返回当前回调数量（相当于一份快照）；
     * - getBroadcastItem(i) 取出每个客户端回调的 Binder 代理；
     * - 必须与 finishBroadcast() 成对调用。
     *
     * 每个客户端单独 try/catch：一个客户端进程死亡或抛异常，不会影响其他客户端。
     *
     * 注意：当前 begin/finish 没有放在 try/finally 中；如果 action 抛出非 Exception
     * 的 Error，finishBroadcast() 可能执行不到。所有回调都是 oneway，风险很低，
     * 但更稳妥的写法是 try { ... } finally { callbacks.finishBroadcast() }。
     */
    private fun dispatch(action: (IVehicleEventListener) -> Unit) {
        val count = callbacks.beginBroadcast()
        for (i in 0 until count) {
            try {
                action(callbacks.getBroadcastItem(i))
            } catch (e: Exception) {
                // 单个客户端失败不影响其余客户端。
            }
        }
        callbacks.finishBroadcast()
    }

    companion object {
        /** Logcat 过滤标签：adb logcat -s CarCoreDaemon */
        private const val TAG = "CarCoreDaemon"
    }
}
