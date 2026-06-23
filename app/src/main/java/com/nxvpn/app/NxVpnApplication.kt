package com.nxvpn.app

import android.app.Application
import com.nxvpn.app.data.ProfileRepository
import com.nxvpn.app.vpn.VpnManager

/** Holds process-wide singletons. Kept deliberately small instead of pulling in a DI framework. */
class NxVpnApplication : Application() {

    val profileRepository: ProfileRepository by lazy { ProfileRepository(this) }
    val vpnManager: VpnManager by lazy { VpnManager(this) }

    companion object {
        lateinit var instance: NxVpnApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}
