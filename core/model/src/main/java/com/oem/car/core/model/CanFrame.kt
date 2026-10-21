package com.oem.car.core.model

/**
 * 模拟底层 CAN 总线数据帧 (CAN Frame Specification)。
 *
 * 符合 ISO 11898-1 CAN 报文标准。
 *
 * @property id 11位或29位 CAN ID（例如 0x120 表示车速与动力总成信号）
 * @property dlc 数据长度代码 (Data Length Code, 0~8 字节)
 * @property payload 8字节 CAN 报文 Payload 载荷
 * @property timestampMs 报文到达时间戳
 */
data class CanFrame(
    val id: Int,
    val dlc: Int,
    val payload: ByteArray,
    val timestampMs: Long = System.currentTimeMillis()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as CanFrame
        if (id != other.id) return false
        if (dlc != other.dlc) return false
        if (!payload.contentEquals(other.payload)) return false
        if (timestampMs != other.timestampMs) return false
        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + dlc
        result = 31 * result + payload.contentHashCode()
        result = 31 * result + timestampMs.hashCode()
        return result
    }

    fun toHexDump(): String {
        return "CAN[0x${id.toString(16).uppercase().padStart(3, '0')}] DLC=$dlc Data=" +
            payload.joinToString(" ") { "%02X".format(it) }
    }
}

/**
 * 解码后的车身动态信号。
 */
data class DecodedVehicleSignal(
    val speed: Float,
    val gear: Int,
    val powerState: Int,
    val counter: Int,
    val isCrcValid: Boolean
)

/**
 * 车载 DBC (Database CAN) 协议编解码器。
 *
 * CAN ID 0x120:
 * Byte 0~1: 车速 (Speed * 100，如 60.5km/h => 6050，大端序)
 * Byte 2: 档位 (Gear)
 * Byte 3: 电源状态 (PowerState)
 * Byte 6: 循环计数器 Rolling Counter (0~15)
 * Byte 7: CRC8 校验码
 */
object CanMessageCodec {
    const val CAN_ID_VEHICLE_DYNAMIC = 0x120

    fun encodeVehicleSignal(
        speed: Float,
        gear: Int,
        powerState: Int,
        counter: Int
    ): CanFrame {
        val payload = ByteArray(8)
        val speedRaw = (speed * 100).toInt().coerceIn(0, 30000)

        // Speed (Big Endian)
        payload[0] = ((speedRaw shr 8) and 0xFF).toByte()
        payload[1] = (speedRaw and 0xFF).toByte()

        // Gear & Power State
        payload[2] = gear.toByte()
        payload[3] = powerState.toByte()

        // Reserved
        payload[4] = 0x00
        payload[5] = 0x00

        // Rolling Counter (0..15)
        payload[6] = (counter and 0x0F).toByte()

        // Calculate CRC8 over bytes 0..6
        payload[7] = calculateCrc8(payload, 7)

        return CanFrame(
            id = CAN_ID_VEHICLE_DYNAMIC,
            dlc = 8,
            payload = payload
        )
    }

    fun decodeVehicleSignal(frame: CanFrame): DecodedVehicleSignal? {
        if (frame.id != CAN_ID_VEHICLE_DYNAMIC || frame.payload.size < 8) return null

        val payload = frame.payload
        val speedRaw = ((payload[0].toInt() and 0xFF) shl 8) or (payload[1].toInt() and 0xFF)
        val speed = speedRaw / 100.0f
        val gear = payload[2].toInt() and 0xFF
        val powerState = payload[3].toInt() and 0xFF
        val counter = payload[6].toInt() and 0x0F

        val expectedCrc = calculateCrc8(payload, 7)
        val actualCrc = payload[7]
        val isCrcValid = expectedCrc == actualCrc

        return DecodedVehicleSignal(
            speed = speed,
            gear = gear,
            powerState = powerState,
            counter = counter,
            isCrcValid = isCrcValid
        )
    }

    /**
     * ISO 0x07 (SAE J1850) CRC8 多项式校验算法。
     */
    fun calculateCrc8(data: ByteArray, length: Int): Byte {
        var crc = 0xFF
        for (i in 0 until length) {
            crc = crc xor (data[i].toInt() and 0xFF)
            repeat(8) {
                crc = if ((crc and 0x80) != 0) {
                    ((crc shl 1) xor 0x1D) and 0xFF
                } else {
                    (crc shl 1) and 0xFF
                }
            }
        }
        return (crc xor 0xFF).toByte()
    }
}
