package org.textshield.project.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.textshield.project.domain.model.SmsMessage
import org.textshield.project.domain.usecase.GetSmsMessagesUseCase
import org.textshield.project.domain.usecase.MarkMessageSpamStatusUseCase

/**
 * UI state for the Inbox screen
 */
data class InboxState(
    val isLoading: Boolean = false,
    val inboxMessages: List<SmsMessage> = emptyList(),
    val spamMessages: List<SmsMessage> = emptyList(),
    val error: String? = null,
    val currentTab: InboxTab = InboxTab.INBOX
)

enum class InboxTab {
    INBOX, SPAM
}

/**
 * ViewModel for the inbox screen that manages inbox and spam messages
 */
class InboxViewModel(
    private val getSmsMessagesUseCase: GetSmsMessagesUseCase,
    private val markSpamUseCase: MarkMessageSpamStatusUseCase
) : ViewModel() {
    
    private val _state = MutableStateFlow(InboxState())
    val state: StateFlow<InboxState> = _state.asStateFlow()
    
    init {
        loadMessages()
    }
    
    /**
     * Load both inbox and spam messages
     */
    fun loadMessages() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            
            try {
                // Load inbox messages
                val inboxMessages = getSmsMessagesUseCase.getInboxMessages()
                
                // Load spam messages
                val spamMessages = getSmsMessagesUseCase.getSpamMessages()
                
                _state.update { 
                    it.copy(
                        isLoading = false,
                        inboxMessages = inboxMessages,
                        spamMessages = spamMessages
                    )
                }
            } catch (e: Exception) {
                _state.update { 
                    it.copy(
                        isLoading = false,
                        error = "Failed to load messages: ${e.message}"
                    )
                }
            }
        }
    }
    
    /**
     * Mark or unmark a message as spam
     */
    fun markMessageSpamStatus(messageId: String, isSpam: Boolean) {
        viewModelScope.launch {
            try {
                val success = markSpamUseCase.execute(messageId, isSpam)
                if (success) {
                    // Reload messages to reflect changes
                    loadMessages()
                }
            } catch (e: Exception) {
                _state.update { 
                    it.copy(
                        error = "Failed to update message: ${e.message}"
                    )
                }
            }
        }
    }
    
    /**
     * Change the currently active tab
     */
    fun setCurrentTab(tab: InboxTab) {
        _state.update { it.copy(currentTab = tab) }
    }
} 