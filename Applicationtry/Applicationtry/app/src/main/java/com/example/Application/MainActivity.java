/**
 * MainActivity is the main entry point of the application.
 * It handles Bluetooth connectivity, user interface elements,
 * and data visualization using a GraphView. It also manages
 * permissions related to Bluetooth and location services.
 */
package com.example.Application;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.applicationtrial.R;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

public class MainActivity extends AppCompatActivity implements DataReciever {
    private static final String TAG = "MainActivity";
    private static final int REQUEST_BLUETOOTH_PERMISSIONS = 1;

    // Creating instances of the Classes
    private BluetoothAdapter bluetoothAdapter;
    private Spo2Device spo2Device;
    private MockupDevice mockupDevice;
    private Alert AlertSensor;

    private Button connectButton;
    private Button spo2Button;
    private Button mockupButton;
    private Button AlertButton;
    public static TextView textViewSpO2;
    public static TextView textViewPulseRate;
    public static TextView textViewMockup;
    public static TextView warningBox;
    private Handler handler;
    private GraphView graph;
    private LineGraphSeries series,series2;
    private int counter;
    /**
     * MainActivity class implements the main functionality of the app, including:
     * - Handling Bluetooth connection for SpO2 and mockup devices.
     * - Displaying real-time data in a GraphView.
     * - Managing user interface elements like buttons and TextViews.
     * - Handling permissions for Bluetooth and location access.
     * - Applying window insets for a better user interface experience.
     */

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        //the graph view ID
        graph = findViewById(R.id.graph);
        counter=0;

        // series of data points
        series = new LineGraphSeries<>(new DataPoint[] {
                new DataPoint(0, 1),
                new DataPoint(1, 5),
                new DataPoint(2, 3),
                new DataPoint(3, 2),
                new DataPoint(4, 6)
        });
        counter=4;



        graph.addSeries(series);



        graph.getViewport().setScalable(true);
        graph.getViewport().setScrollable(true);
        graph.getViewport().setYAxisBoundsManual(true);
        graph.getViewport().setMinY(0);
        graph.getViewport().setMaxY(150);
        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setMinX(0);
        graph.getViewport().setMaxX(100);


        // UI elements
        textViewSpO2 = findViewById(R.id.textViewSpO2);
        textViewPulseRate = findViewById(R.id.textViewPulseRate);
        textViewMockup= findViewById(R.id.textViewMockup);
        connectButton = findViewById(R.id.button3);
        spo2Button = findViewById(R.id.button2);
        mockupButton = findViewById(R.id.button1);
        warningBox = findViewById(R.id.warningBox);
        AlertButton=findViewById(R.id.button4);

        // Initializing Bluetooth connection
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        spo2Device = new Spo2Device(this, bluetoothAdapter, this);
        mockupDevice = new MockupDevice(this, bluetoothAdapter, this);
        Alert AlertSensor = new Alert(this, bluetoothAdapter, this);


        connectButton.setOnClickListener(v -> {
            Toast.makeText(getApplicationContext(), "Starting and Checking permissions...", Toast.LENGTH_SHORT).show();

            checkBluetooth();
            checkPermissions();

        });

        spo2Button.setOnClickListener(view -> spo2Device.connect());
        mockupButton.setOnClickListener(view -> mockupDevice.connect());
        AlertButton.setOnClickListener(view -> AlertSensor.connectAlert());
        handler = new Handler();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
    /**
     * this method is resposible for adding data points to the Graph
     * Adds a data point to the graph series and updates the counter.
     * @param dataPoint The data point to be added to the graph.
     */
       // this method is resposible for adding data points to the Graph
    public void addDataPoint(int dataPoint) {
        series.appendData(new DataPoint(counter, dataPoint), true, 100);
        counter++;
    }

    /**
     * Checks if the necessary Bluetooth and location permissions are granted,
     * and requests them if not.
     */

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            Log.d(TAG, "Requesting permissions");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.ACCESS_FINE_LOCATION},
                        REQUEST_BLUETOOTH_PERMISSIONS);
            }
        } else {
            Log.d(TAG, "Permissions already granted");
            Toast.makeText(getApplicationContext(), "Permissions are granted.", Toast.LENGTH_SHORT).show();
            checkBluetooth();
        }
    }

    /**
     * Checks if Bluetooth is enabled, and if not, the user has to enable it.
     */
    @SuppressLint("MissingPermission")
    private void checkBluetooth() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (bluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(), "Bluetooth is not avalible", Toast.LENGTH_SHORT).show();
        }

        if (!bluetoothAdapter.isEnabled()) {
            Toast.makeText(getApplicationContext(), "please enable bluetooth", Toast.LENGTH_SHORT).show();
            Intent enableIntent = new Intent(bluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_BLUETOOTH_PERMISSIONS);
        }
    }

    /**
     * Closes Bluetooth connections when the activity is no longer visible.
     */
    @Override
    public void onStop() {
        super.onStop();


        if (spo2Device != null) {
            spo2Device.closeConnection();
        }

        if (mockupDevice != null) {
            mockupDevice.closeConnectionMock();
        }


    }




}



