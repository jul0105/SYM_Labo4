package ch.heigvd.iict.sym_labo4

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanFilter.Builder
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.os.*
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModelProvider
import ch.heigvd.iict.sym_labo4.abstractactivies.BaseTemplateActivity
import ch.heigvd.iict.sym_labo4.adapters.ResultsAdapter
import ch.heigvd.iict.sym_labo4.viewmodels.BleOperationsViewModel
import java.util.*


/**
 * Project: Labo4
 * Created by fabien.dutoit on 11.05.2019
 * Updated by fabien.dutoit on 06.11.2020
 * Updated by Julien Béguin, Robin Cuénoud & Gaëtan Daubresse on 20.01.2021
 * (C) 2019 - HEIG-VD, IICT
 */
class BleActivity : BaseTemplateActivity() {
    //system services
    private lateinit var bluetoothAdapter: BluetoothAdapter

    //view model
    private lateinit var bleViewModel: BleOperationsViewModel

    //gui elements
    private lateinit var operationPanel: View
    private lateinit var scanPanel: View
    private lateinit var scanResults: ListView
    private lateinit var emptyScanResults: TextView

    private lateinit var hitButtonTextView: TextView
    private lateinit var temperatureTextView: TextView
    private lateinit var temperatureButton: Button
    private lateinit var sendIntEditText : EditText
    private lateinit var sendButton: Button
    private lateinit var currDateTextView: TextView
    private lateinit var syncDateButton: Button

    //menu elements
    private var scanMenuBtn: MenuItem? = null
    private var disconnectMenuBtn: MenuItem? = null

    //adapters
    private lateinit var scanResultsAdapter: ResultsAdapter

    //states
    private var handler = Handler(Looper.getMainLooper())

    private var isScanning = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ble)

        //enable and start bluetooth - initialize bluetooth adapter
        val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        //link GUI
        operationPanel = findViewById(R.id.ble_operation)
        scanPanel = findViewById(R.id.ble_scan)
        scanResults = findViewById(R.id.ble_scanresults)
        emptyScanResults = findViewById(R.id.ble_scanresults_empty)

        // received data display
        temperatureTextView = findViewById(R.id.temperatureTextView)
        temperatureButton = findViewById(R.id.temperatureButton)
        hitButtonTextView = findViewById(R.id.nbHitButton)
        sendIntEditText = findViewById(R.id.sendIntEditText)
        sendButton = findViewById(R.id.sendButton)
        currDateTextView = findViewById(R.id.currDateTextView)
        syncDateButton = findViewById(R.id.syncDateButton)

        //manage scanned item
        scanResultsAdapter = ResultsAdapter(this)
        scanResults.adapter = scanResultsAdapter
        scanResults.emptyView = emptyScanResults

        //connect to view model
        bleViewModel = ViewModelProvider(this).get(BleOperationsViewModel::class.java)

        updateGui()

        //events
        scanResults.setOnItemClickListener { _: AdapterView<*>?, _: View?, position: Int, _: Long ->
            runOnUiThread {
                //we stop scanning
                scanLeDevice(false)
                //we connect
                bleViewModel.connect(scanResultsAdapter.getItem(position).device)
            }
        }
        temperatureButton.setOnClickListener({
            bleViewModel.readTemperature()
        })

        sendButton.setOnClickListener({
            var number = (sendIntEditText.text).toString().toInt()
            bleViewModel.sendInt(number)
        })

        syncDateButton.setOnClickListener({
            val calendar = Calendar.getInstance()
            bleViewModel.syncDate(calendar)
        })
        //ble events
        bleViewModel.isConnected.observe(this, { updateGui() })
        bleViewModel.temperature.observe(this, { updateGui() })
        bleViewModel.click.observe(this, {updateGui()})
        bleViewModel.date.observe(this, {updateGui()})
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.ble_menu, menu)
        //we link the two menu items
        scanMenuBtn = menu.findItem(R.id.menu_ble_search)
        disconnectMenuBtn = menu.findItem(R.id.menu_ble_disconnect)
        //we update the gui
        updateGui()
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.menu_ble_search) {
            if (isScanning) scanLeDevice(false) else scanLeDevice(true)
            return true
        } else if (id == R.id.menu_ble_disconnect) {
            bleViewModel.disconnect()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onPause() {
        super.onPause()
        if (isScanning) scanLeDevice(false)
        if (isFinishing) bleViewModel.disconnect()
    }

    /*
     * Method used to update the GUI according to BLE status:
     * - connected: display operation panel (BLE control panel)
     * - not connected: display scan result list
     */
    private fun updateGui() {
        val isConnected = bleViewModel.isConnected.value
        if (isConnected != null && isConnected) {

            scanPanel.visibility = View.GONE
            operationPanel.visibility = View.VISIBLE

            if (scanMenuBtn != null && disconnectMenuBtn != null) {
                scanMenuBtn!!.isVisible = false
                disconnectMenuBtn!!.isVisible = true
            }
            temperatureTextView.text = bleViewModel.temperature.value.toString();
            hitButtonTextView.text = ("Number click : " + bleViewModel.click.value.toString());
            currDateTextView.text = ("Current date : " + bleViewModel.date.value.toString());
        } else {
            operationPanel.visibility = View.GONE
            scanPanel.visibility = View.VISIBLE

            if (scanMenuBtn != null && disconnectMenuBtn != null) {
                disconnectMenuBtn!!.isVisible = false
                scanMenuBtn!!.isVisible = true
            }
        }
    }

    //this method need user granted localisation permission, our demo app is requesting it on MainActivity
    @RequiresApi(Build.VERSION_CODES.O)
    private fun scanLeDevice(enable: Boolean) {
        val bluetoothScanner = bluetoothAdapter.bluetoothLeScanner

        if (enable) {
            //config
            val builderScanSettings = ScanSettings.Builder()
            builderScanSettings.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            builderScanSettings.setReportDelay(0)

            //we scan for any BLE device
            //we don't filter them based on advertised services...
            // TODO ajouter un filtre pour n'afficher que les devices proposant
            // le service "SYM" (UUID: "3c0a1000-281d-4b48-b2a7-f15579a1c38f")


            val filter = Builder().setServiceUuid(ParcelUuid.fromString("3c0a1000-281d-4b48-b2a7-f15579a1c38f")).build();
            var filters: ArrayList<ScanFilter>? = ArrayList(1)

            if (filters != null) {
                filters.add(filter)
            }
            //reset display
            scanResultsAdapter.clear()
            bluetoothScanner.startScan(filters, builderScanSettings.build(), leScanCallback)
            Log.d(TAG, "Start scanning...")
            isScanning = true

            //we scan only for 15 seconds
            handler.postDelayed({ scanLeDevice(false) }, 15 * 1000L)
        } else {
            bluetoothScanner.stopScan(leScanCallback)
            isScanning = false
            Log.d(TAG, "Stop scanning (manual)")
        }
    }

    // Device scan callback.
    private val leScanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            runOnUiThread {

                scanResultsAdapter.addDevice(result) }
        }
    }

    companion object {
        private val TAG = BleActivity::class.java.simpleName
    }
}
