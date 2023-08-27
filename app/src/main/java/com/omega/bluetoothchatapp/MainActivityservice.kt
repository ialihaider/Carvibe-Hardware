package com.omega.bluetoothchatapp

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.*
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.location.*
import com.google.android.material.snackbar.Snackbar
import com.jjoe64.graphview.Viewport
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import com.omega.bluetoothchatapp.databinding.ActivityMainBinding
import com.omega.bluetoothchatapp.databinding.ItemMessageBinding
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
//import com.xwray.groupie.viewbinding.BindableItem
//import org.deeplearning4j.nn.api.NeuralNetwork
//import org.deeplearning4j.nn.conf.layers.ActivationLayer
//import org.deeplearning4j.nn.conf.layers.DenseLayer
//import org.nd4j.linalg.activations.Activation
//import org.tensorflow.lite.Tensor
import java.io.*
import java.lang.Math.sqrt
import java.text.SimpleDateFormat
import java.util.*

class MainActivityservice : Service() {

// new try variable
        private var xAccel = 0f
        private var yAccel = 0f
        private var zAccel = 0f

        private var lastXAccel = 0f
        private var lastYAccel = 0f
        private var lastZAccel = 0f

        private var lastAccel = 0f

        private var lastUpdateTime: Long = 0
        private var newlastUpdateTime: Long = 0
        private val updateInterval = 100 // Update interval in milliseconds

    // previous try variable
        private val TAG: String = "MAIN"
        private var bluetoothAdapter: BluetoothAdapter? = null
        private val mUUID = UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66")
        private var mPairedDevices = listOf<BluetoothDevice>()
        private lateinit var mMessagesAdapter: GroupAdapter<GroupieViewHolder>
        private lateinit var mHandler: Handler
        private var fusedLocationClient: FusedLocationProviderClient? = null
        private var mSensorManager: SensorManager? = null
        private var mAccelerometer: Sensor? = null
        val alpha = 0.6f
        val verticalMovementThreshold = 0.25f
        val horizontalMovementThreshold = 0.25f
        val acceleration = DoubleArray(9)
        private val accelerationArray = FloatArray(10)
        private var accelerationIndex = 0
        private var isAccidentDetected = false
        private val sensorHandler = Handler()
        private var lastX = 0.0
        private var lastY = 0.0
        private var lastZ = 0.0
//        private var xAccel = 0.0
//        private var yAccel = 0.0
//        private var zAccel = 0.0
        private var detect = 0.0

