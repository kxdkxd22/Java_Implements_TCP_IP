package protocol;

import Application.ApplicationManager;
import Application.IApplication;
import datalinklayer.DataLinkLayer;
import jpcap.PacketReceiver;
import jpcap.packet.EthernetPacket;
import jpcap.packet.IPPacket;
import jpcap.packet.Packet;

import java.util.Arrays;
import java.util.HashMap;

public class ProtocolManager implements PacketReceiver {
    private static ProtocolManager instance = null;
    private static ARPProtocolLayer arpLayer = null;
    private static DataLinkLayer dataLinkInstance = null;
    private static HashMap<String,byte[]> ipToMacTable = null;
    private static HashMap<String,byte[]> dataWaitToSend = null;

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
        }

        return instance;
    }

    public IProtocol getProtocol(String name){
        switch (name.toLowerCase()){
            case "icmp":
                return new ICMPProtocolLayer();
            case "ip":
                return new IPProtocolLayer();
        }

        return null;
    }

    public void sendData(byte[] data,byte[] ip) throws Exception {
        byte[] mac = ipToMacTable.get(Arrays.toString(ip));
        if(mac == null){
            HashMap<String,Object> headerInfo = new HashMap<String,Object>();
            headerInfo.put("sender_ip",ip);
            byte[] arpRequest = arpLayer.createHeader(headerInfo);
            if(arpRequest == null){
                throw new Exception("Get mac address header fail");
            }
            dataLinkInstance.sendData(arpRequest,broadcast, EthernetPacket.ETHERTYPE_ARP);
            dataWaitToSend.put(Arrays.toString(ip),data);
        }else{
            dataLinkInstance.sendData(data,mac, IPPacket.IPPROTO_IP);
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
            sendWaitingData(senderIP);
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
                    handleICMPPacket(packet);
                    break;
                default:
                    return;
            }

        }
    }

    private void handleICMPPacket(Packet packet){
        IProtocol icmpProtocol = new ICMPProtocolLayer();
        HashMap<String,Object> headerInfo = icmpProtocol.handlePacket(packet);
        short identifier = (short)headerInfo.get("identifier");
        IApplication app = ApplicationManager.getInstance().getApplicationByPort(identifier);
        if(app!=null&&app.isClosed()!=true){
            app.handleData(headerInfo);
        }
    }

    private void sendWaitingData(byte[] destIP){
        byte[] data = dataWaitToSend.get(Arrays.toString(destIP));
        byte[] mac = ipToMacTable.get(Arrays.toString(destIP));
        if(data!=null&&mac!=null){
            dataLinkInstance.sendData(data,mac,EthernetPacket.ETHERTYPE_IP);
        }

    }

}
