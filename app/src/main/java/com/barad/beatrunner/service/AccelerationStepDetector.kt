package com.barad.beatrunner.service

import android.hardware.SensorEvent
import androidx.lifecycle.MutableLiveData
import java.time.Instant
import java.util.*
import kotlin.math.sqrt

class AccelerationStepDetector(
        private val steps: MutableLiveData<Float>,
        private val sensorTempo: MutableLiveData<Float>) {

    private val MINIMUM_THRESHOLD = 12
    private val ALPHA = 0.9f

    private var x: Float = 0f
    private var y: Float = 0f
    private var z: Float = 0f

    private val sensorQueue: Queue<Float> = LinkedList()
    private val timeQueue: Queue<Instant> = LinkedList()
    private var latest = Instant.now()
    private var currentThreshold = 0f

    private val timer = Timer()

    fun clear() {
        timeQueue.clear()
    }

    fun startTimer() {
        timer.schedule(object : TimerTask() {
            override fun run() {
                if (!sensorQueue.isEmpty()) {
                    sensorQueue.add(ALPHA * sensorQueue.last() +
                            (1 - ALPHA)*sqrt(x * x + y * y + z * z))
                }else {
                    sensorQueue.add(sqrt(x * x + y * y + z * z))
                }

                if (sensorQueue.last() > currentThreshold &&
                    sensorQueue.last() > MINIMUM_THRESHOLD) {
                    currentThreshold = sensorQueue.average().toFloat() - 1
                    val diff = Instant.now().toEpochMilli() - latest.toEpochMilli()
                    if (diff > 250) {
                        latest = Instant.now()
                        timeQueue.add(Instant.now())
                        steps.postValue(steps.value?.plus(1))
                    }
                }

                if (sensorQueue.size > 2) {
                    sensorQueue.remove()
                }
                if (timeQueue.size > 10) {
                    timeQueue.remove()
                }

                if (timeQueue.size > 4) {
                    val timeInstantList: List<Instant> = timeQueue.map { x -> x }
                    var sum: Long = 0
                    for (i in 1 until timeInstantList.size) {
                        sum += timeInstantList[i].toEpochMilli() -
                                timeInstantList[i - 1].toEpochMilli()
                    }
                    sensorTempo.postValue(60f / (sum / (timeInstantList.size - 1)) * 1000f)
                }

                if (currentThreshold > MINIMUM_THRESHOLD) {
                    currentThreshold -= 0.01f
                }

            }
        },0,10)
    }

    fun onSensorEvent(event: SensorEvent) {
        x = event.values[0]
        y = event.values[1]
        z = event.values[2]
    }

    fun stopTimer() {
        timer.cancel()
    }
}