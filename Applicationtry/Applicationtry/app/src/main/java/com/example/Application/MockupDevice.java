/**
 * The {MockupDevice} class manages the connection to a mockup Bluetooth Low Energy (BLE) device.
 * It scans for the device, connects to it, and handles services and characteristics for data retrieval.
 * This class is responsible for interacting with the BLE GATT server and updating the UI with
 * received data such as mockup heart rate and SpO2 values.
 */
package com.example.Application;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;

import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;


import androidx.core.content.ContextCompat;

import java.util.UUID;

public class MockupDevice {
    private static final String TAG = "MockupDevice";
    private static final String MOCKUP_DEVICE_NAME = "ESP32_BLE_Server"; // Replace with actual device name
    private static final String MOCKUP_SERVICE_UUID = "ebf2b73d-dfa2-4213-9203-37b161103e98";
    private static final String MOCKUP_CHARACTERISTIC_UUID = "eee60e57-8045-4567-a683-59997efbcd0a";
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothGatt bluetoothGatt;
    private Context context;
    private DataReciever reciever;

    /**
     * Constructor for the {MockupDevice} class.
     *
     * @param context           the application context
     * @param bluetoothAdapter  the Bluetooth adapter for managing Bluetooth connections
     * @param reciever          the receiver for processing incoming data
     */
    public MockupDevice(Context context, BluetoothAdapter bluetoothAdapter, DataReciever reciever) {
        this.context = context;
        this.bluetoothAdapter = bluetoothAdapter;
        this.reciever = reciever;
    }

    /**
     * It starts the process of connecting to the BLE mockup device.
     * Ensures that Bluetooth is enabled and the necessary permissions are granted.
     */

    @SuppressLint("MissingPermission")
    public void connect() {
        if (!bluetoothAdapter.isEnabled()) {
            Log.e(TAG, "Bluetooth is not enabled.");
            return;
        }

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Permission BLUETOOTH_SCAN not granted");
            return;
        }

        bluetoothAdapter.startLeScan(scanCallback);
    }
    /**
     * Callback for the BLE scan process.
     * When a device matching the {@code MOCKUP_DEVICE_NAME} is found, it stops the scan and connects.
     */
    private final BluetoothAdapter.LeScanCallback scanCallback = new BluetoothAdapter.LeScanCallback() {
        @SuppressLint("MissingPermission")
        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
            if (device.getName() != null && device.getName().equals(MOCKUP_DEVICE_NAME)) {
                Log.d(TAG, "Found device: " + device.getName());
                bluetoothAdapter.stopLeScan(this);
                connect(device);
            }
        }
    };
    /**
     *This is responsible for  Connecting to the specified Bluetooth device.
     *
     * @param device the Bluetooth device to connect to
     */
    @SuppressLint("MissingPermission")
    private void connect(BluetoothDevice device) {
        if (device == null) {
            Log.e(TAG, "Device not found. Unable to connect.");
            return;
        }

        Log.d(TAG, "Connecting to device: " + device.getName());
        bluetoothGatt = device.connectGatt(context, false, gattCallback);
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        /**
         * This is called when the connection state changes.
         *
         * @param gatt    the GATT client
         * @param status  the status of the connection
         * @param newState the new connection state
         */
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i(TAG, "Connected to GATT server.");
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    Log.e(TAG, "Permission BLUETOOTH_CONNECT not granted");
                    return;
                }
                gatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i(TAG, "Disconnected from GATT server.");

            }
        }

        /**
         * This is called when GATT services are discovered.
         *
         * @param gatt   the GATT client
         * @param status the status of service discovery
         */
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "Services discovered");
                BluetoothGattService service = gatt.getService(UUID.fromString(MOCKUP_SERVICE_UUID));
                if (service != null) {
                    BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString(MOCKUP_CHARACTERISTIC_UUID));
                    if (characteristic != null) {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                            Log.e(TAG, "Permission BLUETOOTH_CONNECT not granted");
                            return;
                        }
                        gatt.setCharacteristicNotification(characteristic, true);
                    }
                } else {
                    Log.e(TAG, "Service not found");
                }
            } else {
                Log.e(TAG, "onServicesDiscovered received: " + status);
            }
        }

        /**
         * This is called when a characteristic change is received.
         *
         * @param gatt           the GATT client
         * @param characteristic the characteristic that changed
         */
        @SuppressLint("SetTextI18n")
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            if (MOCKUP_CHARACTERISTIC_UUID.equals(characteristic.getUuid().toString())) {
                byte[] value = characteristic.getValue();

                if (context instanceof MainActivity) {
                    ((MainActivity) context).runOnUiThread(() -> {

                        if (value != null && value.length >= 5) {

                            // Extracting the values from the byte array
                            int ppgValue = value[1] & 0xFF;           // PPG value
                            int heartRateValue = (value[3] & 0xFF);   // Heart rate
                            int o2SaturationValue = (value[4] & 0xFF);  // O2 saturation value


                            // Updating the TextView on Main Thread
                            MainActivity.textViewMockup.setText("PPG: " + ppgValue +
                                    ", HeatRate: " + heartRateValue + "bpm" +
                                    ", SPO2: " + o2SaturationValue + "%");


                            ((MainActivity) context).addDataPoint(ppgValue);

                        } else {
                            Log.e(TAG, "Received value is null or does not have the expected length.");
                        }


                    });

                }
            }
        }
    };
    /**
     * This closes the connection to the mockup device.
     */
    @SuppressLint("MissingPermission")
    public void closeConnectionMock() {
        if (bluetoothGatt != null) {
            bluetoothGatt.close();
            bluetoothGatt = null;
        }
    }

}

















