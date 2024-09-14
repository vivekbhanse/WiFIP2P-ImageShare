package com.examples.akshay.wifip2p

import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pDeviceList
import android.net.wifi.p2p.WifiP2pManager.PeerListListener
import android.util.Log

/**
 * Created by ash on 14/2/18.
 */
class MyPeerListener(var mainActivity: MainActivity?) : PeerListListener {
    private val peers: List<WifiP2pDevice> = ArrayList()

    init {
        Log.d(TAG, "MyPeerListener object created")
    }


    override fun onPeersAvailable(wifiP2pDeviceList: WifiP2pDeviceList) {
        val deviceDetails = ArrayList<WifiP2pDevice>()

        Log.d(TAG, "OnPeerAvailable()")
        if (wifiP2pDeviceList != null) {
            if (wifiP2pDeviceList.deviceList.size == 0) {
                Log.d(TAG, "wifiP2pDeviceList size is zero")
                return
            }
            //  Log.d(MainActivity.TAG,"");
            for (device in wifiP2pDeviceList.deviceList) {
                deviceDetails.add(device)
                Log.d(TAG, "Found device :" + device.deviceName + " " + device.deviceAddress)
            }
            if (mainActivity != null) {
                mainActivity!!.setDeviceList(deviceDetails)
            }
        } else {
            Log.d(TAG, "wifiP2pDeviceList is null")
        }
    }

    companion object {
        const val TAG: String = "===MyPeerListener"
    }
}