    private val sensorRunnable = object : Runnable {
        override fun run() {
            val mSensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
            mSensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER).forEach {
                if (it.type == Sensor.TYPE_ACCELEROMETER) {
                    mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let { sensor ->
                        mSensorManager.registerListener(null, sensor, SensorManager.SENSOR_DELAY_FASTEST)
                        mSensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER).lastOrNull()?.let { lastSensor ->
                            mSensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER).forEach { registeredSensor ->
                                mSensorManager.unregisterListener(null, registeredSensor)
                            }
                            mSensorManager.registerListener(object : SensorEventListener {
                                override fun onSensorChanged(event: SensorEvent) {
                                    /*
//                                    acceleration[0] = event.values[0].toDouble()
//                                    acceleration[1] = event.values[1].toDouble()
//                                    acceleration[2] = event.values[2].toDouble()
//                                    accelerationArray[accelerationIndex] = event.values[0]
//                                    accelerationIndex = (accelerationIndex + 1) % 10
//                                    mSensorManager.unregisterListener(this)
//                                    detect = lastValueAccidentDetection(acceleration[0].toFloat(), acceleration[1].toFloat(),acceleration[2].toFloat())
//                                    calculateAcceleration(acceleration[0].toFloat(), acceleration[0].toFloat(), acceleration[0].toFloat())
//                                    detectAccident()

                                     */
                                    if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
                                        acceleration[0] = event.values[0].toDouble()
                                        acceleration[1] = event.values[1].toDouble()
                                        acceleration[2] = event.values[2].toDouble()
                                        accelerationArray[accelerationIndex] = event.values[0]
                                        accelerationIndex = (accelerationIndex + 1) % 10
                                        mSensorManager.unregisterListener(this)
                                        val currentTime = System.currentTimeMillis()
                                        val deltaTime = currentTime - lastUpdateTime
                                        calculateAcceleration(acceleration[0].toFloat(), acceleration[0].toFloat(), acceleration[0].toFloat())
//                                        val elapsedTime = currentTime - lastUpdateTime
                                        if (deltaTime > updateInterval) {
                                            lastUpdateTime = currentTime
                                            /*
                                            // Calculate change in acceleration over time
//                                            deltaXA = Math.abs(xAccel - lastXAccel) / elapsedTime
//                                            deltaYA = Math.abs(yAccel - lastYAccel) / elapsedTime
//                                            val deltaZA = Math.abs(zAccel - lastZAccel) / elapsedTime

                                            // Detect rapid changes in acceleration
//                                            val rapidAccelerationThreshold = 8.5f // adjust as needed
//                                            accident = deltaXA > rapidAccelerationThreshold || deltaYA > rapidAccelerationThreshold || deltaZA > rapidAccelerationThreshold
                                            */
//                                            val alpha = 0.6f // Low-pass filter coefficient
                                            xAccel = alpha * xAccel + (1 - alpha) * event.values[0]
                                            yAccel = alpha * yAccel + (1 - alpha) * event.values[1]
                                            zAccel = alpha * zAccel + (1 - alpha) * event.values[2]

                                            // Calculate the difference between the current and last acceleration values
                                            val diffX = Math.abs(xAccel - lastXAccel)
                                            val diffY = Math.abs(yAccel - lastYAccel)
                                            val diffZ = Math.abs(zAccel - lastZAccel)

                                            // Detect slight changes in movement in any direction

                                            val movementThreshold = 0.4f // Adjust as needed
                                            if (diffX > movementThreshold || diffY > movementThreshold || diffZ > movementThreshold) {
                                                if (diffX >= diffY && diffX >= diffZ) {
                                                    if (xAccel > lastXAccel) {
                                                        // Device moved to the right
                                                        movmentHorizotal = "RIGHT"
                                                        movmentVertivcal = "MOVING"

                                                    } else if (xAccel < lastXAccel){
                                                        // Device moved to the left
                                                        movmentHorizotal = "LEFT"
                                                        movmentVertivcal = "MOVING"
                                                    }
                                                    else{
                                                        movmentHorizotal = "NO MOVEMENT"
                                                        movmentVertivcal = "NO MOVEMENT"

                                                    }
                                                }
                                               else if (diffY >= diffX && diffY >= diffZ) {
                                                    if (yAccel > lastYAccel) {
                                                        // Device moved up
                                                        movmentHorizotal = "DOWN"
                                                        movmentVertivcal = "MOVING"
                                                    } else if (yAccel < lastYAccel) {
                                                        // Device moved down
                                                        movmentHorizotal = "UP"
                                                        movmentVertivcal = "MOVING"
                                                    }else{
                                                        movmentHorizotal = "NO MOVEMENT"
                                                        movmentVertivcal = "NO MOVEMENT"
                                                    }
                                                } else {
                                                    movmentHorizotal = "NO MOVEMENT"
                                                    movmentVertivcal = "NO MOVEMENT"
                                                }
                                            }else{
                                                movmentHorizotal = "NO MOVEMENT"
                                                movmentVertivcal = "NO MOVEMENT"
//                                                Toast.makeText(MainActivitys.context,
//                                                    "Movment Ajeeb ha yarrrrr",
//                                                    Toast.LENGTH_SHORT).show()
                                            }

                                            lastXAccel = xAccel
                                            lastYAccel = yAccel
                                            lastZAccel = zAccel
                                        }
//                                        detectAccident()
                                    }
                                }

                                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
                            }, lastSensor, SensorManager.SENSOR_DELAY_FASTEST)
                        }
                    }
                }
            }
            val x = acceleration[0]
            val y = acceleration[1]
            val z = acceleration[2]
            Log.d("Acceleration values...","X: " + x +" Y: "+y+" Z: "+z)
            updateGame(x.toFloat())

