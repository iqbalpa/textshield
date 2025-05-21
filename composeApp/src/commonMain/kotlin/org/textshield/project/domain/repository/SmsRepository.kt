package org.textshield.project.domain.repository

import org.textshield.project.domain.model.SmsMessage

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
} 