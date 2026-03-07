#include <Arduino.h>
#include <WiFi.h>
#include <FirebaseESP32.h>
#include <SPI.h>
#include <LoRa.h>
#include "BluetoothSerial.h"
#include <Preferences.h>

#define SCK 5
#define MISO 19
#define MOSI 27
#define SS 18
#define RST 14
#define DIO0 26

#define RELAY_A 13 // Fan 50%
#define RELAY_B 32 // Fan 25%
#define RELAY_C 33 // Bulb
#define RELAY_D 21 // Fan 75%
#define RELAY_E 25 // Fan 100%

#define DATABASE_SECRET "YOUR_DATABASE_SECRET"
#define DATABASE_URL "YOUR_DATABASE_URL"

BluetoothSerial SerialBT;
Preferences preferences;
FirebaseData fbdo;
FirebaseAuth auth;
FirebaseConfig config;

String roomName, deviceList, ssid, pass;
bool isConfigured = false;


int currentBulbState = 0; 
int currentFanSpeed = 0;
unsigned long lastLoRaUpdate = 0; 


void syncWithFirebase();
void setFanSpeed(int speed);
void handleLoRaCommand(char cmd);
void updateFirebase(String deviceType, int value);

void setup() {
  Serial.begin(115200);
  

  pinMode(RELAY_A, OUTPUT); pinMode(RELAY_B, OUTPUT);
  pinMode(RELAY_C, OUTPUT); pinMode(RELAY_D, OUTPUT);
  pinMode(RELAY_E, OUTPUT);


  digitalWrite(RELAY_A, HIGH); digitalWrite(RELAY_B, HIGH);
  digitalWrite(RELAY_C, HIGH); digitalWrite(RELAY_D, HIGH);
  digitalWrite(RELAY_E, HIGH);


  SerialBT.register_callback([](esp_spp_cb_event_t event, esp_spp_cb_param_t *param){
    if(event == ESP_SPP_SRV_OPEN_EVT){
      Serial.println("✅ Bluetooth Connected!");
      delay(200);
      SerialBT.println("READY"); 
    }
    if(event == ESP_SPP_CLOSE_EVT){
      Serial.println("❌ Bluetooth Disconnected!");
    }
  });

  if(!SerialBT.begin("ESP32_Smart_Hub_01")) {
    Serial.println("BT Init Failed!");
  } else {
    Serial.println("BT Device Ready: ESP32_Smart_Hub_01");
  }


  preferences.begin("settings", false);
  roomName = preferences.getString("room", "");
  deviceList = preferences.getString("devices", "");
  ssid = preferences.getString("ssid", "");
  pass = preferences.getString("pass", "");

  if (roomName != "" && ssid != "") {
    isConfigured = true;
    WiFi.begin(ssid.c_str(), pass.c_str());
    Serial.print("Connecting WiFi");
    int count = 0;
    while (WiFi.status() != WL_CONNECTED && count < 30) { 
      delay(1000); Serial.print("."); count++; 
    }
    
    if(WiFi.status() == WL_CONNECTED){
      Serial.println("\nWiFi Connected!");
      config.database_url = DATABASE_URL;
      config.signer.tokens.legacy_token = DATABASE_SECRET;
      Firebase.begin(&config, &auth);
      Firebase.reconnectWiFi(true);
    }
  }


  SPI.begin(SCK, MISO, MOSI, SS);
  LoRa.setPins(SS, RST, DIO0);
  if (!LoRa.begin(433E6)) {
    Serial.println("LoRa Init Error!");
  } else {
    LoRa.setSyncWord(0xF3);
    LoRa.setSpreadingFactor(12);
    Serial.println("✅ LoRa Initialized (SF12)");
  }
}

