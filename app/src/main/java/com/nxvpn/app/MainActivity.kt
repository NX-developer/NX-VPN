package com.nxvpn.app

import android.Manifest
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.nxvpn.app.data.model.ServerProfile
import com.nxvpn.app.ui.MainViewModel
import com.nxvpn.app.ui.NxVpnApp
import com.nxvpn.app.ui.theme.NXVPNTheme

class MainActivity : ComponentActivity() {

    private val app get() = application as NxVpnApplication

    private val viewModel: MainViewModel by viewModels {
        MainViewModel.Factory(app.profileRepository, app.vpnManager)
    }

    /** Profile waiting for VPN consent to be granted. */
    private var pendingProfile: ServerProfile? = null

    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val profile = pendingProfile
        pendingProfile = null
        if (result.resultCode == RESULT_OK && profile != null) {
            viewModel.connect(profile)
        }
    }

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri ?: return@registerForActivityResult
        val text = runCatching {
            contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        }.getOrNull().orEmpty()
        val name = uri.lastPathSegment
            ?.substringAfterLast('/')
            ?.substringBeforeLast('.')
            .orEmpty()
        viewModel.importConfig(text, name)
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* result is informational; tunnel works regardless */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        maybeRequestNotificationPermission()

        setContent {
            NXVPNTheme {
                NxVpnApp(
                    viewModel = viewModel,
                    onConnectRequested = ::requestConnect,
                    onPickFile = { filePickerLauncher.launch("*/*") },
                )
            }
        }
    }

    /** Ensures VPN consent before asking the ViewModel to bring the tunnel up. */
    private fun requestConnect(profile: ServerProfile) {
        val consentIntent: Intent? = VpnService.prepare(this)
        if (consentIntent != null) {
            pendingProfile = profile
            vpnPermissionLauncher.launch(consentIntent)
        } else {
            viewModel.connect(profile)
        }
    }

    private fun maybeRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
