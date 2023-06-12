# IOT_MQTT

## **Description:** <br/>

### Sensor are checking conditions on a road and they are sending other road participants in the area this conditions, so that they can react accordingly. <br/>
### This communication is done by MQTT broker and SSDP protocol. <br/>

---

**WINOWS:** <br />

**In mosquitto.conf we add lines:** <br /> 
* listener 4000 <br /> 
* allow_anonymous true <br /> 

**Starting mosquitto:** <br /> 
* mosquitto -v -p 4000 -c mosquitto.conf <br /> <br /> 

**Compiling:** <br /> 
* javac -cp .;org.eclipse.paho.client.mqttv3-1.2.5.jar Controller.java <br /> 
* javac -cp .;org.eclipse.paho.client.mqttv3-1.2.5.jar Sensor.java <br /> 
* javac -cp .;jSerialComm-2.9.3.jar;org.eclipse.paho.client.mqttv3-1.2.5.jar Actuator.java <br /> <br /> 

**Starting:** <br /> 
* java -cp .;org.eclipse.paho.client.mqttv3-1.2.5.jar Controller <br /> 
* java -cp .;org.eclipse.paho.client.mqttv3-1.2.5.jar Sensor 192.168.1.2 Sensor Rain 1 <br /> 
* java -cp .;jSerialComm-2.9.3.jar;org.eclipse.paho.client.mqttv3-1.2.5.jar Actuator 192.168.2.2 Actuator DisplayRain 1 <br /> 
