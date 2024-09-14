package com.examples.akshay.wifip2p

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.io.IOException
import java.net.ServerSocket
import java.net.Socket

class ServerSocketThread {

    private var serverSocket: ServerSocket? = null
    private var receivedData: String = "null"
    private val port = 8888
    @Volatile private var interrupted = false
    private var listener: OnUpdateListener? = null

    interface OnUpdateListener {
        fun onUpdate(data: String)
    }

    fun setUpdateListener(listener: OnUpdateListener?) {
        this.listener = listener
    }

    suspend fun startServer() {
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Started DoInBackground")
                serverSocket = ServerSocket(port)

                while (!interrupted) {
                    val client: Socket = serverSocket!!.accept()
                    Log.d(TAG, "Accepted Connection")

                    client.getInputStream().use { inputStream ->
                        BufferedReader(InputStreamReader(inputStream)).use { bufferedReader ->
                            val sb = StringBuilder()
                            var line: String?
                            while (bufferedReader.readLine().also { line = it } != null) {
                                sb.append(line)
                            }
                            receivedData = sb.toString()
                        }
                    }

                    Log.d(TAG, "Completed ReceiveDataTask")
                    listener?.onUpdate(receivedData)
                    Log.d(TAG, " ================ $receivedData")
                }
                serverSocket?.close()
            } catch (e: IOException) {
                Log.d(TAG, "IOException occurred: ${e.message}")
            }
        }
    }

    fun isInterrupted(): Boolean = interrupted

    fun setInterrupted(interrupted: Boolean) {
        this.interrupted = interrupted
    }

    companion object {
        private const val TAG = "===ServerSocketThread"
    }
}
