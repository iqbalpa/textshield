package org.textshield.project.data.datasource

import org.textshield.project.domain.model.SmsMessage

/**
 * Platform-specific SMS data source
 * Will be implemented differently on Android and Desktop
 */
expect class SmsDataSource() {
    /**
     * Get SMS messages from the platform
     * On Android: from the SMS inbox
     * On Desktop: mock data
     */
    suspend fun getSmsMessages(): List<SmsMessage>
    
    /**
     * Mark a message as spam or not spam
     * On Android: potentially move to a special folder or add metadata
     * On Desktop: update mock data
     */
    suspend fun markMessageSpamStatus(messageId: String, isSpam: Boolean): Boolean
} 