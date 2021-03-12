package com.barad.beatrunner

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.barad.beatrunner.data.MusicStore
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackParameters
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ui.PlayerControlView
import java.nio.ByteOrder


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

            val extractor = MediaExtractor()
            extractor.setDataSource(this, musicList[0].uri, null)


            Log.i("barad-log", "Track count:" + extractor.trackCount.toString())
            extractor.getTrackFormat(0).getString(MediaFormat.KEY_MIME)?.let { Log.i("barad-log", "Track format: " + it) }
            extractor.getTrackFormat(0).getInteger(MediaFormat.KEY_CHANNEL_COUNT).let { Log.i("barad-log", "Channel count: " + it.toString()) }


            val codec: MediaCodec = extractor.getTrackFormat(0).getString(MediaFormat.KEY_MIME)?.let { MediaCodec.createDecoderByType(it) }!!
            codec.configure(extractor.getTrackFormat(0), null, null, 0) //crypto can be a problem
            codec.start()



            extractor.selectTrack(0)
            Log.i("barad-log", "Sample time: " + extractor.sampleTime)
            for(i in 0..50) {
                extractor.advance()

            }
            Log.i("barad-log", "Sample time: " + extractor.sampleTime)

            codec.stop()
            codec.release()

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

    fun getSamplesForChannel(codec: MediaCodec, bufferId: Int, channelIx: Int): ShortArray? {
        val outputBuffer = codec.getOutputBuffer(bufferId)
        val format = codec.getOutputFormat(bufferId)
        val samples = outputBuffer!!.order(ByteOrder.nativeOrder()).asShortBuffer()
        val numChannels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        if (channelIx < 0 || channelIx >= numChannels) {
            return null
        }
        val res = ShortArray(samples.remaining() / numChannels)
        for (i in res.indices) {
            res[i] = samples[i * numChannels + channelIx]
        }
        return res
    }
}