package dev.nighttraders.lumo.launcher.messaging

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import dev.nighttraders.lumo.launcher.ui.theme.LumoLauncherTheme
import kotlinx.coroutines.launch

class MessagingActivity : ComponentActivity() {
    private val smsRepository by lazy { SmsRepository(applicationContext) }
    private var conversations by mutableStateOf<List<SmsConversation>>(emptyList())
    private var messages by mutableStateOf<List<SmsMessage>>(emptyList())
    private var currentThread by mutableStateOf<SmsConversation?>(null)
    private var hasPermission by mutableStateOf(false)
    private var showNewMessage by mutableStateOf(false)

    private val requestSmsPermission =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
            hasPermission = grants.values.all { it }
            if (hasPermission) {
                loadConversations()
            } else {
                Toast.makeText(this, "SMS permission required", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        configureSystemBars()

        hasPermission = hasSmsPermissions()
        if (hasPermission) {
            loadConversations()
        } else {
            requestSmsPermission.launch(SMS_PERMISSIONS)
        }

        setContent {
            LumoLauncherTheme {
                if (!hasPermission) {
                    MessagingScreen(
                        conversations = emptyList(),
                        messages = emptyList(),
                        currentThread = null,
                        onSelectConversation = {},
                        onBack = { finish() },
                        onSend = {},
                        onNewMessage = {},
                        onDeleteThread = {},
                    )
                } else {
                    MessagingScreen(
                        conversations = conversations,
                        messages = messages,
                        currentThread = currentThread,
                        onSelectConversation = { conversation ->
                            showNewMessage = false
                            currentThread = conversation
                            loadMessages(conversation.threadId)
                        },
                        onBack = {
                            if (currentThread != null) {
                                currentThread = null
                                messages = emptyList()
                                loadConversations()
                            } else {
                                finish()
                            }
                        },
                        onSend = { body ->
                            currentThread?.let { thread ->
                                sendMessage(thread.address, body, thread.threadId)
                            }
                        },
                        onNewMessage = { showNewMessage = true },
                        onDeleteThread = { threadId ->
                            lifecycleScope.launch {
                                smsRepository.deleteThread(threadId)
                                if (currentThread?.threadId == threadId) {
                                    currentThread = null
                                    messages = emptyList()
                                }
                                loadConversations()
                            }
                        },
                        showNewMessage = showNewMessage,
                        onSendNewMessage = { address, body ->
                            lifecycleScope.launch {
                                val result = smsRepository.sendSms(address, body)
                                if (result.isSuccess) {
                                    showNewMessage = false
                                    kotlinx.coroutines.delay(500)
                                    loadConversations()
                                } else {
                                    Toast.makeText(
                                        this@MessagingActivity,
                                        "Failed to send message",
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                }
                            }
                        },
                        onCancelNewMessage = { showNewMessage = false },
                        onShareMessage = { msg ->
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, msg.body)
                            }
                            startActivity(Intent.createChooser(shareIntent, "Share message"))
                        },
                        onDeleteMessage = { msg ->
                            lifecycleScope.launch {
                                smsRepository.deleteMessage(msg.id)
                                currentThread?.let { loadMessages(it.threadId) }
                            }
                        },
                        onSearchContacts = { query ->
                            smsRepository.searchContacts(query)
                        },
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (hasPermission) {
            loadConversations()
            currentThread?.let { loadMessages(it.threadId) }
        }
    }

    @Deprecated("Use OnBackPressedDispatcher")
    override fun onBackPressed() {
        when {
            showNewMessage -> showNewMessage = false
            currentThread != null -> {
                currentThread = null
                messages = emptyList()
                loadConversations()
            }
            else -> {
                @Suppress("DEPRECATION")
                super.onBackPressed()
            }
        }
    }

    private fun loadConversations() {
        lifecycleScope.launch {
            conversations = smsRepository.loadConversations()
        }
    }

    private fun loadMessages(threadId: Long) {
        lifecycleScope.launch {
            messages = smsRepository.loadMessages(threadId)
        }
    }

    private fun sendMessage(address: String, body: String, threadId: Long) {
        lifecycleScope.launch {
            val result = smsRepository.sendSms(address, body)
            if (result.isSuccess) {
                // Reload messages after a short delay to let the system process
                kotlinx.coroutines.delay(500)
                loadMessages(threadId)
            } else {
                Toast.makeText(
                    this@MessagingActivity,
                    "Failed to send message",
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
    }

    private fun hasSmsPermissions(): Boolean =
        SMS_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

    private fun configureSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    companion object {
        private val SMS_PERMISSIONS = arrayOf(
            Manifest.permission.READ_SMS,
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_CONTACTS,
        )
    }
}
