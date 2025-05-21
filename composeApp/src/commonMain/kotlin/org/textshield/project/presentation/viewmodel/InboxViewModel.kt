package org.textshield.project.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.textshield.project.domain.model.DetectionMethod
import org.textshield.project.domain.model.SmsMessage
import org.textshield.project.domain.model.SpamAction
import org.textshield.project.domain.repository.SmsRepository
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
    val currentTab: InboxTab = InboxTab.INBOX,
    val defaultSpamAction: SpamAction = SpamAction.MARKED,
    val isDefaultSmsApp: Boolean = false
)

enum class InboxTab {
    INBOX, SPAM
}

/**
 * ViewModel for the inbox screen that manages inbox and spam messages
 */
class InboxViewModel(
    private val getSmsMessagesUseCase: GetSmsMessagesUseCase,
    private val markSpamUseCase: MarkMessageSpamStatusUseCase,
    private val smsRepository: SmsRepository
) : ViewModel() {
    
    private val _state = MutableStateFlow(InboxState())
    val state: StateFlow<InboxState> = _state.asStateFlow()
    
    init {
        loadMessages()
        loadDefaultSpamAction()
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
                
                // Check if we're the default SMS app (platform specific implementation)
                val isDefaultSmsApp = smsRepository.isDefaultSmsApp()
                
                _state.update { 
                    it.copy(
                        isLoading = false,
                        inboxMessages = inboxMessages,
                        spamMessages = spamMessages,
                        isDefaultSmsApp = isDefaultSmsApp
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
     * Perform a specific action on a spam message
     */
    fun performSpamAction(messageId: String, action: SpamAction) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            
            try {
                // If trying to remove and not default SMS app, show error
                if (action == SpamAction.REMOVED && !_state.value.isDefaultSmsApp) {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = "TextShield must be the default SMS app to delete messages"
                        )
                    }
                    return@launch
                }
                
                val success = smsRepository.performSpamAction(messageId, action)
                
                if (success) {
                    // Temporarily update the UI to reflect the change before reloading
                    if (action == SpamAction.REMOVED) {
                        // Remove message from current list in UI
                        _state.update { currentState ->
                            val updatedInbox = currentState.inboxMessages.filter { it.id != messageId }
                            val updatedSpam = currentState.spamMessages.filter { it.id != messageId }
                            
                            currentState.copy(
                                inboxMessages = updatedInbox,
                                spamMessages = updatedSpam,
                                isLoading = false
                            )
                        }
                    }
                    
                    // Reload all messages to ensure consistency
                    loadMessages()
                } else {
                    // If action failed, show error
                    _state.update { 
                        it.copy(
                            isLoading = false,
                            error = when(action) {
                                SpamAction.REMOVED -> "Failed to delete message. Please check app permissions."
                                SpamAction.MARKED -> "Failed to mark message as spam."
                                else -> "Failed to perform action on message."
                            }
                        )
                    }
                }
            } catch (e: Exception) {
                _state.update { 
                    it.copy(
                        isLoading = false,
                        error = "Failed to perform action: ${e.message}"
                    )
                }
            }
        }
    }
    
    /**
     * Set the default action to take on spam messages
     */
    fun setDefaultSpamAction(action: SpamAction) {
        viewModelScope.launch {
            try {
                smsRepository.setDefaultSpamAction(action)
                loadDefaultSpamAction()
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        error = "Failed to set default action: ${e.message}"
                    )
                }
            }
        }
    }
    
    /**
     * Load the current default spam action
     */
    private fun loadDefaultSpamAction() {
        viewModelScope.launch {
            try {
                val defaultAction = smsRepository.getDefaultSpamAction()
                _state.update {
                    it.copy(defaultSpamAction = defaultAction)
                }
            } catch (e: Exception) {
                // Default action couldn't be loaded, keep the current value
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