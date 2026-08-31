package com.kosd.eventa.ui.event

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kosd.eventa.models.EventStaffMember
import com.kosd.eventa.models.InviteCode
import com.kosd.eventa.models.UserRole
import com.kosd.eventa.viewmodel.OrganizationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventStaffScreen(
    orgViewModel: OrganizationViewModel
) {
    val orgId = orgViewModel.activeOrg?.id ?: ""
    val staff by remember { derivedStateOf { orgViewModel.eventStaff } }
    val members by remember { derivedStateOf { orgViewModel.members } }
    val inviteCodes by remember { derivedStateOf { orgViewModel.inviteCodes } }
    val isLoading by remember { derivedStateOf { orgViewModel.isLoading } }
    val context = LocalContext.current
    var showCreateInviteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(orgId) {
        if (orgId.isNotEmpty()) {
            orgViewModel.loadEventStaff(orgId)
            orgViewModel.loadMembers(orgId)
            orgViewModel.loadInviteCodes(orgId)
        }
    }

    // Members who are eligible to be promoted (regular members only)
    val eligibleMembers by remember(members) {
        derivedStateOf {
            members.filter { it.role == UserRole.MEMBER }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Event Staff") })
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // Info card
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Event Staff Privileges", style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text("Grant event staff privileges to members so they can access Events mode and run kiosk check-ins. They cannot create events, view reports, or extend privileges.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }

            // ── Invite Members section ──────────────────────────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Invite Members", style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold)
                    FilledTonalButton(
                        onClick = { showCreateInviteDialog = true },
                        enabled = !isLoading
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("New Code")
                    }
                }
            }

            item {
                Text(
                    "Members join with an invite code, then you grant them event staff below.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Active invite codes
            if (inviteCodes.isNotEmpty()) {
                items(inviteCodes.filter { it.isActive }) { code ->
                    InviteCodeCard(
                        code = code,
                        onCopy = { copyToClipboard(context, code.code) },
                        onShare = { shareInvite(context, code.code) }
                    )
                }
            }

            item { HorizontalDivider() }

            // ── Current Event Staff ──────────────────────────────────────────
            item {
                Text("Current Event Staff (${staff.size})", style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold)
            }

            if (staff.isEmpty() && !isLoading) {
                item {
                    Text("No event staff assigned yet", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            items(staff) { member ->
                StaffRow(
                    member = member,
                    onRevoke = { orgViewModel.revokeEventStaff(member.memberId, orgId) }
                )
            }

            item { HorizontalDivider() }

            // ── Eligible Members ─────────────────────────────────────────────
            item {
                Text("Eligible Members (${eligibleMembers.size})", style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold)
            }

            if (eligibleMembers.isEmpty() && !isLoading) {
                item {
                    Text("No eligible members. Invite members to your organization first using a code above.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            items(eligibleMembers) { member ->
                val name = member.profile?.let { "${it.firstName} ${it.lastName}".trim() } ?: "Unknown"
                val email = member.profile?.email ?: ""
                MemberRow(
                    name = name,
                    email = email,
                    onGrant = { orgViewModel.grantEventStaff(member.id, orgId) }
                )
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }

    // ── Create Invite Code Dialog ────────────────────────────────────────────
    if (showCreateInviteDialog) {
        CreateInviteCodeDialog(
            viewModel = orgViewModel,
            orgId = orgId,
            onDismiss = { showCreateInviteDialog = false }
        )
    }

    // ── Success / Error dialogs ──────────────────────────────────────────────
    if (orgViewModel.showSuccess) {
        AlertDialog(
            onDismissRequest = { orgViewModel.dismissSuccess() },
            icon = { Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Success") },
            text = { Text(orgViewModel.successMessage ?: "") },
            confirmButton = { TextButton(onClick = { orgViewModel.dismissSuccess() }) { Text("OK") } }
        )
    }
    if (orgViewModel.showError) {
        AlertDialog(
            onDismissRequest = { orgViewModel.dismissError() },
            title = { Text("Error") },
            text = { Text(orgViewModel.errorMessage ?: "") },
            confirmButton = { TextButton(onClick = { orgViewModel.dismissError() }) { Text("OK") } }
        )
    }
}

@Composable
private fun InviteCodeCard(code: InviteCode, onCopy: () -> Unit, onShare: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.QrCode, contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer)
                Column {
                    Text(
                        code.code,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    val usesText = when {
                        code.maxUses == null -> "Unlimited uses"
                        else -> "${code.useCount}/${code.maxUses} used"
                    }
                    Text(usesText, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(0.7f))
                }
            }
            Row {
                IconButton(onClick = onCopy) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy",
                        tint = MaterialTheme.colorScheme.onTertiaryContainer)
                }
                IconButton(onClick = onShare) {
                    Icon(Icons.Default.Share, contentDescription = "Share",
                        tint = MaterialTheme.colorScheme.onTertiaryContainer)
                }
            }
        }
    }
}

@Composable
private fun StaffRow(member: EventStaffMember, onRevoke: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Default.Badge, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column {
                    Text(member.fullName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    Text(member.email, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(member.role.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary)
                }
            }
            IconButton(onClick = onRevoke) {
                Icon(Icons.Default.PersonRemove, contentDescription = "Revoke",
                    tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun MemberRow(name: String, email: String, onGrant: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text(email, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = onGrant) {
                Icon(Icons.Default.CheckCircle, contentDescription = null,
                    modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Grant")
            }
        }
    }
}

@Composable
private fun CreateInviteCodeDialog(
    viewModel: OrganizationViewModel,
    orgId: String,
    onDismiss: () -> Unit
) {
    var maxUses by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Invite Code", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Generate a code to invite new members to your organization. " +
                    "They'll join as regular members — you can then grant them event staff privileges.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = maxUses,
                    onValueChange = { maxUses = it.filter { c -> c.isDigit() } },
                    label = { Text("Max Uses (blank = unlimited)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val uses = maxUses.toIntOrNull()
                    viewModel.createInviteCode(orgId, uses, null, "member")
                    onDismiss()
                },
                enabled = !viewModel.isLoading
            ) {
                if (viewModel.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Create")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// ── Helpers ──────────────────────────────────────────────────────────────────

private fun copyToClipboard(context: Context, code: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Invite Code", code))
    Toast.makeText(context, "Code copied: $code", Toast.LENGTH_SHORT).show()
}

private fun shareInvite(context: Context, code: String) {
    val sendIntent = android.content.Intent().apply {
        action = android.content.Intent.ACTION_SEND
        putExtra(android.content.Intent.EXTRA_TEXT,
            "Join my organization on Eventa! Use invite code: $code")
        type = "text/plain"
    }
    context.startActivity(android.content.Intent.createChooser(sendIntent, "Share Invite Code"))
}
