#include <LiquidCrystal_I2C.h>


String text;
int led = 13 ;
byte snowflake[8] = {
  B00000,
  B00000,
  B10101,
  B01110,
  B00100,
  B01110,
  B10101,
  B00000
};
byte rainDrop[8]={
  B00100,
  B00100,
  B01110,
  B01110,
  B11111,
  B11111,
  B01110,
  B00100
};
LiquidCrystal_I2C lcd = LiquidCrystal_I2C(0x27,16,2);
void setup() {
      Serial.begin(9600);
      pinMode(led, OUTPUT);    
      lcd.init();
      lcd.backlight();
      lcd.createChar(3, snowflake);
      lcd.createChar(0, rainDrop);

}
void loop() {
   
     if (Serial.available()) {
          
          lcd.setCursor(0,0);
          text=Serial.readString();
          
          if (text[3]=='0'){
              lcd.clear();
              digitalWrite(led, LOW);
          } 
          else{
            if(text[1]=='0'){
             lcd.write((byte)0);
             lcd.print("RAIN AHEAD!");
             lcd.write((byte)0);
             //lcd.setCursor(0,1);
            
            
            }
            else if(text[1]=='3'){
             lcd.write((byte)3);
             lcd.print("ICE AHEAD!");
             lcd.write((byte)3);
            }
            else if(text[1]=='1')
            {
             lcd.print("FOG AHEAD!");
            }
          }
            
          }
        
}