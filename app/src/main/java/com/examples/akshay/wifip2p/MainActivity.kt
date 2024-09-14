package com.examples.akshay.wifip2p

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.net.Uri
import android.net.wifi.WpsInfo
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.BitmapImageViewTarget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

class MainActivity : AppCompatActivity(), View.OnClickListener,
    WifiP2pManager.ConnectionInfoListener {

    private var manager: WifiP2pManager? = null
    private var channel: WifiP2pManager.Channel? = null
    private var receiver: WifiBroadcastReceiver? = null
    private var intentFilter: IntentFilter? = null
    private var device: WifiP2pDevice? = null

    private lateinit var buttonDiscoveryStart: Button
    private lateinit var buttonDiscoveryStop: Button
    private lateinit var buttonConnect: Button
    private lateinit var buttonServerStart: Button
    private lateinit var buttonServerStop: Button
    private lateinit var buttonClientStart: Button
    private lateinit var buttonClientStop: Button
    private lateinit var buttonConfigure: Button
    private lateinit var editTextTextInput: EditText

    private var serviceDiscovery: ServiceDiscovery? = null

    private lateinit var listViewDevices: ListView
    private lateinit var textViewDiscoveryStatus: TextView
    private lateinit var textViewWifiP2PStatus: TextView
    private lateinit var textViewConnectionStatus: TextView
    private lateinit var textViewReceivedData: TextView
    private lateinit var imgShowImageResult: ImageView
    private lateinit var textViewReceivedDataStatus: TextView
    private var serverSocketThread: ServerSocketThread? = null

    private var adapter: ArrayAdapter<String>? = null
    private var deviceListItems = mutableListOf<WifiP2pDevice>()
    private val PICK_IMAGE_REQUEST = 1
    var base64String = ""
    private val LOCATION_PERMISSIONS_REQUEST_CODE = 1
    private fun selectImage() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    private fun checkLocationPermissions() {
        val locationPermissionGranted = ContextCompat.checkSelfPermission(
            this, android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val wifiStatePermissionGranted = ContextCompat.checkSelfPermission(
            this, android.Manifest.permission.CHANGE_WIFI_STATE
        ) == PackageManager.PERMISSION_GRANTED

        val permissionsNeeded = mutableListOf<String>()

        if (!locationPermissionGranted) {
            permissionsNeeded.add(android.Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if (!wifiStatePermissionGranted) {
            permissionsNeeded.add(android.Manifest.permission.CHANGE_WIFI_STATE)
        }

        if (permissionsNeeded.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this, permissionsNeeded.toTypedArray(), LOCATION_PERMISSIONS_REQUEST_CODE
            )
        } else {
            // All permissions are granted, proceed with Wi-Fi P2P operations
            //onPermissionsGranted()
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        checkLocationPermissions()
        serviceDiscovery = ServiceDiscovery()
        setUpUI()

        manager = getSystemService(WIFI_P2P_SERVICE) as? WifiP2pManager
        channel = manager?.initialize(this, mainLooper, null)
        receiver = WifiBroadcastReceiver(manager, channel, this)

        serverSocketThread = ServerSocketThread()
    }

    override fun onResume() {
        super.onResume()
        setUpIntentFilter()
        registerReceiver(receiver, intentFilter)
    }


    override fun onPause() {
        super.onPause()
        unregisterReceiver(receiver)
    }

    private fun setUpIntentFilter() {
        intentFilter = IntentFilter().apply {
            addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
        }
    }

    private fun setUpUI() {
        buttonDiscoveryStart = findViewById(R.id.main_activity_button_discover_start)
        buttonDiscoveryStop = findViewById(R.id.main_activity_button_discover_stop)
        buttonConnect = findViewById(R.id.main_activity_button_connect)
        buttonServerStart = findViewById(R.id.main_activity_button_server_start)
        buttonServerStop = findViewById(R.id.main_activity_button_server_stop)
        buttonClientStart = findViewById(R.id.main_activity_button_client_start)
        buttonClientStop = findViewById(R.id.main_activity_button_client_stop)
        buttonConfigure = findViewById(R.id.main_activity_button_configure)
        listViewDevices = findViewById(R.id.main_activity_list_view_devices)
        textViewConnectionStatus = findViewById(R.id.main_activiy_textView_connection_status)
        textViewDiscoveryStatus = findViewById(R.id.main_activiy_textView_dicovery_status)
        textViewWifiP2PStatus = findViewById(R.id.main_activiy_textView_wifi_p2p_status)
        textViewReceivedData = findViewById(R.id.main_acitivity_data)
        textViewReceivedDataStatus = findViewById(R.id.main_acitivity_received_data)
        imgShowImageResult = findViewById(R.id.image)

        editTextTextInput = findViewById(R.id.main_acitivity_input_text)

        buttonServerStart.setOnClickListener(this)
        buttonServerStop.setOnClickListener(this)
        buttonClientStart.setOnClickListener(this)
        buttonClientStop.setOnClickListener(this)
        buttonConnect.setOnClickListener(this)
        buttonDiscoveryStop.setOnClickListener(this)
        buttonDiscoveryStart.setOnClickListener(this)
        buttonConfigure.setOnClickListener(this)

        buttonClientStop.visibility = View.INVISIBLE
        buttonClientStart.visibility = View.INVISIBLE
        buttonServerStop.visibility = View.INVISIBLE
        buttonServerStart.visibility = View.INVISIBLE
        editTextTextInput.visibility = View.INVISIBLE
        imgShowImageResult.visibility = View.GONE
        textViewReceivedDataStatus.visibility = View.INVISIBLE
        textViewReceivedData.visibility = View.INVISIBLE

        listViewDevices.onItemClickListener = OnItemClickListener { _, _, position, _ ->
            device = deviceListItems[position]
            Toast.makeText(this, "Selected device: ${device?.deviceName}", Toast.LENGTH_SHORT)
                .show()
        }
    }

    @SuppressLint("MissingPermission")
    private fun discoverPeers() {
        Log.d(TAG, "discoverPeers()")
        setDeviceList(mutableListOf())
        manager?.discoverPeers(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                stateDiscovery = true
                Log.d(TAG, "Peer discovery started")
                makeToast("Peer discovery started")
                manager?.requestPeers(channel, MyPeerListener(this@MainActivity))
            }

            override fun onFailure(reason: Int) {
                stateDiscovery = false
                val message = when (reason) {
                    WifiP2pManager.P2P_UNSUPPORTED -> "P2P_UNSUPPORTED"
                    WifiP2pManager.ERROR -> "ERROR"
                    WifiP2pManager.BUSY -> "BUSY"
                    else -> "Unknown Error"
                }
                Log.d(TAG, "Peer discovery failed: $message")
                makeToast("Peer discovery failed: $message")
            }
        })
    }

    private fun stopPeerDiscovery() {
        manager?.stopPeerDiscovery(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                stateDiscovery = false
                Log.d(TAG, "Peer discovery stopped")
                makeToast("Peer discovery stopped")
            }

            override fun onFailure(reason: Int) {
                Log.d(TAG, "Stopping peer discovery failed")
                makeToast("Stopping peer discovery failed")
            }
        })
    }

    private fun makeToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    @SuppressLint("MissingPermission")
    private fun connect(device: WifiP2pDevice) {
        val config = WifiP2pConfig().apply {
            deviceAddress = device.deviceAddress
            wps.setup = WpsInfo.PBC
        }

        Log.d(TAG, "Trying to connect: ${device.deviceName}")
        manager?.connect(channel, config, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.d(TAG, "Connected to: ${device.deviceName}")
                makeToast("Connection successful with ${device.deviceName}")
            }

            override fun onFailure(reason: Int) {
                val message = when (reason) {
                    WifiP2pManager.P2P_UNSUPPORTED -> "P2P_UNSUPPORTED"
                    WifiP2pManager.ERROR -> "ERROR"
                    WifiP2pManager.BUSY -> "BUSY"
                    else -> "Unknown Error"
                }
                Log.d(TAG, "Connection failed: $message")
                makeToast("Failed establishing connection: $message")
            }
        })
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            LOCATION_PERMISSIONS_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    // All requested permissions granted
                    //onPermissionsGranted()
                } else {
                    // Permissions denied
                    Toast.makeText(
                        this,
                        "Location and Wi-Fi permissions are required for this app.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    fun setDeviceList(deviceDetails: List<WifiP2pDevice>) {
        deviceListItems = deviceDetails.toMutableList()
        val deviceNames = deviceDetails.map { it.deviceName }
        adapter =
            ArrayAdapter(this, android.R.layout.simple_list_item_1, android.R.id.text1, deviceNames)
        listViewDevices.adapter = adapter
    }

    fun setStatusView(status: Int) {
        when (status) {
            Constants.DISCOVERY_INITATITED -> {
                stateDiscovery = true
                textViewDiscoveryStatus.text = "DISCOVERY_INITIATED"
            }

            Constants.DISCOVERY_STOPPED -> {
                stateDiscovery = false
                textViewDiscoveryStatus.text = "DISCOVERY_STOPPED"
            }

            Constants.P2P_WIFI_DISABLED -> {
                stateWifi = false
                textViewWifiP2PStatus.text = "P2P_WIFI_DISABLED"
                buttonDiscoveryStart.isEnabled = false
                buttonDiscoveryStop.isEnabled = false
            }

            Constants.P2P_WIFI_ENABLED -> {
                stateWifi = true
                textViewWifiP2PStatus.text = "P2P_WIFI_ENABLED"
                buttonDiscoveryStart.isEnabled = true
                buttonDiscoveryStop.isEnabled = true
            }

            Constants.NETWORK_CONNECT -> {
                stateConnection = true
                makeToast("It's a connect")
                textViewConnectionStatus.text = "Connected"
            }

            Constants.NETWORK_DISCONNECT -> {
                stateConnection = false
                textViewConnectionStatus.text = "Disconnected"
                makeToast("State is disconnected")
            }

            else -> Log.d(TAG, "Unknown status")
        }
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.main_activity_button_discover_start -> if (!stateDiscovery) discoverPeers()
            R.id.main_activity_button_discover_stop -> if (stateDiscovery) stopPeerDiscovery()
            R.id.main_activity_button_connect -> {
                if (device == null) {
                    makeToast("Please discover and select a device")
                    return
                }
                connect(device!!)
            }

            R.id.main_activity_button_server_start -> {
                lifecycleScope.launch(Dispatchers.IO) {
                    val serverSocketThread = ServerSocketThread().apply {
                        setUpdateListener(object : ServerSocketThread.OnUpdateListener {
                            override fun onUpdate(data: String) {
                                Log.d(TAG, "Received data: $data")
                                if (data.isNotEmpty()) {
                                    // Switch to Main thread to update UI
                                    lifecycleScope.launch(Dispatchers.Main) {
                                        createImageView(this@MainActivity, imageView = imgShowImageResult, base64String = data)
                                    }
                                }
                            }
                        })
                    }
                    serverSocketThread.startServer()
                }

            }

            R.id.main_activity_button_server_stop -> serverSocketThread?.apply {
                isInterrupted()
            }

            R.id.main_activity_button_client_start -> {
                selectImage()



            }

            R.id.main_activity_button_configure -> manager?.requestConnectionInfo(channel, this)
            R.id.main_activity_button_client_stop -> makeToast("Yet to do")
        }
    }

    override fun onConnectionInfoAvailable(info: WifiP2pInfo) {
        val hostAddress = info.groupOwnerAddress.hostAddress ?: "host is null"

        Log.d(TAG, "Group Owner Address: $hostAddress")
        IP = hostAddress
        IS_OWNER = info.isGroupOwner

        if (IS_OWNER) {
            buttonClientStop.visibility = View.GONE
            buttonClientStart.visibility = View.GONE
            editTextTextInput.visibility = View.GONE

            buttonServerStop.visibility = View.VISIBLE
            buttonServerStart.visibility = View.VISIBLE

            textViewReceivedData.visibility = View.VISIBLE
            textViewReceivedDataStatus.visibility = View.VISIBLE
        } else {
            buttonClientStart.visibility = View.VISIBLE
            editTextTextInput.visibility = View.VISIBLE
            buttonServerStop.visibility = View.GONE
            buttonServerStart.visibility = View.GONE
            textViewReceivedData.visibility = View.GONE
            textViewReceivedDataStatus.visibility = View.GONE
        }

        makeToast("Configuration Completed")
    }

    private fun setReceivedText(data: String?) {

        runOnUiThread { textViewReceivedData.text = data }
    }

    companion object {
        private const val TAG = "===MainActivity"
        var IP: String? = null
        var IS_OWNER: Boolean = false

        var stateDiscovery: Boolean = false
        var stateWifi: Boolean = false
        var stateConnection: Boolean = false
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.data != null) {
            val imageUri = data.data
            imageUri?.let {
                val byteArray = getByteArrayFromUri(it)

                base64String = byteArray?.let { it1 -> convertByteArrayToBase64(it1) }.toString()

                lifecycleScope.launch{
                    ClientSocket( base64String).sendData()
                }
            }
        }
    }

    private fun getByteArrayFromUri(uri: Uri): ByteArray? {
        return contentResolver.openInputStream(uri)?.use { inputStream ->
            ByteArrayOutputStream().use { byteArrayOutputStream ->
                val buffer = ByteArray(1024)
                var length: Int
                while (inputStream.read(buffer).also { length = it } != -1) {
                    byteArrayOutputStream.write(buffer, 0, length)
                }
                byteArrayOutputStream.toByteArray()
            }
        }
    }

    private fun convertByteArrayToBase64(byteArray: ByteArray): String {
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    suspend fun createImageView(context: Context, base64String: String, imageView: ImageView) {
        try {
            val bitmap = decodeBase64ToBitmap(base64String, 800, 800)
            Log.d(TAG, "createImageView: 459")
            if (bitmap != null) {
                withContext(Dispatchers.Main){
                    imgShowImageResult.visibility=View.VISIBLE
                    // Use Glide to handle bitmap loading into the ImageView
                    Glide.with(context).asBitmap().override(800, 800).load(bitmap)
                        .into(object : BitmapImageViewTarget(imageView) {
                        })
                }
            } else {
                Toast.makeText(context, "Failed to decode image", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error occurred: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // Suspend function to decode a Base64 string to a Bitmap
    suspend fun decodeBase64ToBitmap(base64String: String, maxWidth: Int, maxHeight: Int): Bitmap? {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "decodeBase64ToBitmap: 477")
                val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true

                    BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size, this)
                    Log.d(TAG, "decodeBase64ToBitmap: 484")
                    inSampleSize = calculateInSampleSize(this, maxWidth, maxHeight)
                    inJustDecodeBounds = false
                }
                Log.d(TAG, "decodeBase64ToBitmap: 485")
                BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size, options)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    // Function to calculate the sample size for image scaling
    fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        Log.d(TAG, "decodeBase64ToBitmap: 505")
        return inSampleSize
    }


    fun getBitmapFromDrawable(context: Context, drawableId: Int): Bitmap? {
        val drawable: Drawable? = ContextCompat.getDrawable(context, drawableId)
        return drawable?.toBitmap()
    }

    fun Drawable.toBitmap(): Bitmap {
        // Create a mutable bitmap with the same dimensions as the drawable
        val bitmap = Bitmap.createBitmap(intrinsicWidth, intrinsicHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        // Set bounds for the drawable to be drawn on the canvas
        setBounds(0, 0, canvas.width, canvas.height)
        // Draw the drawable on the canvas
        draw(canvas)
        return bitmap
    }
}

