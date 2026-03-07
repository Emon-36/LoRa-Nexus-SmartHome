#include <SPI.h>
#include <LoRa.h>

#define SCK 5
#define MISO 19
#define MOSI 27
#define SS 18
#define RST 14
#define DIO0 26


#define BTN_A 15
#define BTN_B 4
#define BTN_C 34  // Needs external pull-down
#define BTN_D 32
#define BTN_E 35  // Needs external pull-down

void setup() {
  Serial.begin(115200);

  pinMode(RST, OUTPUT);
  digitalWrite(RST, HIGH);
  delay(100);
  digitalWrite(RST, LOW);
  delay(100);
  digitalWrite(RST, HIGH);


  SPI.begin(SCK, MISO, MOSI, SS);
  LoRa.setPins(SS, RST, DIO0);

  if (!LoRa.begin(433E6)) {
    Serial.println("TX Init Failed");
    while (1);
  }
  
  LoRa.setSyncWord(0xF3);
  LoRa.setTxPower(20); 
  LoRa.setSpreadingFactor(12);

  pinMode(BTN_A, INPUT);
  pinMode(BTN_B, INPUT);
  pinMode(BTN_C, INPUT);
  pinMode(BTN_D, INPUT);
  pinMode(BTN_E, INPUT);

  Serial.println("✅ TX Ready. Smash some buttons.");
}

void sendCommand(char cmd) {
  Serial.print("Sending: ");
  Serial.println(cmd);
  
  LoRa.beginPacket();
  LoRa.print(cmd);
  LoRa.endPacket();
}

void loop() {
  // Logic: Send once per press, then wait a bit to prevent spam
  if (digitalRead(BTN_A) == HIGH) {delay(300);  if (digitalRead(BTN_B) == HIGH) { sendCommand('S'); delay(300); } else { sendCommand('A'); }}
  if (digitalRead(BTN_B) == HIGH) { sendCommand('B'); delay(300); }
  if (digitalRead(BTN_C) == HIGH) { sendCommand('C'); delay(300); }
  if (digitalRead(BTN_D) == HIGH) { sendCommand('D'); delay(300); }
  if (digitalRead(BTN_E) == HIGH) { sendCommand('E'); delay(300); }


  delay(10); // Small stability delay
}