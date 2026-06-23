package com.nxvpn.app.ui

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nxvpn.app.data.ConfigImporter
import com.nxvpn.app.data.ProfileRepository
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
    private val repository: ProfileRepository,
    private val vpnManager: VpnManager,
) : ViewModel() {

    val profiles: StateFlow<List<ServerProfile>> =
        repository.profiles.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val status: StateFlow<ConnectionStatus> = vpnManager.status

    private val _traffic = MutableStateFlow(TrafficStats())
    val traffic: StateFlow<TrafficStats> = _traffic.asStateFlow()

    private val _snackbar = MutableStateFlow<String?>(null)
    val snackbar: StateFlow<String?> = _snackbar.asStateFlow()

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
        ConfigImporter.fromText(rawText, name)
            .onSuccess { profile ->
                viewModelScope.launch {
                    repository.upsert(profile)
                    _snackbar.value = "Imported \"${profile.name}\""
                }
            }
            .onFailure { _snackbar.value = it.message ?: "Could not parse config" }
    }

    fun deleteProfile(id: String) {
        viewModelScope.launch { repository.delete(id) }
    }

    /** Called by the Activity once VPN consent has been granted. */
    fun connect(profile: ServerProfile) {
        viewModelScope.launch {
            runCatching { vpnManager.connect(profile, SystemClock.elapsedRealtime()) }
                .onFailure { _snackbar.value = it.message ?: "Connection failed" }
        }
    }

    fun disconnect() {
        viewModelScope.launch { vpnManager.disconnect() }
    }

    fun consumeSnackbar() { _snackbar.value = null }

    class Factory(
        private val repository: ProfileRepository,
        private val vpnManager: VpnManager,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            MainViewModel(repository, vpnManager) as T
    }
}
