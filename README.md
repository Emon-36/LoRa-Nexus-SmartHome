# 🏠 Smart Hub Pro - Hybrid IoT Home Automation

A professional-grade Home Automation system featuring a hybrid communication architecture. Control your home devices seamlessly through a **Modern Android App**, **Firebase Realtime Database**, and **Long-Range (LoRa) Wireless Remotes**.

---

## 🚀 Key Features

*   **Hybrid Control:** Simultaneous control via Internet (Firebase) and Long Range Radio (LoRa 433MHz).
*   **Modern UI:** A clean, Jetpack Compose-based Android dashboard with real-time state synchronization.
*   **Dynamic Provisioning:** Configure WiFi credentials and room/device names directly from the app via Bluetooth Serial Handshake.
*   **Smart Multi-Stage Control:** 
    *   Full bulb toggle logic.
    *   4-Stage discrete Fan speed controller (25%, 50%, 75%, 100%).
*   **Persistent Storage:** Hub remembers your configuration even after power loss using ESP32 Preferences.
*   **Industrial Connectivity:** LoRa SF12 configuration for maximum wall penetration and distance.

---

## 🛠 Circuit Diagram (Estimated)

### 1. Central Hub (Receiver)
The Central Hub acts as the bridge between your phone, the internet, and the physical relays.

| ESP32 Pin | Component | Description |
| :--- | :--- | :--- |
| **GPIO 13** | Relay A | Fan Speed 50% (Active LOW) |
| **GPIO 32** | Relay B | Fan Speed 25% (Active LOW) |
| **GPIO 33** | Relay C | Main Bulb (Active LOW) |
| **GPIO 21** | Relay D | Fan Speed 75% (Active LOW) |
| **GPIO 25** | Relay E | Fan Speed 100% (Active LOW) |
| **GPIO 5, 19, 27, 18** | SX1278 | LoRa SPI (SCK, MISO, MOSI, SS) |
| **GPIO 14, 26** | SX1278 | LoRa Control (RST, DIO0) |

### 2. Remote Control (Transmitter)
A portable remote for local offline control.

| ESP32 Pin | Component | Function |
| :--- | :--- | :--- |
| **GPIO 15** | Button A | Toggle Bulb / Start Fan |
| **GPIO 4**  | Button B | Set Fan 50% |
| **GPIO 34** | Button C | Multi-Function |
| **GPIO 32** | Button D | Set Fan 75% |
| **GPIO 35** | Button E | Set Fan 100% |

---

## 📱 Android App Setup

1.  **Firebase Configuration:**
    *   Place your `google-services.json` in the `app/` directory.
    *   Ensure Realtime Database rules are set to public for initial testing.
2.  **Bluetooth Handshake:**
    *   Pair your phone with `ESP32_Smart_Hub_01`.
    *   Inside the app, go to **Configure** to send WiFi and Room details. The Hub will confirm with a `READY` message and reboot to apply settings.

---

## 📂 Project Structure

*   `/app`: Source code for the Android application (Kotlin/Compose).
*   `/ESP_firmware`:
    *   `ESP_receiver_LoRa_Firebase.ino`: The main Hub logic.
    *   `Esp_transmitter_LoRa.ino`: The remote control logic.

---

## ⚖️ License
This project is for educational and DIY purposes. Ensure proper electrical safety precautions when working with high-voltage relays.