void loop() {

  if (SerialBT.available()) {
    String data = SerialBT.readStringUntil('\n');
    data.trim();
    if (data == "RESET") {
      preferences.clear();
      SerialBT.println("RESET_SUCCESS");
      delay(1000); ESP.restart();
    } else if (data.startsWith("ROOM:")) {
      roomName = data.substring(data.indexOf("ROOM:") + 5, data.indexOf(";DEVICES"));
      deviceList = data.substring(data.indexOf("DEVICES:") + 8, data.indexOf(";SSID"));
      ssid = data.substring(data.indexOf("SSID:") + 5, data.indexOf(";PASS"));
      pass = data.substring(data.indexOf("PASS:") + 5);
      preferences.putString("room", roomName);
      preferences.putString("devices", deviceList);
      preferences.putString("ssid", ssid);
      preferences.putString("pass", pass);
      SerialBT.println("CONFIG_OK");
      delay(2000); ESP.restart();
    }
  }


  if (isConfigured && WiFi.status() == WL_CONNECTED && Firebase.ready()) {
    if (millis() - lastLoRaUpdate > 3000) { 
      static uint32_t lastSync = 0;
      if (millis() - lastSync > 1000) { 
        lastSync = millis();
        syncWithFirebase();
      }
    }
  }


  int packetSize = LoRa.parsePacket();
  if (packetSize) {
    while (LoRa.available()) {
      char loraCmd = (char)LoRa.read();
      Serial.print("LoRa CMD Received: "); Serial.println(loraCmd);
      handleLoRaCommand(loraCmd);
    }
  }
}

void syncWithFirebase() {
  String temp = deviceList;
  while (temp.length() > 0) {
    int comma = temp.indexOf(',');
    String device = (comma == -1) ? temp : temp.substring(0, comma);
    device.trim();
    String path = "/" + roomName + "/" + device + "/";

    if (Firebase.getInt(fbdo, path)) {
      int val = fbdo.intData();
      if (device.indexOf("bulb") >= 0) {
        if(val != currentBulbState) {
          currentBulbState = val;
          digitalWrite(RELAY_C, (currentBulbState == 1) ? LOW : HIGH);
        }
      } 
      else if (device.indexOf("fan") >= 0) {
        if(val != currentFanSpeed) {
          currentFanSpeed = val;
          setFanSpeed(currentFanSpeed);
        }
      }
    }
    if (comma == -1) break;
    temp = temp.substring(comma + 1);
  }
}

void handleLoRaCommand(char cmd) {
  lastLoRaUpdate = millis(); 

  if (cmd == 'C') {
    currentBulbState = (currentBulbState == 0) ? 1 : 0;
    digitalWrite(RELAY_C, (currentBulbState == 1) ? LOW : HIGH);
    updateFirebase("bulb", currentBulbState);
  }
  else if (cmd == 'A') { currentFanSpeed = 25;  setFanSpeed(25);  updateFirebase("fan", 25); }
  else if (cmd == 'B') { currentFanSpeed = 50;  setFanSpeed(50);  updateFirebase("fan", 50); }
  else if (cmd == 'D') { currentFanSpeed = 75;  setFanSpeed(75);  updateFirebase("fan", 75); }
  else if (cmd == 'E') { currentFanSpeed = 100; setFanSpeed(100); updateFirebase("fan", 100); }
  else if (cmd == 'S') { currentFanSpeed = 0;   setFanSpeed(0);   updateFirebase("fan", 0); }
}

void updateFirebase(String deviceType, int value) {
  String temp = deviceList;
  while (temp.length() > 0) {
    int comma = temp.indexOf(',');
    String deviceName = (comma == -1) ? temp : temp.substring(0, comma);
    deviceName.trim();
    
    if (deviceName.indexOf(deviceType) >= 0) {
      String path = "/" + roomName + "/" + deviceName + "/";
      Firebase.setInt(fbdo, path, value); 
      Serial.println("Update Firebase Path: " + path + " -> " + String(value));
      break;
    }
    if (comma == -1) break;
    temp = temp.substring(comma + 1);
  }
}

void setFanSpeed(int speed) {
  digitalWrite(RELAY_B, HIGH); digitalWrite(RELAY_A, HIGH);
  digitalWrite(RELAY_D, HIGH); digitalWrite(RELAY_E, HIGH);
  if (speed == 25) digitalWrite(RELAY_B, LOW);
  else if (speed == 50) digitalWrite(RELAY_A, LOW);
  else if (speed == 75) digitalWrite(RELAY_D, LOW);
  else if (speed == 100) digitalWrite(RELAY_E, LOW);
}