package com.barad.beatrunner

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Observer
import com.barad.beatrunner.data.MusicStore
import com.barad.beatrunner.models.Music
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackParameters
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ui.PlayerControlView
import kotlinx.coroutines.*


class MainActivity : AppCompatActivity() {

    private lateinit var playerView: PlayerControlView
    private lateinit var player: SimpleExoPlayer
    private lateinit var btn_slower: Button
    private lateinit var btn_faster: Button
    private lateinit var btn_reset: Button
    private lateinit var tv_tempo: TextView
    private var speed = 1.0F

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if(isStoragePermissionGranted()) {
            val musicStore = MusicStore()

            musicStore.musicList.observe(this, { list ->
                Log.d("barad-log-main", "----music list---")
                list.forEach {
                    Log.d("barad-log-main", it.toString())
                }
                Log.d("barad-log-main", "-----------------")
            })

            musicStore.getAllMusicFromDevice(this,true)

            player = SimpleExoPlayer.Builder(this).build()
            //val mediaItem: MediaItem = MediaItem.fromUri(musicList[2].uri)
            //player.setMediaItem(mediaItem)
            //player.prepare()
            playerView = findViewById(R.id.player_view)
            playerView.showTimeoutMs = 0
            playerView.player = player

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
                if (musicStore.fetchFinished.value == true) Log.d("barad-log-main", "FINISH") else Log.d("barad-log-main", "NOT finished")
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