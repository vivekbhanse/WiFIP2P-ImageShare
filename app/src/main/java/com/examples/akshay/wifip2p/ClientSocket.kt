package com.examples.akshay.wifip2p

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket

class ClientSocket(data1: String?) {

    private var socket: Socket? = null
    private val data: String = data1 ?: "null data"

    suspend fun sendData() {
        withContext(Dispatchers.IO) {
            val host = MainActivity.IP
            val port = 8888
            val buf = ByteArray(1024)

            try {
                socket = Socket().apply {
                    bind(null)
                    connect(InetSocketAddress(host, port), 500)
                }

                socket?.getOutputStream().use { outputStream ->
                    ByteArrayInputStream(data.toByteArray()).use { inputStream ->
                        var len: Int
                        while (inputStream.read(buf).also { len = it } != -1) {
                            outputStream?.write(buf, 0, len)
                        }
                    }
                }
            } catch (e: IOException) {
                Log.d(TAG, e.toString())
            } finally {
                socket?.takeIf { it.isConnected }?.apply {
                    try {
                        close()
                    } catch (e: IOException) {
                        Log.d(TAG, e.toString())
                    }
                }
            }
        }
    }

    companion object {
        private const val TAG = "===ClientSocket"
    }
}
