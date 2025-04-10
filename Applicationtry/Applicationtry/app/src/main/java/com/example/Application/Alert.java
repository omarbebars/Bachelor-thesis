/**
 * This class represents an Alert sensor that connects via Bluetooth to a remote device
 * and monitors its characteristic values for alerts. It handles Bluetooth connection,
 * GATT service discovery, and notification setup for a specific characteristic.
 */
package com.example.Application;

import static com.example.Application.MainActivity.warningBox;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
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
import android.view.View;

import androidx.core.content.ContextCompat;

import java.util.UUID;

public class Alert {
    private static final String TAG = "Alert Button sensor";
    private static final String Alert_MAC_ADDRESS = "FF:FF:FA:A9:C2:5D";
    private static final UUID Alert_SERVICE_UUID = UUID.fromString("0000FFE0-0000-1000-8000-00805F9B34FB");
    private static final UUID Alert_CHARACTERISTIC_UUID = UUID.fromString("0000FFE1-0000-1000-8000-00805F9B34FB");
    private static final UUID Alert_CHARACTERISTIC_CONFIG_UUID = UUID.fromString("00002902-0000-1000-8000-00805F9B34FB");

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothGatt bluetoothGatt;
    private Context context;
    private final Activity activityContext;
    /**
     * Constructor to initialize the Alert class with the given context and Bluetooth adapter.
     *
     * @param context         The application context
     * @param bluetoothAdapter The Bluetooth adapter for managing connections
     * @param mainActivity     The main activity for updating the UI
     */
    public Alert(Context context, BluetoothAdapter bluetoothAdapter, MainActivity mainActivity) {
        this.context = context;
        this.bluetoothAdapter = bluetoothAdapter;
        this.activityContext = (Activity) context;
    }
    /**
     * It initiates the connection to the alert sensor device using its MAC address.
     * If the device is found, it attempts to connect and discover its services.
     */
    @SuppressLint("MissingPermission")
    public void connectAlert() {
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(Alert_MAC_ADDRESS);
        if (device == null) {
            Log.e(TAG, "Device not found. Unable to connect.");
            return;
        }

        Log.d(TAG, "Connecting to device: " + device.getName());
        bluetoothGatt = device.connectGatt(context, false, gattCallback);
    }
    /**
     * Callback to handle Bluetooth GATT events such as connection state changes,
     * service discovery, and characteristic changes.
     */
    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int state) {
            if (state == BluetoothProfile.STATE_CONNECTED) {
                Log.i(TAG, "Connected to GATT server.");
                checkAndDiscoverServices(gatt);
            } else if (state == BluetoothProfile.STATE_DISCONNECTED) {
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
     * Checks if the necessary permissions are granted and discovers services for the GATT device.
     *
     * @param gatt The Bluetooth GATT object
     */
    private void checkAndDiscoverServices(BluetoothGatt gatt) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            gatt.discoverServices();
        } else {
            Log.e(TAG, "Permission BLUETOOTH_CONNECT not granted");

        }
    }
    /**
     * Closes the connection to the Bluetooth GATT device and cleans up resources.
     */
    @SuppressLint("MissingPermission")
    private void closeConnection() {
        if (bluetoothGatt != null) {
            bluetoothGatt.close();
            bluetoothGatt = null;
        }
    }
    /**
     * Sets up characteristic notifications for the alert button.
     *
     * @param gatt The Bluetooth GATT object
     */
    private void setupCharacteristicNotification(BluetoothGatt gatt) {
        BluetoothGattService service = gatt.getService(Alert_SERVICE_UUID);
        if (service != null) {
            BluetoothGattCharacteristic characteristic = service.getCharacteristic(Alert_CHARACTERISTIC_UUID);
            if (characteristic != null) {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    gatt.setCharacteristicNotification(characteristic, true);
                    BluetoothGattDescriptor descriptor = characteristic.getDescriptor(Alert_CHARACTERISTIC_CONFIG_UUID);
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
     * It Handles the data received from the characteristic and updates the UI if an alert is pressed.
     *
     * @param characteristic The Bluetooth GATT characteristic containing the alert data
     */
    private void handleCharacteristicData(BluetoothGattCharacteristic characteristic) {
        if (Alert_CHARACTERISTIC_UUID.equals(characteristic.getUuid())) {
            byte[] data = characteristic.getValue();
            if (data[0] == 0x01) {  
                activityContext.runOnUiThread(() -> {
                    if (warningBox.getVisibility() == View.VISIBLE) {
                        warningBox.setVisibility(View.GONE);  // this hides the warning text
                    } else {
                        warningBox.setVisibility(View.VISIBLE);  // this shows the warning text
                    }
                });
            }
        }
    }



}
