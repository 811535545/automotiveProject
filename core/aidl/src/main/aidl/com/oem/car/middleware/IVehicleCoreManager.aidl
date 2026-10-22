package com.oem.car.middleware;

import com.oem.car.middleware.IVehicleEventListener;

interface IVehicleCoreManager {

     /**
     * 注册车辆事件监听器
     *
     * 客户端通过此方法订阅车辆状态的变化。当车速、档位、电源状态等发生变化时，
     * 服务端将通过 listener 回调通知客户端。
     *
     * @param listener 实现 IVehicleEventListener 接口的远程对象，用于接收状态更新回调。
     *                 建议使用 RemoteCallbackList 在服务端管理此监听器以防止内存泄漏。
     */
    void registerListener(IVehicleEventListener listener);

      /**
         * 注销车辆事件监听器
         *
         * 当客户端不再需要接收车辆状态更新时（例如 Activity 销毁或 Service 停止），
         * 必须调用此方法以移除监听器，避免内存泄漏和无用的 Binder 调用。
         *
         * @param listener 之前通过 registerListener 注册的同一个远程监听器对象。
         */
    void unregisterListener(IVehicleEventListener listener);

       /**
         * 注入模拟车速
         *
         * 【测试/调试专用】强制设置当前车辆的速度值，覆盖真实传感器数据。
         * 此方法通常用于自动化测试、模拟器环境或开发调试阶段。
         *
         * @param speed 模拟的车速值。单位通常为 km/h 或 m/s（具体取决于项目定义）。
         *              有效范围应根据实际车辆物理限制进行校验。
         */
    void injectMockSpeed(float speed);

  /**
     * 注入模拟档位
     *
     * 【测试/调试专用】强制设置当前的变速箱档位。
     *
     * @param gear 模拟的档位整数值。
     *             常见映射示例（需参考具体常量定义）：
     *             0 - Neutral (空挡)
     *             1 - Drive (前进挡)
     *             2 - Reverse (倒挡)
     *             3 - Park (停车挡)
     */
    void injectMockGear(int gear);

       /**
         * 注入模拟电源状态
         *
         * 【测试/调试专用】强制设置车辆的电源/点火模式。
         *
         * @param state 模拟的电源状态整数值。
         *              常见映射示例：
         *              0 - OFF (关闭)
         *              1 - ACC (附件通电)
         *              2 - ON (点火开启/仪表亮)
         *              3 - START (启动中)
         */
    void injectMockPower(int state);

       /**
         * 触发 PTT (Push-To-Talk) 语音对讲
         *
         * 模拟用户按下 PTT 按钮的行为。
         * 调用此方法将激活语音采集通道或向通信模块发送对讲请求。
         * 适用于车队通信、出租车调度或紧急对讲功能的测试与触发。
         */
    void triggerPttVoice();

}
