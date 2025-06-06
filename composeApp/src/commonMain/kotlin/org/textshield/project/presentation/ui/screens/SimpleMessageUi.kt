package org.textshield.project.presentation.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlin.math.absoluteValue
import kotlinx.coroutines.launch
import org.textshield.project.domain.model.SmsMessage
import org.textshield.project.domain.model.SpamAction
import org.textshield.project.presentation.di.Dependencies
import org.textshield.project.presentation.viewmodel.InboxTab
import org.textshield.project.presentation.viewmodel.InboxViewModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * Simple message screen for inbox with modern UI
 */
@Composable
fun SimpleMessageScreen() {
    val viewModel = Dependencies.inboxViewModel
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    // Remember which conversations have been viewed to reset unread counts
    var viewedConversations by remember { mutableStateOf(setOf<String>()) }
    
    // Search functionality state
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val searchTextFieldFocusRequester = remember { FocusRequester() }
    
    // Track whether messages are being moved to avoid auto-tab switching
    var isMovingToSpam by remember { mutableStateOf(false) }
    
    // Helper functions for spam actions
    fun handleMarkAsNotSpam(messageId: String) {
        // Find the sender of this message
        val message = state.spamMessages.find { it.id == messageId }
        if (message != null) {
            // Mark all messages from this sender as not spam
            viewModel.markAllMessagesFromSenderAsNotSpam(message.sender)
            
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = "All messages from ${message.sender} moved to Inbox",
                    duration = SnackbarDuration.Short,
                    withDismissAction = true
                )
            }
        } else {
            // Fallback to single message if sender not found
            viewModel.markMessageSpamStatus(messageId, false)
            
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = "Message moved to Inbox",
                    duration = SnackbarDuration.Short,
                    withDismissAction = true
                )
            }
        }
    }
    
    fun handleDeleteMessage(messageId: String, fromSpam: Boolean) {
        // Find the sender of this message
        val messages = if (fromSpam) state.spamMessages else state.inboxMessages
        val message = messages.find { it.id == messageId }
        
        if (message != null) {
            // Delete all messages from this sender
            viewModel.deleteAllMessagesFromSender(message.sender, fromSpam)
            
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = "All messages from ${message.sender} deleted",
                    duration = SnackbarDuration.Short,
                    withDismissAction = true
                )
            }
        } else {
            // Fallback to single message if sender not found
            viewModel.performSpamAction(messageId, SpamAction.REMOVED)
            
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = "Message deleted",
                    duration = SnackbarDuration.Short,
                    withDismissAction = true
                )
            }
        }
    }
    
    fun handleMarkAsSpam(messageId: String) {
        // Find the sender of this message
        val message = state.inboxMessages.find { it.id == messageId }
        if (message != null) {
            isMovingToSpam = true
            // Mark all messages from this sender as spam
            viewModel.markAllMessagesFromSenderAsSpam(message.sender)
            
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = "All messages from ${message.sender} moved to Spam",
                    duration = SnackbarDuration.Short,
                    withDismissAction = true
                )
                isMovingToSpam = false
            }
        } else {
            // Fallback to single message if sender not found
            isMovingToSpam = true
            viewModel.markMessageSpamStatus(messageId, true)
            
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = "Message moved to Spam",
                    duration = SnackbarDuration.Short,
                    withDismissAction = true
                )
                isMovingToSpam = false
            }
        }
    }
    
    // Automatically show spam tab if there are spam messages, but only if not in the process of marking something as spam
    LaunchedEffect(state.spamMessages) {
        if (state.spamMessages.isNotEmpty() && !isMovingToSpam) {
            viewModel.setCurrentTab(InboxTab.SPAM)
        }
    }
    
    // Set the current screen based on the selected tab
    var currentScreen by remember { 
        mutableStateOf<Screen>(
            if (state.currentTab == InboxTab.SPAM) Screen.Spam else Screen.Inbox
        ) 
    }
    
    // Update the current screen when tab changes (unless we're processing a spam marking action)
    LaunchedEffect(state.currentTab, isMovingToSpam) {
        if (!isMovingToSpam) {
            currentScreen = when (state.currentTab) {
                InboxTab.INBOX -> Screen.Inbox
                InboxTab.SPAM -> Screen.Spam
            }
        }
    }
    
    // When search is activated, automatically focus the text field
    LaunchedEffect(isSearchActive) {
        if (isSearchActive) {
            try {
                searchTextFieldFocusRequester.requestFocus()
            } catch (e: Exception) {
                // Handle potential focus request exceptions
            }
        }
    }
    
    var selectedConversationId by remember { mutableStateOf<String?>(null) }
    var selectedConversationMessages by remember { mutableStateOf<List<SmsMessage>>(emptyList()) }
    
    // Group messages by conversation (sender)
    val messagesBySender = remember(state.inboxMessages) {
        state.inboxMessages.groupBy { it.sender }
    }
    
    val spamMessagesBySender = remember(state.spamMessages) {
        state.spamMessages.groupBy { it.sender }
    }
    
    // Filter messages based on search query if search is active
    val filteredMessagesBySender = remember(messagesBySender, searchQuery, isSearchActive) {
        if (!isSearchActive || searchQuery.isBlank()) {
            messagesBySender
        } else {
            val lowercaseQuery = searchQuery.lowercase()
            messagesBySender.filter { (sender, messages) ->
                sender.lowercase().contains(lowercaseQuery) ||
                messages.any { it.content.lowercase().contains(lowercaseQuery) }
            }
        }
    }
    
    val filteredSpamMessagesBySender = remember(spamMessagesBySender, searchQuery, isSearchActive) {
        if (!isSearchActive || searchQuery.isBlank()) {
            spamMessagesBySender
        } else {
            val lowercaseQuery = searchQuery.lowercase()
            spamMessagesBySender.filter { (sender, messages) ->
                sender.lowercase().contains(lowercaseQuery) ||
                messages.any { it.content.lowercase().contains(lowercaseQuery) }
            }
        }
    }
    
    // Function to open a conversation and mark it as viewed
    val openConversation = { sender: String, messages: List<SmsMessage> ->
        selectedConversationId = sender
        selectedConversationMessages = messages
        // Mark this conversation as viewed
        viewedConversations = viewedConversations + sender
        currentScreen = Screen.Conversation
    }
    
    // Show error message if any
    LaunchedEffect(state.error) {
        state.error?.let {
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = it,
                    duration = SnackbarDuration.Long
                )
            }
        }
    }

    val isDarkTheme = isSystemInDarkTheme()
    val backgroundColor = if (isDarkTheme) {
        Color(0xFF121212) // Dark background
    } else {
        MaterialTheme.colorScheme.background
    }

    Scaffold(
        containerColor = backgroundColor,
        contentColor = MaterialTheme.colorScheme.onBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
        ) {
            when (currentScreen) {
                is Screen.Inbox -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // App bar - either search bar or regular app bar
                        if (isSearchActive) {
                            SearchAppBar(
                                searchQuery = searchQuery,
                                onSearchQueryChange = { searchQuery = it },
                                onCloseSearch = { 
                                    isSearchActive = false 
                                    searchQuery = ""
                                },
                                focusRequester = searchTextFieldFocusRequester
                            )
                        } else {
                            // Regular app bar
                            ModernAppBar(
                                title = "Messages",
                                currentTab = InboxTab.INBOX,
                                onTabChanged = { 
                                    viewModel.setCurrentTab(it)
                                    if (!isMovingToSpam) {
                                        currentScreen = when(it) {
                                            InboxTab.INBOX -> Screen.Inbox
                                            InboxTab.SPAM -> Screen.Spam
                                        }
                                    }
                                },
                                onRefresh = { viewModel.loadMessages() },
                                onSearchClick = { isSearchActive = true }
                            )
                        }
                        
                        // Default SMS app warning
                        if (!state.isDefaultSmsApp) {
                            DefaultSmsAppWarning(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                        
                        // Content
                        if (state.isLoading) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        } else if (filteredMessagesBySender.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isSearchActive) "No messages match your search" else "Your inbox is empty",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(vertical = 8.dp)
                            ) {
                                items(
                                    filteredMessagesBySender.map { (sender, messages) -> 
                                        messages.maxByOrNull { it.timestamp } ?: messages.first()
                                    }.sortedByDescending { it.timestamp }
                                ) { message ->
                                    // Calculate unread count - set to 0 if conversation has been viewed
                                    val unreadCount = remember(message.id, viewedConversations) {
                                        // If this conversation has been viewed, unread count is 0
                                        if (viewedConversations.contains(message.sender)) {
                                            0
                                        } else {
                                            // For demo: generate random unread counts for some messages
                                            // In a real app, this would come from the message data
                                            if (message.id.hashCode() % 3 == 0) (1..12).random() else 0
                                        }
                                    }
                                    
                                    SimpleMessageItem(
                                        message = message,
                                        onClick = { openConversation(message.sender, messagesBySender[message.sender] ?: emptyList()) },
                                        onMarkAsSpam = { handleMarkAsSpam(message.id) },
                                        onMarkAsNotSpam = { handleMarkAsNotSpam(message.id) },
                                        onDelete = { handleDeleteMessage(message.id, false) },
                                        isDefault = state.isDefaultSmsApp,
                                        isSelected = false,
                                        onToggleSelection = null,
                                        showCheckbox = false,
                                        unreadCount = unreadCount
                                    )
                                }
                            }
                        }
                    }
                }
                
                is Screen.Spam -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // App bar - either search bar or regular app bar
                        if (isSearchActive) {
                            SearchAppBar(
                                searchQuery = searchQuery,
                                onSearchQueryChange = { searchQuery = it },
                                onCloseSearch = { 
                                    isSearchActive = false 
                                    searchQuery = ""
                                },
                                focusRequester = searchTextFieldFocusRequester
                            )
                        } else {
                            // Regular app bar
                            ModernAppBar(
                                title = "Messages",
                                currentTab = InboxTab.SPAM,
                                onTabChanged = { 
                                    viewModel.setCurrentTab(it)
                                    currentScreen = when(it) {
                                        InboxTab.INBOX -> Screen.Inbox
                                        InboxTab.SPAM -> Screen.Spam
                                    }
                                },
                                onRefresh = { viewModel.loadMessages() },
                                onSearchClick = { isSearchActive = true }
                            )
                        }
                        
                        // Default SMS app warning
                        if (!state.isDefaultSmsApp) {
                            DefaultSmsAppWarning(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                        
                                                // Show bulk actions toolbar if there are spam messages
                        if (state.spamMessages.isNotEmpty()) {
                            BulkActionsToolbar(
                                selectedCount = state.selectedMessageIds.size,
                                totalCount = state.spamMessages.size,
                                onSelectAll = { viewModel.selectAllSpamMessages() },
                                onClearSelections = { viewModel.clearAllSelections() },
                                onDeleteSelected = { viewModel.deleteSelectedMessages() },
                                isDeleteInProgress = state.isBulkDeleteInProgress,
                                isDefaultSmsApp = state.isDefaultSmsApp
                            )
                        }
                        
                        // Content
                        if (state.isLoading || state.isBulkDeleteInProgress) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        } else if (filteredSpamMessagesBySender.isEmpty()) {
                            SpamEmptyState(
                                modifier = Modifier.fillMaxSize(),
                                isSearchActive = isSearchActive
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(vertical = 8.dp)
                            ) {
                                items(
                                    filteredSpamMessagesBySender.map { (sender, messages) -> 
                                        messages.maxByOrNull { it.timestamp } ?: messages.first()
                                    }.sortedByDescending { it.timestamp }
                                ) { message ->
                                    val isSelected = state.selectedMessageIds.contains(message.id)
                                    
                                    SimpleMessageItem(
                                        message = message,
                                        onClick = {
                                            if (state.selectedMessageIds.isNotEmpty()) {
                                                viewModel.toggleMessageSelection(message.id)
                                            } else {
                                                openConversation(message.sender, spamMessagesBySender[message.sender] ?: emptyList())
                                            }
                                        },
                                        onMarkAsSpam = { handleMarkAsSpam(message.id) },
                                        onMarkAsNotSpam = { handleMarkAsNotSpam(message.id) },
                                        onDelete = { handleDeleteMessage(message.id, true) },
                                        isDefault = state.isDefaultSmsApp,
                                        isSelected = isSelected,
                                        onToggleSelection = { viewModel.toggleMessageSelection(message.id) },
                                        showCheckbox = true,
                                        unreadCount = 0 // Spam messages don't have unread counts
                                    )
                                }
                            }
                        }
                    }
                }
                
                is Screen.Conversation -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Conversation app bar - more compact and cleaner but still readable
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceDim,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            shadowElevation = 4.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp), // Increased padding
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { 
                                        // Just go back without clearing viewed state
                                        currentScreen = if (selectedConversationMessages.any { it.isSpam }) Screen.Spam else Screen.Inbox 
                                    },
                                    modifier = Modifier.size(40.dp) // Slightly larger back button
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowBack,
                                        contentDescription = "Back",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                
                                // Contact initial avatar
                                val sender = selectedConversationId ?: "Unknown"
                                val isDarkTheme = isSystemInDarkTheme()
                                
                                // Generate consistent avatar color
                                val avatarBackgroundColor = remember(sender) {
                                    val hash = sender.hashCode()
                                    val hue = (hash.absoluteValue % 360).toFloat()
                                    val saturation = 0.7f
                                    val lightness = if (isDarkTheme) 0.5f else 0.7f
                                    
                                    Color.hsv(hue = hue, saturation = saturation, value = lightness)
                                }
                                
                                // Text color based on background brightness
                                val avatarTextColor = if (calculateLuminance(avatarBackgroundColor) > 0.5f) 
                                    Color.Black else Color.White
                                
                                Box(
                                    modifier = Modifier
                                        .size(40.dp) // Larger avatar
                                        .clip(CircleShape)
                                        .background(avatarBackgroundColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = sender.firstOrNull()?.uppercase() ?: "?",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = avatarTextColor
                                    )
                                }
                                
                                Spacer(modifier = Modifier.width(16.dp)) // Increased spacing
                                
                                Text(
                                    text = sender,
                                    style = MaterialTheme.typography.titleLarge, // Larger text
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        
                        // Sort messages by timestamp (oldest to newest)
                        val sortedMessages = remember(selectedConversationMessages) {
                            selectedConversationMessages.sortedBy { it.timestamp }
                        }
                        
                        // Use rememberLazyListState to control scrolling
                        val listState = rememberLazyListState()
                        
                        // Automatically scroll to bottom when conversation opens
                        LaunchedEffect(sortedMessages) {
                            if (sortedMessages.isNotEmpty()) {
                                listState.scrollToItem(sortedMessages.size - 1)
                            }
                        }
                        
                        // Messages - displayed in chronological order (oldest first)
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp)
                        ) {
                            items(sortedMessages) { message ->
                                SimpleChatBubble(
                                    message = message,
                                    onDelete = { 
                                        // Delete message and all messages from the same sender
                                        val isSpam = message.isSpam
                                        handleDeleteMessage(message.id, isSpam)
                                        
                                        // Return to appropriate screen if this was the last message
                                        if (selectedConversationMessages.size <= 1) {
                                            currentScreen = if (isSpam) Screen.Spam else Screen.Inbox
                                        }
                                    },
                                    onMarkAsSpam = {
                                        if (message.isSpam) {
                                            // Mark as not spam
                                            handleMarkAsNotSpam(message.id)
                                        } else {
                                            // Mark as spam
                                            isMovingToSpam = true
                                            handleMarkAsSpam(message.id)
                                            
                                            // Return to inbox if this was the only message
                                            if (selectedConversationMessages.size <= 1) {
                                                currentScreen = Screen.Inbox
                                            }
                                        }
                                    },
                                    isDefault = state.isDefaultSmsApp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModernAppBar(
    title: String,
    currentTab: InboxTab,
    onTabChanged: (InboxTab) -> Unit,
    onRefresh: () -> Unit,
    onSearchClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceDim,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shadowElevation = 4.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Top bar with title and actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge
                )
                
                Row(horizontalArrangement = Arrangement.End) {
                    // Search icon
                    IconButton(onClick = onSearchClick) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    
                    // Refresh icon
                    IconButton(onClick = onRefresh) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    
                    // Menu icon
                    IconButton(onClick = { /* More options */ }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More Options",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
            
            // Custom tabs with pill shape indicators
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                InboxTab.values().forEach { tab ->
                    val isSelected = tab == currentTab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.secondaryContainer
                                else Color.Transparent
                            )
                            .clickable { onTabChanged(tab) }
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tab.displayName,
                            color = if (isSelected) 
                                MaterialTheme.colorScheme.onSecondaryContainer
                            else 
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SpamEmptyState(
    modifier: Modifier = Modifier,
    isSearchActive: Boolean
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "No Spam",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(64.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = if (isSearchActive) "No spam messages match your search" else "No spam messages",
                style = MaterialTheme.typography.headlineSmall
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Your inbox is clean!",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun DefaultSmsAppWarning(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "⚠️",
                style = MaterialTheme.typography.headlineSmall
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column {
                Text(
                    text = "Set TextShield as Default SMS App",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "Message deletion requires TextShield to be your default SMS app",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

@Composable
private fun BulkActionsToolbar(
    selectedCount: Int,
    totalCount: Int,
    onSelectAll: () -> Unit,
    onClearSelections: () -> Unit,
    onDeleteSelected: () -> Unit,
    isDeleteInProgress: Boolean,
    isDefaultSmsApp: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Selection status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$selectedCount of $totalCount selected",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Row {
                    // Select All button
                    if (selectedCount < totalCount) {
                        TextButton(onClick = onSelectAll) {
                            Text("Select All")
                        }
                    } else if (selectedCount > 0) {
                        // Clear selection button
                        TextButton(onClick = onClearSelections) {
                            Text("Clear Selection")
                        }
                    }
                }
            }
            
            // Delete button - only show if messages are selected
            if (selectedCount > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                
                Button(
                    onClick = onDeleteSelected,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isDeleteInProgress && isDefaultSmsApp,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    if (isDeleteInProgress) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onError,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = if (isDeleteInProgress) "Deleting..." else "Delete Selected ($selectedCount)"
                    )
                }
                
                if (!isDefaultSmsApp) {
                    Text(
                        text = "Set TextShield as your default SMS app to delete messages",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SimpleMessageItem(
    message: SmsMessage,
    onClick: () -> Unit,
    onMarkAsSpam: (() -> Unit)? = null,
    onMarkAsNotSpam: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    isDefault: Boolean = false,
    isSelected: Boolean = false,
    onToggleSelection: (() -> Unit)? = null,
    showCheckbox: Boolean = false,
    unreadCount: Int = 0
) {
    var showActions by remember { mutableStateOf(false) }
    
    // Get dark theme state at the composable level
    val isDarkTheme = isSystemInDarkTheme()
    
    // Profile background color based on sender (consistent color for same sender)
    val avatarBackgroundColor = remember(message.sender) {
        // Generate a consistent color based on sender name
        val hash = message.sender.hashCode()
        val hue = (hash.absoluteValue % 360).toFloat()
        val saturation = 0.7f
        val lightness = if (isDarkTheme) 0.5f else 0.7f
        
        Color.hsv(hue = hue, saturation = saturation, value = lightness)
    }
    
    // Determine text color for avatar based on background color brightness
    val avatarTextColor = if (calculateLuminance(avatarBackgroundColor) > 0.5f) 
        Color.Black else Color.White
    
    // Determine item background color
    val backgroundColor = if (isDarkTheme) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    } else {
        MaterialTheme.colorScheme.surface
    }
    
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .background(
                    color = backgroundColor,
                    shape = RoundedCornerShape(0.dp)
                )
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Checkbox for selection in spam tab
                if (showCheckbox) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { if (onToggleSelection != null) onToggleSelection() },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
                
                // Contact initial with improved styling
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(avatarBackgroundColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = message.sender.firstOrNull()?.uppercase() ?: "?",
                        style = MaterialTheme.typography.titleMedium,
                        color = avatarTextColor
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                // Message content
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = message.sender,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        // Timestamp
                        Text(
                            text = formatTimestamp(message.timestamp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Message preview with preview text
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = message.content.let { 
                                if (it.length > 50) it.take(50) + "..." else it 
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        
                        // Show message count badge or spam indicator
                        if (message.isSpam) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.error),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "!",
                                    color = MaterialTheme.colorScheme.onError,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        } else if (unreadCount > 0) {
                            // Unread message count badge
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (unreadCount > 9) "9+" else unreadCount.toString(),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }
                }
                
                // Add more options menu
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = { showActions = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More Options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        // Add divider between messages
        Divider(
            modifier = Modifier.fillMaxWidth(),
            color = if (isDarkTheme) 
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else 
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
        )
    }
    
    // Message actions dialog
    if (showActions) {
        AlertDialog(
            onDismissRequest = { showActions = false },
            title = { Text("Message Actions") },
            text = { 
                Column {
                    Text("What would you like to do with messages from ${message.sender}?")
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    if (!message.isSpam) {
                        Text(
                            "Note: Actions will affect all messages from this sender",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        Text(
                            "Note: Actions will affect all messages from this sender",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                if (isDefault && onDelete != null) {
                    TextButton(
                        onClick = {
                            onDelete()
                            showActions = false
                        }
                    ) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            dismissButton = {
                if ((message.isSpam && onMarkAsNotSpam != null) || 
                    (!message.isSpam && onMarkAsSpam != null)) {
                    TextButton(
                        onClick = {
                            if (message.isSpam && onMarkAsNotSpam != null) {
                                onMarkAsNotSpam()
                            } else if (!message.isSpam && onMarkAsSpam != null) {
                                onMarkAsSpam()
                            }
                            showActions = false
                        }
                    ) {
                        Text(if (message.isSpam) "Not Spam" else "Mark as Spam")
                    }
                } else {
                    // Just close dialog if no action is available
                    TextButton(onClick = { showActions = false }) {
                        Text("Close")
                    }
                }
            }
        )
    }
}

@Composable
private fun SimpleChatBubble(
    message: SmsMessage,
    onDelete: () -> Unit,
    onMarkAsSpam: () -> Unit,
    isDefault: Boolean
) {
    var showActions by remember { mutableStateOf(false) }
    val isUserMessage = false // For illustrative purposes, all messages are from the contact
    
    // Define background colors with better contrast
    val backgroundColor = when {
        message.isSpam -> MaterialTheme.colorScheme.errorContainer
        isUserMessage -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    
    // Define dark red color for spam messages
    val DarkRed = Color(0xFF8B0000)
    
    // Ensure high contrast for text colors against their backgrounds
    val textColor = when {
        message.isSpam -> {
            // For spam messages, ensure error text is highly visible
            if (MaterialTheme.colorScheme.isLight()) {
                DarkRed
            } else {
                Color(0xFFFF8A80) // Lighter red for dark theme
            }
        }
        isUserMessage -> {
            // For user messages, ensure clear contrast against primary container
            MaterialTheme.colorScheme.onPrimaryContainer
        }
        else -> {
            // For regular messages, ensure good contrast against surface variant
            if (MaterialTheme.colorScheme.isLight()) {
                Color.Black
            } else {
                Color.White
            }
        }
    }
    
    // Timestamp color with better contrast (less translucent)
    val timestampColor = textColor.copy(alpha = 0.8f)
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = if (isUserMessage) Alignment.End else Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = if (isUserMessage) 16.dp else 0.dp,
                        topEnd = if (isUserMessage) 0.dp else 16.dp,
                        bottomStart = 16.dp,
                        bottomEnd = 16.dp
                    )
                )
                .background(backgroundColor)
                .clickable { showActions = true }
                .padding(12.dp)
        ) {
            Column {
                if (message.isSpam) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "⚠️ Potential spam",
                            color = textColor,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.width(4.dp))
                        
                        Text(
                            text = "(${(message.confidenceScore * 100).toInt()}% confidence)",
                            color = textColor,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                }
                
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = formatTimestamp(message.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.align(Alignment.End),
                    color = timestampColor
                )
            }
        }
        
        if (showActions) {
            Card(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .align(if (isUserMessage) Alignment.End else Alignment.Start),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(8.dp)
                ) {
                    // Show notification about affecting all messages
                    Text(
                        text = "Actions will affect all messages from this sender",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                    
                    Row(
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        TextButton(
                            onClick = {
                                onMarkAsSpam()
                                showActions = false
                            },
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(if (message.isSpam) "Not Spam" else "Mark as Spam")
                        }
                        
                        if (isDefault) {
                            TextButton(
                                onClick = {
                                    onDelete()
                                    showActions = false
                                }
                            ) {
                                Text("Delete", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}

// Extension function to check if the current theme is light or dark
@Composable
private fun ColorScheme.isLight(): Boolean {
    // Compare the brightness of background color to determine if we're in light mode
    return calculateLuminance(background) > 0.5f
}

// Function to calculate luminance from RGB color
private fun calculateLuminance(color: Color): Float {
    // Extract RGB components
    val red = color.red
    val green = color.green
    val blue = color.blue
    
    // Calculate relative luminance using the formula for perceived brightness
    // This formula gives more weight to green as human eyes are more sensitive to it
    return (0.299f * red + 0.587f * green + 0.114f * blue)
}

// Format timestamp according to the requirements:
// - If the message was received today, show the time (e.g., "3:45 PM")
// - If the message was received yesterday, show "Yesterday"
// - If from current year but not today/yesterday, show "MMM dd" (e.g., "May 19")
// - If from previous years, show "MMM dd, yyyy" (e.g., "May 19, 2023")
private fun formatTimestamp(timestamp: Long): String {
    val calendar = Calendar.getInstance()
    val now = calendar.clone() as Calendar
    
    calendar.timeInMillis = timestamp
    
    // Check if the message is from today
    if (calendar.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
        calendar.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)) {
        // Format as time: 3:45 PM 
        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        return timeFormat.format(Date(timestamp))
    }
    
    // Check if the message is from yesterday
    calendar.add(Calendar.DAY_OF_YEAR, 1)
    if (calendar.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
        calendar.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)) {
        return "Yesterday"
    }
    
    // Reset calendar to message date
    calendar.timeInMillis = timestamp
    
    // Check if the message is from current year
    return if (calendar.get(Calendar.YEAR) == now.get(Calendar.YEAR)) {
        // Format as MMM dd (e.g., "May 19")
        val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
        dateFormat.format(Date(timestamp))
    } else {
        // Format as MMM dd, yyyy (e.g., "May 19, 2023") for previous years
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        dateFormat.format(Date(timestamp))
    }
}

// Screens for navigation
sealed class Screen {
    object Inbox : Screen()
    object Spam : Screen()
    object Conversation : Screen()
}

// Extension property to get a darker surface color for app bars in dark theme
private val ColorScheme.surfaceDim: Color
    @Composable
    get() = if (isSystemInDarkTheme()) {
        Color(0xFF121212) // Darker surface for dark theme
    } else {
        surface // Regular surface for light theme
    }

// Function to convert HSV values to Color
private fun Color.Companion.hsv(hue: Float, saturation: Float, value: Float): Color {
    val h = hue / 60f
    val s = saturation
    val v = value
    
    val c = v * s
    val x = c * (1f - kotlin.math.abs((h % 2f) - 1f))
    val m = v - c
    
    val (r, g, b) = when {
        h < 1f -> Triple(c, x, 0f)
        h < 2f -> Triple(x, c, 0f)
        h < 3f -> Triple(0f, c, x)
        h < 4f -> Triple(0f, x, c)
        h < 5f -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    
    return Color(
        red = r + m,
        green = g + m,
        blue = b + m,
        alpha = 1f
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun SearchAppBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onCloseSearch: () -> Unit,
    focusRequester: FocusRequester
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    
    Surface(
        color = MaterialTheme.colorScheme.surfaceDim,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onCloseSearch) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Close Search",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            
            TextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester),
                placeholder = { Text("Search messages...") },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        keyboardController?.hide()
                    }
                ),
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear Search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            )
        }
    }
} 