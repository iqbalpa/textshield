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
    val isDefaultSmsApp: Boolean = false,
    val selectedMessageIds: Set<String> = emptySet(),
    val isBulkDeleteInProgress: Boolean = false
)

enum class InboxTab(val displayName: String) {
    INBOX("Inbox"), 
    SPAM("Spam")
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
                
                // Filter out any selected message IDs that no longer exist
                val existingMessageIds = (inboxMessages + spamMessages).map { it.id }.toSet()
                val validSelectedIds = _state.value.selectedMessageIds.filter { 
                    existingMessageIds.contains(it) 
                }.toSet()
                
                _state.update { 
                    it.copy(
                        isLoading = false,
                        inboxMessages = inboxMessages,
                        spamMessages = spamMessages,
                        isDefaultSmsApp = isDefaultSmsApp,
                        selectedMessageIds = validSelectedIds
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
     * Mark all messages from a sender as spam
     */
    fun markAllMessagesFromSenderAsSpam(sender: String) {
        viewModelScope.launch {
            try {
                val allMessages = _state.value.inboxMessages.filter { it.sender == sender }
                var anySuccess = false
                
                for (message in allMessages) {
                    val success = markSpamUseCase.execute(message.id, true)
                    if (success) {
                        anySuccess = true
                    }
                }
                
                if (anySuccess) {
                    // Reload messages to reflect changes
                    loadMessages()
                }
            } catch (e: Exception) {
                _state.update { 
                    it.copy(
                        error = "Failed to mark messages as spam: ${e.message}"
                    )
                }
            }
        }
    }
    
    /**
     * Mark all messages from a sender as not spam
     */
    fun markAllMessagesFromSenderAsNotSpam(sender: String) {
        viewModelScope.launch {
            try {
                val allMessages = _state.value.spamMessages.filter { it.sender == sender }
                var anySuccess = false
                
                for (message in allMessages) {
                    val success = markSpamUseCase.execute(message.id, false)
                    if (success) {
                        anySuccess = true
                    }
                }
                
                if (anySuccess) {
                    // Reload messages to reflect changes
                    loadMessages()
                }
            } catch (e: Exception) {
                _state.update { 
                    it.copy(
                        error = "Failed to mark messages as not spam: ${e.message}"
                    )
                }
            }
        }
    }
    
    /**
     * Delete all messages from a sender
     */
    fun deleteAllMessagesFromSender(sender: String, fromSpam: Boolean) {
        viewModelScope.launch {
            // Check if the app is the default SMS app
            if (!_state.value.isDefaultSmsApp) {
                _state.update {
                    it.copy(
                        error = "TextShield must be the default SMS app to delete messages"
                    )
                }
                return@launch
            }
            
            try {
                val messagesToDelete = if (fromSpam) {
                    _state.value.spamMessages.filter { it.sender == sender }
                } else {
                    _state.value.inboxMessages.filter { it.sender == sender }
                }
                
                var successCount = 0
                
                for (message in messagesToDelete) {
                    val success = smsRepository.performSpamAction(message.id, SpamAction.REMOVED)
                    if (success) {
                        successCount++
                    }
                }
                
                if (successCount > 0) {
                    // Update UI to reflect changes
                    _state.update { currentState ->
                        val updatedInbox = if (!fromSpam) {
                            currentState.inboxMessages.filter { it.sender != sender }
                        } else {
                            currentState.inboxMessages
                        }
                        
                        val updatedSpam = if (fromSpam) {
                            currentState.spamMessages.filter { it.sender != sender }
                        } else {
                            currentState.spamMessages
                        }
                        
                        // Remove any selected IDs that were from this sender
                        val updatedSelectedIds = currentState.selectedMessageIds.filter { id ->
                            val message = (updatedInbox + updatedSpam).find { it.id == id }
                            message?.sender != sender
                        }.toSet()
                        
                        currentState.copy(
                            inboxMessages = updatedInbox,
                            spamMessages = updatedSpam,
                            selectedMessageIds = updatedSelectedIds
                        )
                    }
                    
                    // Reload messages to ensure consistency
                    loadMessages()
                }
            } catch (e: Exception) {
                _state.update { 
                    it.copy(
                        error = "Failed to delete messages: ${e.message}"
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
                            val updatedSelectedIds = currentState.selectedMessageIds - messageId
                            
                            currentState.copy(
                                inboxMessages = updatedInbox,
                                spamMessages = updatedSpam,
                                selectedMessageIds = updatedSelectedIds,
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
     * Toggle selection of a message
     */
    fun toggleMessageSelection(messageId: String) {
        _state.update { currentState ->
            val newSelectedIds = if (currentState.selectedMessageIds.contains(messageId)) {
                currentState.selectedMessageIds - messageId
            } else {
                currentState.selectedMessageIds + messageId
            }
            
            currentState.copy(selectedMessageIds = newSelectedIds)
        }
    }
    
    /**
     * Select all spam messages
     */
    fun selectAllSpamMessages() {
        _state.update { currentState ->
            val spamIds = currentState.spamMessages.map { it.id }.toSet()
            currentState.copy(selectedMessageIds = spamIds)
        }
    }
    
    /**
     * Clear all message selections
     */
    fun clearAllSelections() {
        _state.update { it.copy(selectedMessageIds = emptySet()) }
    }
    
    /**
     * Delete all selected messages
     */
    fun deleteSelectedMessages() {
        viewModelScope.launch {
            // Check if the app is the default SMS app
            if (!_state.value.isDefaultSmsApp) {
                _state.update {
                    it.copy(
                        error = "TextShield must be the default SMS app to delete messages"
                    )
                }
                return@launch
            }
            
            val selectedIds = _state.value.selectedMessageIds
            if (selectedIds.isEmpty()) {
                _state.update {
                    it.copy(error = "No messages selected for deletion")
                }
                return@launch
            }
            
            _state.update { it.copy(isBulkDeleteInProgress = true) }
            
            try {
                var successCount = 0
                var failCount = 0
                
                // Process each message one by one
                for (messageId in selectedIds) {
                    val success = smsRepository.performSpamAction(messageId, SpamAction.REMOVED)
                    if (success) {
                        successCount++
                    } else {
                        failCount++
                    }
                }
                
                // Update UI with results
                _state.update {
                    val message = when {
                        successCount > 0 && failCount == 0 -> "Successfully deleted $successCount messages"
                        successCount > 0 && failCount > 0 -> "Deleted $successCount messages, failed to delete $failCount"
                        else -> "Failed to delete messages"
                    }
                    
                    it.copy(
                        isBulkDeleteInProgress = false,
                        selectedMessageIds = emptySet(),
                        error = if (failCount > 0) message else null
                    )
                }
                
                // Reload messages to update the UI
                loadMessages()
                
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isBulkDeleteInProgress = false,
                        error = "Error deleting messages: ${e.message}"
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