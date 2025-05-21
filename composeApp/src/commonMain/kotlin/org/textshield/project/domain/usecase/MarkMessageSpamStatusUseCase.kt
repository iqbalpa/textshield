package org.textshield.project.domain.usecase

import org.textshield.project.domain.repository.SmsRepository

class MarkMessageSpamStatusUseCase(private val smsRepository: SmsRepository) {
    suspend fun execute(messageId: String, isSpam: Boolean): Boolean =
        smsRepository.markMessageSpamStatus(messageId, isSpam)
} 