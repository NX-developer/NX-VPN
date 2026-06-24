package com.nxvpn.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nxvpn.app.data.model.ConnectionStatus
import com.nxvpn.app.data.model.ServerProfile
import com.nxvpn.app.ui.theme.NxGreen

@Composable
fun FreeServersScreen(
    servers: List<ServerProfile>,
    loading: Boolean,
    status: ConnectionStatus,
    onConnect: (ServerProfile) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val activeId = status.activeProfile?.id

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Ready-to-use servers", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    "Free public OpenVPN relays (VPN Gate). Needs the OpenVPN engine app.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.width(24.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(12.dp))
            } else {
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Refresh server list")
                }
            }
        }

        if (servers.isEmpty() && !loading) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("No public servers loaded", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.width(8.dp))
                Text(
                    "Tap refresh to fetch the latest free VPN Gate servers.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }
            return@Column
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp),
        ) {
            items(servers, key = { it.id }) { server ->
                FreeServerRow(
                    server = server,
                    isActive = server.id == activeId,
                    onConnect = { onConnect(server) },
                )
            }
        }
    }
}

@Composable
private fun FreeServerRow(
    server: ServerProfile,
    isActive: Boolean,
    onConnect: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onConnect),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) NxGreen.copy(alpha = 0.18f)
            else MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(server.flagEmoji, fontSize = 28.sp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(server.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    metricsLine(server),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }
            if (isActive) {
                Icon(Icons.Filled.CheckCircle, contentDescription = "Active", tint = NxGreen)
            }
        }
    }
}

private fun metricsLine(server: ServerProfile): String = buildString {
    append("OpenVPN")
    server.pingMs?.let { append(" · ").append(it).append(" ms") }
    server.speedBps?.let { bps ->
        val mbps = bps / 1_000_000.0
        append(" · ").append(String.format("%.1f", mbps)).append(" Mbps")
    }
}
