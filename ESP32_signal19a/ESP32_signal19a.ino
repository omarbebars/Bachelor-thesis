#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include <math.h>
#include <PPG_Values.h>

// Constants for signal generation
float frequency = 1.0;           
float dc_part = 50.0;            
int amplitude = 30;              
int samplingRate = 50;          
const int size = 1000;


// Buttons and potentiometer pin constants
const int buttonPinSine = 19;
const int buttonPinSquare = 21;
const int buttonPinTriangle = 22;
const int potPin = 37;
const int buttonPinPPG = 25;


// BLE UUIDs
#define SERVICE_UUID        "ebf2b73d-dfa2-4213-9203-37b161103e98" 
#define CHARACTERISTIC_UUID "eee60e57-8045-4567-a683-59997efbcd0a"

BLECharacteristic *pCharacteristic;
bool deviceConnected=false;
int waveType = 0;  
unsigned long lastSampleTime = 0;

class MyServerCallbacks: public BLEServerCallbacks {
    void onConnect(BLEServer* pServer) {
      deviceConnected = true;
      Serial.println("Device connected");
    };

    void onDisconnect(BLEServer* pServer) {
      deviceConnected = false;
      Serial.println("Device disconnected");
      // Restart advertising
      pServer->startAdvertising();
    }
};


// handles interput opeartion for button presses
void IRAM_ATTR controlSineButton() {
  waveType = 0;
}

void IRAM_ATTR controlSquareButton() {
  waveType = 1;
}

void IRAM_ATTR controlTriangleButton() {
  waveType = 2;
}

void IRAM_ATTR controlPPGButton() {
  waveType = 3;
}


void setup() {
  Serial.begin(9600);

 // BLE
  BLEDevice::init("ESP32");
  BLEServer *pServer = BLEDevice::createServer();
  BLEService *pService = pServer->createService(SERVICE_UUID);
  pCharacteristic = pService->createCharacteristic(
                      CHARACTERISTIC_UUID,
                      BLECharacteristic::PROPERTY_READ | 
                      BLECharacteristic::PROPERTY_NOTIFY
                    );
  pCharacteristic->setValue("connected");
  pService->start();

  BLEAdvertising *pAdvertising = pServer->getAdvertising();
  pAdvertising->addServiceUUID(SERVICE_UUID);
  pAdvertising->setScanResponse(true);
  pAdvertising->setMinPreferred(0x06);
  pAdvertising->setMinPreferred(0x12);
  BLEDevice::startAdvertising();

  // Setting up pins and attach interrupts
  pinMode(buttonPinSine, INPUT_PULLUP);
  pinMode(buttonPinSquare, INPUT_PULLUP);
  pinMode(buttonPinTriangle, INPUT_PULLUP);
  pinMode(potPin, INPUT_PULLUP);
  pinMode(buttonPinPPG, INPUT_PULLUP);
  attachInterrupt(digitalPinToInterrupt(buttonPinSine), controlSineButton, RISING);
  attachInterrupt(digitalPinToInterrupt(buttonPinSquare), controlSquareButton, RISING);
  attachInterrupt(digitalPinToInterrupt(buttonPinTriangle), controlTriangleButton, RISING);
  attachInterrupt(digitalPinToInterrupt(buttonPinPPG), controlPPGButton, RISING);
}

void loop() {
    

    unsigned long currentTime = millis();
    if (currentTime - lastSampleTime >= (1000 / samplingRate)) {
        
    lastSampleTime = currentTime;

    // potentiometer value and mapping from low frequency to high one
    int potValue = analogRead(potPin) / 20;
    long rawPotValue = map(potValue, 0, 204, 5, 10); // Map to a frequency range of 5 Hz to 10 Hz
      frequency = rawPotValue / 10.0 * 2.0;
     
     //  normalized time value
      float time = (float)(currentTime % 1000000) / 1000;

      float waveValue = 0.0;
     // signal Generation
      if (waveType == 0) {
          // Generate sine signal value
          waveValue = dc_part + amplitude * sin(2.0 * PI * frequency * time);
      } else if (waveType == 1) {
          // Generate square signal value
          waveValue = dc_part + (amplitude * ((sin(2.0 * PI * frequency * time) >= 0.0) ? 1 : -1));
      } else if (waveType == 2) {
          // Generate triangle signal value
          waveValue = dc_part + (amplitude * (2.0 / PI) * asin(sin(2.0 * PI * frequency * time)));
      } else if (waveType == 3) {
        static int ppgIndex = 0;
        waveValue =10+45* precomputedPPG[ppgIndex]; // 10 is the dc_part(baseline) , 45 is the Amplitude for only the PPG
        ppgIndex = (ppgIndex + 1) % size;
      }

      // Printing the waveValue and Frequency on the Serial monitor
      Serial.println(waveValue);
      

      // Converting float to byte for BLE transmission 
      int waveValueInt = (int)round(waveValue);
      byte waveBytePaket[5] = {0,0,0,0,0};
      
      // ppg
      waveBytePaket[1] = (byte)waveValueInt;

      // heart rate
      waveBytePaket[3] = (byte) (waveValueInt*frequency*60); 
     
     // O2 saturation
     waveBytePaket[4] = (byte) (waveValueInt); 
       

      // Sending wave value as byte via BLE to Andoid App
      
      pCharacteristic->setValue(waveBytePaket, 5);
      pCharacteristic->notify();
      
        
    }
}

