package com.barad.beatrunner.util

import android.util.Log
import be.tarsos.dsp.onsets.OnsetHandler

class OnSetHandler : OnsetHandler {

    override fun handleOnset(time: Double, salience: Double) {
        Log.v("barad-log-onset", "asd")
        Log.i("barad-log-onset", time.toString() + " - " + salience.toString())
    }
}