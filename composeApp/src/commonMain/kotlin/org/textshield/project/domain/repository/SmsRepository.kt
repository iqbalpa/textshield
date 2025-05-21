package org.textshield.project.domain.repository

import org.textshield.project.domain.model.SmsMessage
import org.textshield.project.domain.model.SpamAction

/**
 * Repository interface for SMS operations
 */
interface SmsRepository {
    /**
     * Get all SMS messages from the device or mock data
     */
    suspend fun getAllMessages(): List<SmsMessage>
    
    /**
     * Get only non-spam messages
     */
    suspend fun getInboxMessages(): List<SmsMessage>
    
    /**
     * Get only spam messages
     */
    suspend fun getSpamMessages(): List<SmsMessage>
    
    /**
     * Mark a message as spam or not spam
     */
    suspend fun markMessageSpamStatus(messageId: String, isSpam: Boolean): Boolean
    
    /**
     * Perform an action on a spam message (mark or remove)
     */
    suspend fun performSpamAction(messageId: String, action: SpamAction): Boolean
    
    /**
     * Set the default action to take on detected spam messages
     */
    suspend fun setDefaultSpamAction(action: SpamAction)
    
    /**
     * Get the current default action for detected spam messages
     */
    suspend fun getDefaultSpamAction(): SpamAction
    
    /**
     * Check if the app is currently set as the default SMS app
     * This is platform-specific and will always return false on desktop
     */
    suspend fun isDefaultSmsApp(): Boolean
} 