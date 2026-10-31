package com.stonic.ai

import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stonic.ai.data.ChatDatabase
import com.stonic.ai.data.ChatMessage
import com.stonic.ai.data.GeminiClient
import com.stonic.ai.settings.SecureStore
import com.stonic.ai.ui.screens.SettingsScreen
import com.stonic.ai.ui.theme.StonicTheme
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : ComponentActivity() {
    private var tts: TextToSpeech? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val store = SecureStore(this)
        val db = ChatDatabase(this)
        tts = TextToSpeech(this) { status -> if (status == TextToSpeech.SUCCESS) tts?.language = Locale.getDefault() }
        setContent { StonicTheme { StonicApp(store, db, onSpeak = { text ->
            tts?.setSpeechRate(store.voiceSpeed)
            val locale = when (store.language) { "Urdu" -> Locale("ur", "PK"); "English" -> Locale.US; else -> Locale("ur", "PK") }
            tts?.language = locale
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "stonic_reply")
        }, onStopSpeak = { tts?.stop() }) } }
    }
    override fun onDestroy() { tts?.stop(); tts?.shutdown(); super.onDestroy() }
}

@Composable
private fun StonicApp(store: SecureStore, db: ChatDatabase, onSpeak: (String) -> Unit, onStopSpeak: () -> Unit) {
    var selected by remember { mutableStateOf(0) }
    var assistantName by remember { mutableStateOf(store.assistantName) }
    var keyConfigured by remember { mutableStateOf(store.hasGeminiKey()) }
    Scaffold(bottomBar = {
        NavigationBar {
            NavigationBarItem(selected == 0, { selected = 0 }, icon = { Text("✦") }, label = { Text("Chat") })
            NavigationBarItem(selected == 1, { selected = 1 }, icon = { Text("⚙") }, label = { Text("Settings") })
        }
    }) { padding ->
        Box(Modifier.padding(padding)) {
            if (selected == 0) ChatScreen(store, db, assistantName, keyConfigured, onSpeak, onStopSpeak)
            else SettingsScreen(store, { assistantName = it }, { keyConfigured = it })
        }
    }
}

@Composable
private fun ChatScreen(store: SecureStore, db: ChatDatabase, assistantName: String, keyConfigured: Boolean, onSpeak: (String) -> Unit, onStopSpeak: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val client = remember { GeminiClient() }
    var messages by remember { mutableStateOf(db.all()) }
    var input by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf(if (keyConfigured) "Ready when you are, Boss." else "Settings mein Gemini API key add karo.") }
    val speechLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (!spoken.isNullOrBlank()) input = spoken
    }
    fun listen() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) { status = "Is phone par speech recognition available nahi hai."; return }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, when (store.language) { "Urdu" -> "ur-PK"; "English" -> "en-US"; else -> "ur-PK" })
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Boss, bolo...")
        }
        speechLauncher.launch(intent)
    }
    fun send() {
        val text = input.trim()
        if (text.isEmpty() || busy) return
        if (!store.hasGeminiKey()) { status = "Pehle Settings mein Gemini API key save karo."; return }
        input = ""; onStopSpeak()
        db.add("user", text); messages = db.all(); busy = true; status = "Stonic soch rahi hai..."
        scope.launch {
            try {
                val answer = client.reply(store.geminiApiKey, text, messages.dropLast(1), assistantName, store.language, store.personality)
                db.add("model", answer); messages = db.all(); status = "Online · Gemini AI"
                if (store.voiceEnabled) onSpeak(answer)
            } catch (e: Exception) {
                val error = e.message ?: "Unknown error"
                db.add("model", "Sorry Boss, request complete nahi hui: $error")
                messages = db.all(); status = "Connection / API issue"
            } finally { busy = false }
        }
    }
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(avatarResource(store.avatarIndex)), null, Modifier.size(52.dp).clip(CircleShape))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(assistantName, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                Text(status, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
            }
            TextButton(onClick = { db.clear(); messages = emptyList(); status = "Chat history cleared." }) { Text("Clear") }
        }
        if (messages.isEmpty()) {
            Column(Modifier.fillMaxSize().weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Image(painterResource(avatarResource(store.avatarIndex)), null, Modifier.size(170.dp).clip(RoundedCornerShape(28.dp)))
                Spacer(Modifier.height(16.dp))
                Text("Hello Boss!", fontSize = 27.sp, fontWeight = FontWeight.Bold)
                Text("Main Stonic hoon. Kya help chahiye?", color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (!keyConfigured) Text("Start karne ke liye Settings mein API key add karo.", modifier = Modifier.padding(20.dp), color = MaterialTheme.colorScheme.error)
            }
        } else {
            LazyColumn(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp), contentPadding = PaddingValues(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(messages, key = { it.id }) { message ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (message.role == "user") Arrangement.End else Arrangement.Start) {
                        Surface(color = if (message.role == "user") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(18.dp), modifier = Modifier.widthIn(max = 320.dp)) {
                            Text(message.text, Modifier.padding(13.dp), color = if (message.role == "user") MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
                if (busy) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            }
        }
        Row(Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(value = input, onValueChange = { input = it }, modifier = Modifier.weight(1f), placeholder = { Text("Message Stonic...") }, maxLines = 4, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send), keyboardActions = KeyboardActions(onSend = { send() }))
            IconButton(onClick = { listen() }) { Text("🎙") }
            IconButton(onClick = { send() }, enabled = !busy && input.isNotBlank()) { Text("➤") }
        }
    }
}

private fun avatarResource(index: Int): Int = when (index) {
    1 -> R.drawable.avatar_night
    2 -> R.drawable.avatar_companion
    3 -> R.drawable.avatar_greeting
    else -> R.drawable.avatar_default
}
