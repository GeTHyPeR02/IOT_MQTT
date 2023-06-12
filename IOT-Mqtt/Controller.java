import org.eclipse.paho.client.mqttv3.*;
import java.net.*;
import java.io.*;
import java.util.*;
import java.lang.Integer;
import java.util.ArrayList;
import java.io.FileWriter;  
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
public class Controller
{
    public static MqttClient client;
    public static String MQTT_BROKER = "";
    public static final String MQTT_TOPIC = "Automobil/Sensor/#";
    static String messageMSearch;
    static String messageNotify;
    static volatile boolean finished = false;
    public static  ArrayList<Device> deviceList;
    public static String address;
    public static void main(String[] args) throws InterruptedException
    {
    
        try
        {
            
            address = SocketFunctionsServer.getIpAddress();
            MQTT_BROKER = "tcp://" + address + ":4000";
            messageMSearch = 
            "HOST:"+ address +"\n"+
            "ssdp:msearch\n"+ 
            "type:controller";
            messageNotify = 
            "HOST:"+ address +"\n"+
            "ssdp:notify\n"+
            "type:controller\n"+
            "data:2|0|0|1";

            InetAddress group = InetAddress.getByName("239.255.255.250");
            int port = 1900;
            MulticastSocket socket = new MulticastSocket(port);
            deviceList = new ArrayList<Device>();
            socket.setTimeToLive(1);
            //socket.setLoopbackMode(true);
              
            socket.joinGroup(group);
            Thread t = new Thread(new
            ReadThreadServer(socket,group,port));
          
            t.start(); 
            SocketFunctionsServer.sendData(messageMSearch,group,port,socket);
            //System.out.println("Sending msearch");
            MqttHelperServer.initMqtt();  //Na pocetku
            client.subscribe(MQTT_TOPIC, new IMqttMessageListener() {   //Sub na sve senzore 
                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {  //pristigle poruke se obradjuju i salju svim aktuatorima 
                    String mqttMessage = new String(message.getPayload());
                    //System.out.println(mqttMessage);
                  
                    String data[] = mqttMessage.split("\\|");
                    MqttMessage actuatorMessage = new MqttMessage(mqttMessage.getBytes());   //Salje se samo trenutno stanje 
                    actuatorMessage.setQos(1);
                    // Publish the message to all subscribed devices (actuators)
                        for (Device device : deviceList) {   //Prolazi kroz sve uredjaje, nalazi samo actuatore koji su subovani na taj tip senzora i salje poruku 
                             //System.out.println(device.getCategory()+"   "+ device.getSubTo()+"   "+ device.getTopic());
                                //Proverava da li je aktuator subovan na taj senzor 
                                if(device.getCategory().equals("Actuator") && device.getSubTo().equals(data[1])){
                                    //System.out.println("data");
                                    String actuatorTopic = device.getTopic();
                                    Controller.client.publish(actuatorTopic, actuatorMessage);

                                }
                        }
                    
                }
                
            });
            while(true)
            {
                 
               Thread.sleep(2000);
                System.out.println("#####################");  
                Device.printAllDevices();
                SocketFunctionsServer.sendData(messageNotify,group,port,socket);
              
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
}
class ReadThreadServer implements Runnable
{
    private MulticastSocket socket;
    private InetAddress group;
    private int port;
    public static final int MAX_LEN = 1000;
   
    ReadThreadServer(MulticastSocket socket,InetAddress group,int port)
    {
        this.socket = socket;
        this.group = group;
        this.port = port;
    }
      
    @Override
    public void run()
    {
        
        while(true){
            String message = SocketFunctionsServer.recvData(group,port,socket);
           // System.out.println("POZZ IZ RUN");
           
            if(!message.equals("")){
            String[] lines=message.split("\n");
            if(lines.length==3 || lines.length==4){
            String hostIp = message.split("\n")[0].split(":")[1];
            String messageType = message.split("\n")[1].split(":")[1];
            String deviceType = message.split("\n")[2].split(":")[1];

             if(!deviceType.equals("controller")){
                 if(messageType.equals("notify")){
                            Device device = new Device(message);
                            Device.addDevice(device);
                        }
                        else if(messageType.equals("msearch")){
                        SocketFunctionsServer.sendData(Controller.messageNotify,group,port,socket);
                     }
                    }
                }

            }
        }
    }
}



class SocketFunctionsServer{
   
    
    public static void sendData(String message, InetAddress group, int port, MulticastSocket socket){
        try{
            byte[] buffer = message.getBytes();
            DatagramPacket datagram = new DatagramPacket(buffer,buffer.length,group,port);
            socket.send(datagram);
        }
        catch(IOException ie)
        {
            System.out.println("Error reading/writing from/to socket");
            ie.printStackTrace();
        
        }
    }
    public static String recvData(InetAddress group, int port, MulticastSocket socket){
            String message = "";
            byte[] buffer = new byte[ReadThreadServer.MAX_LEN];
            DatagramPacket datagram = new
            DatagramPacket(buffer,buffer.length,group,port);
           // System.out.println("RECV DATA");
            try
            {
                socket.receive(datagram);
                message = new String(buffer,0,datagram.getLength(),"UTF-8");
              //  System.out.println(message);
            }
            catch(IOException e)
            {
                System.out.println("Socket closed!");
            }
            return message;
        
    }
    public static String getIpAddress(){
        final Pattern pattern = Pattern.compile("192", Pattern.CASE_INSENSITIVE);
        try{ 
            Enumeration en = NetworkInterface.getNetworkInterfaces(); 
            while (en.hasMoreElements()) { 
                NetworkInterface ni = (NetworkInterface) en.nextElement(); 
                Enumeration ee = ni.getInetAddresses(); 
                while (ee.hasMoreElements()) { 
                    InetAddress ia = (InetAddress) ee.nextElement(); 
                    String ip = ia.getHostAddress(); 
                    Matcher matcher = pattern.matcher(ip);
                    boolean matched = matcher.find();
                    if(matched)
                      return ip;
                } 
            } 
        } 
        catch(Exception e){ 
        }
        return "";
  }
}

class Device{
    public static final String deviceCategoryList[] = new String[]{"Sensor","Actuator","Controller"};
    public static final String deviceTypeSensor[] = new String[]{"Rain", "Fog", "Ice", "Distance"};
    public static final String deviceTypeActuator[] = new String[]{"DisplayRain","FogLights", "DisplayIce", "BreakLights"};
    private String ipAddress;
    private String deviceCategory;
    private String deviceType;
    private int deviceId;
    private long timeAlive;
    private String subTo;
    private String topic;

    public Device(String message){
        String lines[] = message.split("\n");
        this.ipAddress = lines[0].split(":")[1];
        String header[] = lines[3].split(":");
        String data[] = header[1].split("\\|");
        
        this.deviceCategory = deviceCategoryList[Integer.parseInt(data[0])];
        //System.out.println(this.deviceCategory);

        if(this.deviceCategory.equals("Sensor") ){
            this.deviceType = deviceTypeSensor[Integer.parseInt(data[1])];
            this.deviceId = Integer.parseInt(data[2]);
            this.subTo="";
            this.topic="";
           // System.out.println("topic"+ "   "+ this.subTo);


        }
        else if(this.deviceCategory.equals("Actuator"))
        {
            this.deviceType = deviceTypeActuator[Integer.parseInt(data[1])];
            this.deviceId = Integer.parseInt(data[2]);
            this.subTo=data[1];
            this.topic="Automobil/Actuator/"+this.deviceType+"/"+this.deviceId;
           // System.out.println(data[3]);
        }
        else{
          //  System.out.println(this.topic+ "  LALALA "+ this.subTo);
            this.deviceType = "Controller";
            this.deviceId=0;
            this.subTo="";
            this.topic="";
        }
        timeAlive = System.currentTimeMillis();
    }
    public void printDevice(){
        System.out.println(ipAddress);
        System.out.println(deviceCategory);
        System.out.println(deviceType);
        System.out.println(deviceId);
    }
    public String getCategory(){
        return this.deviceCategory;
    }
    public String getType(){
        return this.deviceType;
    }
    public String getIp(){
        return this.ipAddress;
    }
    public int getId(){
        return this.deviceId;
    }
    public long getTime(){
        return this.timeAlive;
    }
    public void setTime(long time){
        this.timeAlive = time;
    }
    public String getSubTo() {
        return subTo;
    }
    public String getTopic(){
        return topic;
    }

    @Override
    public boolean equals(Object o) {
        Device compDevice = (Device) o;
        boolean deviceCategorySame = this.deviceCategory.equals(compDevice.getCategory());
        boolean deviceTypeSame= this.deviceType.equals(compDevice.getType());
        boolean deviceIDSame = this.deviceId == compDevice.getId();
        boolean deviceIpSame = this.ipAddress.equals(compDevice.getIp());

        if(deviceCategorySame && deviceTypeSame && deviceIDSame && deviceIpSame)
            return true;
        else
            return false;
    }
    public static void addDevice(Device device){
        boolean inList=false;
        for(int i=0;i<Controller.deviceList.size();i++){
            if(Controller.deviceList.get(i).equals(device)){
                inList=true;
                break;
            }
        }
        if(!inList){
            Controller.deviceList.add(device);
         
        }
        else{
            for(int i=0;i<Controller.deviceList.size();i++){
                if(Controller.deviceList.get(i).equals(device)){
                     Controller.deviceList.get(i).setTime(System.currentTimeMillis());
                     break;
                }
            }
        }
    }
    public static void printAllDevices(){
        for(int i=0;i<Controller.deviceList.size();i++){
            if((System.currentTimeMillis() - Controller.deviceList.get(i).getTime()) < 15000){
                Controller.deviceList.get(i).printDevice();
                

            }
            else{
                Controller.deviceList.remove(i);
                i--;
            }
            
        }
        String output ="";
        for(int i=0;i<Controller.deviceList.size();i++){
            output = output + Controller.deviceList.get(i).getCategory() + " " + Controller.deviceList.get(i).getType() + "\n";
        }
    }

}
class MqttHelperServer{

    public static void initMqtt(){
        try{
            Controller.client = new MqttClient(Controller.MQTT_BROKER, MqttClient.generateClientId());
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            Controller.client.connect(options);
      }
        catch (MqttException e) {
            System.out.println("Error publishing message");
            e.printStackTrace();
        }
    }
}