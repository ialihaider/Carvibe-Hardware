package com.omega.bluetoothchatapp

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.*
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.ml.DataFrame
import com.example.ml.DecisionTree
import com.jjoe64.graphview.GraphView
import com.jjoe64.graphview.Viewport
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import com.opencsv.CSVReader
import com.opencsv.CSVWriter
import java.io.File
import java.io.FileReader
import java.io.FileWriter

class MainActivitys : AppCompatActivity(), AdapterView.OnItemClickListener {

    private val directoryName = "dataset"
    private lateinit var listView: ListView
    private lateinit var viewOfList: LinearLayout
    private lateinit var viewDat: LinearLayout
    private lateinit var main: LinearLayout
    private lateinit var button: Button
    private  lateinit var accidentStatus:TextView
    private  lateinit var accelDef:TextView
    private  lateinit var time:TextView
    private  lateinit var tilt:TextView
    private  lateinit var move:TextView
    private  lateinit var X:TextView
    private  lateinit var Y:TextView
    private  lateinit var Z:TextView
    private  lateinit var Lat:TextView
    private  lateinit var Lng:TextView
    private  lateinit var Angle:TextView
    private  lateinit var Speed:TextView
    private  lateinit var Accuracy:TextView
    private var dialogVisible:Boolean = false
    private var verifyAccident = false
    private var speedCount = 0
    private var speedArray = FloatArray(5)
    private  var multiColor = true
    private lateinit var adapter: ArrayAdapter<String>
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var devices: Set<BluetoothDevice>
    private lateinit var graph: GraphView
    var viewport: Viewport? = null
    private var pointsPlotted = 2.0
    var series = LineGraphSeries(
        arrayOf<DataPoint>(
            DataPoint(0.0, 1.0),
            DataPoint(1.0, 5.0),
        )
    )
    private var accelerationCurrentValue = 0.0
    private var accelerationPreviousValue = 0.0

    @SuppressLint("MissingPermission", "MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mains)
        context = this

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                Companion.LOCATION_PERMISSION_CODE
            )
        } else {
//            startLocationUpdates()
            Toast.makeText(this ,"Permission Already granted!", Toast.LENGTH_LONG).show()

        }

        graph = findViewById<GraphView>(R.id.graph) as GraphView
        series = LineGraphSeries()
        series.color = Color.MAGENTA
        graph.addSeries(series)
        viewport = graph.viewport
        viewport!!.isScrollable = true
        viewport!!.isXAxisBoundsManual = true
        graph.addSeries(series)
        viewDat = findViewById<LinearLayout>(R.id.data) as LinearLayout
        viewOfList = findViewById<LinearLayout>(R.id.viewOfList) as LinearLayout
        main = findViewById<LinearLayout>(R.id.main) as LinearLayout
        button = findViewById<Button>(R.id.close) as Button
        listView = findViewById<View>(R.id.list) as ListView
        accidentStatus = findViewById<TextView>(R.id.accident) as TextView
        accelDef = findViewById<TextView>(R.id.accelDef) as TextView
        time = findViewById<TextView>(R.id.time) as TextView
        tilt = findViewById<TextView>(R.id.tilt) as TextView
        move = findViewById<TextView>(R.id.move) as TextView
        X = findViewById<TextView>(R.id.X) as TextView
        Y = findViewById<TextView>(R.id.Y) as TextView
        Z = findViewById<TextView>(R.id.Z) as TextView
        Lat = findViewById<TextView>(R.id.lat) as TextView
        Lng = findViewById<TextView>(R.id.lng) as TextView
        Angle = findViewById<TextView>(R.id.angle) as TextView
        Speed = findViewById<TextView>(R.id.speed) as TextView
        Accuracy = findViewById<TextView>(R.id.accuracy) as TextView
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1)
        listView.adapter = adapter
        listView.onItemClickListener = this
        showHide(viewOfList)
        showHide(button)
        showHide(viewDat)
        button.setOnClickListener{
            if(viewOfList.visibility == View.VISIBLE){
                button.setText("Hide")
            }else{
                button.setText("Show")
            }
            showHide(viewOfList)
            showHide(viewDat)
            Toast.makeText(this, "Closing...", Toast.LENGTH_SHORT).show()
        }
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        devices = bluetoothAdapter.bondedDevices
        for (device in devices) {
            adapter.add(device.name + "\n" + device.address)
        }
    }
