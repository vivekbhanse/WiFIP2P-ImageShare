package com.examples.akshay.wifip2p

import android.annotation.SuppressLint
import android.net.wifi.p2p.WifiP2pManager
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest
import android.util.Log

/**
 * Created by ash on 16/2/18.
 */
class ServiceDiscovery {
    private var serviceRequest: WifiP2pDnsSdServiceRequest? = null
    @SuppressLint("MissingPermission")
    fun discoverService(manager: WifiP2pManager, channel: WifiP2pManager.Channel?) {
        /*
         * Register listeners for DNS-SD services. These are callbacks invoked
         * by the system when a service is actually discovered.
         */

        manager.setDnsSdResponseListeners(channel,
            { instanceName, registrationType, srcDevice -> // A service has been discovered. Is this our app?
                // update the UI and add the item the discovered
                // device.
                Log.d(TAG, "=========================yessssssssss")
            }, { fullDomainName, record, device ->


                /**
                 * A new TXT record is available. Pick up the advertised
                 * buddy name.
                 */
                Log.d(
                    TAG,
                    device.deviceName + " is "
                            + record[TXTRECORD_PROP_AVAILABLE]
                )
            })

        // After attaching listeners, create a service request and initiate
        // discovery.
        serviceRequest = WifiP2pDnsSdServiceRequest.newInstance()
        manager.addServiceRequest(channel, serviceRequest,
            object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    Log.d(TAG, "Added service discovery request")
                }

                override fun onFailure(arg0: Int) {
                    Log.d(TAG, "Failed adding service discovery request")
                }
            })
        manager.discoverServices(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.d(TAG, "Service discovery initiated")
            }

            override fun onFailure(arg0: Int) {
                Log.d(TAG, "Service discovery failed")
            }
        })
    }


    @SuppressLint("MissingPermission")
    fun startRegistrationAndDiscovery(manager: WifiP2pManager, channel: WifiP2pManager.Channel?) {
        val record: MutableMap<String, String> = HashMap()
        record[TXTRECORD_PROP_AVAILABLE] = "visible"

        val service = WifiP2pDnsSdServiceInfo.newInstance(
            SERVICE_INSTANCE, SERVICE_REG_TYPE, record
        )
        manager.addLocalService(channel, service, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.d(TAG, "Added Local Service")
            }

            override fun onFailure(error: Int) {
                Log.d(TAG, "Failed to add a service")
            }
        })
    }

    companion object {
        private const val TAG = "===ServiceDiscovery"
        private const val SERVER_PORT = 4545

        const val TXTRECORD_PROP_AVAILABLE: String = "available"
        const val SERVICE_INSTANCE: String = "_wifidemotest"
        const val SERVICE_REG_TYPE: String = "_presence._tcp"
    }
}
