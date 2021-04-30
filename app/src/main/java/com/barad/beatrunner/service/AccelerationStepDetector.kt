package com.barad.beatrunner.service

import android.hardware.SensorEvent
import android.util.Log
import androidx.lifecycle.MutableLiveData
import java.time.Instant
import java.util.*
import kotlin.math.sqrt

class AccelerationStepDetector(
        private val steps: MutableLiveData<Int>,
        private val sensorTempo: MutableLiveData<Float>) {

    var MINIMUM_THRESHOLD = 12

    private val sensorQueue: Queue<Float> = LinkedList()
    val timeQueue: Queue<Instant> = LinkedList()
    private var latest = Instant.now()
    private var currentThreshold = 0f

    fun onSensorEvent(event: SensorEvent) {
        val x: Float = event.values[0]
        val y: Float = event.values[1]
        val z: Float = event.values[2]

        sensorQueue.add(sqrt(x * x + y * y + z * z))
        if (sensorQueue.average() > currentThreshold && sensorQueue.average() > MINIMUM_THRESHOLD) {
            currentThreshold = sensorQueue.average().toFloat()
            val diff = Instant.now().toEpochMilli() - latest.toEpochMilli()
            if (diff > 250) {
                latest = Instant.now()
                timeQueue.add(Instant.now())
                steps.value = steps.value?.plus(1)
            }
        }

        if (sensorQueue.size > 10) {
            sensorQueue.remove()
        }
        if (timeQueue.size > 10) {
            timeQueue.remove()
        }

        if (timeQueue.size > 2) {
            val timeInstantList: List<Instant> = timeQueue.map { x -> x }
            var sum: Long = 0
            for (i in 1 until timeInstantList.size) {
                sum += timeInstantList[i].toEpochMilli() - timeInstantList[i - 1].toEpochMilli()
            }
            sensorTempo.value = (60f / (sum / (timeInstantList.size - 1)) * 1000f)
        }

        currentThreshold *= 0.997f
    }
}