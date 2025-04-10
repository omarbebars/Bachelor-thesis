#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include <BLE2902.h>

BLEServer* pServer = NULL;
BLECharacteristic* pCharacteristic = NULL;
bool deviceConnected = false;
uint8_t value = 0; // 0 for LOW, 1 for HIGH
bool oldState = LOW; // Keep track of the previous state

const int BUTTON_PIN = 26; // Button pin


#define SERVICE_UUID         "295949c7-e88e-4a9b-8002-0ad3856ed627"
#define CHARACTERISTIC_UUID "861df530-5370-4847-aea7-0937abd4c788"

class MyServerCallbacks: public BLEServerCallbacks {
    void onConnect(BLEServer* pServer) {
        deviceConnected = true;
    }

    void onDisconnect(BLEServer* pServer) {
        deviceConnected = false;
    }
};

void setup() {
    Serial.begin(115200);

    // Create the BLE Device
    BLEDevice::init("Alert");

    // Create the BLE Server
    pServer = BLEDevice::createServer();
    pServer->setCallbacks(new MyServerCallbacks());

    // Create the BLE Service
    BLEService *pService = pServer->createService(SERVICE_UUID);

    // Create a BLE Characteristic
    pCharacteristic = pService->createCharacteristic(
                        CHARACTERISTIC_UUID,
                        BLECharacteristic::PROPERTY_READ   |
                        BLECharacteristic::PROPERTY_NOTIFY |
                        BLECharacteristic::PROPERTY_INDICATE
                      );

    // This Creates a BLE Descriptor
    pCharacteristic->addDescriptor(new BLE2902());

    // This starts the service
    pService->start();

    // Start advertising
    BLEAdvertising *pAdvertising = BLEDevice::getAdvertising();
    pAdvertising->addServiceUUID(SERVICE_UUID);
    pAdvertising->setScanResponse(false);
    pAdvertising->setMinPreferred(0x0); // set value to 0x00 to not advertise this parameter
    BLEDevice::startAdvertising();
    Serial.println("Waiting for BLE client to connect...");

    pinMode(BUTTON_PIN, INPUT_PULLUP); // 
}

void loop() {
    bool currentState = digitalRead(BUTTON_PIN) == LOW; // Button pressed when LOW
    
    // If the button state has changed
    if (currentState != oldState) {
        value = currentState ? 5 : 2; // value based on button state when pressed is 5 / when not pressed is 2
        
        // Notify only if there is a connected client
        if (deviceConnected) {
            pCharacteristic->setValue(&value, sizeof(value)); // Set the value to notify
            pCharacteristic->notify(); // Send notification to the Android app
            Serial.print("Current value: ");
            Serial.println(value);
        }
        
        oldState = currentState; // This Update the old state to current state

        delay(50); // Simple debounce delay
    }
}

