package com.example.turnoshospi

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DirectChatScreen(
    plantId: String,
    currentUserId: String,
    otherUserId: String,
    otherUserName: String,
    onBack: () -> Unit
) {
    val chatId = if (currentUserId < otherUserId) "${currentUserId}_${otherUserId}" else "${otherUserId}_${currentUserId}"

    val database = FirebaseDatabase.getInstance("https://turnoshospi-f4870-default-rtdb.firebaseio.com/")
    val messagesRef = database.getReference("plants/$plantId/direct_chats/$chatId/messages")

    val messages = remember { mutableStateListOf<DirectMessage>() }
    var textState by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    DisposableEffect(chatId) {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                messages.clear()
                snapshot.children.mapNotNull { it.getValue(DirectMessage::class.java) }
                    .forEach { messages.add(it) }
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        messagesRef.addValueEventListener(listener)
        onDispose { messagesRef.removeEventListener(listener) }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(otherUserName, color = Color.White, fontWeight = FontWeight.Bold)
                        Text("Chat privado", color = Color(0xAAFFFFFF), fontSize = 12.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F172A))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = textState,
                    onValueChange = { textState = it },
                    placeholder = { Text("Escribe un mensaje...", color = Color.Gray) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0x11FFFFFF),
                        unfocusedContainerColor = Color(0x11FFFFFF),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent
                    ),
                    maxLines = 4
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (textState.isNotBlank()) {
                            val key = messagesRef.push().key ?: return@IconButton
                            val msg = DirectMessage(
                                id = key,
                                senderId = currentUserId,
                                text = textState.trim()
                            )
                            messagesRef.child(key).setValue(msg)
                            textState = ""
                        }
                    },
                    modifier = Modifier.background(Color(0xFF54C7EC), androidx.compose.foundation.shape.CircleShape)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, "Enviar", tint = Color.Black)
                }
            }
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(messages) { msg ->
                MessageBubble(msg, isMine = msg.senderId == currentUserId)
            }
        }
    }
}

@Composable
fun MessageBubble(message: DirectMessage, isMine: Boolean) {
    val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
    ) {
        Card(
            shape = if (isMine) RoundedCornerShape(16.dp, 16.dp, 2.dp, 16.dp)
            else RoundedCornerShape(16.dp, 16.dp, 16.dp, 2.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isMine) Color(0xFF54C7EC) else Color(0xFF334155)
            ),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = message.text,
                    color = if (isMine) Color.Black else Color.White,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = dateFormat.format(Date(message.timestamp)),
                    color = if (isMine) Color(0x99000000) else Color(0x99FFFFFF),
                    fontSize = 10.sp,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}