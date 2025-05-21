package org.textshield.project.data.repository

import org.textshield.project.data.datasource.SmsDataSource
import org.textshield.project.domain.detector.SpamDetector
import org.textshield.project.domain.model.SmsMessage
import org.textshield.project.domain.model.SpamAction
import org.textshield.project.domain.repository.SmsRepository

/**
 * Implementation of SmsRepository that uses SmsDataSource
 * and applies spam filtering logic
 */
class SmsRepositoryImpl(
    private val smsDataSource: SmsDataSource,
    private val spamDetector: SpamDetector
) : SmsRepository {
    
    // Cache messages to avoid multiple fetches
    private var cachedMessages: List<SmsMessage>? = null
    
    override suspend fun getAllMessages(): List<SmsMessage> {
        // Get messages from data source if not cached
        if (cachedMessages == null) {
            refreshMessages()
        }
        
        return cachedMessages ?: emptyList()
    }
    
    override suspend fun getInboxMessages(): List<SmsMessage> {
        return getAllMessages().filterNot { it.isSpam }
    }
    
    override suspend fun getSpamMessages(): List<SmsMessage> {
        return getAllMessages().filter { it.isSpam }
    }
    
    override suspend fun markMessageSpamStatus(messageId: String, isSpam: Boolean): Boolean {
        // Update the message status in the data source
        val result = smsDataSource.markMessageSpamStatus(messageId, isSpam)
        
        // If successful, update our cached copy
        if (result) {
            cachedMessages = cachedMessages?.map { message ->
                if (message.id == messageId) {
                    message.copy(isSpam = isSpam)
                } else {
                    message
                }
            }
        }
        
        return result
    }
    
    override suspend fun performSpamAction(messageId: String, action: SpamAction): Boolean {
        // Perform the action on the message
        val result = smsDataSource.performSpamAction(messageId, action)
        
        // If successful, update our cached copy or remove it if needed
        if (result) {
            cachedMessages = when (action) {
                SpamAction.REMOVED -> {
                    // Remove from cache if it was deleted
                    cachedMessages?.filter { it.id != messageId }
                }
                SpamAction.MARKED -> {
                    // Mark as spam in cache
                    cachedMessages?.map { message ->
                        if (message.id == messageId) {
                            message.copy(
                                isSpam = true,
                                actionTaken = action
                            )
                        } else {
                            message
                        }
                    }
                }
                SpamAction.NONE -> {
                    // No changes needed
                    cachedMessages
                }
            }
        }
        
        return result
    }
    
    override suspend fun setDefaultSpamAction(action: SpamAction) {
        smsDataSource.setDefaultSpamAction(action)
    }
    
    override suspend fun getDefaultSpamAction(): SpamAction {
        return smsDataSource.getDefaultSpamAction()
    }
    
    override suspend fun isDefaultSmsApp(): Boolean {
        return smsDataSource.isDefaultSmsApp()
    }
    
    /**
     * Refresh messages from data source and apply spam filtering
     */
    private suspend fun refreshMessages() {
        cachedMessages = smsDataSource.getSmsMessages()
    }
} 