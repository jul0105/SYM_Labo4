package ch.heigvd.iict.sym_labo4

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.util.Log
import android.view.Window
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import ch.heigvd.iict.sym_labo4.gl.OpenGLRenderer

/**
 * Project: Labo4
 * Created by fabien.dutoit on 21.11.2016
 * Updated by fabien.dutoit on 06.11.2020
 * Updated by Julien Béguin, Robin Cuénoud & Gaëtan Daubresse on 06.01.2021
 * (C) 2016 - HEIG-VD, IICT
 */
class CompassActivity : AppCompatActivity(), SensorEventListener {

    // opengl
    private lateinit var opglr: OpenGLRenderer
    private lateinit var m3DView: GLSurfaceView

    // Sensors
    private lateinit var sensorManager: SensorManager
    private lateinit var accelerometer: Sensor
    private lateinit var magneticField: Sensor

    // Sensors data
    private var accelerometerData = FloatArray(3)
    private var magneticFieldData = FloatArray(3)
    private var rotationMatrix = FloatArray(16)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // we need fullscreen
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)

        // we initiate the view
        setContentView(R.layout.activity_compass)

        // we create the renderer
        opglr = OpenGLRenderer(applicationContext)

        // link to GUI
        m3DView = findViewById(R.id.compass_opengl)

        // init opengl surface view
        m3DView.setRenderer(opglr)

        // Get sensors
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        magneticField = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
    }

    /*
        TODO
        your activity need to register to accelerometer and magnetometer sensors' updates
        then you may want to call
        opglr.swapRotMatrix()
        with the 4x4 rotation matrix, every time a new matrix is computed
        more information on rotation matrix can be found online:
        https://developer.android.com/reference/android/hardware/SensorManager.html#getRotationMatrix(float[],%20float[],%20float[],%20float[])
    */

    override fun onResume() {
        super.onResume()
        // Register to sensors update
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
        sensorManager.registerListener(this, magneticField, SensorManager.SENSOR_DELAY_UI)
    }

    override fun onPause() {
        super.onPause()
        // Unregister to sensors update
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        // Set sensors value
        when (event?.sensor?.type) {
            Sensor.TYPE_ACCELEROMETER -> accelerometerData = event.values
            Sensor.TYPE_MAGNETIC_FIELD -> magneticFieldData = event.values
        }

        // Get rotation matrix
        SensorManager.getRotationMatrix(rotationMatrix, null, accelerometerData, magneticFieldData)

        // Swap rotation matrix
        rotationMatrix = opglr.swapRotMatrix(rotationMatrix)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // 0 = Unreliable, 1 = Low accuracy, 2 = Medium accuracy, 3 = High accuracy
        Log.i("CompassActivity", "Accuracy of sensor '${sensor?.name}' changed to $accuracy")
    }
}
