package com.jarvis.ai

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

class MainActivity : ComponentActivity() {

    private lateinit var tts: TextToSpeech

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts.language = Locale("hi", "IN")
            }
        }

        setContent {
            JarvisScreen(
                onSpeak = { text ->
                    tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "jarvis")
                }
            )
        }
    }

    override fun onDestroy() {
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
        super.onDestroy()
    }
}

@Composable
fun JarvisScreen(onSpeak: (String) -> Unit) {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var apiKey by remember { mutableStateOf("") }
    var question by remember { mutableStateOf("") }
    var answer by remember {
        mutableStateOf("Good day, sir. JARVIS online hai. Aapki seva mein hazir hoon.")
    }
    var loading by remember { mutableStateOf(false) }
    var listening by remember { mutableStateOf(false) }

    val recognizer = remember {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            SpeechRecognizer.createSpeechRecognizer(context)
        } else null
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startListening(recognizer)
        } else {
            answer = "Sir, microphone permission dena zaroori hai."
        }
    }

    DisposableEffect(recognizer) {
        recognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                listening = true
            }

            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                listening = false
            }

            override fun onError(error: Int) {
                listening = false
                answer = "Voice samajhne mein dikkat hui. Dobara try kijiye."
            }

            override fun onResults(results: Bundle?) {
                listening = false
                val words = results?.getStringArrayList(
                    SpeechRecognizer.RESULTS_RECOGNITION
                )
                question = words?.firstOrNull() ?: ""
            }

            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        onDispose {
            recognizer?.destroy()
        }
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF00E5FF),
            background = Color(0xFF050B16),
            surface = Color(0xFF101C30)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF050B16))
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(modifier = Modifier.height(22.dp))

            Text(
                text = "J.A.R.V.I.S",
                color = Color(0xFF00E5FF),
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "PERSONAL AI ASSISTANT",
                color = Color.LightGray,
                fontSize = 12.sp,
                letterSpacing = 3.sp
            )

            Spacer(modifier = Modifier.height(28.dp))

            Box(
                modifier = Modifier
                    .size(150.dp)
                    .background(
                        Color(0xFF102D48),
                        RoundedCornerShape(100.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "J",
                    color = Color(0xFF00E5FF),
                    fontSize = 76.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = if (listening) "LISTENING..." else if (loading) "THINKING..." else "SYSTEM ONLINE",
                color = Color(0xFF00E5FF),
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(22.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF101C30)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "JARVIS",
                        color = Color(0xFF00E5FF),
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        answer,
                        color = Color.White,
                        fontSize = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Gemini API Key") },
                placeholder = { Text("Paste your Gemini API key") },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = question,
                onValueChange = { question = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Ask JARVIS anything") },
                placeholder = { Text("Hello Jarvis...") }
            )

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = {
                    if (ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        startListening(recognizer)
                    } else {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (listening) "Listening..." else "🎙  Speak to JARVIS")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    if (apiKey.isBlank()) {
                        answer = "Sir, pehle Gemini API Key enter kijiye."
                    } else if (question.isBlank()) {
                        answer = "Sir, apna question type ya speak kijiye."
                    } else {
                        loading = true
                        val prompt = question
                        scope.launch {
                            answer = try {
                                askGemini(apiKey, prompt)
                            } catch (e: Exception) {
                                "Connection error: ${e.message ?: "Please try again."}"
                            }
                            loading = false
                            onSpeak(answer)
                        }
                    }
                },
                enabled = !loading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (loading) "Please wait..." else "➤  Send to JARVIS")
            }

            Spacer(modifier = Modifier.height(22.dp))

            Text(
                "PHONE CONTROL",
                color = Color(0xFF00E5FF),
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = {
                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://www.youtube.com/results?search_query=lofi+music")
                    )
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("▶  Open YouTube")
            }

            OutlinedButton(
                onClick = {
                    val intent = context.packageManager
                        .getLaunchIntentForPackage("com.whatsapp")
                    if (intent != null) {
                        context.startActivity(intent)
                    } else {
                        answer = "WhatsApp is not installed."
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("✉  Open WhatsApp")
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "JARVIS AI • Version 1.0",
                color = Color.Gray,
                fontSize = 11.sp
            )
        }
    }
}

private fun startListening(recognizer: SpeechRecognizer?) {
    if (recognizer == null) return

    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "hi-IN")
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
    }

    recognizer.startListening(intent)
}

private suspend fun askGemini(
    apiKey: String,
    prompt: String
): String = withContext(Dispatchers.IO) {

    val url = URL(
        "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"
    )

    val body = JSONObject().apply {
        put(
            "contents",
            JSONArray().put(
                JSONObject().put(
                    "parts",
                    JSONArray().put(
                        JSONObject().put(
                            "text",
                            "You are JARVIS, a helpful male AI assistant. Reply naturally in Hindi or English according to the user's language. User says: $prompt"
                        )
                    )
                )
            )
        )
    }

    val connection = (url.openConnection() as HttpURLConnection).apply {
        requestMethod = "POST"
        connectTimeout = 20000
        readTimeout = 30000
        doOutput = true
        setRequestProperty("Content-Type", "application/json")
    }

    try {
        connection.outputStream.use {
            it.write(body.toString().toByteArray(Charsets.UTF_8))
        }

        val code = connection.responseCode
        val stream = if (code in 200..299) {
            connection.inputStream
        } else {
            connection.errorStream
        }

        val response = stream.bufferedReader().use { it.readText() }

        if (code !in 200..299) {
            throw Exception("Gemini API error $code: $response")
        }

        val json = JSONObject(response)
        json.getJSONArray("candidates")
            .getJSONObject(0)
            .getJSONObject("content")
            .getJSONArray("parts")
            .getJSONObject(0)
            .getString("text")
    } finally {
        connection.disconnect()
    }
}
