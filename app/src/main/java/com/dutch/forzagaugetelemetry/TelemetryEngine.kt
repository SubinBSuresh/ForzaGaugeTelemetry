package com.dutch.forzagaugetelemetry

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.nio.ByteBuffer
import java.nio.ByteOrder

class TelemetryEngine(private val port: Int = 20066) {
    private val TAG = "FORZA__TelemetryEngine"

    private val _telemetryState = MutableStateFlow(ForzaTelemetry())
    val telemetryState = _telemetryState.asStateFlow()

    suspend fun startListening() = withContext(Dispatchers.IO) {
        val buffer = ByteArray(324)
        val packet = DatagramPacket(buffer, buffer.size)

        Log.i(TAG, "Listening on port $port")

        try {
            DatagramSocket(port).use { socket ->
                socket.reuseAddress = true
                Log.i(TAG, "Socket bound, waiting for data...")

                while (isActive) {
                    socket.receive(packet)

                    if (packet.length < 324) continue

                    val byteBuffer = ByteBuffer.wrap(packet.data, 0, packet.length).order(ByteOrder.LITTLE_ENDIAN)

                    val telemetry = ForzaTelemetry(
                        isRaceOn = byteBuffer.getInt(0) == 1,
                        timestampMS = byteBuffer.getInt(4).toLong() and 0xFFFFFFFFL,
                        engineMaxRpm = byteBuffer.getFloat(8),
                        engineIdleRpm = byteBuffer.getFloat(12),
                        currentEngineRpm = byteBuffer.getFloat(16),
                        
                        // Acceleration
                        accelX = byteBuffer.getFloat(20),
                        accelY = byteBuffer.getFloat(24),
                        accelZ = byteBuffer.getFloat(28),

                        // Tire Temps
                        tireTempFL = byteBuffer.getFloat(204),
                        tireTempFR = byteBuffer.getFloat(208),
                        tireTempRL = byteBuffer.getFloat(212),
                        tireTempRR = byteBuffer.getFloat(216),

                        speedMps = byteBuffer.getFloat(256),
                        powerWatts = byteBuffer.getFloat(260),
                        torqueNm = byteBuffer.getFloat(264),
                        boostPsi = byteBuffer.getFloat(284),
                        fuelRatio = byteBuffer.getFloat(288),
                        
                        currentRaceTime = byteBuffer.getFloat(308),
                        lapNumber = byteBuffer.getShort(312).toInt() and 0xFFFF,
                        racePosition = byteBuffer.get(314).toInt() and 0xFF,
                        accel = byteBuffer.get(315).toInt() and 0xFF,
                        brake = byteBuffer.get(316).toInt() and 0xFF,
                        clutch = byteBuffer.get(317).toInt() and 0xFF,
                        handbrake = byteBuffer.get(318).toInt() and 0xFF,
                        gear = byteBuffer.get(319).toInt() and 0xFF,
                        steer = byteBuffer.get(320).toInt()
                    )
                    
                    _telemetryState.value = telemetry
                }
            }
        } catch (exception: Exception) {
            Log.e(TAG, "Error: ${exception.message}")
        }
    }
}
