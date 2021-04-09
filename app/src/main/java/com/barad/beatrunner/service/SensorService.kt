package com.barad.beatrunner.service

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import androidx.core.content.ContextCompat.getSystemService
import androidx.lifecycle.MutableLiveData
import com.barad.beatrunner.models.Music
import com.google.android.exoplayer2.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.*
import kotlin.math.abs
import kotlin.math.sqrt

class SensorService(private val sensorManager: SensorManager) : SensorEventListener {

    private val _tempo = MutableLiveData<Float>()
    val tempo
        get() = _tempo

    private var sensorGyro: Sensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private var sensorAcc: Sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private var sensorLinAcc: Sensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)

    val sensorQueue: Queue<Float> = LinkedList()
    val timeQueue: Queue<Instant> = LinkedList()

    private var latest = Instant.now()

    init {
        tempo.value = 150f
    }

    // Use in onStart()
    fun register() {
        sensorManager.registerListener(this,sensorGyro,SensorManager.SENSOR_DELAY_FASTEST)
        sensorManager.registerListener(this,sensorAcc,SensorManager.SENSOR_DELAY_NORMAL)
        sensorManager.registerListener(this,sensorLinAcc,SensorManager.SENSOR_DELAY_FASTEST)
    }

    // Use in onStop()
    fun unregister() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event != null) {
            when (event.sensor.type) {
                Sensor.TYPE_ACCELEROMETER -> {
                    val x: Float = event.values[0]
                    val y: Float = event.values[1]
                    val z: Float = event.values[2]

                    sensorQueue.add(sqrt(x*x+y*y+z*z))
                    if (sensorQueue.average() > 20) {
                        var diff = Instant.now().toEpochMilli() - latest.toEpochMilli()
                        if (diff > 150) {
                            latest = Instant.now()
                            timeQueue.add(Instant.now())

                        }
                    }

                    if (sensorQueue.size > 10) { sensorQueue.remove() }
                    if (timeQueue.size > 10) { timeQueue.remove() }

                    if (timeQueue.size > 2) {
                        val timeInstantList:List<Instant> = timeQueue.map { x -> x }
                        var sum: Long = 0
                        for(i in 1 until timeInstantList.size) {
                            sum += timeInstantList[i].toEpochMilli()-timeInstantList[i-1].toEpochMilli()
                        }
                        tempo.value = (60f/(sum/timeInstantList.size)*1000f)
                    }
                }
                else -> {
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

    }
}