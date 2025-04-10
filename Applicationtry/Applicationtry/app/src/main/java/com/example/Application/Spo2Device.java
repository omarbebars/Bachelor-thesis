/**
 * This class represents a SpO2 sensor device that connects via Bluetooth and retrieves data such as
 * SpO2 levels, pulse rate, and PPG. The device communicates using BLE (Bluetooth Low Energy) and
 * It processes packets of data received from the sensor to update the UI.
 */

package com.example.Application;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;
import androidx.core.content.ContextCompat;
import java.util.UUID;


public class Spo2Device  {
    private static final String TAG = "Spo2Device";
    private static final String SPO2_MAC_ADDRESS = "00:A0:50:0E:18:15";
    private static final String SPO2_SERVICE_UUID = "49535343-fe7d-4ae5-8fa9-9fafd205e455";
    private static final String SPO2_CHARACTERISTIC_UUID = "49535343-1e4d-4bd9-ba61-23c647249616";
    private static final UUID SPO2_CHARACTERISTIC_CONFIG_UUID = UUID.fromString("00002902-0000-1000-8000-00805F9B34FB");
    private static final long UPDATE_INTERVAL_MS = 100;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothGatt bluetoothGatt;
    private Context context;
    private long lastUpdateTime = 0; //  the last update time
    private DataReciever reciever;

    /**
     * Constructor for the Spo2Device class.
     *
     * @param context The application context.
     * @param bluetoothAdapter The Bluetooth adapter used to initiate connections.
     * @param reciever The custom data receiver for handling incoming data.
     */
    public Spo2Device(Context context, BluetoothAdapter bluetoothAdapter, DataReciever reciever) {
        this.context = context;
        this.bluetoothAdapter = bluetoothAdapter;
        this.reciever = reciever;

    }
    /**
     * Connecting to the SpO2 device using its predefined MAC address.
     * Attempts to establish a GATT connection for further communication.
     */
    @SuppressLint("MissingPermission")
    public void connect() {
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(SPO2_MAC_ADDRESS);
        if (device == null) {
            Log.e(TAG, "Device not found. Unable to connect.");
            return;
        }

        Log.d(TAG, "Connecting to device: " + device.getName());
        bluetoothGatt = device.connectGatt(context, false, gattCallback);
    }

    /**
     * Callback for GATT events, including connection state changes, service discovery,
     * characteristic reading, and characteristic changes.
     */
    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int State) {
            if (State == BluetoothProfile.STATE_CONNECTED) {
                Log.i(TAG, "Connected to GATT server.");
                checkAndDiscoverServices(gatt);
            } else if (State == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i(TAG, "Disconnected from GATT server.");
                closeConnection();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "Services discovered");
                setupCharacteristicNotification(gatt);
            } else {
                Log.e(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                handleCharacteristicData(characteristic);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            handleCharacteristicData(characteristic);
        }
    };

    /**
     * This discovers services on the connected GATT server, checking for the necessary
     * Bluetooth permissions.
     *
     * @param gatt The GATT server instance.
     */
    private void checkAndDiscoverServices(BluetoothGatt gatt) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            gatt.discoverServices();
        } else {
            Log.e(TAG, "Permission BLUETOOTH_CONNECT not granted");


        }
    }
    /**
     * Closes the GATT connection to the SpO2 device.
     */
    @SuppressLint("MissingPermission")
    public void closeConnection() {
        if (bluetoothGatt != null) {
            bluetoothGatt.close();
            bluetoothGatt = null;
        }
    }
    /**
     * It sets up notifications for the specified characteristic on the SpO2 service.
     * This enables the app to receive updates when the characteristic value changes.
     *
     * @param gatt The GATT server instance.
     */
    private void setupCharacteristicNotification(BluetoothGatt gatt) {
        BluetoothGattService service = gatt.getService(UUID.fromString(SPO2_SERVICE_UUID));
        if (service != null) {
            BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString(SPO2_CHARACTERISTIC_UUID));
            if (characteristic != null) {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    gatt.setCharacteristicNotification(characteristic, true);
                    BluetoothGattDescriptor descriptor = characteristic.getDescriptor(SPO2_CHARACTERISTIC_CONFIG_UUID);
                    if (descriptor != null) {
                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        gatt.writeDescriptor(descriptor);
                    } else {
                        Log.e(TAG, "Descriptor not found for characteristic");
                    }
                } else {
                    Log.e(TAG, "Permission BLUETOOTH_CONNECT not granted");
                }
            } else {
                Log.e(TAG, "Characteristic not found");
            }
        } else {
            Log.e(TAG, "Service not found");
        }
    }
    /**
     * This handles the received data from the characteristic and processes it into readable values.
     *
     * @param characteristic The BluetoothGattCharacteristic containing the data.
     */
    private void handleCharacteristicData(BluetoothGattCharacteristic characteristic) {
        if (SPO2_CHARACTERISTIC_UUID.equals(characteristic.getUuid().toString())) {
            byte[] data = characteristic.getValue();
            processPacket(data);
        }
    }
    /**
     * It processes the incoming data packet and extracts SpO2, pulse rate, and PPG values.
     * The data is then used to update the UI on the main activity.
     *
     * @param packet The byte array containing the data packet.
     */
    private void processPacket(byte[] packet) {
        if (packet.length < 5) {
            Log.e(TAG, "Packet length is not enough");
            return;
        }

        if (packet.length % 5 != 0) {
            Log.e(TAG, "Packet length is not a multiple of 5");
            return;
        }

        for(int i = 0; i < packet.length - 4; i+=5){
            int spo2 = packet[i + 4];
            int pulseRate = packet[i + 3] | ((packet[2] & 64) << 1);
            int ppg = packet[i + 1];

            ((MainActivity) context).runOnUiThread(() -> updateUI(spo2, pulseRate, ppg));
        }



    }

    /**
     * This is responsible for Updating the UI with the latest SpO2, pulse rate, and PPG values.
     *
     * @param spo2 The SpO2 value (blood oxygen saturation).
     * @param pulseRate The pulse rate (heartbeats per minute).
     * @param ppg The (PPG) value.
     */

    private void updateUI(int spo2, int pulseRate, int ppg) {
        MainActivity mainActivity = (MainActivity) context;
        mainActivity.addDataPoint(ppg);

        MainActivity.textViewSpO2.setText("SpO2: " + spo2 + "%");
        MainActivity.textViewPulseRate.setText("Pulse Rate: " + pulseRate + " bpm");

        if (spo2 < 95 || spo2 > 99) {
            MainActivity.textViewSpO2.append("\nWarning: SpO2 value out of range!");
        }
        if (pulseRate < 60 || pulseRate > 100) {
            MainActivity.textViewPulseRate.append("\nWarning: Pulse Rate value out of range!");
        }

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastUpdateTime >= UPDATE_INTERVAL_MS) {
            lastUpdateTime = currentTime;

        }
    }
}

