package com.stt.demo.stt

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log

class STTManager(
    context: Context,
    private val sttListener: ISttListener?,
    private val sttRecognitionListener: ISttRecognitionListener?,
) {
    private var speechRecognizer: SpeechRecognizer? = null

    private var intent: Intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en-US")
    }

    private val TAG = "STTManager Native"

    init {
        errorLog("Speech Recognition init.", false)
        Handler(Looper.getMainLooper()).post {

            val intent1 = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            val activities = context.packageManager.queryIntentActivities(intent1, 0)
            if (activities.isEmpty()) {
                Log.e(TAG, "Распознавание речи не поддерживается на этом устройстве")
            } else {
                Log.d(TAG, "STT доступен, можно использовать RecognizerIntent")
            }

            val intent = Intent(RecognizerIntent.ACTION_GET_LANGUAGE_DETAILS)
            context.sendOrderedBroadcast(intent, null, object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    val results = getResultExtras(true)
                    if (results == null) {
                        Log.e(TAG, "No results from ACTION_GET_LANGUAGE_DETAILS")
                        return
                    }

                    // Предпочитаемый язык
                    val prefLang = results.getString(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE)
                    Log.d(TAG, "EXTRA_LANGUAGE_PREFERENCE: $prefLang")

                    // Поддерживаемые языки
                    val allSupportedLangs = results.getCharSequenceArrayList(RecognizerIntent.EXTRA_SUPPORTED_LANGUAGES)
                    Log.d(TAG, "EXTRA_SUPPORTED_LANGUAGES count: ${allSupportedLangs?.size ?: 0}")
                    allSupportedLangs?.forEach {
                        Log.d(TAG, "Supported language: $it")
                    }

                    // Текущая языковая модель
                    val languageModel = results.getString(RecognizerIntent.EXTRA_LANGUAGE)
                    Log.d(TAG, "EXTRA_LANGUAGE: $languageModel")

                    // Предпочтение оффлайн-режима
                    val offlineSupported = results.getBoolean(RecognizerIntent.EXTRA_PREFER_OFFLINE, false)
                    Log.d(TAG, "EXTRA_PREFER_OFFLINE: $offlineSupported")

                    // Максимальное количество результатов
                    val maxResults = results.getInt(RecognizerIntent.EXTRA_MAX_RESULTS, -1)
                    Log.d(TAG, "EXTRA_MAX_RESULTS: $maxResults")

                    // Разрешены ли частичные результаты
                    val partialResults = results.getBoolean(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
                    Log.d(TAG, "EXTRA_PARTIAL_RESULTS: $partialResults")

                    // Минимальное время прослушивания (мс)
                    val minSpeechLength = results.getLong(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, -1)
                    Log.d(TAG, "EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS: $minSpeechLength")

                    // Задержка перед завершением прослушивания (мс)
                    val completeSilence = results.getLong(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, -1)
                    Log.d(TAG, "EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS: $completeSilence")

                    // Время возможного завершения прослушивания (мс)
                    val possiblyCompleteSilence = results.getLong(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, -1)
                    Log.d(TAG, "EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS: $possiblyCompleteSilence")

                    // Оценки уверенности
                    val confidenceScores = results.getFloatArray(RecognizerIntent.EXTRA_CONFIDENCE_SCORES)
                    if (confidenceScores != null) {
                        confidenceScores.forEachIndexed { index, score ->
                            Log.d(TAG, "Confidence score [$index]: $score")
                        }
                    } else {
                        Log.d(TAG, "EXTRA_CONFIDENCE_SCORES: null")
                    }

                    // Флаг возврата только предпочитаемого языка
                    val onlyReturnLangPref = results.getBoolean(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, false)
                    Log.d(TAG, "EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE: $onlyReturnLangPref")
                }
            }, null, Activity.RESULT_OK, null, null)

           /* if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                errorLog("Speech Recognition is not available on this device.")
            } else {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
                speechRecognizer?.setRecognitionListener( object : RecognitionListener {
                        override fun onBeginningOfSpeech() {
                            errorLog("onBeginningOfSpeech", false)
                            sttRecognitionListener?.onBeginningOfSpeech()
                        }

                        override fun onBufferReceived(buffer: ByteArray) {
                            errorLog("onBufferReceived: $buffer", false)
                            sttRecognitionListener?.onBufferReceived()
                        }

                        override fun onEndOfSpeech() {
                            errorLog("onEndOfSpeech", false)
                            sttRecognitionListener?.onEndOfSpeech()
                            speechRecognizer?.stopListening()
                        }

                        override fun onResults(results: Bundle) {
                            errorLog("onResults", false)
                            val matches: ArrayList<String>? = results
                                .getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)

                            var text = ""

                            if (matches != null) {
                                for (result in matches)
                                    text += result.trimIndent()

                                sttRecognitionListener?.onResults(text)
                            }
                            else
                                errorLog("onResults: matches is null or empty")
                        }

                        override fun onError(errorCode: Int) {
                            val errorMessage = getErrorText(errorCode)
                            errorLog("FAILED: $errorMessage")
                        }

                        override fun onEvent(arg0: Int, arg1: Bundle) {
                            errorLog("onEvent", false)
                            sttRecognitionListener?.onEvent()
                        }

                        override fun onPartialResults(results: Bundle) {
                            errorLog("onPartialResults", false)
                            val matches: ArrayList<String>? = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)

                            var text = ""

                            if (matches != null) {
                                for (result in matches)
                                    text += result.trimIndent()

                                sttRecognitionListener?.onPartialResults(text)
                            }
                            else
                                errorLog("onPartialResults: matches is null or empty")
                        }

                        override fun onReadyForSpeech(arg0: Bundle) {
                            errorLog("onReadyForSpeech", false)
                            sttRecognitionListener?.onReadyForSpeech()
                        }

                        override fun onRmsChanged(rmsdB: Float) {

                        }
                    })
                errorLog("Speech Recognition is available on this device.", false)
                sttListener?.onSuccess()
            }*/
        }
    }

    fun startListening() {
        Handler(Looper.getMainLooper()).post {
            speechRecognizer?.startListening(intent)
        }
    }

    fun stopListening() {
        Handler(Looper.getMainLooper()).post {
            speechRecognizer?.stopListening()
        }
    }

    fun cancelListening() {
        Handler(Looper.getMainLooper()).post {
            speechRecognizer?.cancel()
        }
    }

    fun destroy() {
        Handler(Looper.getMainLooper()).post {
            speechRecognizer?.destroy()
        }
    }

    fun errorLog(msg: String?, sendError: Boolean = true) {
        Log.e(TAG, msg ?: "Error")

        if (sendError)
            sttListener?.onError(msg?: "Error");
    }

    fun getErrorText(errorCode: Int): String {
        val message: String = when (errorCode) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
            SpeechRecognizer.ERROR_CLIENT -> "Client side error"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
            SpeechRecognizer.ERROR_NETWORK -> "Network error"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
            SpeechRecognizer.ERROR_NO_MATCH -> "No match"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "RecognitionService busy"
            SpeechRecognizer.ERROR_SERVER -> "error from server"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
            SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED -> "Language Not supported"
            SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE -> "Language Unavailable"
            else -> "Didn't understand, please try again."
        }
        return message
    }
}