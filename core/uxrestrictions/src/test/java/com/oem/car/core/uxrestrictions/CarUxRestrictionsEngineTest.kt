package com.oem.car.core.uxrestrictions

import com.oem.car.core.model.VehicleGear
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CarUxRestrictionsEngineTest {

    private val engine = CarUxRestrictionsEngine()

    @Test
    fun `parked state allows video and long lists`() {
        engine.evaluate(0f, VehicleGear.PARK)

        val state = engine.uxRestrictionState.value
        assertFalse(state.isRestricted)
        assertFalse(state.noVideo)
        assertFalse(state.noKeyboard)
        assertEquals(Int.MAX_VALUE, state.maxListLength)
    }

    @Test
    fun `drive gear blocks video at standstill`() {
        engine.evaluate(0f, VehicleGear.DRIVE)

        val state = engine.uxRestrictionState.value
        assertTrue(state.isRestricted)
        assertTrue(state.noVideo)
        assertTrue(state.noKeyboard)
        assertEquals(10, state.maxListLength)
    }

    @Test
    fun `moving speed blocks video and limits list`() {
        engine.evaluate(5f, VehicleGear.PARK)

        val state = engine.uxRestrictionState.value
        assertTrue(state.isRestricted)
        assertTrue(state.noVideo)
        assertEquals(10, state.maxListLength)
    }

    @Test
    fun `high speed limits list to five`() {
        engine.evaluate(20f, VehicleGear.PARK)

        val state = engine.uxRestrictionState.value
        assertTrue(state.isRestricted)
        assertEquals(5, state.maxListLength)
    }

    @Test
    fun `state recovers after parking`() {
        engine.evaluate(80f, VehicleGear.DRIVE)
        assertTrue(engine.uxRestrictionState.value.isRestricted)

        engine.evaluate(0f, VehicleGear.PARK)

        val state = engine.uxRestrictionState.value
        assertFalse(state.isRestricted)
        assertFalse(state.noVideo)
        assertEquals(Int.MAX_VALUE, state.maxListLength)
    }
}
