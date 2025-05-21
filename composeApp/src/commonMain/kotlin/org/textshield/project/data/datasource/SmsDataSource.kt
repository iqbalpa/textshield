package org.textshield.project.data.datasource

import org.textshield.project.domain.model.SmsMessage
import org.textshield.project.domain.model.SpamAction

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
    
    /**
     * Perform an action on a spam message
     */
    suspend fun performSpamAction(messageId: String, action: SpamAction): Boolean
    
    /**
     * Set the default action for detected spam messages
     */
    suspend fun setDefaultSpamAction(action: SpamAction)
    
    /**
     * Get the current default action for detected spam messages
     */
    suspend fun getDefaultSpamAction(): SpamAction
    
    /**
     * Check if the app is set as the default SMS app
     * On Android: checks system default SMS app status
     * On Desktop: always returns false
     */
    suspend fun isDefaultSmsApp(): Boolean
}