package com.nxvpn.app

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.nxvpn.app.data.model.ServerProfile
import com.nxvpn.app.data.model.VpnProtocol
import com.nxvpn.app.ui.MainViewModel
import com.nxvpn.app.ui.NxVpnApp
import com.nxvpn.app.ui.theme.NXVPNTheme
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch

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

    /** Generic launcher used to await the OpenVPN engine's one-time consent dialogs. */
    private var pendingActivityResult: CompletableDeferred<Boolean>? = null
    private val openVpnConsentLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        pendingActivityResult?.complete(result.resultCode == RESULT_OK)
        pendingActivityResult = null
    }

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

    /** Routes a connect request to the right backend, handling each one's consent flow. */
    private fun requestConnect(profile: ServerProfile) {
        when (profile.protocol) {
            VpnProtocol.WIREGUARD -> {
                val consentIntent: Intent? = VpnService.prepare(this)
                if (consentIntent != null) {
                    pendingProfile = profile
                    vpnPermissionLauncher.launch(consentIntent)
                } else {
                    viewModel.connect(profile)
                }
            }
            VpnProtocol.OPENVPN -> connectOpenVpn(profile)
        }
    }

    /**
     * Brings up an OpenVPN tunnel through the installed OpenVPN for Android engine. Walks the two
     * one-time consent dialogs (external-API permission, then the system VPN dialog) before
     * handing over the inline config.
     */
    private fun connectOpenVpn(profile: ServerProfile) {
        val openVpn = app.vpnManager.openVpn
        if (!openVpn.isEngineInstalled()) {
            promptInstallEngine()
            return
        }
        lifecycleScope.launch {
            runCatching {
                openVpn.beginConnecting(profile)
                openVpn.awaitService()
                openVpn.prepareApiPermission()?.let { intent ->
                    if (!launchForOk(intent)) { openVpn.cancel(); return@launch }
                }
                openVpn.registerStatusCallback()
                openVpn.prepareVpnService()?.let { intent ->
                    if (!launchForOk(intent)) { openVpn.cancel(); return@launch }
                }
                openVpn.startVpn(profile)
            }.onFailure {
                openVpn.cancel()
                viewModel.showMessage(it.message ?: "OpenVPN connection failed")
            }
        }
    }

    /** Launches [intent] for result and suspends until the user responds. */
    private suspend fun launchForOk(intent: Intent): Boolean {
        val deferred = CompletableDeferred<Boolean>()
        pendingActivityResult = deferred
        openVpnConsentLauncher.launch(intent)
        return deferred.await()
    }

    private fun promptInstallEngine() {
        viewModel.showMessage("Install \"OpenVPN for Android\" to use these servers")
        runCatching {
            startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse("https://f-droid.org/packages/de.blinkt.openvpn/"))
            )
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
