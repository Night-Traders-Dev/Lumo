package dev.nighttraders.lumo.launcher.messaging

import android.content.ContentResolver
import android.content.ContentValues
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
    val isMms: Boolean = false,
    val imageUri: Uri? = null,
    val mmsSubject: String? = null,
)

class SmsRepository(private val context: Context) {
    private val contentResolver: ContentResolver = context.contentResolver
    private val contactCache = mutableMapOf<String, String>()

    // ── Conversations ────────────────────────────────────────────────────────

    suspend fun loadConversations(): List<SmsConversation> = withContext(Dispatchers.IO) {
        val conversations = mutableListOf<SmsConversation>()

        val cursor = contentResolver.query(
            Telephony.Threads.CONTENT_URI.buildUpon()
                .appendQueryParameter("simple", "true")
                .build(),
            null, null, null, "date DESC",
        )

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

    // ── Messages (SMS + MMS merged) ─────────────────────────────────────────

    suspend fun loadMessages(threadId: Long): List<SmsMessage> = withContext(Dispatchers.IO) {
        val smsMessages = loadSmsMessages(threadId)
        val mmsMessages = loadMmsMessages(threadId)

        val merged = (smsMessages + mmsMessages).sortedBy { it.timestamp }

        markThreadRead(threadId)

        merged
    }

    private fun loadSmsMessages(threadId: Long): List<SmsMessage> {
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
        ) ?: return emptyList()

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

        return messages
    }

    @Suppress("DEPRECATION")
    private fun loadMmsMessages(threadId: Long): List<SmsMessage> {
        val messages = mutableListOf<SmsMessage>()

        val cursor = contentResolver.query(
            Telephony.Mms.CONTENT_URI,
            arrayOf(
                Telephony.Mms._ID,
                Telephony.Mms.THREAD_ID,
                Telephony.Mms.DATE,
                Telephony.Mms.READ,
                Telephony.Mms.MESSAGE_BOX,
                Telephony.Mms.SUBJECT,
            ),
            "${Telephony.Mms.THREAD_ID} = ?",
            arrayOf(threadId.toString()),
            "${Telephony.Mms.DATE} ASC",
        ) ?: return emptyList()

        cursor.use { c ->
            while (c.moveToNext()) {
                val mmsId = c.getLongOrNull(Telephony.Mms._ID) ?: continue
                val tid = c.getLongOrNull(Telephony.Mms.THREAD_ID) ?: threadId
                // MMS date is in SECONDS, SMS is in MILLISECONDS
                val dateSec = c.getLongOrNull(Telephony.Mms.DATE) ?: 0L
                val dateMs = dateSec * 1000L
                val read = c.getIntOrNull(Telephony.Mms.READ) ?: 1
                val msgBox = c.getIntOrNull(Telephony.Mms.MESSAGE_BOX) ?: Telephony.Mms.MESSAGE_BOX_INBOX
                val subject = c.getStringOrNull(Telephony.Mms.SUBJECT)

                val isOutgoing = msgBox == Telephony.Mms.MESSAGE_BOX_SENT ||
                    msgBox == Telephony.Mms.MESSAGE_BOX_OUTBOX

                val address = getMmsAddress(mmsId, isOutgoing)
                val textBody = getMmsTextBody(mmsId)
                val imageUri = getMmsImageUri(mmsId)

                val body = when {
                    textBody.isNotBlank() && imageUri != null -> textBody
                    textBody.isNotBlank() -> textBody
                    subject != null -> subject
                    imageUri != null -> ""
                    else -> "(MMS)"
                }

                messages.add(
                    SmsMessage(
                        id = mmsId,
                        threadId = tid,
                        address = address,
                        body = body,
                        timestamp = dateMs,
                        isOutgoing = isOutgoing,
                        isRead = read == 1,
                        isMms = true,
                        imageUri = imageUri,
                        mmsSubject = subject,
                    ),
                )
            }
        }

        return messages
    }

