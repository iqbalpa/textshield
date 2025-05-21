package org.textshield.project.domain.usecase

import org.textshield.project.domain.model.SmsMessage
import org.textshield.project.domain.repository.SmsRepository

class GetSmsMessagesUseCase(private val smsRepository: SmsRepository) {
    suspend fun getAllMessages(): List<SmsMessage> = 
        smsRepository.getAllMessages()
        
    suspend fun getInboxMessages(): List<SmsMessage> = 
        smsRepository.getInboxMessages()
        
    suspend fun getSpamMessages(): List<SmsMessage> = 
        smsRepository.getSpamMessages()
} 