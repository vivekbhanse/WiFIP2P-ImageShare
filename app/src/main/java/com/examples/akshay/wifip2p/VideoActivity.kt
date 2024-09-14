package com.examples.akshay.wifip2p

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Size
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.io.OutputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.Executors

class VideoActivity : AppCompatActivity() {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
    private var outputStream: OutputStream? = null
    private var imageView: ImageView? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        enableEdgeToEdge()
        setContentView(R.layout.activity_video)
//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
//            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
//            insets
//        }
        imageView=findViewById(R.id.image)
        initVideoView()
    }

    private fun initVideoView() {
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build()
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            val imageAnalysis = ImageAnalysis.Builder()
                .setTargetResolution(Size(1280, 720))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            imageAnalysis.setAnalyzer(Executors.newSingleThreadExecutor(), { imageProxy ->
                val buffer = imageProxy.planes[0].buffer
                val byteArray = ByteArray(buffer.remaining())
                buffer.get(byteArray)

                // Send byteArray over the network to the other device
                sendFrame(byteArray)

                imageProxy.close()
            })

            cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis)
        }, ContextCompat.getMainExecutor(this))
    }

    private fun setupServerSocket() {
        val serverSocket = ServerSocket(8888)
        val clientSocket = serverSocket.accept()
        val outputStream = clientSocket.getOutputStream()

        // Continuously send video frames
        while (true) {
            val frame: ByteArray = getNextFrame()
            outputStream.write(frame)
            outputStream.flush()
        }
    }

    private fun connectToServer(groupOwnerAddress: InetAddress) {
        val clientSocket = Socket(groupOwnerAddress, 8888)
        val inputStream = clientSocket.getInputStream()

        val buffer = ByteArray(1024 * 1024) // Adjust size as necessary
        while (true) {
            val bytesRead = inputStream.read(buffer)
            if (bytesRead > 0) {
                val frame = buffer.copyOf(bytesRead)
                // Display the received frame
                displayFrame(frame)
            }
        }
    }

    private fun displayFrame(frame: ByteArray) {
        val bitmap = BitmapFactory.decodeByteArray(frame, 0, frame.size)
        runOnUiThread {
            imageView?.setImageBitmap(bitmap)
        }
    }
    private fun sendFrame(frameData: ByteArray) {
        Executors.newSingleThreadExecutor().execute {
            try {
                // Ensure outputStream is initialized
                if (outputStream != null) {
                    // Send frame size first
                    val frameSize = frameData.size
                    outputStream!!.write(frameSize)
                    outputStream!!.flush()

                    // Send frame data
                    outputStream!!.write(frameData)
                    outputStream!!.flush()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    private fun getNextFrame(): ByteArray {
        // Implement this method to retrieve the next frame of video data
        return ByteArray(0)
    }


}