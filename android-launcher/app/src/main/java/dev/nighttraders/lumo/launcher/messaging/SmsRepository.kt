package dev.nighttraders.lumo.launcher.messaging

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract
import android.provider.Telephony
import android.telephony.SmsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class SmsConversation(
    val threadId: Long,
    val address: String,
    val contactName: String,
    val snippet: String,
    val timestamp: Long,
    val unreadCount: Int,
    val messageCount: Int,
)

data class SmsMessage(
    val id: Long,
    val threadId: Long,
    val address: String,
    val body: String,
    val timestamp: Long,
    val isOutgoing: Boolean,
    val isRead: Boolean,
)

class SmsRepository(private val context: Context) {
    private val contentResolver: ContentResolver = context.contentResolver
    private val contactCache = mutableMapOf<String, String>()

    suspend fun loadConversations(): List<SmsConversation> = withContext(Dispatchers.IO) {
        val conversations = mutableListOf<SmsConversation>()

        val cursor = contentResolver.query(
            Telephony.Threads.CONTENT_URI.buildUpon()
                .appendQueryParameter("simple", "true")
                .build(),
            null, null, null, "date DESC",
        )

        // Threads content URI may not work on all devices — fall back to raw SMS grouping
        if (cursor == null || cursor.count == 0) {
            cursor?.close()
            return@withContext loadConversationsFromSms()
        }

        cursor.use { c ->
            while (c.moveToNext()) {
                val threadId = c.getLongOrNull("_id") ?: continue
                val snippet = c.getStringOrNull("snippet").orEmpty()
                val date = c.getLongOrNull("date") ?: 0L
                val msgCount = c.getIntOrNull("message_count") ?: 0
                val unread = c.getIntOrNull("read")?.let { if (it == 0) 1 else 0 } ?: 0

                val address = getThreadAddress(threadId)
                if (address.isBlank()) continue

                conversations.add(
                    SmsConversation(
                        threadId = threadId,
                        address = address,
                        contactName = resolveContactName(address),
                        snippet = snippet,
                        timestamp = date,
                        unreadCount = unread,
                        messageCount = msgCount,
                    ),
                )
            }
        }

        conversations
    }

    private fun loadConversationsFromSms(): List<SmsConversation> {
        val threadMap = linkedMapOf<Long, SmsConversation>()

        val cursor = contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            arrayOf(
                Telephony.Sms.THREAD_ID,
                Telephony.Sms.ADDRESS,
                Telephony.Sms.BODY,
                Telephony.Sms.DATE,
                Telephony.Sms.READ,
                Telephony.Sms.TYPE,
            ),
            null, null, "${Telephony.Sms.DATE} DESC",
        ) ?: return emptyList()

        cursor.use { c ->
            while (c.moveToNext()) {
                val threadId = c.getLongOrNull(Telephony.Sms.THREAD_ID) ?: continue
                if (threadId in threadMap) {
                    // Update unread count
                    val existing = threadMap[threadId]!!
                    val isRead = c.getIntOrNull(Telephony.Sms.READ) ?: 1
                    if (isRead == 0) {
                        threadMap[threadId] = existing.copy(
                            unreadCount = existing.unreadCount + 1,
                            messageCount = existing.messageCount + 1,
                        )
                    } else {
                        threadMap[threadId] = existing.copy(messageCount = existing.messageCount + 1)
                    }
                    continue
                }

                val address = c.getStringOrNull(Telephony.Sms.ADDRESS).orEmpty()
                if (address.isBlank()) continue
                val body = c.getStringOrNull(Telephony.Sms.BODY).orEmpty()
                val date = c.getLongOrNull(Telephony.Sms.DATE) ?: 0L
                val isRead = c.getIntOrNull(Telephony.Sms.READ) ?: 1

                threadMap[threadId] = SmsConversation(
                    threadId = threadId,
                    address = address,
                    contactName = resolveContactName(address),
                    snippet = body,
                    timestamp = date,
                    unreadCount = if (isRead == 0) 1 else 0,
                    messageCount = 1,
                )
            }
        }

