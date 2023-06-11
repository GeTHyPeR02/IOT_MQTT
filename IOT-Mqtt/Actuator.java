import java.net.*;
import org.eclipse.paho.client.mqttv3.*;
import java.io.*;
import java.util.*;
import java.util.Arrays;
import java.io.FileWriter;   // Import the FileWriter class
import java.io.IOException;
import java.net.UnknownHostException;
public class Actuator 
{
    public static String MQTT_BROKER = "";
    public static String MQTT_TOPIC = "Automobil/Actuator/";
    public static String CurrData="0";
    public static MqttClient client;
    public static final String deviceTypeActuator[] = new String[]{"DisplayRain","FogLights", "DisplayIce", "BreakLights"};
    static volatile boolean finished = false;
    public static String messageNotify="";
    public static String adrress;
    public static boolean MqttInitialized = false;
    public static void main(String[] args)  throws InterruptedException
    {
        if(args.length == 4){   //Ip Actuator tip id 
            try
            {
                String data="1|";
                data = data  + SocketFunctions.getIndexDevice(deviceTypeActuator,args[2]) + "|"+args[3]+"|0";
            
                adrress = args[0];
                MQTT_TOPIC+=args[2]+"/"+args[3];
                System.out.println(MQTT_TOPIC);
                String messageMSearch = 
                    "HOST:"+ adrress +"\n"+
                    "ssdp:msearch\n"+ 
                    "type:actuator";
                messageNotify = 
                    "HOST:" + adrress + "\n"+
                    "ssdp:notify\n"+
                    "type:actuator\n"+
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
                ReadThreadActuator(socket,group,port));
                t.start();
                SocketFunctions.sendData(messageNotify,group,port,socket);
                while(true)
                {
                    Thread.sleep(3000);
                    SocketFunctions.sendData(messageNotify,group,port,socket);
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
        }
        else
            return ;
    }
    
}
class ReadThreadActuator implements Runnable
{
    private MulticastSocket socket;
    private InetAddress group;
    private int port;
    public static final int MAX_LEN = 1000;
    ReadThreadActuator(MulticastSocket socket,InetAddress group,int port)
    {
        this.socket = socket;
        this.group = group;
        this.port = port;
    }
      
    @Override
    public void run()
    {
        while(true){

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
                    SocketFunctions.sendData(Actuator.messageNotify,group,port,socket);
                    
                }
                else if(messageType.equals("notify") && messageSender.equals("controller")){
                    Actuator.MQTT_BROKER = "tcp://"+hostIp+":4000";
                    //Actuator.MQTT_BROKER = "tcp://localhost:4000";
                    if(!Actuator.MQTT_BROKER.equals("") && !Actuator.MqttInitialized){
                        Actuator.MqttInitialized = true;
                        MqttHelperActuator.initMqtt();
                   //     System.out.println("OVDE");
                        MqttHelperActuator.subscribeToController(Actuator.MQTT_TOPIC);  //sub na odredjeni topic 
                    }
                }
            }
            }
        }
        } 
    }



}
class SocketFunctions{
   
    
    public static void sendData(String message, InetAddress group, int port, MulticastSocket socket){
        try{
            byte[] buffer = message.getBytes();
            DatagramPacket datagram = new DatagramPacket(buffer,buffer.length,group,port);
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
class MqttHelperActuator{

    public static void initMqtt(){  //jednom na pocetku, isto kao senzori 
        try{
            System.out.println(Actuator.MQTT_BROKER);
            Actuator.client = new MqttClient(Actuator.MQTT_BROKER, MqttClient.generateClientId());
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            Actuator.client.connect(options);
      }
        catch (MqttException e) {
            System.out.println("Error publishing message");
            e.printStackTrace();
        }
    }
    public static void subscribeToController(String Topic){  
        try{
            
            Actuator.client.subscribe(Topic,(topic,controllerMessage) -> {   //sub na topic
                String mqttMessage = new String(controllerMessage.getPayload()); 
                if(!Actuator.CurrData.equals(mqttMessage)){          //Ukoliko se dosadasnje stanje razlikuje od trenutnog pristiglog obavestava se korisnik
                    System.out.print("Doslo je do promene stanja! \nTrenutno stanje:");
                    System.out.println(mqttMessage);
                    Actuator.CurrData=mqttMessage;
                }
            }); 
        }
        catch(MqttException e){
            e.printStackTrace();
        }
    }
}