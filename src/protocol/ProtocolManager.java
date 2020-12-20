package protocol;

import Application.Application;
import Application.ApplicationManager;
import Application.IApplication;
import datalinklayer.DataLinkLayer;
import jpcap.PacketReceiver;
import jpcap.packet.EthernetPacket;
import jpcap.packet.IPPacket;
import jpcap.packet.Packet;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class ProtocolManager implements PacketReceiver {
    private static ProtocolManager instance = null;
    private static ARPProtocolLayer arpLayer = null;
    private static DataLinkLayer dataLinkInstance = null;
    private static HashMap<String,byte[]> ipToMacTable = null;
    private static HashMap<String,byte[]> dataWaitToSend = null;
    private static ArrayList<Application> icmpPacketReceiverList = null;

    private static InetAddress routerAddress = null;

    private static byte[] broadcast = new byte[]{(byte)255,(byte)255,(byte)255,(byte)255,(byte)255,(byte)255};
    private ProtocolManager(){}
    public static ProtocolManager getInstance(){
        if(instance == null){
            instance = new ProtocolManager();
            dataLinkInstance = DataLinkLayer.getInstance();
            ipToMacTable = new HashMap<String,byte[]>();
            dataWaitToSend = new HashMap<String,byte[]>();
            dataLinkInstance.registerPacketReceiver(instance);
            arpLayer = new ARPProtocolLayer();

            icmpPacketReceiverList = new ArrayList<Application>();

            try {
                routerAddress = InetAddress.getByName("10.4.0.1");
                instance.prepareRouterMac();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return instance;
    }

    public void registToReceiveICMPPacket(Application receiver){
        if(icmpPacketReceiverList.contains(receiver)!=true){
            icmpPacketReceiverList.add(receiver);
        }
    }

    private void prepareRouterMac() throws Exception {
        HashMap<String,Object>headerInfo = new HashMap<String,Object>();
        headerInfo.put("sender_ip",routerAddress.getAddress());
        byte[] arpRequest = arpLayer.createHeader(headerInfo);
        if(arpRequest == null){
            throw new Exception("");
        }
        dataLinkInstance.sendData(arpRequest,broadcast,EthernetPacket.ETHERTYPE_ARP);
    }

    private byte[] getRouterMac(){return ipToMacTable.get(Arrays.toString(routerAddress.getAddress()));}

    public IProtocol getProtocol(String name){
        switch (name.toLowerCase()){
            case "icmp":
                return new ICMPProtocolLayer();
            case "ip":
                return new IPProtocolLayer();
            case "udp":
                return new UDPProtocolLayer();
        }

        return null;
    }

    public void sendData(byte[] data,byte[] ip) throws Exception {
        byte[] mac = getRouterMac();
        if(mac == null){
            prepareRouterMac();
            dataWaitToSend.put(Arrays.toString(ip),data);
        }else{
            dataLinkInstance.sendData(data,mac,EthernetPacket.ETHERTYPE_IP);
        }
    }

    @Override
    public void receivePacket(Packet packet) {
        if(packet == null){
            return;
        }

        EthernetPacket ethernetPacket = (EthernetPacket) packet.datalink;

        if(ethernetPacket.frametype == EthernetPacket.ETHERTYPE_ARP){
            ARPProtocolLayer arpLayer = new ARPProtocolLayer();
            HashMap<String, Object> info = arpLayer.handlePacket(packet);
            byte[] senderIP = (byte[]) info.get("sender_ip");
            byte[] senderMac = (byte[]) info.get("sender_mac");

            ipToMacTable.put(Arrays.toString(senderIP),senderMac);
            sendWaitingData();
        }

        if(ethernetPacket.frametype == EthernetPacket.ETHERTYPE_IP){
            handleIPPacket(packet);
        }

    }

    private void handleIPPacket(Packet packet){
        IProtocol ipProtocol = new IPProtocolLayer();
        HashMap<String,Object> info = ipProtocol.handlePacket(packet);
        if(info == null){
            return ;
        }

        byte protocol = 0;
        if(info.get("protocol")!=null){
            protocol = (byte) info.get("protocol");
            packet.header = (byte[])info.get("header");
            System.out.println("receive packet with protocol: "+protocol);
        }
        if(protocol!=0){
            switch (protocol){
                case IPPacket.IPPROTO_ICMP:
                    handleICMPPacket(packet,info);
                    break;
                default:
                    return;
            }

        }
    }

    private void handleICMPPacket(Packet packet,HashMap<String,Object>infoFromUpLayer){
        IProtocol icmpProtocol = new ICMPProtocolLayer();
        HashMap<String,Object> headerInfo = icmpProtocol.handlePacket(packet);
        for(String key:infoFromUpLayer.keySet()){
            headerInfo.put(key,infoFromUpLayer.get(key));
        }

        for(int i=0; i < icmpPacketReceiverList.size(); i++){
            Application receiver = icmpPacketReceiverList.get(i);
            receiver.handleData(headerInfo);
        }
    }

    private void sendWaitingData(){

        byte[] mac = getRouterMac();
        if(mac!=null){
            for(String key:dataWaitToSend.keySet()){
                byte[] data = dataWaitToSend.get(key);
                dataLinkInstance.sendData(data,mac,EthernetPacket.ETHERTYPE_IP);
            }
           dataWaitToSend.clear();
        }

    }

}
