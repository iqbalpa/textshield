package org.textshield.project.data.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.textshield.project.domain.detector.SpamDetectorProvider
import org.textshield.project.domain.model.SpamAction

/**
 * Receives SMS messages when the app is set as the default SMS app
 */
class SmsReceiver : BroadcastReceiver() {
    private val TAG = "SmsReceiver"
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        // Log the received intent for debugging
        Log.d(TAG, "SMS intent received: ${intent.action}")
        
        when (intent.action) {
            Telephony.Sms.Intents.SMS_DELIVER_ACTION -> {
                // This is called when app is the default SMS app
                processSmsMessages(context, intent)
            }
            Telephony.Sms.Intents.SMS_RECEIVED_ACTION -> {
                // This is called for non-default SMS apps
                // When we're the default, we should get SMS_DELIVER instead
                Log.d(TAG, "Received SMS_RECEIVED_ACTION - app might not be default SMS app")
                processSmsMessages(context, intent)
            }
            else -> {
                Log.d(TAG, "Received unhandled SMS intent: ${intent.action}")
            }
        }
    }
    
    private fun processSmsMessages(context: Context, intent: Intent) {
        try {
            // Extract SMS messages from the intent
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            
            if (messages.isEmpty()) {
                Log.w(TAG, "No SMS messages found in intent")
                return
            }
            
            // Process each message
            for (smsMessage in messages) {
                val sender = smsMessage.displayOriginatingAddress
                val body = smsMessage.messageBody
                
                Log.d(TAG, "Processing SMS from: $sender")
                
                // Process spam detection in a background coroutine
                scope.launch {
                    try {
                        // Get spam detector
                        val spamDetector = SpamDetectorProvider.getSpamDetector()
                        val result = spamDetector.detectSpam(body)
                        
                        // Log the spam detection result
                        Log.d(TAG, "Message from $sender: spam=${result.isSpam}, " +
                                "confidence=${result.confidenceScore}, " +
                                "method=${result.detectionMethod}")
                        
                        // If high confidence spam, we could automatically perform actions
                        // like moving to spam folder or blocking in a real implementation
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing message for spam: ${e.message}", e)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing SMS messages: ${e.message}", e)
        }
    }
} 