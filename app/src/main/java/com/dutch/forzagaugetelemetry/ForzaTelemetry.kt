package com.dutch.forzagaugetelemetry

data class ForzaTelemetry(
    val isRaceOn: Boolean = false,
    val timestampMS: Long = 0L,

    // Engine State
    val engineMaxRpm: Float = 0f,
    val engineIdleRpm: Float = 0f,
    val currentEngineRpm: Float = 0f,

    // Core Dynamics
    val speedMps: Float = 0f,
    val powerWatts: Float = 0f,
    val torqueNm: Float = 0f,
    val boostPsi: Float = 0f,
    val fuelRatio: Float = 0f,

    // G-forces (m/s^2)
    val accelX: Float = 0f,
    val accelY: Float = 0f,
    val accelZ: Float = 0f,

    // Tire Temperatures (Celsius)
    val tireTempFL: Float = 0f,
    val tireTempFR: Float = 0f,
    val tireTempRL: Float = 0f,
    val tireTempRR: Float = 0f,

    // Race Info
    val currentRaceTime: Float = 0f,
    val lapNumber: Int = 0,
    val racePosition: Int = 0,

    // Driver Inputs (0 to 255)
    val accel: Int = 0,
    val brake: Int = 0,
    val clutch: Int = 0,
    val handbrake: Int = 0,
    val gear: Int = 0,
    val steer: Int = 0 // Signed S8 (-127 to 127)
) {
    val speedKmh: Int get() = (speedMps * 3.6f).toInt()
    val speedMph: Int get() = (speedMps * 2.23694f).toInt()
    val horsepower: Int get() = (powerWatts * 0.00134102f).toInt()
    
    // Convert m/s^2 to G
    val gForceLat: Float get() = accelX / 9.81f
    val gForceLon: Float get() = accelZ / 9.81f
}
