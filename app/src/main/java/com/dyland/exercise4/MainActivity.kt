package com.dyland.exercise4

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import com.dyland.exercise4.ui.theme.Exercise4Theme
import java.util.Locale

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {

    private lateinit var textToSpeech: TextToSpeech
    private var isTextToSpeechInitialized = false

    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var recognizerIntent: Intent

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val requestPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                if (isGranted) {
                    Log.i("Exercise4", "Permission granted")
                } else {
                    Log.i("Exercise4", "Permission not granted")
                }
            }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        }
        textToSpeech = TextToSpeech(this, this)

        setContent {
            var hasRecordAudioPermission = remember {
                ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED
            }

            Log.d("hasRecordAudioPermission", hasRecordAudioPermission.toString())

            if (!hasRecordAudioPermission) {
                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }

            Exercise4Theme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainView(
                        modifier = Modifier.padding(innerPadding),
                    )
                }
            }
        }
    }

    override fun onInit(status: Int) {
        Log.d("MainActivity", "onInit: $status")
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech.setLanguage(Locale("en", "US"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.i("MainActivity", "Language not supported")
            } else {
                isTextToSpeechInitialized = true
            }
        }
    }

    @Composable
    fun MainView(
        modifier: Modifier = Modifier,
    ) {

        val valueText = remember {
            mutableStateOf("")
        }

        val isLoading = remember {
            mutableStateOf(false)
        }

        Column (
            modifier = modifier
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center
        ) {
            TextField(
                value = valueText.value,
                onValueChange = {
                    valueText.value = it
                },
                maxLines = 10,
                modifier = Modifier
                    .height(300.dp)
                    .fillMaxWidth()
                    .background(Color.LightGray),
                singleLine = false,
                label = {
                    Text(text = "Enter text")
                }
            )

            Row {
                Button(
                    onClick = {
                       if (!isLoading.value) {
                           speechRecognizer.startListening(recognizerIntent)
                       } else {
                           speechRecognizer.stopListening()
                       }
                    },
                ) {
                    Text(text = if (isLoading.value) "Stop" else "Start")
                }

                Button(
                    onClick = {
                        Log.d("Exercise4", "isTextToSpeechInitialized: $isTextToSpeechInitialized")
                        if (isTextToSpeechInitialized) {
                            textToSpeech.speak(valueText.value, TextToSpeech.QUEUE_FLUSH, null, "")
                        }
                    }
                ) {
                    Text(text = "Speech")
                }
            }
        }

        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                isLoading.value = true
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val result = matches?.get(0) ?: "No result"

                valueText.value = result
                isLoading.value = false
            }

            override fun onError(error: Int) {
                Log.d("Exercise4", "onError: $error")
                isLoading.value = false
            }

            override fun onBeginningOfSpeech() {}

            override fun onEndOfSpeech() {
                Log.d("Exercise4", "onEndOfSpeech: ")
            }
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    override fun onDestroy() {
        if (isTextToSpeechInitialized) {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }

        speechRecognizer.destroy()
        super.onDestroy()
    }
}

