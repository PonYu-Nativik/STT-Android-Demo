package com.stt.demo

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.stt.demo.stt.ISttListener
import com.stt.demo.stt.ISttRecognitionListener
import com.stt.demo.stt.STTManager
import com.stt.demo.ui.theme.ApplicationTheme

class MainActivity : ComponentActivity() {

    companion object {
        private const val REQUEST_CODE_AUDIO = 1001
        private const val REQUEST_CODE_SPEECH = 1001
    }

    private lateinit var sttManager: STTManager
    private var isSpeak: Boolean = false

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_SPEECH && resultCode == RESULT_OK) {
            val matches = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            matches?.forEach {
                Log.d("MainActivity", "Распознанный текст: $it")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        /*val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Скажите что-нибудь")
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US") // Принудительно укажем язык
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, this.packageName)

        try {
            startActivityForResult(intent, REQUEST_CODE_SPEECH)
        } catch (e: ActivityNotFoundException) {
            Log.e("MainActivity", "Speech recognition not supported on this device")
        }*/

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                REQUEST_CODE_AUDIO
            )
        }

        val logs = mutableStateListOf<String>()
        val recognizedText = mutableStateOf("")
        val isSpeak = mutableStateOf(false)

        val sttListener = object : ISttListener {
            override fun onSuccess() {
                logs.add("STT Initialized Successfully")
            }

            override fun onError(error: String) {
                isSpeak.value = false
                logs.add("Error: $error")
            }
        }

        val sttRecognitionListener = object : ISttRecognitionListener {
            override fun onReadyForSpeech() {
                logs.add("Ready for Speech")
            }

            override fun onBeginningOfSpeech() {
                logs.add("Speech Started")
            }

            override fun onBufferReceived() {
                logs.add("Speech onBufferReceived")
            }

            override fun onEndOfSpeech() {
                isSpeak.value = false
                logs.add("Speech Ended")
            }

            override fun onResults(result: String) {
                recognizedText.value = result
                logs.add("Recognized: $result")
            }

            override fun onPartialResults(result: String) {
                logs.add("Partial: $result")
            }

            override fun onEvent() {
                logs.add("Event Occurred")
            }
        }

        sttManager = STTManager(this, sttListener, sttRecognitionListener)

        enableEdgeToEdge()
        setContent {
            val logsState = remember { logs }
            val recognizedState = remember { recognizedText }
            var isSpeakingState by remember { isSpeak }

            ApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    StartScreen(
                        modifier = Modifier.padding(innerPadding),
                        onClick = {
                            if (isSpeakingState) {
                                sttManager.stopListening()
                                isSpeakingState = false
                            } else {
                                sttManager.startListening()
                                isSpeakingState = true
                            }
                        },
                        logs = logsState,
                        recognizedText = recognizedState.value,
                        isSpeaking = isSpeakingState
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        sttManager.destroy()
        super.onDestroy()
    }
}

@Composable
fun StartScreen(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    logs: List<String>,
    recognizedText: String,
    isSpeaking: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = recognizedText, modifier = Modifier.padding(bottom = 16.dp))
        Button(onClick = onClick) {
            Text(text = if (isSpeaking) "Stop" else "Start")
        }
        Spacer(modifier = Modifier.height(16.dp))
        logs.forEach { log ->
            Text(text = log)
        }
    }
}