package com.peterlaurence.trekme.core.wifip2p

import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDeviceList
import android.net.wifi.p2p.WifiP2pManager
import android.net.wifi.p2p.nsd.WifiP2pServiceInfo
import android.net.wifi.p2p.nsd.WifiP2pServiceRequest
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

suspend fun WifiP2pManager.requestPeers(c: WifiP2pManager.Channel): WifiP2pDeviceList? = suspendCoroutine { continuation ->
    val wrapper = WifiP2pManager.PeerListListener { peers -> continuation.resume(peers) }
    requestPeers(c, wrapper)
}

suspend fun WifiP2pManager.addLocalService(c: WifiP2pManager.Channel, servInfo: WifiP2pServiceInfo): Boolean = suspendCoroutine { cont ->
    val listener = object : WifiP2pManager.ActionListener {
        override fun onSuccess() {
            cont.resume(true)
        }

        override fun onFailure(reason: Int) {
            if (reason == WifiP2pManager.P2P_UNSUPPORTED) {
                cont.resumeWithException(IllegalStateException())
            } else cont.resume(false)
        }
    }
    addLocalService(c, servInfo, listener)
}

suspend fun WifiP2pManager.clearLocalServices(c: WifiP2pManager.Channel?): Boolean = suspendCoroutine { cont ->
    if (c == null) {
        return@suspendCoroutine
    }
    val listener = object : WifiP2pManager.ActionListener {
        override fun onSuccess() {
            cont.resume(true)
        }

        override fun onFailure(reason: Int) {
            cont.resume(false)
        }
    }
    clearLocalServices(c, listener)
}

suspend fun WifiP2pManager.clearServiceRequests(c: WifiP2pManager.Channel?): Boolean = suspendCoroutine { cont ->
    if (c == null) {
        return@suspendCoroutine
    }
    val listener = object : WifiP2pManager.ActionListener {
        override fun onSuccess() {
            cont.resume(true)
        }

        override fun onFailure(reason: Int) {
            cont.resume(false)
        }
    }
    clearServiceRequests(c, listener)
}

suspend fun WifiP2pManager.cancelConnect(c: WifiP2pManager.Channel): Boolean = suspendCoroutine { cont ->
    val listener = object : WifiP2pManager.ActionListener {
        override fun onSuccess() {
            cont.resume(true)
        }

        override fun onFailure(reason: Int) {
            cont.resume(false)
        }
    }
    cancelConnect(c, listener)
}

suspend fun WifiP2pManager.removeGroup(c: WifiP2pManager.Channel): Boolean = suspendCoroutine { cont ->
    val listener = object : WifiP2pManager.ActionListener {
        override fun onSuccess() {
            cont.resume(true)
        }

        override fun onFailure(reason: Int) {
            cont.resume(false)
        }
    }
    removeGroup(c, listener)
}

suspend fun WifiP2pManager.stopPeerDiscovery(c: WifiP2pManager.Channel?): Boolean = suspendCoroutine { cont ->
    if (c == null) {
        return@suspendCoroutine
    }
    val listener = object : WifiP2pManager.ActionListener {
        override fun onSuccess() {
            cont.resume(true)
        }

        override fun onFailure(reason: Int) {
            cont.resume(false)
        }
    }
    stopPeerDiscovery(c, listener)
}

suspend fun WifiP2pManager.discoverPeers(c: WifiP2pManager.Channel?): Boolean = suspendCoroutine { cont ->
    if (c == null) {
        return@suspendCoroutine
    }
    val listener = object : WifiP2pManager.ActionListener {
        override fun onSuccess() {
            cont.resume(true)
        }

        override fun onFailure(reason: Int) {
            if (reason == WifiP2pManager.P2P_UNSUPPORTED) {
                cont.resumeWithException(IllegalStateException())
            } else cont.resume(false)
        }
    }
    discoverPeers(c, listener)
}

suspend fun WifiP2pManager.addServiceRequest(c: WifiP2pManager.Channel, req: WifiP2pServiceRequest): Boolean = suspendCoroutine { cont ->
    val listener = object : WifiP2pManager.ActionListener {
        override fun onSuccess() {
            cont.resume(true)
        }

        override fun onFailure(reason: Int) {
            cont.resume(false)
        }
    }
    addServiceRequest(c, req, listener)
}

suspend fun WifiP2pManager.discoverServices(c: WifiP2pManager.Channel): Boolean = suspendCoroutine { cont ->
    val listener = object : WifiP2pManager.ActionListener {
        override fun onSuccess() {
            cont.resume(true)
        }

        override fun onFailure(reason: Int) {
            if (reason == WifiP2pManager.P2P_UNSUPPORTED) {
                cont.resumeWithException(IllegalStateException())
            } else cont.resume(false)
        }
    }
    discoverServices(c, listener)
}

suspend fun WifiP2pManager.connect(c: WifiP2pManager.Channel, conf: WifiP2pConfig): Boolean = suspendCoroutine { cont ->
    val listener = object : WifiP2pManager.ActionListener {
        override fun onSuccess() {
            cont.resume(true)
        }

        override fun onFailure(reason: Int) {
            cont.resume(false)
        }
    }
    connect(c, conf, listener)
}