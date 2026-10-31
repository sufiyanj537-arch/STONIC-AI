package com.stonic.ai.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stonic.ai.settings.SecureStore

@Composable
fun SettingsScreen(
    store: SecureStore,
    onNameSaved: (String) -> Unit,
    onKeyChanged: (Boolean) -> Unit
) {
    var nameInput by remember { mutableStateOf(store.assistantName) }
    var keyInput by remember { mutableStateOf("") }
    var showKey by remember { mutableStateOf(false) }
    var keySaved by remember { mutableStateOf(store.hasGeminiKey()) }
    var message by remember { mutableStateOf("") }
    var language by remember { mutableStateOf(store.language) }
    var avatarIndex by remember { mutableStateOf(store.avatarIndex) }
    var personality by remember { mutableStateOf(store.personality) }
    var voiceEnabled by remember { mutableStateOf(store.voiceEnabled) }
    var voiceSpeed by remember { mutableStateOf(store.voiceSpeed) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Settings", fontSize = 28.sp, fontWeight = FontWeight.Bold)

        Text("Assistant name", fontWeight = FontWeight.SemiBold)
        OutlinedTextField(
            value = nameInput,
            onValueChange = { nameInput = it },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Button(onClick = {
            store.assistantName = nameInput
            nameInput = store.assistantName
            onNameSaved(store.assistantName)
            message = "Name saved."
        }) { Text("Save name") }

        Spacer(Modifier.height(12.dp))
        Text("Gemini API key", fontWeight = FontWeight.SemiBold)
        Text(
            if (keySaved) "A key is saved (encrypted). Enter a new one to replace it."
            else "No key saved yet.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedTextField(
            value = keyInput,
            onValueChange = { keyInput = it },
            label = { Text("Paste API key") },
            singleLine = true,
            visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                if (keyInput.isBlank()) {
                    message = "Key box is empty."
                } else {
                    store.geminiApiKey = keyInput
                    keyInput = ""
                    keySaved = true
                    onKeyChanged(true)
                    message = "API key saved securely."
                }
            }) { Text("Save key") }
            OutlinedButton(onClick = {
                store.clearGeminiKey()
                keySaved = false
                onKeyChanged(false)
                message = "API key deleted."
            }) { Text("Delete key") }
            TextButton(onClick = { showKey = !showKey }) {
                Text(if (showKey) "Hide" else "Show")
            }
        }

        Text("Assistant avatar", fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("Default", "Night", "Companion", "Greeting").forEachIndexed { index, label ->
                OutlinedButton(onClick = { avatarIndex = index; store.avatarIndex = index }) {
                    Text(if (avatarIndex == index) "✓ $label" else label)
                }
            }
        }
        Text("Language", fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Roman Urdu", "English", "Urdu").forEach { option ->
                OutlinedButton(onClick = { language = option; store.language = option }) { Text(if (language == option) "✓ $option" else option) }
            }
        }
        Text("Personality", fontWeight = FontWeight.SemiBold)
        OutlinedTextField(value = personality, onValueChange = { personality = it; store.personality = it }, modifier = Modifier.fillMaxWidth(), minLines = 2)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Speak replies aloud")
            Switch(checked = voiceEnabled, onCheckedChange = { voiceEnabled = it; store.voiceEnabled = it })
        }
        Text("Voice speed: ${String.format(java.util.Locale.US, "%.1f", voiceSpeed)}x")
        Slider(value = voiceSpeed, onValueChange = { voiceSpeed = it; store.voiceSpeed = it }, valueRange = 0.7f..1.3f)
        if (message.isNotEmpty()) Text(message, color = MaterialTheme.colorScheme.secondary)
    }
}
