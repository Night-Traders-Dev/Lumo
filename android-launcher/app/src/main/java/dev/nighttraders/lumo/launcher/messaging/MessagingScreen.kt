package dev.nighttraders.lumo.launcher.messaging

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Ubuntu Touch palette
private val BgDark = Color(0xFF1A0816)
private val BgGradientTop = Color(0xFF2C001E)
private val BgGradientMid = Color(0xFF1A0816)
private val BgGradientBottom = Color(0xFF0C0A10)
private val UbuntuOrange = Color(0xFFE95420)
private val TextPrimary = Color(0xFFEEEEEE)
private val TextSecondary = Color(0xFF999999)
private val TextMuted = Color(0xFF777777)
private val DividerColor = Color(0xFF2A1F2E)
private val BubbleOutgoing = Color(0xFF3C2847)
private val BubbleIncoming = Color(0xFF1E1A24)
private val InputBg = Color(0xFF2A1F2E)
private val UnreadBadge = UbuntuOrange

@Composable
fun MessagingScreen(
    conversations: List<SmsConversation>,
    messages: List<SmsMessage>,
    currentThread: SmsConversation?,
    onSelectConversation: (SmsConversation) -> Unit,
    onBack: () -> Unit,
    onSend: (String) -> Unit,
    onNewMessage: () -> Unit,
    onDeleteThread: (Long) -> Unit,
    showNewMessage: Boolean = false,
    onSendNewMessage: (address: String, body: String) -> Unit = { _, _ -> },
    onCancelNewMessage: () -> Unit = {},
    onShareMessage: (SmsMessage) -> Unit = {},
    onDeleteMessage: (SmsMessage) -> Unit = {},
    onSearchContacts: suspend (String) -> List<SmsRepository.ContactResult> = { emptyList() },
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(BgGradientTop, BgGradientMid, BgGradientBottom),
                ),
            )
            .windowInsetsPadding(WindowInsets.systemBars),
    ) {
        when {
            showNewMessage -> {
                NewMessageScreen(
                    onBack = onCancelNewMessage,
                    onSend = onSendNewMessage,
                    onSearchContacts = onSearchContacts,
                )
            }
            currentThread != null -> {
                ThreadScreen(
                    conversation = currentThread,
                    messages = messages,
                    onBack = onBack,
                    onSend = onSend,
                    onShareMessage = onShareMessage,
                    onDeleteMessage = onDeleteMessage,
                )
            }
            else -> {
                ConversationListScreen(
                    conversations = conversations,
                    onSelect = onSelectConversation,
                    onNewMessage = onNewMessage,
                )
            }
        }
    }
}

// ── Conversation List ────────────────────────────────────────────────────────

