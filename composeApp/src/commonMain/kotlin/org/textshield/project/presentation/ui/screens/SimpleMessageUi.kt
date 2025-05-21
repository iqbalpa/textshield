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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
    
    // Automatically show spam tab if there are spam messages
    LaunchedEffect(state.spamMessages) {
        if (state.spamMessages.isNotEmpty()) {
            viewModel.setCurrentTab(InboxTab.SPAM)
        }
    }
    
    // Set the current screen based on the selected tab
    var currentScreen by remember { 
        mutableStateOf<Screen>(
            if (state.currentTab == InboxTab.SPAM) Screen.Spam else Screen.Inbox
        ) 
    }
    
    // Update the current screen when tab changes
    LaunchedEffect(state.currentTab) {
        currentScreen = when (state.currentTab) {
            InboxTab.INBOX -> Screen.Inbox
            InboxTab.SPAM -> Screen.Spam
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
                        // App bar - updated to match reference design
                        ModernAppBar(
                            title = "Messages",
                            currentTab = InboxTab.INBOX,
                            onTabChanged = { 
                                viewModel.setCurrentTab(it)
                                currentScreen = when(it) {
                                    InboxTab.INBOX -> Screen.Inbox
                                    InboxTab.SPAM -> Screen.Spam
                                }
                            },
                            onRefresh = { viewModel.loadMessages() }
                        )
                        
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
                        } else if (messagesBySender.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Your inbox is empty",
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
                                    messagesBySender.map { (sender, messages) -> 
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
                                        onMarkAsSpam = { viewModel.markMessageSpamStatus(message.id, true) },
                                        onDelete = { viewModel.performSpamAction(message.id, SpamAction.REMOVED) },
                                        isDefault = state.isDefaultSmsApp,
                                        unreadCount = unreadCount
                                    )
                                }
                            }
                        }
                    }
                }
                
                is Screen.Spam -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // App bar - updated to match reference design
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
                            onRefresh = { viewModel.loadMessages() }
                        )
                        
                        // Default SMS app warning
                        if (!state.isDefaultSmsApp) {
                            DefaultSmsAppWarning(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                        
                        // Settings action selector
                        SpamActionSelector(
                            currentAction = state.defaultSpamAction,
                            onActionSelected = { viewModel.setDefaultSpamAction(it) }
                        )
                        
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
                        } else if (spamMessagesBySender.isEmpty()) {
                            SpamEmptyState(
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(vertical = 8.dp)
                            ) {
                                items(
                                    spamMessagesBySender.map { (sender, messages) -> 
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
                                        onMarkAsNotSpam = { viewModel.markMessageSpamStatus(message.id, false) },
                                        onDelete = { viewModel.performSpamAction(message.id, SpamAction.REMOVED) },
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
                        // Conversation app bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { 
                                    // Just go back without clearing viewed state
                                    currentScreen = if (selectedConversationMessages.any { it.isSpam }) Screen.Spam else Screen.Inbox 
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = "Back",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            
                            Text(
                                text = selectedConversationId ?: "Conversation",
                                style = MaterialTheme.typography.titleMedium
                            )
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
                                        viewModel.performSpamAction(message.id, SpamAction.REMOVED)
                                        // If this was the last message, go back to the previous screen
                                        if (selectedConversationMessages.size <= 1) {
                                            currentScreen = if (message.isSpam) Screen.Spam else Screen.Inbox
                                        }
                                    },
                                    onMarkAsSpam = { viewModel.markMessageSpamStatus(message.id, !message.isSpam) },
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
    onRefresh: () -> Unit
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
                    IconButton(onClick = { /* Search functionality */ }) {
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
    modifier: Modifier = Modifier
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
                text = "No spam messages",
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
private fun SpamActionSelector(
    currentAction: SpamAction,
    onActionSelected: (SpamAction) -> Unit
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
            Text(
                text = "Default action for detected spam:",
                style = MaterialTheme.typography.bodyLarge
            )
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = currentAction == SpamAction.MARKED,
                    onClick = { onActionSelected(SpamAction.MARKED) }
                )
                Text(
                    text = "Mark as spam",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.clickable { onActionSelected(SpamAction.MARKED) }
                )
                
                Spacer(modifier = Modifier.width(24.dp))
                
                RadioButton(
                    selected = currentAction == SpamAction.REMOVED,
                    onClick = { onActionSelected(SpamAction.REMOVED) }
                )
                Text(
                    text = "Remove message",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.clickable { onActionSelected(SpamAction.REMOVED) }
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
            text = { Text("What would you like to do with this message?") },
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
                Row(
                    modifier = Modifier.padding(8.dp)
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