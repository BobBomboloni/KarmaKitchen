package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.api.ChatMessage
import com.example.api.chatWithGemini
import com.example.ui.theme.OnPrimaryGreen
import com.example.ui.theme.OutlineColor
import com.example.ui.theme.PrimaryGreen
import com.example.ui.theme.SurfaceVariantColor
import com.example.ui.theme.TextPrimary
import kotlinx.coroutines.launch

@Composable
fun DonationChatbot() {
    val scope = rememberCoroutineScope()
    var messages by remember { mutableStateOf(listOf<ChatMessage>()) }
    var inputText by remember { mutableStateOf("") }
    var isTyping by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
                Box(
                    modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(PrimaryGreen),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.SupportAgent, contentDescription = null, tint = OnPrimaryGreen, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        "Food safety help",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        "Ask about food safety, storage, or disposal",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Divider(color = OutlineColor)
            
            // Messages List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                reverseLayout = false
            ) {
                if (messages.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
                            Text("Have a question about donating food? Ask me anything.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                } else {
                    items(messages) { msg ->
                        ChatBubble(msg)
                    }
                }
                if (isTyping) {
                    item {
                        Text("Assistant is typing...", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(8.dp))
                    }
                }
            }
            
            // Input Field
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("Ask a question...") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryGreen,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = {
                        if (inputText.isNotBlank() && !isTyping) {
                            val userMsg = inputText
                            inputText = ""
                            messages = messages + ChatMessage(isUser = true, text = userMsg)
                            isTyping = true
                            scope.launch {
                                val reply = chatWithGemini(messages.dropLast(1), userMsg)
                                messages = messages + ChatMessage(isUser = false, text = reply)
                                isTyping = false
                            }
                        }
                    })
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (inputText.isNotBlank() && !isTyping) {
                            val userMsg = inputText
                            inputText = ""
                            messages = messages + ChatMessage(isUser = true, text = userMsg)
                            isTyping = true
                            scope.launch {
                                val reply = chatWithGemini(messages.dropLast(1), userMsg)
                                messages = messages + ChatMessage(isUser = false, text = reply)
                                isTyping = false
                            }
                        }
                    },
                    modifier = Modifier.background(PrimaryGreen, CircleShape)
                ) {
                    Icon(Icons.Filled.Send, contentDescription = "Send", tint = OnPrimaryGreen)
                }
            }
        }
    }
}

@Composable
fun ChatBubble(msg: ChatMessage) {
    val alignment = if (msg.isUser) Alignment.CenterEnd else Alignment.CenterStart
    val bgColor = if (msg.isUser) PrimaryGreen else SurfaceVariantColor
    val textColor = if (msg.isUser) OnPrimaryGreen else TextPrimary
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentAlignment = alignment
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (msg.isUser) 16.dp else 2.dp,
                    bottomEnd = if (msg.isUser) 2.dp else 16.dp
                ))
                .background(bgColor)
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .widthIn(max = 260.dp)
        ) {
            Text(
                text = msg.text,
                color = textColor,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