// Now you can access the x, y and z values of acceleration array

            sensorHandler.postDelayed(this, 500) // set the delay for 5 milisecond
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates(device:String) {
        // Set up location listener
        val locationCallback: LocationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.getLocations()) {
                    val latitude = location.latitude
                    val longitude = location.longitude
                    val accuracy = location.accuracy.toDouble()
                    val roundedAccuracy = String.format("%.7f", accuracy)
                    Log.d("MyApp", "Location accuracy: $roundedAccuracy meters")
                    val speed = location.speed
//                    data = ""
//                    data = data+  latitude.toString() + ","+ longitude.toString() + ","+ speed.toString()+","+ accuracy.toString()+",";
                    Log.d("Location values",latitude.toString() + "- "+ longitude + "- "+speed + " - "+ accuracy)
                    println("Current location: $latitude, $longitude")
                    println("Current speed: $speed m/s")
                    acceleration[4] = latitude
                    acceleration[5] = longitude
                    acceleration[6] = roundedAccuracy.toDouble()
                    acceleration[7] = speed.toDouble()
                    acceleration[8] = accelDiff.toDouble()
                    enableBluetooth(device!!)
                }
            }
        }
        // Start location updates
        fusedLocationClient!!.requestLocationUpdates(locationRequest, locationCallback, null)
