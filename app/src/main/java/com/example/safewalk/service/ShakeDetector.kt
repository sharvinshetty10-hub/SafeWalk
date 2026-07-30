package com.example.safewalk.service

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import kotlin.math.abs

class ShakeDetector(
    private val onShake: () -> Unit
) : SensorEventListener {

    private var lastUpdate = 0L
    private var lastX = 0f
    private var lastY = 0f
    private var lastZ = 0f
    private val shakeThreshold = 800

    override fun onSensorChanged(event: SensorEvent) {
        val now = System.currentTimeMillis()
        if (now - lastUpdate < 100) return
        
        val diff = now - lastUpdate
        lastUpdate = now

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        val speed = abs(x + y + z - lastX - lastY - lastZ) / diff * 10000
        if (speed > shakeThreshold) {
            onShake()
        }
        
        lastX = x
        lastY = y
        lastZ = z
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
