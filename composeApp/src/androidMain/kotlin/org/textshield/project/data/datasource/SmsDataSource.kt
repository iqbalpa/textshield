package org.textshield.project.data.datasource

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.Telephony
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.textshield.project.domain.detector.SpamDetectorProvider
import org.textshield.project.domain.model.SmsMessage
import org.textshield.project.domain.model.SpamAction

/**
 * Android implementation of SmsDataSource
 * Accesses actual SMS messages from the device
 */
actual class SmsDataSource {
    private var context: Context? = null
    // Default action to take for spam messages
    private var defaultSpamAction = SpamAction.MARKED
    private val TAG = "SmsDataSource"

    // Primary constructor to satisfy the expect/actual pattern
    actual constructor()

    // Secondary constructor that takes the Android Context
    constructor(context: Context) {
        this.context = context
    }

    actual suspend fun getSmsMessages(): List<SmsMessage> = withContext(Dispatchers.IO) {
        val messages = mutableListOf<SmsMessage>()
        val ctx = context ?: return@withContext messages
        val contentResolver: ContentResolver = ctx.contentResolver
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
                
                // Get the spam detector and analyze message
                val spamDetector = SpamDetectorProvider.getSpamDetector()
                val spamResult = spamDetector.detectSpam(body)
                
                val smsMessage = SmsMessage(
                    id = id,
                    sender = address,
                    content = body,
                    timestamp = date,
                    isSpam = spamResult.isSpam,
                    confidenceScore = spamResult.confidenceScore,
                    detectionMethod = spamResult.detectionMethod
                )
                
                // Automatically process spam messages if detected
                var actionTaken = SpamAction.NONE
                if (spamResult.isSpam && spamResult.confidenceScore >= 0.8f) {
                    actionTaken = defaultSpamAction
                    performSpamAction(id, defaultSpamAction)
                }
                
                // Add the message to our list with the processed status
                messages.add(smsMessage.copy(
                    isAutoProcessed = spamResult.isSpam && spamResult.confidenceScore >= 0.8f,
                    actionTaken = actionTaken
                ))
            }
        }
        
        messages
    }
    
    actual suspend fun markMessageSpamStatus(messageId: String, isSpam: Boolean): Boolean = 
        withContext(Dispatchers.IO) {
            // In a real implementation, you'd update a database record or other storage
            // For now, we'll demonstrate the pattern but return true
            true
        }
    
    actual suspend fun performSpamAction(messageId: String, action: SpamAction): Boolean =
        withContext(Dispatchers.IO) {
            if (context == null) return@withContext false
            val ctx = context ?: return@withContext false
            
            when (action) {
                SpamAction.NONE -> {
                    // No action needed
                    true
                }
                SpamAction.MARKED -> {
                    // Mark message as spam in our database or system
                    markMessageSpamStatus(messageId, true)
                }
                SpamAction.REMOVED -> {
                    // Check if this app is the default SMS app
                    val isDefaultSmsApp = isDefaultSmsAppInternal(ctx)
                    
                    if (!isDefaultSmsApp) {
                        Log.w(TAG, "Cannot delete SMS: App is not the default SMS app")
                        return@withContext false
                    }
                    
                    // Try multiple deletion approaches
                    deleteMessage(ctx, messageId)
                }
            }
        }
    
    private fun isDefaultSmsAppInternal(context: Context): Boolean {
        val defaultApp = Telephony.Sms.getDefaultSmsPackage(context)
        val result = context.packageName == defaultApp
        Log.d(TAG, "isDefaultSmsApp check: $result (our=${context.packageName}, default=$defaultApp)")
        return result
    }
    
    actual suspend fun isDefaultSmsApp(): Boolean = withContext(Dispatchers.IO) {
        val ctx = context ?: return@withContext false
        isDefaultSmsAppInternal(ctx)
    }
    
    private fun deleteMessage(ctx: Context, messageId: String): Boolean {
        // Try multiple deletion methods
        return try {
            Log.d(TAG, "Attempting to delete message with ID: $messageId")
            
            // Try first with direct ID
            val uri = Uri.withAppendedPath(Telephony.Sms.CONTENT_URI, messageId)
            var deletedRows = ctx.contentResolver.delete(uri, null, null)
            
            if (deletedRows > 0) {
                Log.d(TAG, "Successfully deleted message (method 1)")
                return true
            }
            
            // Try with selection args
            val selection = "${Telephony.Sms._ID} = ?"
            val selectionArgs = arrayOf(messageId)
            deletedRows = ctx.contentResolver.delete(Telephony.Sms.CONTENT_URI, selection, selectionArgs)
            
            if (deletedRows > 0) {
                Log.d(TAG, "Successfully deleted message (method 2)")
                return true
            }
            
            // Try with content URI
            try {
                val id = messageId.toLong()
                val contentUri = ContentUris.withAppendedId(Telephony.Sms.CONTENT_URI, id)
                deletedRows = ctx.contentResolver.delete(contentUri, null, null)
                
                if (deletedRows > 0) {
                    Log.d(TAG, "Successfully deleted message (method 3)")
                    return true
                }
            } catch (e: NumberFormatException) {
                // messageId might not be a valid Long, just continue
            }
            
            Log.e(TAG, "Failed to delete message with ID: $messageId")
            false
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting message: ${e.message}", e)
            false
        }
    }
    
    actual suspend fun setDefaultSpamAction(action: SpamAction) {
        defaultSpamAction = action
    }
    
    actual suspend fun getDefaultSpamAction(): SpamAction {
        return defaultSpamAction
    }
} 