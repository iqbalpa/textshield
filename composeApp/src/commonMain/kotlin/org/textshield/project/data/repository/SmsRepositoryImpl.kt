package org.textshield.project.data.repository

import org.textshield.project.data.datasource.SmsDataSource
import org.textshield.project.domain.model.SmsMessage
import org.textshield.project.domain.repository.SmsRepository
import org.textshield.project.domain.usecase.FilterSpamUseCase

/**
 * Implementation of SmsRepository that uses SmsDataSource
 * and applies spam filtering logic
 */
class SmsRepositoryImpl(
    private val smsDataSource: SmsDataSource,
    private val filterSpamUseCase: FilterSpamUseCase
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
    
    /**
     * Refresh messages from data source and apply spam filtering
     */
    private suspend fun refreshMessages() {
        val messages = smsDataSource.getSmsMessages()
        
        // Apply spam filtering to messages that don't already have a spam status
        cachedMessages = messages.map { message ->
            // If it's already marked as spam or not spam, keep that status
            if (message.isSpam) {
                message
            } else {
                // Apply spam filter
                val filterResult = filterSpamUseCase.execute(message.content)
                message.copy(isSpam = filterResult.isSpam)
            }
        }
    }
} 