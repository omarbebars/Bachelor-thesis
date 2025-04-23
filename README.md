# BLE Biosignal Sensor Mock-Up

**Bachelor Thesis Project – HTW Berlin**  
*Development of a Software-based Mock-Up for Bluetooth Low Energy Biosignal Sensors*

## Overview
This project presents a software-based mock-up system that simulates biosignal sensors using Bluetooth Low Energy (BLE). The system was designed to provide a controllable test environment and preserve user data privacy by avoiding real sensor data, which can raise ethical concerns.

## Objectives
- Simulate various biosignal waveforms (Sine, Square, Triangle) without physical sensors.
- Enable BLE communication between multiple mock and real sensors and a mobile application.
- Visualize signals in real time on Android.
- Generate mock Photoplethysmography (PPG) signals for offline analysis.
- Explore privacy-friendly biosignal processing methods using open-source tools.

## Technologies Used
- **ESP32** – BLE + Wi-Fi microcontroller for signal simulation
- **Pulse Oximeter (SpO2 Sensor)** – BLE sensor transmitting real biosignals
- **Alarm Sensor (BLE Button)** – BLE server for triggering alerts
- **Arduino IDE** – Microcontroller programming
- **Android Studio (Java)** – Android app development (BLE client)
- **Python + NeuroKit2** – Simulated PPG signal generation and analysis
- **BLE Protocol** – Low energy, short-range communication

## System Architecture
- **ESP32** acts as a BLE server, generating configurable waveforms via mathematical functions.
- **Android App** connects as a BLE client to:
  - The **ESP32 mock sensor**
  - A **Pulse Oximeter sensor** providing real SpO2 and heart rate values
  - An **Alarm sensor** acting as a BLE server to simulate emergency button alerts
- NeuroKit2 is used for mock PPG generation and signal analysis without real-world data.

## Features
- Real-time signal simulation and visualization
- Adjustable parameters for signal control
- BLE integration with multiple sensor types (mock and real)
- Alert system integration via BLE alarm sensor
- Ethical signal analysis pipeline using mock data
- Modular setup for future sensor integration

## Screenshots / Diagrams
<img src="https://github.com/user-attachments/assets/0215116f-d3b9-41d5-b459-13b9c8509a8d" width="360" height="400" />
<img src="https://github.com/user-attachments/assets/c9c55980-2b64-4624-803f-a5430585984a" width="360" height="400" />
<img src="https://github.com/user-attachments/assets/b3a525f7-0a6e-474d-b48a-49e016477592" width="360" height="400" />
<img src="https://github.com/user-attachments/assets/c0939eb8-422a-47a4-a37e-06e24f5c95e8" width="360" height="400" />






## What I Learned
- BLE communication protocols and multi-device client-server design
- Real-time signal generation and processing
- Mobile development for medical device interfaces
- Using Python tools (NeuroKit2) for biomedical signal simulation
- Addressing ethical challenges in biosignal research with mock data
- Integrating and visualizing data from real sensors (SpO2) and mock sources





