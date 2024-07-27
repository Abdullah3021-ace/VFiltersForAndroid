package com.example.videofilters

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.daasuu.gpuv.composer.FillMode
import com.daasuu.gpuv.composer.GPUMp4Composer
import com.daasuu.gpuv.composer.Rotation
import com.daasuu.gpuv.egl.filter.GlFilterGroup
import com.daasuu.gpuv.egl.filter.GlMonochromeFilter
import com.daasuu.gpuv.egl.filter.GlVignetteFilter
import com.example.videofilters.databinding.ActivityMainBinding
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import java.io.File

class MainActivity : AppCompatActivity() {
    private val TAG = "MainActivity"
    private val REQUEST_VIDEO_PICK = 1
    private lateinit var exoPlayer: ExoPlayer
    private lateinit var binding : ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        exoPlayer = ExoPlayer.Builder(this).build()
        binding.idExoPlayerVIew.player = exoPlayer

        // Pick video from gallery
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, REQUEST_VIDEO_PICK)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_VIDEO_PICK && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                val srcMp4Path = getRealPathFromURI(uri)
                val destMp4Path = File(getExternalFilesDir(null), "filtered_video.mp4").absolutePath
                applyFilters(srcMp4Path, destMp4Path)
            }
        }
    }

    private fun getRealPathFromURI(contentUri: Uri): String {
        val cursor = contentResolver.query(contentUri, null, null, null, null)
        cursor?.moveToFirst()
        val idx = cursor?.getColumnIndex(MediaStore.Video.VideoColumns.DATA)
        val realPath = cursor?.getString(idx!!)
        cursor?.close()
        return realPath ?: ""
    }

    private fun applyFilters(srcMp4Path: String, destMp4Path: String) {
        GPUMp4Composer(srcMp4Path, destMp4Path)
              .size(540, 960)
            .fillMode(FillMode.PRESERVE_ASPECT_FIT)
            .filter(GlFilterGroup(GlMonochromeFilter(), GlVignetteFilter()))
            .listener(object : GPUMp4Composer.Listener {
                override fun onProgress(progress: Double) {
                    Log.d(TAG, "onProgress = $progress")
                }

                override fun onCompleted() {
                    Log.d(TAG, "onCompleted()")
                    runOnUiThread {
                        Toast.makeText(
                            this@MainActivity,
                            "Codec complete path = $destMp4Path",
                            Toast.LENGTH_SHORT
                        ).show()
                        playVideo(destMp4Path)
                    }
                }

                override fun onCanceled() {
                    Log.d(TAG, "onCanceled")
                }

                override fun onFailed(exception: Exception) {
                    Log.e(TAG, "onFailed()", exception)
                }
            })
            .start()
    }

    private fun playVideo(videoPath: String) {
        val mediaItem = MediaItem.fromUri(Uri.fromFile(File(videoPath)))
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.play()
    }

    override fun onDestroy() {
        super.onDestroy()
        exoPlayer.release()
    }
}