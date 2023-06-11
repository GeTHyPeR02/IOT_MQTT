# IOT_MQTT

WIN 
U mosquitto.conf dodamo linije: 
listener 4000
allow_anonymous true


Pokrenemo mosquitto: 
mosquitto -v -p 4000 -c mosquitto.conf 

Kompajliranje: 
 javac -cp .;org.eclipse.paho.client.mqttv3-1.2.5.jar Controller.java
 javac -cp .;org.eclipse.paho.client.mqttv3-1.2.5.jar Sensor.java
 javac -cp .;org.eclipse.paho.client.mqttv3-1.2.5.jar Actuator.java


Pokretanje:
java -cp .;org.eclipse.paho.client.mqttv3-1.2.5.jar Controller
java -cp .;org.eclipse.paho.client.mqttv3-1.2.5.jar Sensor 192.168.1.2 Sensor Rain 1 1   //ima kise
java -cp .;org.eclipse.paho.client.mqttv3-1.2.5.jar Sensor 192.168.1.2 Sensor Rain 1 0   //nema kise
java -cp .;org.eclipse.paho.client.mqttv3-1.2.5.jar Actuator 192.168.2.2 Actuator DisplayRain 1
