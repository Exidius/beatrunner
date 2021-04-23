package com.barad.beatrunner.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Binder
import android.os.IBinder
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

class SensorService() : Service(), SensorEventListener {

    inner class SensorServiceBinder : Binder() {
        val service
            get() = this@SensorService
    }

    private val _sensorTempo = MutableLiveData<Float>()
    val sensorTempo
        get() = _sensorTempo

    private val _steps = MutableLiveData<Int>()
    val steps
        get() = _steps

    private var sensorManager: SensorManager? = null

    private var sensorGyro: Sensor? = null
    private var sensorAcc: Sensor? = null
    private var sensorLinAcc: Sensor? = null

    val sensorQueue: Queue<Float> = LinkedList()
    val timeQueue: Queue<Instant> = LinkedList()

    private var latest = Instant.now()

    init {
        sensorTempo.value = 150f
        steps.value = 0
    }

    override fun onBind(intent: Intent?): IBinder? {
        intent?.let {
            registerManager()
        }
        return SensorServiceBinder()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            registerManager()
        }

        return START_NOT_STICKY
    }

    private fun registerManager() {
        sensorManager = application.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sensorGyro = sensorManager!!.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        sensorAcc = sensorManager!!.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        sensorLinAcc = sensorManager!!.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)

        sensorManager!!.registerListener(this,sensorGyro,SensorManager.SENSOR_DELAY_FASTEST)
        sensorManager!!.registerListener(this,sensorAcc,SensorManager.SENSOR_DELAY_NORMAL)
        sensorManager!!.registerListener(this,sensorLinAcc,SensorManager.SENSOR_DELAY_FASTEST)
    }



    // Use in onStart()
//    fun register() {
//        sensorManager.registerListener(this,sensorGyro,SensorManager.SENSOR_DELAY_FASTEST)
//        sensorManager.registerListener(this,sensorAcc,SensorManager.SENSOR_DELAY_NORMAL)
//        sensorManager.registerListener(this,sensorLinAcc,SensorManager.SENSOR_DELAY_FASTEST)
//    }

    // Use in onStop()
//    fun unregister() {
//        sensorManager.unregisterListener(this)
//    }

    override fun onDestroy() {
        sensorManager?.unregisterListener(this)
        super.onDestroy()
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
                        val diff = Instant.now().toEpochMilli() - latest.toEpochMilli()
                        if (diff > 150) {
                            latest = Instant.now()
                            timeQueue.add(Instant.now())
                            steps.value = steps.value?.plus(1)
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
                        sensorTempo.value = (60f/(sum/(timeInstantList.size-1))*1000f)
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