//        enableBluetooth(device!!)
    }

    private val locationRequest: LocationRequest
        private get() = LocationRequest.create()
            .setInterval(1000)
            .setFastestInterval(500)
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
    
    private fun updateGame(x: Float) {
        val maxSteeringAngle = 45f
        val maxTiltAngle = 30f
        val steeringAngles = x * maxSteeringAngle / maxTiltAngle
        Log.d("steering angle", steeringAngles.toString())
        acceleration[3] = steeringAngles.toDouble()
//        val steeringAngle = if (acceleration[0] > 0) {
//            acceleration[0] * maxTiltAngle / SensorManager.GRAVITY_EARTH
//        } else {
//            acceleration[0] * maxTiltAngle / -SensorManager.GRAVITY_EARTH
//        }
        // Detect the tilt direction
         if (steeringAngles > 5) {
             Log.d("is steering angle right?", steeringAngles.toString())
             tiltDirection = "LEFT"
        }
        else if (steeringAngles < -5) {
             Log.d("is steering angle Left?", steeringAngles.toString())
        tiltDirection =  "RIGHT"
        }
        else {
             Log.d("is steering angle None?", steeringAngles.toString())
        tiltDirection = "NONE"
        }
        // Log the tilt direction
        Log.d("Tilt Direction", tiltDirection+" steering angle")
    }

    private fun calculateAcceleration(x: Float, y: Float, z: Float) {
        xAccel = alpha * xAccel + (1 - alpha) * x
        yAccel = alpha * yAccel + (1 - alpha) * y
        zAccel = alpha * zAccel + (1 - alpha) * z
        val currentAccel = Math.sqrt((xAccel * xAccel + yAccel * yAccel + zAccel * zAccel).toDouble()).toFloat()
        accelDiff = Math.abs(currentAccel - lastAccel)
        val brakingThreshold = 5.0f // adjust as needed
        if (accelDiff > brakingThreshold) {
            // Sudden braking detected
            accident = true
            Log.d("MainActivity", "Sudden braking detected")
        } else {
            // Normal braking detected
            accident = false
            Log.d("MainActivity", "Normal braking detected")
        }
        lastAccel = currentAccel
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (intent != null && intent.getExtras() != null) {
            val device = intent?.getStringExtra("DEVICE_ADDRESS")
            Log.d(TAG, "onStartCommand: Device is :" + device.toString())
            createNotificationChannel()
            val notificationIntent = Intent(this, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                this,
                0, notificationIntent, 0
            )
            val notification = NotificationCompat.Builder(this, "11")
                .setContentTitle("Foreground Service Kotlin Example")
                .setContentText("pyeyeyeyye")
                .setContentIntent(pendingIntent)
                .build()
            startForeground(1, notification)
            //stopSelf();
            mSensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
            mAccelerometer = mSensorManager!!.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            sensorHandler.postDelayed(sensorRunnable, 500) // start the sensor readings with a delay of 5 milisecond
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
            startLocationUpdates(device!!)


//            enableBluetooth(device!!)
        }
        return START_NOT_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                "11", "Foreground Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager!!.createNotificationChannel(serviceChannel)
        }
    }

    override fun onBind(p0: Intent?): IBinder? {
        TODO("Not yet implemented")
    }

    private fun enableBluetooth(dev:String) {
            // There's one Bluetooth adapter for the entire system, call getDefaultAdapter to get one
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            if (bluetoothAdapter == null) {
                Log.d("TAG", "Your Device Does Not Support Bluetooth.")
                return
            }
            if (bluetoothAdapter?.isEnabled == false) {
                Log.d("TAG", "Please enable Bluetooth.")
            }

            setupBluetoothClientConnection(dev)

            AcceptThread().start()
        }

    private fun setupBluetoothClientConnection(devi:String) {
            val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
            val allPairs: MutableList<String> = pairedDevices?.map { device ->
                val deviceName = device.name
                return@map deviceName
            }?.toMutableList() ?: mutableListOf()

            allPairs.add(0, "Select Connection")
            mPairedDevices = pairedDevices?.toList() ?: listOf()
                        for (pair in mPairedDevices){
                            Log.d("TAG", pair.name + " " + pair.address)
                            if(pair.address == devi){
                                val connectionSocket = ConnectThread(pair)
                                connectionSocket.start()
                                break
                            }
                        }
                }

    private inner class AcceptThread : Thread() {

            private val mmServerSocket: BluetoothServerSocket? by lazy(LazyThreadSafetyMode.NONE) {
                bluetoothAdapter?.listenUsingInsecureRfcommWithServiceRecord(
                    bluetoothAdapter!!.name,
                    mUUID
                )
            }

            override fun run() {
                // Keep listening until exception occurs or a socket is returned.
                var shouldLoop = true
                while (shouldLoop) {
                    val socket: BluetoothSocket? = try {
                        Log.d(TAG, "Establishing new Connection")
                        mmServerSocket?.accept()
                    } catch (e: IOException) {
                        Log.e(TAG, "Socket's accept() method failed", e)
                        shouldLoop = false
                        null
                    }
                    socket?.also { bluetoothSocket ->
                        val client = bluetoothSocket.remoteDevice.name
                        manageServerSocketConnection(bluetoothSocket, client)
                        mHandler.post {
                            Log.d("Tag", "Connection Established With $client")
                        }
                        mmServerSocket?.close()
                        shouldLoop = false
                    }
                }
            }

            // Closes the connect socket and causes the thread to finish.
            fun cancel() {
                try {
                    mmServerSocket?.close()
                } catch (e: IOException) {
                    Log.e(TAG, "Could not close the connect socket", e)
                }
            }
        }

    private inner class ConnectThread(val device: BluetoothDevice) : Thread() {

            private val mmSocket: BluetoothSocket? by lazy(LazyThreadSafetyMode.NONE) {
                device.createRfcommSocketToServiceRecord(mUUID)
            }

            public override fun run() {
                // Cancel discovery because it otherwise slows down the connection.
                bluetoothAdapter?.cancelDiscovery()

                mmSocket?.let { socket ->
                    // Connect to the remote device through the socket. This call blocks
                    // until it succeeds or throws an exception.
                    try {
                        socket.connect()
//                        Thread.sleep(5000)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to connect", e)
                    }

                    // The connection attempt succeeded. Perform work associated with
                    // the connection in a separate thread.
                    val client = socket.remoteDevice.name
                    manageServerSocketConnection(socket, client)
                    Log.d("Tag", "Connection Established With $client 2")
                }
            }

            // Closes the client socket and causes the thread to finish.
            fun cancel() {
                try {
                    mmSocket?.close()
                } catch (e: IOException) {
                    Log.e(TAG, "Could not close the client socket", e)
                }
            }
        }

    private inner class ConnectedThread(private val mmSocket: BluetoothSocket, val opName: String) : Thread() {


            override fun run() {
                Log.d("Checking.....??????????","checking...")
                // Keep listening to the InputStream until an exception occurs.
                while (true) {
                    // Read from the InputStream.
                    var send = ""
//                   data = send
                    if(accident){
//                        Toast.makeText(applicationContext, "Accident added", Toast.LENGTH_SHORT).show()
                        data = ""+acceleration[0]+","+acceleration[1]+","+acceleration[2]+","+acceleration[3]+","+acceleration[4]+","+acceleration[5]+","+acceleration[6]+","+acceleration[7]+","+ accelDiff+ ","+1.0
                        send= ""+acceleration[4]+","+acceleration[5]+","+1.0+"]"
                    }else{
//                        Toast.makeText(applicationContext, " Not Accident added", Toast.LENGTH_SHORT).show()
                        data = ""+acceleration[0]+","+acceleration[1]+","+acceleration[2]+","+acceleration[3]+","+acceleration[4]+","+acceleration[5]+","+acceleration[6]+","+acceleration[7]+","+ accelDiff+ ","+0.0
                        send= ""+acceleration[4]+","+acceleration[5]+","+0.0+"]"
                        Log.d(TAG, "run: The acceleration 8 is :"+accelDiff)

                    }
//                    data = send
                    val sdf = SimpleDateFormat("dd/M/yyyy hh:mm:ss")
                    time =  sdf.format(Date())
                    val averageAcceleration = accelerationArray.average()
                    // Check if the average acceleration value is above a threshold to detect an accident
                    /* if (Math.abs(averageAcceleration) > ACCIDENT_THRESHOLD) {
                        isAccidentDetected = true
                        send+=", Accident Detected!"
                        accident = true
                        Log.d("accident","Accident detected by average"+ Math.abs(averageAcceleration))
                    }
                    else {
                        isAccidentDetected = false
                        accident = false
                        Log.d("accident","Accident not detected " + Math.abs(averageAcceleration))
                        if(detect > ACCIDENT_THRESHOLD_last){
                            isAccidentDetected = true
                            accident = true
                            send+=", Accident Detected!"
                            Log.d("accident","Accident detected by last value"+ detect + " speed "+ acceleration[7])
                        }
                        else{
                            isAccidentDetected = false
                            accident = false
                            Log.d("accident","Accident not detected " + Math.abs(detect) + " speed "+ acceleration[7])
                        }
                    }
                     */
                    var arr = send.encodeToByteArray()
                    try{
                        if(send.length>2){
                            Log.d("My data", send.toString())
                            var mmOutStream: OutputStream = mmSocket.outputStream
                            mmOutStream.write(arr)
                            mmOutStream.flush()
                        Log.d("Tag", "writting..." + send)
                        }else{
                            Log.d("Tag", "writting Nothing empty data..." + send)
                        }

                    } catch (e: IOException) {
                        Log.d(TAG, "Input stream was disconnected", e)
                        break
                    }
                }
            }

            fun cancel() {
                try {
                    mmSocket.close()
                } catch (e: IOException) {
                    Log.e(TAG, "Could not close the connect socket", e)
                }
            }
        }

    private fun manageServerSocketConnection(socket: BluetoothSocket, name: String) {
            mHandler = Handler(this.mainLooper, Handler.Callback {
                try {
                    val response = it.obj as Pair<String, ByteArray>
                    val from = response.first
                    val msg = response.second.decodeToString()
                    Toast.makeText(this, "New Message Received", Toast.LENGTH_SHORT).show()
                    mMessagesAdapter.add(
                        ChatMessageItem(
                            msg,
                            from,
                            resources.getColor(R.color.reply, null)
                        )
                    )

                    return@Callback true
                } catch (e: Exception) {
                    return@Callback false
                }
            })
            val communicationService = ConnectedThread(socket, name)
            communicationService.start()
        }

    /*private inner class LabelEncoder(private val labels: Array<String>) {
        private val labelToIndex = HashMap<String, Int>()
        private val indexToLabel = HashMap<Int, String>()

        init {
            for (i in labels.indices) {
                labelToIndex[labels[i]] = i
                indexToLabel[i] = labels[i]
            }
        }

        fun encode(label: String): Int {
            return labelToIndex[label] ?: -1
        }

        fun decode(index: Int): String {
            return indexToLabel[index] ?: ""
        }

        fun numClasses(): Int {
            return labels.size
        }
    }*/
    /*private  fun accidentDetectionThroughMl(){
       // Load the CSV data
       val data = mutableListOf<FloatArray>()
       val labels = mutableListOf<String>()
       val reader = BufferedReader(InputStreamReader(assets.open("data.csv")))
       var line: String? = reader.readLine()
       while (line != null) {
           val vals = line.split(",")
           val values = vals.map { it.toFloat() }.toFloatArray()
           data.add(values.copyOf(values.size - 1)) // Exclude the last value, which is the label
           labels.add(vals.last()) // The last value is the label
           line = reader.readLine()
       }

// Define the label encoder
       val labelEncoder = LabelEncoder(labels.distinct().toTypedArray())

// Convert the CSV data to TensorFlow Lite input tensors
       val inputTensor = Tensor.allocateFloat16Buffer(data.size.toLong() * data[0].size)
       for (i in data.indices) {
           for (j in data[i].indices) {
               inputTensor.putFloat((i * data[0].size + j).toLong(), data[i][j])
           }
       }
       val outputTensor = Tensor.allocateFloat16Buffer(labels.size.toLong() * labelEncoder.numClasses())
       for (i in labels.indices) {
           val labelIndex = labelEncoder.encode(labels[i])
           for (j in 0 until labelEncoder.numClasses()) {
               outputTensor.putFloat((i * labelEncoder.numClasses() + j).toLong(),
                   if (j == labelIndex) 1.0f else 0.0f)
           }
       }

// Define the TensorFlow Lite model
       val model = NeuralNetwork.create(
           inputTensor.shape(), // The shape of the input tensor
           listOf(
               DenseLayer(16), // A dense layer with 16 neurons
               ActivationLayer(Activation.RELU), // A ReLU activation function
               DenseLayer(labelEncoder.numClasses()), // A dense layer with the number of classes neurons
               ActivationLayer(Activation.SOFTMAX) // A softmax activation function
           ),
           outputTensor.shape() // The shape of the output tensor
       )

// Train the TensorFlow Lite model
       val trainer = Trainer(model, inputTensor, outputTensor)
       trainer.train(1000) // Train for 1000 epochs

// Test the TensorFlow Lite model with user input
       val userInput = floatArrayOf(31.4993109f, 74.4088683f, 8.2715356E-4f, 0.55806637f, 0.037950001657009125f, 1.4170501232147217f, 9.75195026397705f, 0.05692500248551369f) // Replace this with your user input values
       val inputTensorForUserInput = Tensor.allocateFloat16Buffer(userInput.size.toLong())
       for (i in userInput.indices) {
           inputTensorForUserInput.putFloat(i.toLong(), userInput[i])
       }
       val outputTensorForUserInput = model.predict(inputTensorForUserInput.buffer)
       val outputForUserInput = FloatArray(labelEncoder.numClasses())
       outputTensorForUserInput.buffer.asFloatBuffer().get(outputForUserInput)
       val predictionIndex = outputForUserInput.indices.maxByOrNull { outputForUserInput[it] } ?: 0
       val prediction = labelEncoder.decode(predictionIndex)
       println("The model predicts that the input belongs to the $prediction class")


   }*/
    /*
    private fun lastValueAccidentDetection(x: Float, y: Float, z: Float): Double {
        val deltaX = x - lastX
        val deltaY = y - lastY
        val deltaZ = z - lastZ

        lastX = x.toDouble()
        lastY = y.toDouble()
        lastZ = z.toDouble()
        return sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ)
    }
*/
    /*
    private val sensorEventListener: SensorEventListener = object : SensorEventListener {
        override fun onSensorChanged(sensorEvent: SensorEvent) {
            if (sensorEvent.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            val x = sensorEvent.values[0]
            val y = sensorEvent.values[1]
            val z = sensorEvent.values[2]

            accelerationCurrentValue = Math.sqrt((x * x + y * y + z * z).toDouble())
            val changeInAcceleration =
                Math.abs(accelerationCurrentValue - accelerationPreviousValue)
            accelerationPreviousValue = accelerationCurrentValue
            data = data + changeInAcceleration.toString() + ","+ accelerationPreviousValue + ","+accelerationCurrentValue + ","+ x +","+ y + ","+z+",";
            Log.d("Acceleration values ",changeInAcceleration.toString() + "- "+ accelerationPreviousValue + "- "+accelerationCurrentValue)
            updateGame(x)
            }
            pointsPlotted++
            if (pointsPlotted > 1000) {
                pointsPlotted = 1
//                series.resetData(arrayOf(DataPoint(1.0, 0.0)))
            }
//            series.appendData(DataPoint(pointsPlotted.toDouble(), changeInAcceleration), true, pointsPlotted)

        }

        override fun onAccuracyChanged(sensor: Sensor, i: Int) {}
    }
*/
    /*private fun detectAccident() {
        if (Math.abs(yAccel) > verticalMovementThreshold) {
            if (yAccel > 0) {
                // phone moved up
                movmentVertivcal = "DOWN"
            } else if(yAccel < 0){
                // phone moved down
                movmentVertivcal = "UP"
            }
            else{
                movmentVertivcal = "NO MOVEMENT"
            }
        }
        else {
            movmentVertivcal = "NO MOVEMENT"
        }
        // Detect horizontal movement
        if (Math.abs(xAccel) > horizontalMovementThreshold) {
            if (xAccel > 0) {
                // phone moved right
                movmentHorizotal = "LEFT"
            } else if (xAccel < 0){
                // phone moved left
                movmentHorizotal = "RIGHT"
            }
            else{
                movmentVertivcal = "NO MOVEMENT"
            }
        }
        else{
            movmentHorizotal = "NO MOVEMENT"
        }
    }*/
    /*private  fun MLDetection(){
        // Load the CSV data
        val data = mutableListOf<FloatArray>()
        val labels = mutableListOf<Int>()
        val reader = BufferedReader(InputStreamReader(assets.open("data.csv")))
        var line: String? = reader.readLine()
        while (line != null) {
            val values = line.split(",").map { it.toFloat() }.toFloatArray()
            data.add(values.copyOf(values.size - 1)) // Exclude the last value, which is the label
            labels.add(values.last().toInt()) // The last value is the label
            line = reader.readLine()
        }

// Convert the CSV data to TensorFlow Lite input tensors
        val inputTensor = Tensor.allocateFloat16Buffer(data.size.toLong() * data[0].size)
        for (i in data.indices) {
            for (j in data[i].indices) {
                inputTensor.putFloat((i * data[0].size + j).toLong(), data[i][j])
            }
        }
        val outputTensor = Tensor.allocateFloat16Buffer(labels.size.toLong())
        for (i in labels.indices) {
            outputTensor.putFloat(i.toLong(), labels[i].toFloat())
        }

// Define the TensorFlow Lite model
        val model = NeuralNetwork.create(
            inputTensor.shape(), // The shape of the input tensor
            listOf(
                DenseLayer(16), // A dense layer with 16 neurons
                ActivationLayer(Activation.RELU), // A ReLU activation function
                DenseLayer(1), // A dense layer with 1 neuron
                ActivationLayer(Activation.SIGMOID) // A sigmoid activation function
            ),
            outputTensor.shape() // The shape of the output tensor
        )

// Train the TensorFlow Lite model
        val trainer = Trainer(model, inputTensor, outputTensor)
        trainer.train(1000) // Train for 1000 epochs

// Test the TensorFlow Lite model with user input
        val userInput = floatArrayOf(1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f) // Replace this with your user input values
        val inputTensorForUserInput = Tensor.allocateFloat16Buffer(userInput.size.toLong())
        for (i in userInput.indices) {
            inputTensorForUserInput.putFloat(i.toLong(), userInput[i])
        }
        val outputTensorForUserInput = model.predict(inputTensorForUserInput.buffer)
        val outputForUserInput = outputTensorForUserInput.getFloat(0.toLong())
        val prediction = if (outputForUserInput >= 0.5f) "Accident" else "Not Accident"
        println("The model predicts that the input is $prediction")

    }*/
    companion object {
            const val REQUEST_ENABLE_BT = 100
            // Defines several constants used when transmitting messages between the
            // service and the UI.
            const val MESSAGE_READ: Int = 0
            const val MESSAGE_WRITE: Int = 1
            const val MESSAGE_TOAST: Int = 2
            const val ACCIDENT_THRESHOLD = 2.0f
            const val ACCIDENT_THRESHOLD_last = 14.0f
            var tiltDirection = ""
            var movmentHorizotal = ""
            var movmentVertivcal = ""
            var data: String = ""
            var accident: Boolean = false
            var time:String = ""
            var accelDiff:Float = 0F
        }
    }