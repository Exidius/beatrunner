package com.barad.beatrunner.service

import android.hardware.SensorEvent
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.paramsen.noise.Noise
import kotlin.math.sqrt
import java.time.Instant
import java.util.*

class GyroscopeStepDetector(private val steps: MutableLiveData<Float>,
                            private val sensorTempo: MutableLiveData<Float>) {

    private var x: Float = 0f
    private var y: Float = 0f
    private var z: Float = 0f
    private var sensorQueue: MutableList<Float> = mutableListOf()
    private val sensorBuffer: MutableList<Float> = mutableListOf()

    private val timer = Timer()

    private var isBufferFilled = false

    fun startTimer() {
        timer.schedule(object : TimerTask() {
            override fun run() {
                sensorBuffer.add((x+y+z)/3)

                if(isBufferFilled && sensorBuffer.size >= 25) {
                    sensorQueue = sensorQueue.subList(25,sensorQueue.size)
                    sensorQueue.addAll(sensorBuffer)
                    sensorBuffer.clear()
                }

                if (!isBufferFilled && sensorBuffer.size >= 64) {
                    isBufferFilled = true
                    sensorQueue.addAll(sensorBuffer)
                    sensorBuffer.clear()
                }

                if(sensorQueue.size == 64) {
                    val noise = Noise.real(64)

                    val input = sensorQueue.toFloatArray()
                    val output = FloatArray(input.size+2)

                    noise.fft(input, output)

                    val bins = FloatArray(32)
                    for (i in 0 until input.size-1 step 2) {
                        val square = sqrt((output[i] * output[i] + output[i + 1] * output[i + 1]))
                        if (square.isNaN()) {
                            bins[i/2] = 0f
                        } else {
                            bins[i/2] = square
                        }

                    }

                    val indexOfMaxBin = bins.indexOfFirst { it == bins.maxOrNull() }

                    if(indexOfMaxBin != -1) {
                        if (indexOfMaxBin != 0 && indexOfMaxBin < 31) {
                            val p = 0.5f * ((bins[indexOfMaxBin - 1] - bins[indexOfMaxBin + 1]) /
                                    (bins[indexOfMaxBin - 1] - 2 * bins[indexOfMaxBin] + bins[indexOfMaxBin + 1]))

                            val interpolatedLocation = indexOfMaxBin + p

                            val peakFrequency = interpolatedLocation * 20 / 64
                            val peakValue = bins[indexOfMaxBin] - 0.25f * (bins[indexOfMaxBin-1] - bins[indexOfMaxBin+1]) * p



                            if ( peakValue > 5 ) {
                                sensorTempo.postValue(peakFrequency * 60)
                                steps.postValue(steps.value?.plus(peakFrequency * 0.05f))
                            }
                        }
                    }
                }

            }
        },0,50)
    }

    fun stopTimer() {
        timer.cancel()
    }

    fun onSensorEvent(event: SensorEvent) {
        x = event.values[0]
        y = event.values[1]
        z = event.values[2]
    }
}