@Composable
private fun ConversationListScreen(
    conversations: List<SmsConversation>,
    onSelect: (SmsConversation) -> Unit,
    onNewMessage: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Messages",
                fontSize = 28.sp,
                fontWeight = FontWeight.Light,
                color = TextPrimary,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onNewMessage) {
                Text(
                    text = "+",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Light,
                    color = UbuntuOrange,
                )
            }
        }

        if (conversations.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "No messages",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextMuted,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tap + to start a conversation",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted,
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp),
            ) {
                items(conversations, key = { it.threadId }) { conversation ->
                    ConversationRow(
                        conversation = conversation,
                        onClick = { onSelect(conversation) },
                    )
                    HorizontalDivider(
                        color = DividerColor,
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(start = 80.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ConversationRow(
    conversation: SmsConversation,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(avatarColor(conversation.address)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = conversation.contactName.firstOrNull()?.uppercase() ?: "#",
                fontSize = 22.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White,
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = conversation.contactName,
                    fontWeight = if (conversation.unreadCount > 0) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 16.sp,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = SmsRepository.formatTimestamp(conversation.timestamp),
                    fontSize = 12.sp,
                    color = if (conversation.unreadCount > 0) UbuntuOrange else TextSecondary,
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = conversation.snippet,
                    fontSize = 14.sp,
                    color = TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (conversation.unreadCount > 0) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(UnreadBadge),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = conversation.unreadCount.toString(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                    }
                }
            }
        }
    }
}

// ── Thread View ──────────────────────────────────────────────────────────────

@Composable
private fun ThreadScreen(
    conversation: SmsConversation,
    messages: List<SmsMessage>,
    onBack: () -> Unit,
    onSend: (String) -> Unit,
    onShareMessage: (SmsMessage) -> Unit = {},
    onDeleteMessage: (SmsMessage) -> Unit = {},
) {
    val listState = rememberLazyListState()
    var inputText by rememberSaveable { mutableStateOf("") }
    var selectedMessage by remember { mutableStateOf<SmsMessage?>(null) }
    var replyToMessage by remember { mutableStateOf<SmsMessage?>(null) }

    // Auto-scroll to bottom when messages change
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
    ) {
        // Thread header
        Surface(
            color = Color(0xFF2C001E),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Text(
                        text = "\u2190",
                        fontSize = 24.sp,
                        color = TextPrimary,
                    )
                }

                // Avatar
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(avatarColor(conversation.address)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = conversation.contactName.firstOrNull()?.uppercase() ?: "#",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White,
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = conversation.contactName,
                        fontWeight = FontWeight.Medium,
                        fontSize = 17.sp,
                        color = TextPrimary,
                    )
                    if (conversation.contactName != conversation.address) {
                        Text(
                            text = conversation.address,
                            fontSize = 12.sp,
                            color = TextSecondary,
                        )
                    }
                }
            }
        }

        // Messages
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(messages, key = { it.id }) { message ->
                MessageBubble(
                    message = message,
                    isSelected = selectedMessage?.id == message.id,
                    onLongPress = { selectedMessage = message },
                    onTap = { if (selectedMessage != null) selectedMessage = null },
                )
            }
        }

        // Message action bar (appears when a message is long-pressed)
        AnimatedVisibility(visible = selectedMessage != null) {
            selectedMessage?.let { msg ->
                MessageActionBar(
                    onReply = {
                        replyToMessage = msg
                        inputText = ""
                        selectedMessage = null
                    },
                    onShare = {
                        onShareMessage(msg)
                        selectedMessage = null
                    },
                    onDelete = {
                        onDeleteMessage(msg)
                        selectedMessage = null
                    },
                    onDismiss = { selectedMessage = null },
                )
            }
        }

        // Reply indicator
        if (replyToMessage != null) {
            Surface(color = Color(0xFF2A1F2E)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(32.dp)
                            .background(UbuntuOrange),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Reply",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = UbuntuOrange,
                        )
                        Text(
                            text = replyToMessage!!.body,
                            fontSize = 12.sp,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text(
                        text = "\u2715",
                        fontSize = 18.sp,
                        color = TextMuted,
                        modifier = Modifier
                            .clickable { replyToMessage = null }
                            .padding(8.dp),
                    )
                }
            }
        }

        // Input bar
        Surface(
            color = Color(0xDD0E0A10),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BasicTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(InputBg)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    textStyle = TextStyle(
                        color = TextPrimary,
                        fontSize = 15.sp,
                    ),
                    cursorBrush = SolidColor(UbuntuOrange),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (inputText.isNotBlank()) {
                                onSend(inputText.trim())
                                inputText = ""
                            }
                        },
                    ),
                    decorationBox = { innerTextField ->
                        Box {
                            if (inputText.isEmpty()) {
                                Text(
                                    text = "Type a message...",
                                    color = TextMuted,
                                    fontSize = 15.sp,
                                )
                            }
                            innerTextField()
                        }
                    },
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Send button
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (inputText.isNotBlank()) UbuntuOrange else Color(0xFF3C2847))
                        .clickable(enabled = inputText.isNotBlank()) {
                            onSend(inputText.trim())
                            inputText = ""
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "\u2191",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                }
            }
        }

        // Orange accent bar at the very bottom (Ubuntu Touch style)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(UbuntuOrange),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageBubble(
    message: SmsMessage,
    isSelected: Boolean = false,
    onLongPress: () -> Unit = {},
    onTap: () -> Unit = {},
) {
    val isOutgoing = message.isOutgoing
    val alignment = if (isOutgoing) Alignment.CenterEnd else Alignment.CenterStart
    val bubbleColor = if (isSelected) {
        Color(0xFF5E2750)
    } else if (isOutgoing) BubbleOutgoing else BubbleIncoming
    val shape = if (isOutgoing) {
        RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp)
    } else {
        RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = if (isOutgoing) 48.dp else 0.dp,
                end = if (isOutgoing) 0.dp else 48.dp,
            ),
        contentAlignment = alignment,
    ) {
        Box(
            modifier = Modifier
                .clip(shape)
                .combinedClickable(
                    onClick = onTap,
                    onLongClick = onLongPress,
                ),
        ) {
            Surface(
                color = bubbleColor,
                shape = shape,
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    Text(
                        text = message.body,
                        fontSize = 15.sp,
                        color = TextPrimary,
                        lineHeight = 20.sp,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = SmsRepository.formatTimestamp(message.timestamp),
                        fontSize = 11.sp,
                        color = TextMuted,
                        modifier = Modifier.align(Alignment.End),
                    )
                }
            }
        }
    }
}

// ── Message Action Bar ───────────────────────────────────────────────────────

@Composable
private fun MessageActionBar(
    onReply: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    Surface(color = Color(0xEE1A0816)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            ActionButton(label = "Reply", icon = "\u21A9", onClick = onReply)
            ActionButton(label = "Share", icon = "\u2197", onClick = onShare)
            ActionButton(label = "Delete", icon = "\u2717", color = Color(0xFFED3146), onClick = onDelete)
            ActionButton(label = "Cancel", icon = "\u2715", onClick = onDismiss)
        }
    }
}

