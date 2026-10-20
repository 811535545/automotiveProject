package com.oem.car.middleware.vehicle

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.Process
import android.util.Log

/**
 * ============================================================================
 * 服务端进程（:vehicle）的 Android 入口
 * ============================================================================
 *
 * 【它在整条跨进程链路中的位置】
 *
 *   主进程 com.oem.car.app                          :vehicle 进程
 *   ┌────────────────────────────────┐             ┌──────────────────────────────────┐
 *   │ VehicleServiceConnectionManager│             │ VehicleCoreService               │
 *   │   connect() ─── bindService ───┼────────────>│   onCreate(): new CarCoreDaemon  │
 *   │                                │             │   onBind(): return daemon        │
 *   │ IVehicleCoreManager.Proxy ─────┼── Binder ──>│ CarCoreDaemon : Stub             │
 *   │ IVehicleEventListener.Stub <───┼── Binder ───│ RemoteCallbackList               │
 *   └────────────────────────────────┘             └──────────────────────────────────┘
 *
 * 【一次绑定到底发生了什么】
 * 1. 客户端 VehicleServiceConnectionManager.connect() 调用 bindService(...)；
 * 2. AMS 从 AndroidManifest.xml 读到本 Service 声明了 android:process=":vehicle"，
 *    发现它与调用方不在同一个进程，于是 fork 出 :vehicle 进程（如果还没启动）；
 * 3. 系统在 :vehicle 进程里实例化本 Service，回调 onCreate()，这里创建 daemon；
 * 4. 系统回调 onBind()，把 daemon 作为 IBinder 返回；
 * 5. Binder 驱动在客户端侧生成 BinderProxy，客户端在 onServiceConnected() 拿到它；
 * 6. 之后客户端调用 IVehicleCoreManager.Proxy 的方法，最终落到 CarCoreDaemon。
 *
 * 【为什么拆成 Service + Daemon 两个类】
 * - VehicleCoreService：只负责 Android 生命周期和进程入口，回答"什么时候创建、
 *   什么时候把 Binder 交出去、什么时候释放"；
 * - CarCoreDaemon：只负责 Binder 协议和服务端状态，回答"收到调用后做什么、
 *   状态怎么保存、回调怎么发给客户端"；
 * - 职责分离后，daemon 不依赖 Service 的生命周期，可以直接 new 出来做单元测试；
 *   Service 也不需要关心 AIDL 细节。
 * - 这也是把 CarCoreDaemon 从 object 改成 class 的原因：object 是进程级单例，
 *   没有明确的销毁时机；class 由 Service.onDestroy() 显式 release()，
 *   生命周期与 Service 对齐，未来也方便替换实现或注入 mock。
 *
 * 【为什么 exported=false 也可以】
 * 客户端和服务端在同一个 APK、同一个 UID（1010204）里，只是运行时处于不同进程。
 * 同一个应用的组件可以绑定 exported=false 的 Service；
 * 如果未来把服务端拆成独立 APK，才需要 exported=true + 签名级权限来保护。
 *
 * 【线程模型】
 * - onCreate()/onBind()/onDestroy() 运行在 :vehicle 进程的主线程；
 * - 客户端通过 Binder 调进来的 AIDL 方法运行在 Binder 线程池（binder:pid_N），
 *   不是主线程，因此 CarCoreDaemon 内部必须使用线程安全的数据结构。
 *
 * 【只支持绑定，不支持 startService】
 * 本 Service 没有"被 startService 长期启动"的语义，因此没有重写 onStartCommand()。
 * 它的存活由绑定关系决定：所有客户端解绑且系统回收后，Service 会被销毁。
 * 注意：进程被 kill -9 或 crash 时不会走 onDestroy()，见下方 onDestroy 注释。
 */
class VehicleCoreService : Service() {

    /**
     * 真正的 Binder 服务对象，服务端能力都在它里面。
     *
     * 客户端拿不到这个对象的引用，拿到的是它在 Binder 驱动另一侧的代理（BinderProxy）。
     * lateinit 的原因：Service 构造时还没到 onCreate，而 onBind 一定发生在 onCreate 之后，
     * 所以 onBind 中读取 daemon 是安全的。
     */
    private lateinit var daemon: CarCoreDaemon

    /**
     * Service 创建时回调，整个 Service 生命周期只执行一次。
     *
     * 在这里创建 daemon，相当于服务端进程启动后初始化服务状态。
     * 此时客户端还没拿到 Binder，不会发生跨进程调用，因此这里不需要考虑并发。
     *
     * 日志字段含义：
     * - pid：当前 :vehicle 进程的 pid，与客户端 pid 不同，是跨进程的直接证据；
     * - startUptimeMs：该进程被 fork 出来的时间点（系统 uptime），不是 Service 创建时间；
     * - thread=main：生命周期回调在主线程执行。
     */
    override fun onCreate() {
        super.onCreate()
        daemon = CarCoreDaemon()
        Log.i(
            TAG,
            "onCreate pid=${Process.myPid()} startUptimeMs=${Process.getStartUptimeMillis()} " +
                "thread=${Thread.currentThread().name}"
        )
    }

    /**
     * 客户端绑定到本 Service 时回调，返回值就是跨进程通信的服务端 Binder。
     *
     * - 系统通常只在第一个客户端绑定时调用 onBind()，后续客户端复用同一个 IBinder；
     * - 返回的 daemon 继承 IVehicleCoreManager.Stub，而 Stub 本身就是一个 Binder，
     *   所以不需要额外包装；
     * - 如果返回 null，表示拒绝绑定。
     */
    override fun onBind(intent: Intent?): IBinder {
        Log.i(TAG, "onBind pid=${Process.myPid()} action=${intent?.action}")
        return daemon
    }

    /**
     * Service 正常销毁时回调。
     *
     * 触发场景：所有客户端都 unbind，且没有 startService 维持存活，系统回收 Service；
     * 或者显式调用 stopService/stopSelf。
     *
     * release() 会 kill 掉 RemoteCallbackList，清空对客户端回调的引用，避免内存泄漏。
     *
     * 重要边界：进程被 kill -9、发生 native crash、或系统直接杀进程时，
     * onDestroy() 不会执行，release() 也没有机会执行。
     * 这种情况下由内核回收 Binder 资源，客户端通过 DeathRecipient 感知服务端死亡并重连。
     * 因此不能把"资源清理"完全依赖 onDestroy()。
     */
    override fun onDestroy() {
        Log.i(TAG, "onDestroy pid=${Process.myPid()}")
        daemon.release()
        super.onDestroy()
    }

    companion object {
        /** Logcat 过滤标签：adb logcat -s VehicleCoreService */
        private const val TAG = "VehicleCoreService"
    }
}
