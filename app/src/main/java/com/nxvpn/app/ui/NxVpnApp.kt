package com.nxvpn.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nxvpn.app.R
import com.nxvpn.app.data.model.ConnectionStatus
import com.nxvpn.app.data.model.ServerProfile

private enum class Tab(val labelRes: Int) {
    HOME(R.string.nav_home),
    FREE(R.string.nav_free),
    SERVERS(R.string.nav_servers),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NxVpnApp(
    viewModel: MainViewModel,
    onConnectRequested: (ServerProfile) -> Unit,
    onPickFile: () -> Unit,
) {
    val status by viewModel.status.collectAsStateWithLifecycle()
    val profiles by viewModel.profiles.collectAsStateWithLifecycle()
    val traffic by viewModel.traffic.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbar.collectAsStateWithLifecycle()
    val freeServers by viewModel.freeServers.collectAsStateWithLifecycle()
    val freeLoading by viewModel.freeLoading.collectAsStateWithLifecycle()

    var tab by remember { mutableStateOf(Tab.HOME) }
    var showImport by remember { mutableStateOf(false) }
    var selectedId by remember { mutableStateOf<String?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeSnackbar()
        }
    }

    // Lazily fetch the public server list the first time the user opens the Free tab.
    LaunchedEffect(tab) {
        if (tab == Tab.FREE && freeServers.isEmpty() && !freeLoading) {
            viewModel.refreshFreeServers()
        }
    }

    val selectedProfile: ServerProfile? = status.activeProfile
        ?: profiles.firstOrNull { it.id == selectedId }
        ?: profiles.firstOrNull()

    Scaffold(
        topBar = { TopAppBar(title = { Text("NX-VPN") }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == Tab.HOME,
                    onClick = { tab = Tab.HOME },
                    icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                    label = { Text(stringResource(Tab.HOME.labelRes)) },
                )
                NavigationBarItem(
                    selected = tab == Tab.FREE,
                    onClick = { tab = Tab.FREE },
                    icon = { Icon(Icons.Filled.Public, contentDescription = null) },
                    label = { Text(stringResource(Tab.FREE.labelRes)) },
                )
                NavigationBarItem(
                    selected = tab == Tab.SERVERS,
                    onClick = { tab = Tab.SERVERS },
                    icon = { Icon(Icons.Filled.Dns, contentDescription = null) },
                    label = { Text(stringResource(Tab.SERVERS.labelRes)) },
                )
            }
        },
        floatingActionButton = {
            if (tab == Tab.SERVERS) {
                FloatingActionButton(onClick = { showImport = true }) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.import_title))
                }
            }
        },
    ) { innerPadding ->
        when (tab) {
            Tab.HOME -> HomeScreen(
                status = status,
                selectedProfile = selectedProfile,
                traffic = traffic,
                onConnect = onConnectRequested,
                onDisconnect = viewModel::disconnect,
                onPickServer = { tab = Tab.SERVERS },
                modifier = Modifier.padding(innerPadding),
            )
            Tab.FREE -> FreeServersScreen(
                servers = freeServers,
                loading = freeLoading,
                status = status,
                onConnect = { profile ->
                    selectedId = profile.id
                    if (status.activeProfile?.id == profile.id &&
                        (status is ConnectionStatus.Connected || status is ConnectionStatus.Connecting)
                    ) {
                        viewModel.disconnect()
                    } else {
                        onConnectRequested(profile)
                    }
                },
                onRefresh = viewModel::refreshFreeServers,
                modifier = Modifier.padding(innerPadding),
            )
            Tab.SERVERS -> ServersScreen(
                profiles = profiles,
                status = status,
                onConnect = { profile ->
                    selectedId = profile.id
                    if (status is ConnectionStatus.Connected && status.activeProfile?.id == profile.id) {
                        viewModel.disconnect()
                    } else {
                        onConnectRequested(profile)
                    }
                },
                onDelete = { viewModel.deleteProfile(it.id) },
                modifier = Modifier.padding(innerPadding),
            )
        }
    }

    if (showImport) {
        ImportDialog(
            onDismiss = { showImport = false },
            onImportText = { config, name ->
                viewModel.importConfig(config, name)
                showImport = false
            },
            onPickFile = {
                showImport = false
                onPickFile()
            },
        )
    }
}