    private fun getMmsTextBody(mmsId: Long): String {
        val partUri = Uri.parse("content://mms/$mmsId/part")
        val cursor = contentResolver.query(
            partUri,
            arrayOf("_id", "ct", "text"),
            "ct = ?",
            arrayOf("text/plain"),
            null,
        ) ?: return ""

        return cursor.use { c ->
            if (c.moveToFirst()) {
                c.getStringOrNull("text").orEmpty()
            } else ""
        }
    }

    private fun getMmsImageUri(mmsId: Long): Uri? {
        val partUri = Uri.parse("content://mms/$mmsId/part")
        val cursor = contentResolver.query(
            partUri,
            arrayOf("_id", "ct"),
            "ct LIKE ?",
            arrayOf("image/%"),
            null,
        ) ?: return null

        return cursor.use { c ->
            if (c.moveToFirst()) {
                val partId = c.getLongOrNull("_id") ?: return@use null
                Uri.parse("content://mms/part/$partId")
            } else null
        }
    }

    private fun getMmsAddress(mmsId: Long, isOutgoing: Boolean): String {
        val addrUri = Uri.parse("content://mms/$mmsId/addr")
        // type 137 = FROM, type 151 = TO
        val wantedType = if (isOutgoing) "151" else "137"

        val cursor = contentResolver.query(
            addrUri,
            arrayOf("address", "type"),
            "type = ?",
            arrayOf(wantedType),
            null,
        ) ?: return ""

        return cursor.use { c ->
            if (c.moveToFirst()) {
                val addr = c.getStringOrNull("address").orEmpty()
                // Filter out "insert-address-token" placeholder
                if (addr == "insert-address-token" || addr.isBlank()) "" else addr
            } else ""
        }
    }

    // ── Send SMS ─────────────────────────────────────────────────────────────

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

    // ── Send MMS ─────────────────────────────────────────────────────────────

    suspend fun sendMms(
        address: String,
        body: String,
        imageUri: Uri,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            // Build MMS PDU
            val pduFile = java.io.File(context.cacheDir, "mms_send_${System.currentTimeMillis()}.dat")
            val pdu = buildMmsPdu(address, body, imageUri)
            pduFile.writeBytes(pdu)

            val contentUri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.mms_provider",
                pduFile,
            )

            val smsManager = context.getSystemService(SmsManager::class.java)
                ?: throw IllegalStateException("MMS not available")