@Composable
private fun ActionButton(
    label: String,
    icon: String,
    color: Color = TextPrimary,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = icon,
            fontSize = 20.sp,
            color = color,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = TextSecondary,
        )
    }
}

// ── New Message Screen ───────────────────────────────────────────────────────

@Composable
private fun NewMessageScreen(
    onBack: () -> Unit,
    onSend: (address: String, body: String) -> Unit,
    onSearchContacts: suspend (String) -> List<SmsRepository.ContactResult> = { emptyList() },
) {
    var recipientText by rememberSaveable { mutableStateOf("") }
    var messageText by rememberSaveable { mutableStateOf("") }
    var contactSuggestions by remember { mutableStateOf<List<SmsRepository.ContactResult>>(emptyList()) }
    var showSuggestions by remember { mutableStateOf(false) }

    // Search contacts as user types
    LaunchedEffect(recipientText) {
        if (recipientText.length >= 2) {
            contactSuggestions = onSearchContacts(recipientText)
            showSuggestions = contactSuggestions.isNotEmpty()
        } else {
            contactSuggestions = emptyList()
            showSuggestions = false
        }
    }

    Column(modifier = Modifier.fillMaxSize().imePadding()) {
        // Header
        Surface(color = Color(0xFF2C001E)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Text(
                        text = "\u2190",
                        fontSize = 24.sp,
                        color = TextPrimary,
                    )
                }
                Text(
                    text = "New message",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Light,
                    color = TextPrimary,
                )
            }
        }

        // Recipient field
        Surface(color = Color(0xFF1E1A24)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "To:",
                    fontSize = 15.sp,
                    color = TextSecondary,
                )
                Spacer(modifier = Modifier.width(12.dp))
                BasicTextField(
                    value = recipientText,
                    onValueChange = { recipientText = it },
                    modifier = Modifier.weight(1f),
                    textStyle = TextStyle(color = TextPrimary, fontSize = 15.sp),
                    cursorBrush = SolidColor(UbuntuOrange),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    singleLine = true,
                    decorationBox = { innerTextField ->
                        Box {
                            if (recipientText.isEmpty()) {
                                Text(
                                    text = "Name or phone number",
                                    color = TextMuted,
                                    fontSize = 15.sp,
                                )
                            }
                            innerTextField()
                        }
                    },
                )
            }
        }

        // Contact suggestions dropdown
        AnimatedVisibility(visible = showSuggestions) {
            Surface(color = Color(0xFF1E1A24)) {
                Column {
                    contactSuggestions.forEach { contact ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    recipientText = contact.phoneNumber
                                    showSuggestions = false
                                }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            // Avatar
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(avatarColor(contact.phoneNumber)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = contact.name.firstOrNull()?.uppercase() ?: "#",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White,
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = contact.name,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextPrimary,
                                )
                                Text(
                                    text = contact.phoneNumber,
                                    fontSize = 13.sp,
                                    color = TextSecondary,
                                )
                            }
                        }
                        HorizontalDivider(color = DividerColor, thickness = 0.5.dp)
                    }
                }
            }
        }

        HorizontalDivider(color = DividerColor, thickness = 0.5.dp)

        // Message area
        Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(16.dp)) {
            BasicTextField(
                value = messageText,
                onValueChange = { messageText = it },
                modifier = Modifier.fillMaxSize(),
                textStyle = TextStyle(color = TextPrimary, fontSize = 15.sp, lineHeight = 22.sp),
                cursorBrush = SolidColor(UbuntuOrange),
                decorationBox = { innerTextField ->
                    Box {
                        if (messageText.isEmpty()) {
                            Text(
                                text = "Type your message...",
                                color = TextMuted,
                                fontSize = 15.sp,
                            )
                        }
                        innerTextField()
                    }
                },
            )
        }

        // Send bar
        Surface(color = Color(0xDD0E0A10)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                val canSend = recipientText.isNotBlank() && messageText.isNotBlank()
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (canSend) UbuntuOrange else Color(0xFF3C2847))
                        .clickable(enabled = canSend) {
                            onSend(recipientText.trim(), messageText.trim())
                        }
                        .padding(horizontal = 24.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Send",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White,
                    )
                }
            }
        }

        // Orange accent bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(UbuntuOrange),
        )
    }
}

// ── Helpers ──────────────────────────────────────────────────────────────────

private fun avatarColor(address: String): Color {
    val colors = listOf(
        Color(0xFFE95420), // Ubuntu orange
        Color(0xFF77216F), // Ubuntu aubergine
        Color(0xFF5E2750), // Purple
        Color(0xFF2C001E), // Dark aubergine
        Color(0xFFAA3926), // Warm red
        Color(0xFF38B44A), // Green
        Color(0xFF19B6EE), // Blue
        Color(0xFFEF7D00), // Amber
    )
    return colors[address.hashCode().and(0x7FFFFFFF) % colors.size]
}
