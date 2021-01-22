package ch.heigvd.iict.sym_labo4.viewmodels

import android.app.Application
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import no.nordicsemi.android.ble.BleManager
import no.nordicsemi.android.ble.data.Data
import no.nordicsemi.android.ble.observer.ConnectionObserver
import java.util.*

/**
 * Project: Labo4
 * Created by fabien.dutoit on 11.05.2019
 * Updated by fabien.dutoit on 06.11.2020
 * (C) 2019 - HEIG-VD, IICT
 */
class BleOperationsViewModel(application: Application) : AndroidViewModel(application) {

    private var ble = SYMBleManager(application.applicationContext)
    private var mConnection: BluetoothGatt? = null

    //live data - observer
    val isConnected = MutableLiveData(false)
    val date = MutableLiveData("D/M/Y-hh:mm:ss")
    val temperature = MutableLiveData("temp")
    val click = MutableLiveData(0)
    //Services and Characteristics of the SYM Pixl
    private var timeService: BluetoothGattService? = null
    private var symService: BluetoothGattService? = null
    private var currentTimeChar: BluetoothGattCharacteristic? = null
    private var integerChar: BluetoothGattCharacteristic? = null
    private var temperatureChar: BluetoothGattCharacteristic? = null
    private var buttonClickChar: BluetoothGattCharacteristic? = null

    //UUIDs des services
    private val TIMESERVICE = UUID.fromString("00001805-0000-1000-8000-00805f9b34fb")
    private val SYMSERVICE = UUID.fromString("3c0a1000-281d-4b48-b2a7-f15579a1c38f")

