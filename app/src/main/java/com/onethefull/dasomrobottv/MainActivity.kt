package com.onethefull.dasomrobottv

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Point
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable
import com.onethefull.dasomrobottv.databinding.ActivityMainBinding
import com.onethefull.dasomrobottv.provider.DasomProviderHelper
import com.onethefull.dasomrobottv.voice.GCSpeechToTextImpl
import com.onethefull.dasomrobottv.voice.SpeechToTextCallback
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {
    private val TAG = "dasomTV"
    private lateinit var binding: ActivityMainBinding

    private lateinit var speechToText: GCSpeechToTextImpl

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        var size = Point()
        windowManager.defaultDisplay.getSize(size)
        Log.d("DIS_TEST_TEST", ">>> size.x : " + size.x + ", size.y : " + size.y);

        setLottieAnimation(SpeechToTextStatus.End)

        speechToText = GCSpeechToTextImpl(this)
        if (checkPermission())
            initSpeechToText()

        Log.e("TAG", "customerCode = " + DasomProviderHelper.getCustomerCode(this))
        Log.e("TAG", "deviceCode = " + DasomProviderHelper.getDeviceCode(this))
        Log.e("TAG", "SerialNumber = " + DasomProviderHelper.getSerialNumber(this))
    }

    private fun initSpeechToText() {
        speechToText.setCallback(object : SpeechToTextCallback{
            override fun onSTTConnected() {
                Log.e(TAG, "onSTTConnected")
            }

            override fun onSTTDisconneted() {
                Log.e(TAG, "onSTTDisconneted")
            }

            override fun onVoiceStart() {
                Log.e(TAG, "onVoiceStart")
                GlobalScope.launch {
                    withContext(Dispatchers.Main) {
                        setLottieAnimation(SpeechToTextStatus.Start)
                    }
                }
            }

            override fun onVoice(data: ByteArray?, size: Int) {
                Log.e(TAG, "onVoice")
            }

            override fun onVoiceEnd() {
                Log.e(TAG, "onVoiceEnd")
                if (nowSpeechToTextStatus == SpeechToTextStatus.Sending)
                    return
                GlobalScope.launch {
                    withContext(Dispatchers.Main) {
                        setLottieAnimation(SpeechToTextStatus.End)
                    }
                }
            }

            override fun onVoiceResult(result: String?) {
                Log.e(TAG, "onVoiceResult result = $result")

                var isFinish = false
                result?.let {
                    if (result.contains("종료")){
                        isFinish = true
                    }
                }
                GlobalScope.launch {
                    withContext(Dispatchers.Main) {
                        setLottieAnimation(SpeechToTextStatus.Sending)
                    }

                    if (isFinish) {
                        withContext(Dispatchers.Main) {
                            finish()
                        }
                        return@launch
                    }
                    //TODO send
                    delay(5000)
                    withContext(Dispatchers.Main) {
                        setLottieAnimation(SpeechToTextStatus.End)
                    }
                    speechToText.resume()
                }
            }

        })
        speechToText.start()
    }

    private fun checkPermission(): Boolean {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                arrayOf(
                    Manifest.permission.RECORD_AUDIO
                ),
                REQUEST_PERMISSION
            )
            return false
        }
        return true
    }

    private val REQUEST_PERMISSION = 11
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String?>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_PERMISSION ->
                if (grantResults.isNotEmpty()) {
                    val a = grantResults[0] == PackageManager.PERMISSION_GRANTED
                    if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        initSpeechToText()
                    } else {
                        val toast = Toast.makeText(this@MainActivity, "동의하지 않으시면 사용하실 수 없습니다.", Toast.LENGTH_SHORT)
                        toast.setGravity(Gravity.CENTER, 0, 0)
                        toast.show()
                        finish()
                    }
                } else {
                    finish()
                }
        }
    }

    private enum class SpeechToTextStatus {
        Start, End, Sending
    }
    private var nowSpeechToTextStatus = SpeechToTextStatus.End
    private fun setLottieAnimation(status: SpeechToTextStatus) {
        nowSpeechToTextStatus = status
        val assetName = when (status) {
            SpeechToTextStatus.Start -> "stt_loading.json"
            SpeechToTextStatus.End -> "stt_end.json"
            SpeechToTextStatus.Sending -> "server_sending.json"
        }
        binding.lottieImg.cancelAnimation()
        binding.lottieImg.setAnimation(assetName)
        binding.lottieImg.repeatMode = LottieDrawable.RESTART
        binding.lottieImg.repeatCount = LottieDrawable.INFINITE
        binding.lottieImg.playAnimation()
    }

    override fun onResume() {
        super.onResume()
        speechToText.resume()
    }

    override fun onPause() {
        super.onPause()
        speechToText.pause()
    }

    override fun onDestroy() {
        Log.e(TAG, "ondestroy")
        super.onDestroy()
        speechToText.release()
    }
}