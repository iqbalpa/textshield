package org.textshield.project.data.service

import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.IBinder
import android.telephony.SmsManager
import android.util.Log

/**
 * This service is required for the app to be able to become
 * the default SMS app on Android. It's a headless service that
 * allows the system to send SMS messages through our app.
 */
class HeadlessSmsService : Service() {
    private val TAG = "HeadlessSmsService"

    override fun onBind(intent: Intent): IBinder? {
        Log.d(TAG, "onBind: ${intent.action}")
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: ${intent?.action}")
        
        // Handle the respond via message intent if present
        if (intent?.action == "android.intent.action.RESPOND_VIA_MESSAGE") {
            try {
                // In a real implementation, this would send an actual SMS reply
                val data = intent.data
                if (data != null) {
                    val recipients = getRecipients(data)
                    val text = intent.getStringExtra(Intent.EXTRA_TEXT)
                    
                    if (!text.isNullOrEmpty() && recipients.isNotEmpty()) {
                        Log.d(TAG, "Would send SMS to ${recipients.joinToString()} with text: $text")
                        
                        // Here we would actually send the SMS in a real implementation
                        // But for now, we just log it
                        
                        // Example code for sending SMS (commented out):
                        // val smsManager = SmsManager.getDefault()
                        // for (recipient in recipients) {
                        //     smsManager.sendTextMessage(recipient, null, text, null, null)
                        // }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling response intent: ${e.message}", e)
            }
        }
        
        return START_NOT_STICKY
    }
    
    /**
     * Extract recipient phone numbers from the intent data URI
     */
    private fun getRecipients(data: Uri): List<String> {
        val recipients = mutableListOf<String>()
        
        when (data.scheme) {
            "sms", "mms", "smsto", "mmsto" -> {
                val path = data.schemeSpecificPart
                if (path != null) {
                    // Remove any leading slashes
                    val cleanPath = path.removePrefix("//")
                    if (cleanPath.isNotEmpty()) {
                        recipients.add(cleanPath)
                    }
                }
            }
        }
        
        return recipients
    }
} 