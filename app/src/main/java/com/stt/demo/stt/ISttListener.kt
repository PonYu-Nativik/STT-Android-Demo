package com.stt.demo.stt

interface ISttListener {
    fun onSuccess()
    fun onError(error: String)
}