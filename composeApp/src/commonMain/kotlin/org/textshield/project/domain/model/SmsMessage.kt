package org.textshield.project.domain.model

/**
 * Domain model representing an SMS message
 */
data class SmsMessage(
    val id: String,
    val sender: String,
    val content: String,
    val timestamp: Long,
    val isSpam: Boolean = false,
    val confidenceScore: Float = 0.0f,  // ML confidence (0.0-1.0) 
    val detectionMethod: DetectionMethod = DetectionMethod.KEYWORD_BASED,
    val isAutoProcessed: Boolean = false, // Whether the message was automatically processed
    val actionTaken: SpamAction = SpamAction.NONE // Action taken on this message
)

/**
 * Actions that can be taken on spam messages
 */
enum class SpamAction {
    NONE,       // No action was taken
    MARKED,     // Message was marked as spam
    REMOVED     // Message was removed/deleted from inbox
} 