package protocol;

import datalinklayer.DataLinkLayer;
import jpcap.PacketReceiver;
import jpcap.packet.ARPPacket;
import jpcap.packet.EthernetPacket;
import jpcap.packet.Packet;
import utils.IMacReceiver;

import javax.xml.crypto.Data;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;

public class ARPProtocolLayer implements PacketReceiver {

    private HashMap<byte[],byte[]>ipToMacTable = new HashMap<byte[],byte[]>();
    private HashMap<Integer, ArrayList<IMacReceiver>> ipToMacReceiverTable = new HashMap<Integer, ArrayList<IMacReceiver>>();

    private static int ARP_OPCODE_START = 20;
    private static int ARP_SENDER_MAC_START = 22;
    private static int ARP_SENDER_IP_START = 28;
    private static int ARP_TARGET_IP_START =38;

    @Override
    public void receivePacket(Packet packet) {
        if(packet==null){
            return;
        }

        EthernetPacket ethernetPacket = (EthernetPacket) packet.datalink;

        if(ethernetPacket.frametype!=EthernetPacket.ETHERTYPE_ARP){
            return;
        }
        byte[] header = packet.header;
        analyzeARPMessage(header);

    }

    private boolean analyzeARPMessage(byte[] data){
        byte[] opcode = new byte[2];
        System.arraycopy(data,ARP_OPCODE_START,opcode,0,2);
        short op = ByteBuffer.wrap(opcode).getShort();
        if(op!= ARPPacket.ARP_REPLY){
            return false;
        }
        byte[] ip = DataLinkLayer.getInstance().deviceIPAddress();
        for(int  i = 0; i < 4; i++){
            if(ip[i]!=data[ARP_TARGET_IP_START+i]){
                return false;
            }
        }
        byte[] senderIP = new byte[4];
        System.arraycopy(data,ARP_SENDER_IP_START,senderIP,0,4);
        byte[] senderMac = new byte[6];
        System.arraycopy(data,ARP_SENDER_MAC_START,senderMac,0,6);
        ipToMacTable.put(senderIP,senderMac);

        int ipToInteger = ByteBuffer.wrap(senderIP).getInt();
        ArrayList<IMacReceiver> receiverList = ipToMacReceiverTable.get(ipToInteger);
        if(receiverList!=null){
            for(IMacReceiver receiver:receiverList){
                receiver.receiveMacAddress(senderIP,senderMac);
            }
        }

        return true;
    }

    public void getMacByIP(byte[] ip, IMacReceiver receiver){
        if(receiver == null){
            return;
        }

        int ipToInt = ByteBuffer.wrap(ip).getInt();
        if(ipToMacReceiverTable.get(ipToInt)!=null){
            receiver.receiveMacAddress(ip,ipToMacTable.get(ip));
        }

        if(ipToMacReceiverTable.get(ipToInt)==null){
            ipToMacReceiverTable.put(ipToInt,new ArrayList<IMacReceiver>());
            sendARPRequestMsg(ip);
        }

        ArrayList<IMacReceiver> receiverList = ipToMacReceiverTable.get(ipToInt);
        if(receiverList.contains(receiver)!=true){
            receiverList.add(receiver);
        }
        return;
    }

    private void sendARPRequestMsg(byte[] ip){
        if(ip == null){
            return;
        }

        DataLinkLayer dataLinkLayer = DataLinkLayer.getInstance();
        byte[] broadcast = new byte[]{(byte)255,(byte)255,(byte)255,(byte)255,(byte)255,(byte)255};
        int pointer = 0;
        byte[] data = new byte[28];
        data[pointer] = 0;
        pointer++;
        data[pointer] = 1;
        pointer++;

        ByteBuffer buffer = ByteBuffer.allocate(2);
        buffer.order(ByteOrder.BIG_ENDIAN);
        buffer.putShort(ARPPacket.PROTOTYPE_IP);
        for(int i = 0;i< buffer.array().length; i++){
            data[pointer] = buffer.array()[i];
            pointer++;
        }
        data[pointer] = 6;
        pointer++;
        data[pointer] = 4;
        pointer++;

        buffer = ByteBuffer.allocate(2);
        buffer.order(ByteOrder.BIG_ENDIAN);
        buffer.putShort(ARPPacket.ARP_REQUEST);
        for(int i = 0; i < buffer.array().length; i++){
            data[pointer] = buffer.array()[i];
            pointer++;
        }

        byte[] macAddress = dataLinkLayer.deviceMacAddress();
        for(int i = 0; i < macAddress.length; i++){
            data[pointer] = macAddress[i];
            pointer++;
        }

        byte[] srcip = dataLinkLayer.deviceIPAddress();
        for(int i = 0; i < srcip.length; i++){
            data[pointer] = srcip[i];
            pointer++;
        }

        for(int i = 0; i < broadcast.length; i++){
            data[pointer] = broadcast[i];
            pointer++;
        }

        for(int i = 0; i < ip.length; i++){
            data[pointer] = ip[i];
            pointer++;
        }
        dataLinkLayer.sendData(data,broadcast,EthernetPacket.ETHERTYPE_ARP);

    }

}
