package com.nxvpn.app.ui

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nxvpn.app.R
import com.nxvpn.app.data.ConfigImporter
import com.nxvpn.app.data.ProfileRepository
import com.nxvpn.app.data.VpnGateRepository
import com.nxvpn.app.data.model.ConnectionStatus
import com.nxvpn.app.data.model.ServerProfile
import com.nxvpn.app.data.model.TrafficStats
import com.nxvpn.app.vpn.VpnManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MainViewModel(
    application: Application,
    private val repository: ProfileRepository,
    private val vpnManager: VpnManager,
    private val vpnGate: VpnGateRepository = VpnGateRepository(),
) : AndroidViewModel(application) {

    private fun string(resId: Int, vararg args: Any): String =
        getApplication<Application>().getString(resId, *args)

    val profiles: StateFlow<List<ServerProfile>> =
        repository.profiles.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val status: StateFlow<ConnectionStatus> = vpnManager.status

    private val _traffic = MutableStateFlow(TrafficStats())
    val traffic: StateFlow<TrafficStats> = _traffic.asStateFlow()

    private val _snackbar = MutableStateFlow<String?>(null)
    val snackbar: StateFlow<String?> = _snackbar.asStateFlow()

    /** Ready-made public servers fetched from VPN Gate. */
    private val _freeServers = MutableStateFlow<List<ServerProfile>>(emptyList())
    val freeServers: StateFlow<List<ServerProfile>> = _freeServers.asStateFlow()

    private val _freeLoading = MutableStateFlow(false)
    val freeLoading: StateFlow<Boolean> = _freeLoading.asStateFlow()

    init {
        viewModelScope.launch {
            while (isActive) {
                if (status.value is ConnectionStatus.Connected) {
                    _traffic.value = vpnManager.currentTraffic()
                } else {
                    _traffic.value = TrafficStats()
                }
                delay(1_000)
            }
        }
    }

    fun importConfig(rawText: String, name: String?) {
        if (rawText.isBlank()) {
            _snackbar.value = string(R.string.msg_config_empty)
            return
        }
        ConfigImporter.fromText(rawText, name)
            .onSuccess { profile ->
                viewModelScope.launch {
                    repository.upsert(profile)
                    _snackbar.value = string(R.string.msg_imported, profile.name)
                }
            }
            .onFailure { _snackbar.value = string(R.string.msg_parse_failed) }
    }

    fun deleteProfile(id: String) {
        viewModelScope.launch { repository.delete(id) }
    }

    /** Loads the public VPN Gate server list. Safe to call repeatedly; the UI calls it lazily. */
    fun refreshFreeServers() {
        if (_freeLoading.value) return
        viewModelScope.launch {
            _freeLoading.value = true
            vpnGate.fetch()
                .onSuccess { servers ->
                    _freeServers.value = servers
                    if (servers.isEmpty()) _snackbar.value = string(R.string.msg_no_public_servers)
                }
                .onFailure { _snackbar.value = string(R.string.msg_load_failed) }
            _freeLoading.value = false
        }
    }

    /** Pushes a one-off message to the snackbar (used by the Activity's OpenVPN flow). */
    fun showMessage(message: String) { _snackbar.value = message }

    /** Called by the Activity once VPN consent has been granted. */
    fun connect(profile: ServerProfile) {
        viewModelScope.launch {
            runCatching { vpnManager.connect(profile, SystemClock.elapsedRealtime()) }
                .onFailure { _snackbar.value = it.message ?: string(R.string.msg_connect_failed) }
        }
    }

    fun disconnect() {
        viewModelScope.launch { vpnManager.disconnect() }
    }

    fun consumeSnackbar() { _snackbar.value = null }

    class Factory(
        private val application: Application,
        private val repository: ProfileRepository,
        private val vpnManager: VpnManager,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            MainViewModel(application, repository, vpnManager) as T
    }
}