/*
//    override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
//        val deviceName = adapter.getItem(position)
//        val deviceAddress = deviceName!!.substring(deviceName!!.length - 17)
//        val intent = Intent(this, BluetoothSenderService::class.java)
//        intent.putExtra("DEVICE_ADDRESS", deviceAddress)
//        intent.putExtra("DEVICE_UUID", mDeviceUUID.toString())
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            Log.d("Checking........","true version")
//            startForegroundService(intent)
//        }else{
//            Log.d("Checking........","false version")
//        }
//    }

//    override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
//        val deviceName = adapter.getItem(position)
//        val deviceAddress = deviceName!!.substring(deviceName!!.length - 17)
//        Log.d("checking......", deviceAddress)
//        val intent = Intent(this, BluetoothReceiverService::class.java)
//        intent.putExtra("DEVICE_ADDRESS", deviceAddress)
//        intent.putExtra("DEVICE_UUID", mDeviceUUID.toString())
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            Log.d("Checking........","true version")
//            startService(intent)
//        }else{
//            Log.d("Checking........","false version")
//        }
//    }
    */

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_CODE) {
            if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                startLocationUpdates()
                Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        val deviceName = adapter.getItem(position)
        val deviceAddress = deviceName!!.substring(deviceName!!.length - 17)
        Log.d("checking......", deviceAddress)
        val intent = Intent(this, MainActivityservice::class.java)
        intent.putExtra("DEVICE_ADDRESS", deviceAddress)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.d("Checking........","true version")
            startService(intent)
        }else{
            Log.d("Checking........","false version")
        }
        showHide(viewOfList)
        showHide(viewDat)
        showHide(button)
        handler.postDelayed(sensorRunnable, 100)
    }

    private val handler = Handler()
    private val sensorRunnable = object : Runnable {
        override fun run() {
            val data = MainActivityservice.data.split(',')
            /*
//            if(verifyAccident){
//                if(data[7].toFloat()>1){
//                    main.setBackgroundColor(Color.GREEN) // False accident detected
//                }else{
//                    main.setBackgroundColor(Color.BLUE) // True accident detected
//                }
//            }

             */
            val accident = MainActivityservice.accident
            if(viewDat.visibility == View.VISIBLE && data.size > 7) {
                if(multiColor){
                    tilt.setText(MainActivityservice.Companion.tiltDirection)
                    tilt.setTextColor(Color.WHITE)
                    move.setText(MainActivityservice.Companion.movmentVertivcal)
                    move.setTextColor(Color.WHITE)
                    multiColor = !multiColor
                }else{
                    tilt.setText(MainActivityservice.Companion.tiltDirection)
                    tilt.setTextColor(Color.YELLOW)
                    move.setText(MainActivityservice.Companion.movmentVertivcal)
                    move.setTextColor(Color.YELLOW)
                    multiColor = !multiColor
                }
                accelDef.setText(MainActivityservice.accelDiff.toString())
                time.setText(MainActivityservice.time)
                X.setText(data[0])
                Y.setText(data[1])
                Z.setText(data[2])
                Angle.setText(data[3])
                Lat.setText(data[4])
                Lng.setText(data[5])
                Accuracy.setText(data[6])
                Speed.setText(data[7].toFloat().toString())
                accidentStatus.text= "NOT DETECTED"
                if(verifyAccident && verifyAccidentMain){
                    if(MainActivityservice.Companion.movmentVertivcal.equals("NO MOVEMENT") && MainActivityservice.Companion.movmentHorizotal.equals("NO MOVEMENT") ){
                        main.setBackgroundColor(Color.BLUE)
                        accidentstat = "Accident"
                        accidentStatus.text= "DETECTED"
                    }else{
                        main.setBackgroundColor(Color.GREEN)
                        accidentstat = "Not Accident"
                        accidentStatus.text= "NOT DETECTED"
                    }
                    verifyAccident = false
                }
                else if(verifyAccidentMain && !verifyAccident) {
                    /*
//                    if(speedCount == 4){
//                        speedArray[speedCount] = data[7].toFloat()
//                        var sum = speedArray.sum()
//                        if(sum < 10){
//                            main.setBackgroundColor(Color.BLUE) // True accident detected
//                        }else{
//                            main.setBackgroundColor(Color.GREEN) // False accident detected
//                        }
//                        speedCount = 0
//                        verifyAccidentMain = false
//                    }else{
//                        speedArray[speedCount] = data[7].toFloat()
//                        speedCount = speedCount + 1
//                    }

                     */
                    accidentstat = "Not Accident"
                    accidentStatus.text= "NOT DETECTED"
                    verifyAccidentMain = false
                }
                else{
                    if(accident){
                        accidentStatus.setText("DETECTED")
                        main.setBackgroundColor(Color.RED)
                        verifyAccident = true
                        verifyAccidentMain = true
                        accidentstat = "Accident"
                        if(!dialogVisible) {
                            showDialogue(
                                "Accident Detected",
                                "We detect the Accident Please confirm us if it's not an accident, If You are Alive!"
                            )
                        }
                    }
//                    accidentStatus.setText("NOT DETECTED")
//                    main.setBackgroundColor(Color.TRANSPARENT)
//                    if(detectAccidentThroughAI(X.text.toString(), Y.text.toString(), Z.text.toString(), accelDef.text.toString(), tilt.text.toString(), Speed.text.toString(),Angle.text.toString())){
//                        accidentStatus.setText("DETECTED BY AI")
//                        main.setBackgroundColor(Color.RED)
//                        verifyAccident = true
//                        verifyAccidentMain = true
//                        if(!dialogVisible) {
//                            showDialogue(
//                                "Accident Detected",
//                                "AI detect the Accident Please confirm us if it's not an accident, If You are Alive!"
//                            )
//                        }
//                        }else if(accident){
//                        accidentStatus.setText("DETECTED")
//                        main.setBackgroundColor(Color.RED)
//                        verifyAccident = true
//                        verifyAccidentMain = true
//                        if(!dialogVisible) {
//                            showDialogue(
//                                "Accident Detected",
//                                "We detect the Accident Please confirm us if it's not an accident, If You are Alive!"
//                            )
//                        }
//                    }else{
//                        accidentStatus.setText("NOT DETECTED")
//                        main.setBackgroundColor(Color.TRANSPARENT)
//                    }
                }
                updateGraph(data[0].toFloat(),data[1].toFloat(),data[2].toFloat())
            }
            Log.d("mera datav ian mein ya a raha ha", "->" + MainActivityservice.data)
            handler.postDelayed(this, 500) // set the delay for 5 minutes
        }
    }

    fun detectAccidentThroughAI(X:String, Y:String, Z:String, Accldiff:String, Tilt:String, speed:String, angles:String):Boolean{
        val directoryPath = "${filesDir.absolutePath}/$directoryName"
        val directory = File(directoryPath)
        if (!directory.exists()) {
            directory.mkdir()
            Toast.makeText(this, "New directory created", Toast.LENGTH_SHORT).show()
        }
        val fileName = "data.csv"
        val filePath = "$directoryPath/$fileName"
        val file = File(filePath)
        if (!file.exists()) {
            file.createNewFile()
            Toast.makeText(this, "New File created", Toast.LENGTH_SHORT).show()
        }
        val reader = CSVReader(FileReader(filePath))
        var nextLine: Array<String>?
        val dataArray = mutableListOf<Array<String>>()
        var lat: String = ""
        var lng:String = ""
        var X:String = ""
        var Y:String = ""
        var Z:String = ""
        var Angle:String = ""
        var diff:String = ""
        var speed:String = ""
        var tilt:String = ""
        var accident:String = ""
        while (reader.readNext().also { nextLine = it } != null) {
            dataArray.add(nextLine!!)
        }
        // print the array
        for (row in dataArray) {
            var cols = 0
            for (col in row) {
                if(cols == 0){
                    lat+= col+","
                }else if(cols == 1){
                    lng+= col+","
                }else if(cols == 2){
                    speed+= col+","
                }else if(cols == 3){
                    diff+= col+","
                }else if(cols == 4){
                    X+= col+","
                }else if(cols == 5){
                    Y+= col+","
                }else if(cols == 6){
                    Z+= col+","
                }else if(cols == 7){
                    Angle+= col+","
                }else if(cols == 8){
                    tilt+= col+","
                }else if(cols == 9){
                    accident+= col+","
                }else{
                    println("Has some errors")
                }
                cols++
//                print("$col, ")
            }
//            println("=====")
        }
//        println(lat)
//        println(lng)
//        println(speed)
//        println(Angle)
//        println(diff)
//        println(X)
//        println(Y)
//        println(Z)
//        println(tilt)
//        println(accident)


        // Get some sample data
        val dataFrame = DataFrame()
        var temparray = speed.split(",")
        var listOfStrings: ArrayList<String> = ArrayList(temparray.size)
        listOfStrings.addAll(temparray)
        dataFrame.addColumn(listOfStrings, "speed")
         temparray = diff.split(",")
         listOfStrings = ArrayList(temparray.size)
        listOfStrings.addAll(temparray)
        dataFrame.addColumn(listOfStrings , "AccelerationDifference")
         temparray = Angle.split(",")
         listOfStrings = ArrayList(temparray.size)
        listOfStrings.addAll(temparray)
        dataFrame.addColumn(listOfStrings, "Angle")
//        temparray = X.split(",")
//        listOfStrings = ArrayList(temparray.size)
//        listOfStrings.addAll(temparray)
//        dataFrame.addColumn(listOfStrings, "X")
//        temparray = Y.split(",")
//        listOfStrings = ArrayList(temparray.size)
//        listOfStrings.addAll(temparray)
//        dataFrame.addColumn(listOfStrings, "Y")
//        temparray = Z.split(",")
//        listOfStrings = ArrayList(temparray.size)
//        listOfStrings.addAll(temparray)
//        dataFrame.addColumn(listOfStrings, "Z")
        temparray = tilt.split(",")
        listOfStrings = ArrayList(temparray.size)
        listOfStrings.addAll(temparray)
        dataFrame.addColumn(listOfStrings, "tilt")
        temparray = accident.split(",")
        listOfStrings = ArrayList(temparray.size)
        listOfStrings.addAll(temparray)
        dataFrame.addColumn(listOfStrings, "Label")
//        dataFrame.addColumn(
//            arrayOf("No", "No", "Yes", "No", "Yes", "Yes", "No", "Yes", "Yes", "Yes").toList() as ArrayList<String>,
//            "Label"
//        )

        // Set the data in the tree. Soon, the tree is created.
        val decisionTree = RandomForest( dataFrame )
        val sample = HashMap<String,String>().apply {
            put( "speed" , speed)
            put( "AccelerationDifference" , accelDef.text.toString())
//            put( "X" , X)
//            put( "Y" , Y )
//            put( "Z" , Z )
            put( "Angle" , angles)
            put( "tilt" , tilt)
        }

        // Print the tree as a `HashMap`.
        val resultPredected = decisionTree.predict( sample )
        println( "ML say's it is "+resultPredected )
        if (resultPredected == "Accident"){
            return true
        }else{
            return false
        }
        println("-><-"+ decisionTree )
    }

    fun showHide(view:View) {
        view.visibility = if (view.visibility == View.VISIBLE){
            View.GONE
        } else{
            View.VISIBLE
        }
    }

    private fun updateGraph(x: Float, y: Float, z: Float) {
        accelerationCurrentValue = Math.sqrt((x * x + y * y + z * z).toDouble())
        val changeInAcceleration =
            Math.abs(accelerationCurrentValue - accelerationPreviousValue)
        accelerationPreviousValue = accelerationCurrentValue
        pointsPlotted++
        if (pointsPlotted > 1000) {
            pointsPlotted = 1.0
            series.resetData(arrayOf(DataPoint(1.0, 0.0)))
        }
        series.appendData(DataPoint(pointsPlotted.toDouble(), changeInAcceleration), true,
            pointsPlotted.toInt()
        )
        viewport!!.setMaxX(pointsPlotted.toDouble())
        viewport!!.setMinX((pointsPlotted - 100).toDouble())
    }

    override fun onResume() {
        super.onResume()

        handler.postDelayed(sensorRunnable, 500) // start the sensor readings with a delay of 5 minutes
    }

    override fun onPause() {
        super.onPause()

        handler.removeCallbacks(sensorRunnable) // stop the sensor readings when the app is paused or stopped
    }

    fun showDialogue(title: String, message: String) {
        val dialogBuilder = AlertDialog.Builder(this)
        dialogBuilder.setTitle(title)
        dialogBuilder.setMessage(message)
        dialogBuilder.setPositiveButton("Yes") { dialog, which ->
            // Do something when OK button is clicked
            dialog.dismiss()
            dialogVisible = false
            makeDataset(Lat.text.toString(), Lng.text.toString(), Speed.text.toString(),accelDef.text.toString(),X.text.toString(),Y.text.toString(),Z.text.toString(), Angle.text.toString(),tilt.text.toString(),"Accident")
        }
        dialogBuilder.setNegativeButton("No") { dialog, which ->
            // Do something when Cancel button is clicked
            dialog.dismiss()
            dialogVisible = false
            makeDataset(Lat.text.toString(), Lng.text.toString(), Speed.text.toString(),accelDef.text.toString(),X.text.toString(),Y.text.toString(),Z.text.toString(), Angle.text.toString(),tilt.text.toString(),"Not Accident")

        }
        val dialog = dialogBuilder.create()

        // Create the timer
        val timer = object : CountDownTimer(3000, 3000) { // 10 seconds with 1 second interval
            override fun onTick(millisUntilFinished: Long) {
                // Update the message with the remaining time
                dialog.setMessage("$message\n\n Remaining Time: ${millisUntilFinished / 1000} seconds")
            }

            override fun onFinish() {
                // Dismiss the dialog when the timer finishes
                dialog.dismiss()
                dialogVisible = false
            }
        }

        // Show the dialog and start the timer
        dialog.show()
        dialogVisible = true
        timer.start()
    }

    fun makeDataset(lat:String, long:String, speed:String,acldefrience:String,xVlaue:String,yVlaue:String,zVlaue:String, angle:String,tilidirection:String,accidentstatus:String) {
        val directoryPath = "${filesDir.absolutePath}/$directoryName"
        val directory = File(directoryPath)
        if (!directory.exists()) {
            directory.mkdir()
            Toast.makeText(this, "New directory created", Toast.LENGTH_SHORT).show()
        }
        val fileName = "data.csv"
        val filePath = "$directoryPath/$fileName"
        val file = File(filePath)
        if (!file.exists()) {
            file.createNewFile()
            Toast.makeText(this, "New File created", Toast.LENGTH_SHORT).show()
        }
        val fileWriter = FileWriter(file, true)
        val csvWriter = CSVWriter(fileWriter)
        val data1 = arrayOf(lat, long, speed, acldefrience, xVlaue, yVlaue, zVlaue, angle, tilidirection, accidentstatus )
        csvWriter.writeNext(data1)
        Toast.makeText(this, "Data Added", Toast.LENGTH_SHORT).show()
        csvWriter.close()
        fileWriter.close()
    }


    companion object {
        private const val LOCATION_PERMISSION_CODE = 1
        var verifyAccidentMain = false
        lateinit var context:Context
        var accidentstat:String = "Not Accident"
    }

}