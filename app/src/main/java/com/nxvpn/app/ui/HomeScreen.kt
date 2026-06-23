package com.nxvpn.app.ui

import android.os.SystemClock
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.PublicOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nxvpn.app.data.model.ConnectionStatus
import com.nxvpn.app.data.model.ServerProfile
import com.nxvpn.app.data.model.TrafficStats
import com.nxvpn.app.ui.theme.NxGreen
import com.nxvpn.app.ui.theme.NxRed
import com.nxvpn.app.util.formatBytes
import com.nxvpn.app.util.formatDuration
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    status: ConnectionStatus,
    selectedProfile: ServerProfile?,
    traffic: TrafficStats,
    onConnect: (ServerProfile) -> Unit,
    onDisconnect: () -> Unit,
    onPickServer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isConnected = status is ConnectionStatus.Connected
    val isBusy = status is ConnectionStatus.Connecting || status is ConnectionStatus.Disconnecting
    val activeProfile = status.activeProfile ?: selectedProfile

    val buttonColor by animateColorAsState(
        targetValue = when {
            isConnected -> NxGreen
            isBusy -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.surfaceVariant
        },
        label = "buttonColor",
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(
            text = statusLabel(status),
            style = MaterialTheme.typography.titleMedium,
            color = when {
                isConnected -> NxGreen
                status is ConnectionStatus.Error -> NxRed
                else -> MaterialTheme.colorScheme.onSurface
            },
            modifier = Modifier.padding(top = 12.dp),
        )

        Box(
            modifier = Modifier
                .size(200.dp)
                .clip(CircleShape)
                .background(buttonColor.copy(alpha = 0.15f))
                .clickable(enabled = !isBusy) {
                    if (isConnected) onDisconnect()
                    else activeProfile?.let(onConnect) ?: onPickServer()
                },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .clip(CircleShape)
                    .background(buttonColor),
                contentAlignment = Alignment.Center,
            ) {
                if (isBusy) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 3.dp)
                } else {
                    Icon(
                        imageVector = Icons.Filled.Power,
                        contentDescription = if (isConnected) "Disconnect" else "Connect",
                        tint = Color.White,
                        modifier = Modifier.size(64.dp),
                    )
                }
            }
        }

        Text(
            text = if (isConnected) "Tap to disconnect" else "Tap to connect",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )

        Spacer(Modifier.height(4.dp))

        SelectedServerCard(activeProfile, onPickServer)

        if (isConnected && status is ConnectionStatus.Connected) {
            ConnectionStatsCard(connectedAtMillis = status.connectedAtMillis, traffic = traffic)
        }
    }
}

@Composable
private fun SelectedServerCard(profile: ServerProfile?, onPickServer: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPickServer),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (profile == null) {
                Icon(Icons.Filled.PublicOff, contentDescription = null)
                Spacer(Modifier.width(12.dp))
                Text("No server selected — tap to choose", style = MaterialTheme.typography.bodyLarge)
            } else {
                Text(profile.flagEmoji, fontSize = 28.sp)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(profile.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        profile.subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                }
            }
        }
    }
}

@Composable
private fun ConnectionStatsCard(connectedAtMillis: Long, traffic: TrafficStats) {
    var nowMillis by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(connectedAtMillis) {
        while (true) {
            nowMillis = SystemClock.elapsedRealtime()
            delay(1_000)
        }
    }
    val elapsedSeconds = (nowMillis - connectedAtMillis) / 1000

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Connected for", style = MaterialTheme.typography.bodyMedium)
            Text(
                formatDuration(elapsedSeconds),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                TrafficPill(Icons.Filled.ArrowDownward, "Download", formatBytes(traffic.rxBytes))
                TrafficPill(Icons.Filled.ArrowUpward, "Upload", formatBytes(traffic.txBytes))
            }
        }
    }
}

@Composable
private fun TrafficPill(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = label, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Column {
            Text(value, style = MaterialTheme.typography.titleMedium)
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        }
    }
}

private fun statusLabel(status: ConnectionStatus): String = when (status) {
    is ConnectionStatus.Connected -> "Connected"
    is ConnectionStatus.Connecting -> "Connecting…"
    is ConnectionStatus.Disconnecting -> "Disconnecting…"
    is ConnectionStatus.Error -> "Error: ${status.message}"
    ConnectionStatus.Disconnected -> "Not connected"
}