    private val CURRENTTIMECHAR = UUID.fromString("00002A2B-0000-1000-8000-00805f9b34fb")
    private val INTEGERCHAR = UUID.fromString("3c0a1001-281d-4b48-b2a7-f15579a1c38f")
    private val TEMPERATURECHAR = UUID.fromString("3c0a1002-281d-4b48-b2a7-f15579a1c38f")
    private val BUTTONCLICKCHAR = UUID.fromString("3c0a1003-281d-4b48-b2a7-f15579a1c38f")



    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "onCleared")
        ble.disconnect()
    }

    fun connect(device: BluetoothDevice) {
        Log.d(TAG, "User request connection to: $device")
        if (!isConnected.value!!) {
            ble.connect(device)
                    .retry(1, 100)
                    .useAutoConnect(false)
                    .enqueue()
        }
    }

    fun disconnect() {
        Log.d(TAG, "User request disconnection")
        ble.disconnect()
        mConnection?.disconnect()
    }

    /* TODO
        vous pouvez placer ici les différentes méthodes permettant à l'utilisateur
        d'interagir avec le périphérique depuis l'activité
     */

    fun readTemperature(): Boolean {
        if (!isConnected.value!! || temperatureChar == null)
            return false
        else
            return ble.readTemperature()
    }

    private val bleConnectionObserver: ConnectionObserver = object : ConnectionObserver {
        override fun onDeviceConnecting(device: BluetoothDevice) {
            Log.d(TAG, "onDeviceConnecting")
            isConnected.value = false
        }

        override fun onDeviceConnected(device: BluetoothDevice) {
            Log.d(TAG, "onDeviceConnected")
            isConnected.value = true
        }

        override fun onDeviceDisconnecting(device: BluetoothDevice) {
            Log.d(TAG, "onDeviceDisconnecting")
            isConnected.value = false
        }

        override fun onDeviceReady(device: BluetoothDevice) {
            Log.d(TAG, "onDeviceReady")
        }

        override fun onDeviceFailedToConnect(device: BluetoothDevice, reason: Int) {
            Log.d(TAG, "onDeviceFailedToConnect")
        }

        override fun onDeviceDisconnected(device: BluetoothDevice, reason: Int) {
            if(reason == ConnectionObserver.REASON_NOT_SUPPORTED) {
                Log.d(TAG, "onDeviceDisconnected - not supported")
                Toast.makeText(getApplication(), "Device not supported - implement method isRequiredServiceSupported()", Toast.LENGTH_LONG).show()
            }
            else
                Log.d(TAG, "onDeviceDisconnected")
            isConnected.value = false
        }

    }

    private inner class SYMBleManager(applicationContext: Context) : BleManager(applicationContext) {
        /**
         * BluetoothGatt callbacks object.
         */
        private var mGattCallback: BleManagerGattCallback? = null

        public override fun getGattCallback(): BleManagerGattCallback {
            //we initiate the mGattCallback on first call, singleton
            if (mGattCallback == null) {
                mGattCallback = object : BleManagerGattCallback() {

                    public override fun isRequiredServiceSupported(gatt: BluetoothGatt): Boolean {
                        mConnection = gatt //trick to force disconnection

                        Log.d(TAG, "isRequiredServiceSupported - TODO")


                        timeService = mConnection!!.getService(TIMESERVICE);
                        //currentTimeChar = mConnection!!.getService(CURRENT_TIME_SERVICE).getCharacteristic(CURRENT_TIME);
                        for (i in gatt.services) {
                            when (i.uuid) {
                                TIMESERVICE -> {
                                    timeService = i;
                                    for (j in i.characteristics) {
                                        when (j.uuid) {
                                            CURRENTTIMECHAR -> {
                                                currentTimeChar = j;
                                            }
                                        }
                                    }
                                }
                                SYMSERVICE -> {
                                    symService = i;
                                    for (j in i.characteristics) {
                                        when (j.uuid) {
                                            INTEGERCHAR -> {
                                                integerChar = j;
                                            }
                                            BUTTONCLICKCHAR -> {
                                                buttonClickChar = j;
                                            }
                                            TEMPERATURECHAR -> {
                                                temperatureChar = j;
                                            }
                                        }
                                    }
                                }
                            }
                        }


                        /*
                        - Nous devons vérifier ici que le périphérique auquel on vient de se connecter possède
                          bien tous les services et les caractéristiques attendues, on vérifiera aussi que les
                          caractéristiques présentent bien les opérations attendues
                        - On en profitera aussi pour garder les références vers les différents services et
                          caractéristiques (déclarés en lignes 39 à 44)
                        */


                        return (timeService != null && symService != null && currentTimeChar != null && integerChar != null && buttonClickChar != null && temperatureChar != null);
                    }

                    override fun initialize() {
                        /*  TODO
                            Ici nous somme sûr que le périphérique possède bien tous les services et caractéristiques
                            attendus et que nous y sommes connectés. Nous pouvous effectuer les premiers échanges BLE:
                            Dans notre cas il s'agit de s'enregistrer pour recevoir les notifications proposées par certaines
                            caractéristiques, on en profitera aussi pour mettre en place les callbacks correspondants.
                         */
                        setNotificationCallback(currentTimeChar).with { _: BluetoothDevice, data: Data ->
                            val year = data.getIntValue(Data.FORMAT_UINT16, 0).toString()
                            var month = data.getIntValue(Data.FORMAT_UINT8, 2).toString()
                            var day = data.getIntValue(Data.FORMAT_UINT8, 3).toString()

                            var hour = data.getIntValue(Data.FORMAT_UINT8, 4).toString()
                            var min = data.getIntValue(Data.FORMAT_UINT8, 5).toString()
                            var sec = data.getIntValue(Data.FORMAT_UINT8, 6).toString()

                            if (month.toInt() < 10) {
                                month = "0$month"
                            }
                            if (day.toInt() < 10) {
                                day = "0$day"
                            }
                            if (hour.toInt() < 10) {
                                hour = "0$hour"
                            }
                            if (min.toInt() < 10) {
                                min = "0$min"
                            }
                            if (sec.toInt() < 10) {
                                sec = "0$sec"
                            }

                            date.postValue("$day/$month/$year.$hour:$min:$sec")
                        }
                        enableNotifications(currentTimeChar).enqueue()

                        setNotificationCallback(buttonClickChar).with { _: BluetoothDevice, data: Data ->
                            click.postValue(data.getIntValue(Data.FORMAT_UINT8, 0))
                        }
                        enableNotifications(buttonClickChar).enqueue()
                    }

                    override fun onDeviceDisconnected() {
                        //we reset services and characteristics
                        timeService = null
                        currentTimeChar = null
                        symService = null
                        integerChar = null
                        temperatureChar = null
                        buttonClickChar = null
                    }
                }
            }
            return mGattCallback!!
        }

        fun readTemperature(): Boolean {
            /*  TODO
                on peut effectuer ici la lecture de la caractéristique température
                la valeur récupérée sera envoyée à l'activité en utilisant le mécanisme
                des MutableLiveData
                On placera des méthodes similaires pour les autres opérations
            */
            return if (temperatureChar != null) {
                readCharacteristic(temperatureChar).with { _: BluetoothDevice, data: Data -> kotlin.run {
                    temperature.postValue(data.getIntValue(Data.FORMAT_UINT16, 0)?.div(10).toString())
                    
                    log(0,data.getIntValue(Data.FORMAT_UINT16, 0)?.div(10).toString())
                }
                }.enqueue()
                true
            } else {
                false
            }
        }
    }

    companion object {
        private val TAG = BleOperationsViewModel::class.java.simpleName
    }

    init {
        ble.setConnectionObserver(bleConnectionObserver)
    }

}