            smsManager.sendMultimediaMessage(
                context,
                contentUri,
                null,
                null,
                null,
            )
        }
    }

    private fun buildMmsPdu(address: String, body: String, imageUri: Uri): ByteArray {
        // Minimal M-Send.req PDU
        val out = java.io.ByteArrayOutputStream()

        // Message type: M-Send.req (0x80)
        out.write(0x8C); out.write(0x80)
        // Transaction ID
        val txnId = "T${System.currentTimeMillis()}"
        out.write(0x98); out.writeString(txnId)
        // MMS Version 1.0
        out.write(0x8D); out.write(0x90)
        // To
        out.write(0x97); out.writeString("$address/TYPE=PLMN")
        // Content-Type: multipart/mixed
        out.write(0x84)
        val ctBytes = "application/vnd.wap.multipart.mixed".toByteArray()
        out.writeUintvar(ctBytes.size)
        out.write(ctBytes)

        // Parts count
        out.writeUintvar(if (body.isNotBlank()) 2 else 1)

        // Text part (if body is present)
        if (body.isNotBlank()) {
            val textBytes = body.toByteArray(Charsets.UTF_8)
            val textCt = "text/plain; charset=utf-8"
            // Header length
            out.writeUintvar(textCt.length + 1)
            // Data length
            out.writeUintvar(textBytes.size)
            // Content-Type
            out.write(textCt.toByteArray())
            out.write(0)
            // Data
            out.write(textBytes)
        }

        // Image part
        val imageBytes = contentResolver.openInputStream(imageUri)?.use { it.readBytes() }
            ?: throw IllegalStateException("Cannot read image")
        val imageCt = contentResolver.getType(imageUri) ?: "image/jpeg"
        out.writeUintvar(imageCt.length + 1)
        out.writeUintvar(imageBytes.size)
        out.write(imageCt.toByteArray())
        out.write(0)
        out.write(imageBytes)

        return out.toByteArray()
    }

    private fun java.io.ByteArrayOutputStream.writeString(s: String) {
        write(s.toByteArray())
        write(0)
    }

    private fun java.io.ByteArrayOutputStream.writeUintvar(value: Int) {
        if (value < 0x80) {
            write(value)
        } else {
            val bytes = mutableListOf<Int>()
            var v = value
            bytes.add(v and 0x7F)
            v = v shr 7
            while (v > 0) {
                bytes.add((v and 0x7F) or 0x80)
                v = v shr 7
            }
            bytes.reversed().forEach { write(it) }
        }
    }

    // ── Contacts ─────────────────────────────────────────────────────────────

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

    // ── Delete ───────────────────────────────────────────────────────────────

    suspend fun deleteMessage(messageId: Long, isMms: Boolean = false) = withContext(Dispatchers.IO) {
        runCatching {
            val uri = if (isMms) Telephony.Mms.CONTENT_URI else Telephony.Sms.CONTENT_URI
            val idCol = if (isMms) Telephony.Mms._ID else Telephony.Sms._ID
            contentResolver.delete(
                uri,
                "$idCol = ?",
                arrayOf(messageId.toString()),
            )
        }
    }

    suspend fun deleteThread(threadId: Long) = withContext(Dispatchers.IO) {
        runCatching {
            // Delete both SMS and MMS for this thread
            contentResolver.delete(
                Telephony.Sms.CONTENT_URI,
                "${Telephony.Sms.THREAD_ID} = ?",
                arrayOf(threadId.toString()),
            )
            contentResolver.delete(
                Telephony.Mms.CONTENT_URI,
                "${Telephony.Mms.THREAD_ID} = ?",
                arrayOf(threadId.toString()),
            )
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun markThreadRead(threadId: Long) {
        runCatching {
            val values = ContentValues().apply { put(Telephony.Sms.READ, 1) }
            contentResolver.update(
                Telephony.Sms.CONTENT_URI,
                values,
                "${Telephony.Sms.THREAD_ID} = ? AND ${Telephony.Sms.READ} = 0",
                arrayOf(threadId.toString()),
            )
            // Also mark MMS as read
            val mmsValues = ContentValues().apply { put(Telephony.Mms.READ, 1) }
            contentResolver.update(
                Telephony.Mms.CONTENT_URI,
                mmsValues,
                "${Telephony.Mms.THREAD_ID} = ? AND ${Telephony.Mms.READ} = 0",
                arrayOf(threadId.toString()),
            )
        }
    }

    private fun getThreadAddress(threadId: Long): String {
        // Try SMS first
        val smsCursor = contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            arrayOf(Telephony.Sms.ADDRESS),
            "${Telephony.Sms.THREAD_ID} = ?",
            arrayOf(threadId.toString()),
            "${Telephony.Sms.DATE} DESC LIMIT 1",
        )

        val smsAddr = smsCursor?.use { c ->
            if (c.moveToFirst()) c.getStringOrNull(Telephony.Sms.ADDRESS).orEmpty() else ""
        }.orEmpty()

        if (smsAddr.isNotBlank()) return smsAddr

        // Fall back to MMS address
        val mmsCursor = contentResolver.query(
            Telephony.Mms.CONTENT_URI,
            arrayOf(Telephony.Mms._ID),
            "${Telephony.Mms.THREAD_ID} = ?",
            arrayOf(threadId.toString()),
            "${Telephony.Mms.DATE} DESC LIMIT 1",
        )

        val mmsId = mmsCursor?.use { c ->
            if (c.moveToFirst()) c.getLongOrNull(Telephony.Mms._ID) else null
        }

        if (mmsId != null) {
            val addr = getMmsAddress(mmsId, false)
            if (addr.isNotBlank()) return addr
            // Try outgoing address
            return getMmsAddress(mmsId, true)
        }

        return ""
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
