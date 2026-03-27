package com.example.dynamiccallblocker

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<MainViewModel> {
        MainViewModelFactory(BlockRepository(applicationContext))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            App(viewModel)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun App(viewModel: MainViewModel) {
    val rules by viewModel.rules.collectAsState()
    val input by viewModel.inputNumber.collectAsState()

    val roleRequestLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { }

    val contactsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) viewModel.setAllowContacts(false)
    }

    val activity = androidx.compose.ui.platform.LocalContext.current as ComponentActivity

    LaunchedEffect(Unit) {
        val requestIntent = CallScreeningRoleHelper.createRequestIntent(activity)
        if (requestIntent != null && !CallScreeningRoleHelper.isRoleHeld(activity)) {
            roleRequestLauncher.launch(requestIntent)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val hasPermission = ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasPermission) {
                contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
            }
        }
    }

    var listType by rememberSaveable { mutableStateOf(ListType.BLOCK) }
    var matchMode by rememberSaveable { mutableStateOf(MatchMode.EXACT) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Dynamic Call Blocker") }) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "This app only handles block decisions. Caller ID and dialer UI remain in your existing phone apps.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            item {
                RoleStatusCard(isRoleHeld = CallScreeningRoleHelper.isRoleHeld(activity))
            }

            item {
                BlockingEnabledToggle(
                    enabled = rules.blockingEnabled,
                    onToggle = { viewModel.setBlockingEnabled(it) }
                )
            }

            item {
                ContactToggle(
                    enabled = rules.allowContacts,
                    onToggle = { viewModel.setAllowContacts(it) }
                )
            }

            item {
                RuleInputCard(
                    input = input,
                    onInputChange = viewModel::onInputChanged,
                    listType = listType,
                    onListTypeChanged = { listType = it },
                    matchMode = matchMode,
                    onMatchModeChanged = { matchMode = it },
                    onAdd = { viewModel.addRule(listType, matchMode) }
                )
            }

            item {
                Text("Block list", style = MaterialTheme.typography.titleMedium)
            }

            items(rules.blockExact.toList().sorted()) { number ->
                RuleRow(
                    label = "Exact",
                    number = number,
                    onDelete = { viewModel.removeRule(ListType.BLOCK, MatchMode.EXACT, number) }
                )
            }

            items(rules.blockPrefix.toList().sorted()) { number ->
                RuleRow(
                    label = "Starts with",
                    number = number,
                    onDelete = { viewModel.removeRule(ListType.BLOCK, MatchMode.PREFIX, number) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Allow list (exceptions)", style = MaterialTheme.typography.titleMedium)
            }

            items(rules.allowExact.toList().sorted()) { number ->
                RuleRow(
                    label = "Exact",
                    number = number,
                    onDelete = { viewModel.removeRule(ListType.ALLOW, MatchMode.EXACT, number) }
                )
            }

            items(rules.allowPrefix.toList().sorted()) { number ->
                RuleRow(
                    label = "Starts with",
                    number = number,
                    onDelete = { viewModel.removeRule(ListType.ALLOW, MatchMode.PREFIX, number) }
                )
            }
        }
    }
}

@Composable
private fun RoleStatusCard(isRoleHeld: Boolean) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Call screening role", style = MaterialTheme.typography.titleMedium)
            Text(
                if (isRoleHeld) {
                    "Active: this app can block incoming calls."
                } else {
                    "Not active yet. Accept the role request prompt to enable blocking."
                }
            )
        }
    }
}

@Composable
private fun ContactToggle(enabled: Boolean, onToggle: (Boolean) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Allow contacts through block list")
                Text("Enabled by default; contacts bypass block rules.", style = MaterialTheme.typography.bodySmall)
            }
            Switch(checked = enabled, onCheckedChange = onToggle)
        }
    }
}

@Composable
private fun BlockingEnabledToggle(enabled: Boolean, onToggle: (Boolean) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Enable call blocking")
                Text(
                    "Turn off to temporarily bypass all block rules.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Switch(checked = enabled, onCheckedChange = onToggle)
        }
    }
}

@Composable
private fun RuleInputCard(
    input: String,
    onInputChange: (String) -> Unit,
    listType: ListType,
    onListTypeChanged: (ListType) -> Unit,
    matchMode: MatchMode,
    onMatchModeChanged: (MatchMode) -> Unit,
    onAdd: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Add rule", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = input,
                onValueChange = onInputChange,
                label = { Text("Number or prefix") },
                singleLine = true
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = listType == ListType.BLOCK,
                    onClick = { onListTypeChanged(ListType.BLOCK) },
                    label = { Text("Block list") },
                    border = null,
                    colors = chipColors()
                )
                FilterChip(
                    selected = listType == ListType.ALLOW,
                    onClick = { onListTypeChanged(ListType.ALLOW) },
                    label = { Text("Allow list") },
                    border = null,
                    colors = chipColors()
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = matchMode == MatchMode.EXACT,
                    onClick = { onMatchModeChanged(MatchMode.EXACT) },
                    label = { Text("Exact") },
                    border = null,
                    colors = chipColors()
                )
                FilterChip(
                    selected = matchMode == MatchMode.PREFIX,
                    onClick = { onMatchModeChanged(MatchMode.PREFIX) },
                    label = { Text("Starts with") },
                    border = null,
                    colors = chipColors()
                )
            }
            Button(onClick = onAdd, modifier = Modifier.fillMaxWidth()) {
                Text("Add rule")
            }
        }
    }
}

@Composable
private fun RuleRow(label: String, number: String, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("$label: $number")
            AssistChip(onClick = onDelete, label = { Text("Delete") })
        }
    }
}

@Composable
private fun chipColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = MaterialTheme.colorScheme.primary,
    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
    containerColor = MaterialTheme.colorScheme.surfaceVariant,
    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
)