        return threadMap.values.toList()
    }

    suspend fun loadMessages(threadId: Long): List<SmsMessage> = withContext(Dispatchers.IO) {
        val messages = mutableListOf<SmsMessage>()

        val cursor = contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            arrayOf(
                Telephony.Sms._ID,
                Telephony.Sms.THREAD_ID,
                Telephony.Sms.ADDRESS,
                Telephony.Sms.BODY,
                Telephony.Sms.DATE,
                Telephony.Sms.TYPE,
                Telephony.Sms.READ,
            ),
            "${Telephony.Sms.THREAD_ID} = ?",
            arrayOf(threadId.toString()),
            "${Telephony.Sms.DATE} ASC",
        ) ?: return@withContext emptyList()

        cursor.use { c ->
            while (c.moveToNext()) {
                val id = c.getLongOrNull(Telephony.Sms._ID) ?: continue
                val tid = c.getLongOrNull(Telephony.Sms.THREAD_ID) ?: threadId
                val address = c.getStringOrNull(Telephony.Sms.ADDRESS).orEmpty()
                val body = c.getStringOrNull(Telephony.Sms.BODY).orEmpty()
                val date = c.getLongOrNull(Telephony.Sms.DATE) ?: 0L
                val type = c.getIntOrNull(Telephony.Sms.TYPE) ?: Telephony.Sms.MESSAGE_TYPE_INBOX
                val read = c.getIntOrNull(Telephony.Sms.READ) ?: 1

                messages.add(
                    SmsMessage(
                        id = id,
                        threadId = tid,
                        address = address,
                        body = body,
                        timestamp = date,
                        isOutgoing = type == Telephony.Sms.MESSAGE_TYPE_SENT ||
                            type == Telephony.Sms.MESSAGE_TYPE_OUTBOX,
                        isRead = read == 1,
                    ),
                )
            }
        }

        // Mark thread as read
        markThreadRead(threadId)

        messages
    }

    suspend fun sendSms(address: String, body: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val smsManager = context.getSystemService(SmsManager::class.java)
                ?: throw IllegalStateException("SMS not available")
            val parts = smsManager.divideMessage(body)
            if (parts.size == 1) {
                smsManager.sendTextMessage(address, null, body, null, null)
            } else {
                smsManager.sendMultipartTextMessage(address, null, parts, null, null)
            }
        }
    }

    fun resolveContactName(address: String): String {
        if (address.isBlank()) return address
        contactCache[address]?.let { return it }

        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(address),
        )

        val name = runCatching {
            contentResolver.query(
                uri,
                arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
                null, null, null,
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getString(0)
                } else null
            }
        }.getOrNull()

        val result = name ?: address
        contactCache[address] = result
        return result
    }

    data class ContactResult(
        val name: String,
        val phoneNumber: String,
    )

    suspend fun searchContacts(query: String): List<ContactResult> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val results = mutableListOf<ContactResult>()
        val seen = mutableSetOf<String>()

        runCatching {
            val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
            )
            val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ? OR ${ContactsContract.CommonDataKinds.Phone.NUMBER} LIKE ?"
            val selectionArgs = arrayOf("%$query%", "%$query%")
            val sortOrder = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"

            contentResolver.query(uri, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
                val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                while (cursor.moveToNext() && results.size < 8) {
                    val name = if (nameIdx >= 0) cursor.getString(nameIdx) else null
                    val number = if (numberIdx >= 0) cursor.getString(numberIdx) else null
                    if (!name.isNullOrBlank() && !number.isNullOrBlank()) {
                        val normalized = number.replace(Regex("[^0-9+]"), "")
                        if (seen.add(normalized)) {
                            results.add(ContactResult(name, number))
                        }
                    }
                }
            }
        }

        results
    }

    suspend fun deleteMessage(messageId: Long) = withContext(Dispatchers.IO) {
        runCatching {
            contentResolver.delete(
                Telephony.Sms.CONTENT_URI,
                "${Telephony.Sms._ID} = ?",
                arrayOf(messageId.toString()),
            )
        }
    }

    suspend fun deleteThread(threadId: Long) = withContext(Dispatchers.IO) {
        runCatching {
            contentResolver.delete(
                Telephony.Sms.CONTENT_URI,
                "${Telephony.Sms.THREAD_ID} = ?",
                arrayOf(threadId.toString()),
            )
        }
    }

    private fun markThreadRead(threadId: Long) {
        runCatching {
            val values = android.content.ContentValues().apply {
                put(Telephony.Sms.READ, 1)
            }
            contentResolver.update(
                Telephony.Sms.CONTENT_URI,
                values,
                "${Telephony.Sms.THREAD_ID} = ? AND ${Telephony.Sms.READ} = 0",
                arrayOf(threadId.toString()),
            )
        }
    }

    private fun getThreadAddress(threadId: Long): String {
        val cursor = contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            arrayOf(Telephony.Sms.ADDRESS),
            "${Telephony.Sms.THREAD_ID} = ?",
            arrayOf(threadId.toString()),
            "${Telephony.Sms.DATE} DESC LIMIT 1",
        ) ?: return ""

        return cursor.use { c ->
            if (c.moveToFirst()) c.getStringOrNull(Telephony.Sms.ADDRESS).orEmpty()
            else ""
        }
    }

    companion object {
        fun formatTimestamp(timestamp: Long): String {
            if (timestamp == 0L) return ""
            val date = Date(timestamp)
            val now = Calendar.getInstance()
            val then = Calendar.getInstance().apply { time = date }

            return when {
                now.get(Calendar.DATE) == then.get(Calendar.DATE) &&
                    now.get(Calendar.YEAR) == then.get(Calendar.YEAR) -> {
                    SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
                }
                now.get(Calendar.YEAR) == then.get(Calendar.YEAR) -> {
                    SimpleDateFormat("HH:mm - MMM d", Locale.getDefault()).format(date)
                }
                else -> {
                    SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(date)
                }
            }
        }
    }
}

// Extension helpers for safe cursor access
private fun Cursor.getLongOrNull(column: String): Long? {
    val idx = getColumnIndex(column)
    return if (idx >= 0 && !isNull(idx)) getLong(idx) else null
}

private fun Cursor.getStringOrNull(column: String): String? {
    val idx = getColumnIndex(column)
    return if (idx >= 0 && !isNull(idx)) getString(idx) else null
}

private fun Cursor.getIntOrNull(column: String): Int? {
    val idx = getColumnIndex(column)
    return if (idx >= 0 && !isNull(idx)) getInt(idx) else null
}

private fun Cursor.getLongOrNull(column: Int): Long? =
    if (column >= 0 && !isNull(column)) getLong(column) else null

@Suppress("unused")
private fun Cursor.getStringOrNull(column: Int): String? =
    if (column >= 0 && !isNull(column)) getString(column) else null

private fun Cursor.getIntOrNull(column: Int): Int? =
    if (column >= 0 && !isNull(column)) getInt(column) else null
