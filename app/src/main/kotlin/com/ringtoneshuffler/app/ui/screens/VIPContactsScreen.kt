package com.ringtoneshuffler.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import android.content.ContentUris
import android.provider.ContactsContract
import com.ringtoneshuffler.app.data.VipContact
import com.ringtoneshuffler.app.ui.theme.*
import com.ringtoneshuffler.app.viewmodel.ShufflerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VIPContactsScreen(
    viewModel: ShufflerViewModel,
    onNavigateBack: () -> Unit
) {
    val vipContacts by viewModel.vipContactsList.collectAsState(initial = emptyList())
    val context = LocalContext.current
    
    // State to hold the selected contact URI temporarily while we pick the audio file
    var pendingContactUri by remember { mutableStateOf<Uri?>(null) }

    val audioPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null && pendingContactUri != null) {
            viewModel.setContactRingtone(context, pendingContactUri!!, uri)
        }
        pendingContactUri = null
    }

    val contactPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact()
    ) { uri: Uri? ->
        if (uri != null) {
            pendingContactUri = uri
            audioPicker.launch("audio/*")
        }
    }

    Scaffold(
        containerColor = SurfaceObsidian,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "VIP Contacts",
                        style = MaterialTheme.typography.headlineMedium,
                        color = ElectricVioletLight
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = OnSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceObsidian)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { contactPicker.launch(null) },
                shape = CircleShape,
                containerColor = ElectricViolet,
                contentColor = GlassWhite,
                elevation = FloatingActionButtonDefaults.elevation(8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add VIP Contact")
            }
        }
    ) { paddingValues ->
        if (vipContacts.isEmpty()) {
            EmptyVIPHint(modifier = Modifier.padding(paddingValues))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { Spacer(Modifier.height(8.dp)) }
                
                items(vipContacts, key = { it.id }) { contact ->
                    VIPContactCard(
                        contact = contact,
                        onEdit = {
                            pendingContactUri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contact.id.toLong())
                            audioPicker.launch("audio/*")
                        },
                        onDelete = { viewModel.clearContactRingtone(contact.id) }
                    )
                }
                
                item { Spacer(Modifier.height(80.dp)) } // Space for FAB
            }
        }
    }
}

@Composable
fun VIPContactCard(contact: VipContact, onEdit: () -> Unit, onDelete: () -> Unit) {
    val ringtoneName = contact.ringtoneUri?.let { uriString ->
        com.ringtoneshuffler.app.utils.rememberDisplayName(android.net.Uri.parse(uriString))
    } ?: "Unknown"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceContainer)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(SurfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Person, contentDescription = null, tint = ElectricVioletLight)
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = contact.name,
                style = MaterialTheme.typography.titleMedium,
                color = OnSurface,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = ringtoneName,
                style = MaterialTheme.typography.bodySmall,
                color = SignalEmerald,
                maxLines = 1
            )
        }
        
        Row {
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Change Ringtone", tint = ElectricVioletLight)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Remove VIP", tint = ErrorRed)
            }
        }
    }
}

@Composable
private fun EmptyVIPHint(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                Icons.Default.Person, 
                contentDescription = null, 
                tint = OnSurfaceVariant,
                modifier = Modifier.size(64.dp)
            )
            Text(
                "No VIP Contacts",
                style = MaterialTheme.typography.headlineSmall,
                color = OnSurface
            )
            Text(
                "Tap the + button to assign a special ringtone to a contact.",
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
