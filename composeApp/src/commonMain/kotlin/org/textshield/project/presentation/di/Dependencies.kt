package org.textshield.project.presentation.di

import org.textshield.project.data.datasource.SmsDataSourceProvider
import org.textshield.project.data.repository.SmsRepositoryImpl
import org.textshield.project.domain.repository.SmsRepository
import org.textshield.project.domain.usecase.FilterSpamUseCase
import org.textshield.project.domain.usecase.GetSmsMessagesUseCase
import org.textshield.project.domain.usecase.MarkMessageSpamStatusUseCase
import org.textshield.project.presentation.viewmodel.InboxViewModel

/**
 * Simple dependency injection for the app
 * In a larger app, you might use Koin, Dagger, or other DI libraries
 */
object Dependencies {
    // Use by lazy for thread-safe, lazy initialization
    
    // Domain layer
    private val filterSpamUseCase by lazy { FilterSpamUseCase() }
    
    // Data layer
    private val smsDataSource by lazy { SmsDataSourceProvider.createSmsDataSource() }
    
    private val smsRepository: SmsRepository by lazy { 
        SmsRepositoryImpl(smsDataSource, filterSpamUseCase) 
    }
    
    private val getSmsMessagesUseCase by lazy { GetSmsMessagesUseCase(smsRepository) }
    private val markSpamStatusUseCase by lazy { MarkMessageSpamStatusUseCase(smsRepository) }
    
    // ViewModels
    val inboxViewModel by lazy { 
        InboxViewModel(getSmsMessagesUseCase, markSpamStatusUseCase) 
    }
} 