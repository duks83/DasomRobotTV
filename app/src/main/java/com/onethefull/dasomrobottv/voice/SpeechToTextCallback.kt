package com.onethefull.dasomrobottv.voice

/**
 * Created by Douner on 2020/06/03.
 */
interface SpeechToTextCallback {
    fun onSTTConnected()
    fun onSTTDisconneted()
    fun onVoiceStart()
    fun onVoice(data: ByteArray?, size: Int)
    fun onVoiceEnd()
    fun onVoiceResult(result: String?)
}