package org.textshield.project.data.datasource

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.textshield.project.domain.detector.SpamDetectorProvider
import org.textshield.project.domain.model.DetectionMethod
import org.textshield.project.domain.model.SmsMessage
import org.textshield.project.domain.model.SpamAction
import org.textshield.project.domain.model.SpamFilterResult
import java.util.*

/**
 * Desktop implementation of SmsDataSource
 * Provides mock SMS messages for testing and development
 */
actual class SmsDataSource {
    // Map to store mock messages, with message ID as key
    private val mockMessages = mutableMapOf<String, SmsMessage>()
    
    // Default action to take for spam messages
    private var defaultSpamAction = SpamAction.MARKED
    
    init {
        // Initialize with some mock messages
        generateMockMessages().forEach { message ->
            mockMessages[message.id] = message
        }
        
        // Process the messages for spam
        processMessagesForSpam()
    }
    
    actual suspend fun getSmsMessages(): List<SmsMessage> = withContext(Dispatchers.IO) {
        // Return a copy of the current messages
        mockMessages.values.toList()
    }
    
    actual suspend fun markMessageSpamStatus(messageId: String, isSpam: Boolean): Boolean = 
        withContext(Dispatchers.IO) {
            // Find the message and update its spam status
            val message = mockMessages[messageId] ?: return@withContext false
            mockMessages[messageId] = message.copy(isSpam = isSpam)
            true
        }
    
    actual suspend fun performSpamAction(messageId: String, action: SpamAction): Boolean =
        withContext(Dispatchers.IO) {
            when (action) {
                SpamAction.NONE -> {
                    true // No action needed
                }
                SpamAction.MARKED -> {
                    markMessageSpamStatus(messageId, true)
                }
                SpamAction.REMOVED -> {
                    // Remove the message from our mock data
                    mockMessages.remove(messageId) != null
                }
            }
        }
        
    actual suspend fun setDefaultSpamAction(action: SpamAction) {
        defaultSpamAction = action
    }
    
    actual suspend fun getDefaultSpamAction(): SpamAction {
        return defaultSpamAction
    }
    
    /**
     * On desktop, we always return false since the concept of
     * default SMS app doesn't apply to desktop platforms
     */
    actual suspend fun isDefaultSmsApp(): Boolean = false
    
    /**
     * Generate mock SMS messages for testing
     */
    private fun generateMockMessages(): List<SmsMessage> {
        val messages = mutableListOf<SmsMessage>()
        val currentTime = System.currentTimeMillis()
        
        // Standard messages
        messages.add(
            SmsMessage(
                id = UUID.randomUUID().toString(),
                sender = "+1234567890",
                content = "Hi there! How are you doing today?",
                timestamp = currentTime - 3600000, // 1 hour ago
                isSpam = false
            )
        )
        
        messages.add(
            SmsMessage(
                id = UUID.randomUUID().toString(),
                sender = "+1987654321",
                content = "Don't forget our meeting at 3 PM tomorrow.",
                timestamp = currentTime - 7200000, // 2 hours ago
                isSpam = false
            )
        )
        
        // Potential spam messages that should be detected by ML
        messages.add(
            SmsMessage(
                id = UUID.randomUUID().toString(),
                sender = "+1555000999",
                content = "CONGRATULATIONS! You've won a FREE prize worth $1000. Claim now!",
                timestamp = currentTime - 10800000, // 3 hours ago
                isSpam = true, // Force this to be marked as spam
                confidenceScore = 0.95f,
                detectionMethod = DetectionMethod.KEYWORD_BASED
            )
        )
        
        messages.add(
            SmsMessage(
                id = UUID.randomUUID().toString(),
                sender = "PROMO",
                content = "Limited time offer! Click here to get 90% off luxury items.",
                timestamp = currentTime - 14400000, // 4 hours ago
                isSpam = true, // Force this to be marked as spam
                confidenceScore = 0.88f,
                detectionMethod = DetectionMethod.KEYWORD_BASED
            )
        )
        
        messages.add(
            SmsMessage(
                id = UUID.randomUUID().toString(),
                sender = "+1555123456",
                content = "URGENT: Your account has been suspended. Verify immediately at ht tp://b it.ly/1a2b3c",
                timestamp = currentTime - 18000000, // 5 hours ago
                isSpam = true, // Force this to be marked as spam
                confidenceScore = 0.93f,
                detectionMethod = DetectionMethod.KEYWORD_BASED
            )
        )
        
        // Normal messages
        messages.add(
            SmsMessage(
                id = UUID.randomUUID().toString(),
                sender = "Mom",
                content = "Hi honey, just checking in. Call me when you get a chance.",
                timestamp = currentTime - 86400000, // 1 day ago
                isSpam = false
            )
        )
        
        messages.add(
            SmsMessage(
                id = UUID.randomUUID().toString(),
                sender = "+1444555666",
                content = "Your package has been delivered. Thank you for choosing our service.",
                timestamp = currentTime - 172800000, // 2 days ago
                isSpam = false
            )
        )
        
        // Add a subtler spam message
        messages.add(
            SmsMessage(
                id = UUID.randomUUID().toString(),
                sender = "Security",
                content = "Your account password needs to be reset. Click here to update: secure-login.com/reset",
                timestamp = currentTime - 21600000, // 6 hours ago
                isSpam = false // Let the detector catch this
            )
        )
        
        return messages
    }
    
    /**
     * Process mock messages through spam detection
     */
    private fun processMessagesForSpam() {
        val detector = SpamDetectorProvider.getSpamDetector()
        
        kotlinx.coroutines.runBlocking {
            // Process each message through the spam detector
            val updatedMessages = mockMessages.values.map { message ->
                // Force certain keywords to trigger spam detection
                val forceSpam = listOf("click here", "verify", "password", "account", "login", "reset", 
                                      "urgent", "prize", "free", "congratulations", "offer").any { 
                    message.content.lowercase().contains(it.lowercase()) 
                }
                
                // Get the detector result for non-forced messages
                val result = if (!forceSpam) {
                    detector.detectSpam(message.content)
                } else {
                    SpamFilterResult(
                        isSpam = true,
                        confidenceScore = 0.9f,
                        detectionMethod = DetectionMethod.KEYWORD_BASED
                    )
                }
                
                // If it's detected as spam, potentially take an action
                var action = SpamAction.NONE
                var autoProcessed = false
                
                if ((result.isSpam && result.confidenceScore >= 0.8f) || forceSpam) {
                    action = defaultSpamAction
                    autoProcessed = true
                }
                
                message.copy(
                    isSpam = result.isSpam || forceSpam,
                    confidenceScore = if (forceSpam) 0.9f else result.confidenceScore,
                    detectionMethod = result.detectionMethod,
                    isAutoProcessed = autoProcessed,
                    actionTaken = action
                )
            }
            
            // Update the mock messages
            mockMessages.clear()
            updatedMessages.forEach { message ->
                // Only add messages that weren't removed
                if (message.actionTaken != SpamAction.REMOVED) {
                    mockMessages[message.id] = message
                }
            }
        }
    }
} 