package org.textshield.project.data.datasource

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.Telephony
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.textshield.project.domain.model.SmsMessage

/**
 * Android implementation of SmsDataSource
 * Accesses actual SMS messages from the device
 */
actual class SmsDataSource(private val context: Context) {

    actual suspend fun getSmsMessages(): List<SmsMessage> = withContext(Dispatchers.IO) {
        val messages = mutableListOf<SmsMessage>()
        val contentResolver: ContentResolver = context.contentResolver
        val uri = Telephony.Sms.CONTENT_URI
        
        // Define which columns to retrieve
        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE
        )
        
        // Query SMS inbox
        val cursor: Cursor? = contentResolver.query(
            uri, projection, null, null, Telephony.Sms.DEFAULT_SORT_ORDER
        )
        
        cursor?.use { c ->
            // Get column indices
            val idColumn = c.getColumnIndexOrThrow(Telephony.Sms._ID)
            val addressColumn = c.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            val bodyColumn = c.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val dateColumn = c.getColumnIndexOrThrow(Telephony.Sms.DATE)
            
            while (c.moveToNext()) {
                val id = c.getString(idColumn)
                val address = c.getString(addressColumn)
                val body = c.getString(bodyColumn)
                val date = c.getLong(dateColumn)
                
                // We'll check if the message has "isSpam" flag in custom data or storage later
                // For now, assume they're not spam
                messages.add(
                    SmsMessage(
                        id = id,
                        sender = address,
                        content = body,
                        timestamp = date,
                        isSpam = false // Initially not marked as spam
                    )
                )
            }
        }
        
        messages
    }
    
    actual suspend fun markMessageSpamStatus(messageId: String, isSpam: Boolean): Boolean = 
        withContext(Dispatchers.IO) {
            // In a real app, you might:
            // 1. Store spam status in a separate database table
            // 2. Add metadata to the SMS (if possible)
            // 3. Move it to a different folder
            
            // For simplicity, we'll just return true for now
            // Implementing a proper storage for spam status would be needed
            true
        }
} 