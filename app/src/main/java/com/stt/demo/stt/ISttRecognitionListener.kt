package com.stt.demo.stt

interface ISttRecognitionListener {
    fun onBeginningOfSpeech()
    fun onBufferReceived()
    fun onEndOfSpeech()
    fun onResults(result: String)
    fun onEvent()
    fun onPartialResults(result: String)
    fun onReadyForSpeech()
}