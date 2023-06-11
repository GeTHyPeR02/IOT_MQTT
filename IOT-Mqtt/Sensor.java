import java.net.*;
import org.eclipse.paho.client.mqttv3.*;
import java.io.*;
import java.util.*;
import java.util.Arrays;
import java.io.FileWriter;   // Import the FileWriter class
import java.io.IOException;
import java.net.UnknownHostException;
public class Sensor 
{
    public static String MQTT_BROKER = "";
    public static String MQTT_TOPIC = "";
    public static MqttClient client;
    public static final String deviceTypeSensor[] = new String[]{"Rain", "Fog", "Ice", "Distance"};

    static volatile boolean finished = false;
    public static String messageNotify="";
    public static String address;
    public static boolean MqttInitialized = false;
    public static void main(String[] args)  throws InterruptedException
    {
        if(args.length == 5){   //IP sensor type_sensor id vrednost      
            //vrednost samo 0 ili 1 ! Da li ima opasnosti ili nema 
            try
            {
               String data="0|";
               data = data  + SocketFunctions.getIndexDevice(deviceTypeSensor,args[2]) + "|"+args[3]+"|"+args[4];
                
                MQTT_TOPIC="Automobil/Sensor/"+args[2]+"/"+args[3];  //primer Automobil/Sensor/Rain/1
                System.out.println(MQTT_TOPIC);
                System.out.println(data);
                // try{
                //     address = InetAddress.getLocalHost().getHostAddress();
                // }
                // catch (UnknownHostException e) {
                //     e.printStackTrace();
                // }
                address = args[0];
                String messageMSearch = 
                    "HOST:"+ address +"\n"+
                    "ssdp:msearch\n"+ 
                    "type:sensor";
                messageNotify = 
                    "HOST:" + address + "\n"+
                    "ssdp:notify\n"+
                    "type:sensor\n"+
                    "data:" + data;
                String mqttData = "20";
                InetAddress group = InetAddress.getByName("239.255.255.250");
                int port = 1900;
                MulticastSocket socket = new MulticastSocket(port);
                //socket.setLoopbackMode(true);
                // Since we are deploying
                socket.setTimeToLive(1);
                //this on localhost only (For a subnet set it as 1)
                  
                socket.joinGroup(group);
                SocketFunctions.sendData(messageMSearch,group,port,socket);
                Thread t = new Thread(new
                ReadThreadSensor(socket,group,port));
                t.start();
                SocketFunctions.sendData(messageNotify,group,port,socket);
                while(true)
                {
                    Thread.sleep(3000);
                    SocketFunctions.sendData(messageNotify,group,port,socket);
                    if(!Sensor.MQTT_BROKER.equals("")){  //Ukoliko je uradio recive onda je pronasao IP brokera i moze slati poruke
                       // System.out.println("debug");
                        String message = data;
                        MqttMessage mqttMessage = new MqttMessage(message.getBytes());
                        if(Sensor.client!=null){
                        client.publish(Sensor.MQTT_TOPIC, mqttMessage);  //na odredjeni topic salje poruku 
                        //System.out.println("Sending message...");
                        }
                    }
                }
            }
            catch(SocketException se)
            {
                System.out.println("Error creating socket");
                se.printStackTrace();
            }
            catch(IOException ie)
            {
                System.out.println("Error reading/writing from/to socket");
                ie.printStackTrace();
            }
            catch (MqttException e) {
                System.out.println("Error publishing message");
                e.printStackTrace();
            }
        }
        else
            return ;
    }
    
}
class ReadThreadSensor implements Runnable
{
    private MulticastSocket socket;
    private InetAddress group;
    private int port;
    public static final int MAX_LEN = 1000;
    ReadThreadSensor(MulticastSocket socket,InetAddress group,int port)
    {
        this.socket = socket;
        this.group = group;
        this.port = port;
    }
      
    @Override
    public void run()
    {
        while(true){
            //Prethodna kt 
            String message = SocketFunctions.recvData(group,port,socket);
            if(!message.equals("")){
            String[] lines=message.split("\n");
            if(lines.length>=3){
            String hostIp = message.split("\n")[0].split(":")[1];
            String messageType = message.split("\n")[1].split(":")[1];
            String messageSender = message.split("\n")[2].split(":")[1];
            if(!messageSender.equals("sensor")){
                
                if(messageType.equals("msearch")){
                    System.out.println("msearch received");
                    SocketFunctions.sendData(Sensor.messageNotify,group,port,socket);
                    
                }
                else if(messageType.equals("notify") && messageSender.equals("controller")){
                    Sensor.MQTT_BROKER = "tcp://"+hostIp+":4000";
                    //Sensor.MQTT_BROKER = "tcp://localhost:4000";
                    if(!Sensor.MQTT_BROKER.equals("") && !Sensor.MqttInitialized){
                        Sensor.MqttInitialized = true;
                        MqttHelperSensor.initMqtt();
                    }
                }
            }
        }
        }
        } 
    }



}
class SocketFunctions{
   
    //Prethodna kt - samo salje notify poruke kako bi se oglasio da je i dalje u mrezi
    public static void sendData(String message, InetAddress group, int port, MulticastSocket socket){
        try{
            byte[] buffer = message.getBytes();
            DatagramPacket datagram = new DatagramPacket(buffer,buffer.length,group,port);
            //System.out.println("SALJEM NOTIFY");
            socket.send(datagram);
            //System.out.println("Sending data...");
        }
        catch(IOException ie)
        {
            System.out.println("Error reading/writing from/to socket");
            ie.printStackTrace();
        
        }
    }
    public static String recvData(InetAddress group, int port, MulticastSocket socket){
            String message = "";
            byte[] buffer = new byte[1000];
            DatagramPacket datagram = new
            DatagramPacket(buffer,buffer.length,group,port);
            try
            {
                socket.receive(datagram);
                message = new
                String(buffer,0,datagram.getLength(),"UTF-8");
            }

            catch(IOException e)
            {
                System.out.println("Socket closed!");
            }
            return message;
        
    }
    public static String getIndexDevice(String list[],String name){
        for(int i=0;i<list.length;i++){
            if(list[i].equals(name)){
                return String.valueOf(i);
            }
        }
        return "0";
    }
}
class MqttHelperSensor{

    public static void initMqtt(){  //kada se poveze sa brokerom da uradi init
        try{
            System.out.println(Sensor.MQTT_BROKER);
            Sensor.client = new MqttClient(Sensor.MQTT_BROKER, MqttClient.generateClientId());   
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            Sensor.client.connect(options);  //konektuje se na brokera
           // System.out.println(Sensor.client);
      }
        catch (MqttException e) {
            System.out.println("Error publishing message");
            e.printStackTrace();
        }
    }
}