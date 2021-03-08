package com.barad.beatrunner

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import be.tarsos.dsp.AudioDispatcher
import be.tarsos.dsp.io.PipeDecoder
import be.tarsos.dsp.io.PipedAudioStream
import be.tarsos.dsp.io.TarsosDSPAudioInputStream
import be.tarsos.dsp.io.UniversalAudioInputStream
import be.tarsos.dsp.io.android.AndroidFFMPEGLocator
import be.tarsos.dsp.io.android.AudioDispatcherFactory
import be.tarsos.dsp.onsets.BeatRootSpectralFluxOnsetDetector
import be.tarsos.dsp.onsets.ComplexOnsetDetector
import be.tarsos.dsp.onsets.OnsetHandler
import be.tarsos.dsp.util.AudioResourceUtils
import com.barad.beatrunner.data.MusicStore
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackParameters
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ui.PlayerControlView


class MainActivity : AppCompatActivity() {

    private lateinit var playerView: PlayerControlView
    private lateinit var player: SimpleExoPlayer
    private lateinit var btn_slower: Button
    private lateinit var btn_faster: Button
    private lateinit var btn_reset: Button
    private var speed = 1.0F

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if(isStoragePermissionGranted()) {
            val musicList = MusicStore().getAllMusicFromDevice(this)

            player = SimpleExoPlayer.Builder(this).build()
            val mediaItem: MediaItem = MediaItem.fromUri(musicList[0].uri)
            player.setMediaItem(mediaItem)
            player.prepare()
            playerView = findViewById(R.id.player_view)
            playerView.showTimeoutMs = 0
            playerView.player = player

            AndroidFFMPEGLocator(this)


            val size = 2048
            val overlap = 2048-441
            val dispatcher: AudioDispatcher = AudioDispatcherFactory.fromPipe(musicList[0].path, 44100, size, overlap)

            dispatcher.setZeroPadFirstBuffer(true);

            val detector = ComplexOnsetDetector(2048)

            detector.setHandler(object : OnsetHandler {
                var i = 0
                override fun handleOnset(actualTime: Double, salience: Double) {
                    Log.i("barad-log", "$i $actualTime $salience")
                }
            })

            dispatcher.addAudioProcessor(detector)
            dispatcher.run()

            //val path = AudioResourceUtils.sanitizeResource(musicList[0].path)
            //val decoder = PipeDecoder()
            //val decodedStream = decoder.getDecodedStream(path, 44100, 0.0, -1.0)
            //val stream = decodedStream.readBytes()
            //decodedStream.close()




            //musicList[0].path?.let { Log.i("barad-log", stream.toString()) }




            btn_slower = findViewById(R.id.btn_slower)
            btn_faster = findViewById(R.id.btn_faster)
            btn_reset = findViewById(R.id.btn_reset)

            btn_slower.setOnClickListener{
                speed -= 0.1F
                player.setPlaybackParameters(PlaybackParameters(speed))
            }
            btn_reset.setOnClickListener{
                speed = 1.0F
                player.setPlaybackParameters(PlaybackParameters(speed))
            }
            btn_faster.setOnClickListener{
                speed += 0.1F
                player.setPlaybackParameters(PlaybackParameters(speed))
            }

        }



    }

    @SuppressLint("ObsoleteSdkInt")
    fun isStoragePermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED
            ) {
                true
            } else {
                ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                        1
                )
                false
            }
        } else {
            true
        }
    }

}