package datalinklayer;

import ICMPProtocolLayer.ICMPProtocolLayer;
import jpcap.*;
import jpcap.packet.EthernetPacket;
import jpcap.packet.Packet;
import ARPProtocolLayer.ARPProtocolLayer;
import utils.IMacReceiver;
import utils.PacketProvider;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.UnknownHostException;

public class DataLinkLayer extends PacketProvider implements jpcap.PacketReceiver, IMacReceiver {
    private static DataLinkLayer instance = null;
    private NetworkInterface device = null;
    private Inet4Address ipAddress = null;
    private byte[] macAddress = null;
    JpcapSender sender = null;

    private DataLinkLayer(){

    }

    public static DataLinkLayer getInstance(){
        if(instance == null){
            instance = new DataLinkLayer();
        }

        return instance;
    }

    public void initWithOpenDevice(NetworkInterface device){
        this.device = device;
        this.ipAddress = this.getDeviceIpAddress();
        this.macAddress = new byte[6];
        this.getDeviceMacAddress();

        JpcapCaptor captor = null;

        try {
            captor = JpcapCaptor.openDevice(device,2000,false,3000);
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.sender = captor.getJpcapSenderInstance();

        this.testARPProtocol();
        this.testICMPProtocol();
    }

    private Inet4Address getDeviceIpAddress(){
        for(NetworkInterfaceAddress addr:this.device.addresses){
            if(!(addr.address instanceof Inet4Address)){
                continue;
            }

            return (Inet4Address) addr.address;
        }
        return null;
    }

    private void getDeviceMacAddress(){
        int count = 0;
        for(byte b:this.device.mac_address){
            this.macAddress[count] = (byte) (b&0xff);
            count++;
        }
    }

    public byte[] deviceIPAddress(){
        return this.ipAddress.getAddress();
    }

    public byte[] deviceMacAddress(){
        return this.macAddress;
    }

    @Override
    public void receivePacket(Packet packet) {
        this.pushPacketToReceivers(packet);
    }

    public void sendData(byte[] data,byte[] dstMacAddress,short frameType){
        if(data == null){
            return;
        }

        Packet packet = new Packet();
        packet.data = data;

        EthernetPacket ether = new EthernetPacket();
        ether.frametype = EthernetPacket.ETHERTYPE_ARP;
        ether.src_mac = this.macAddress;
        ether.dst_mac = dstMacAddress;
        packet.datalink = ether;

        sender.sendPacket(packet);
    }

    private void testARPProtocol(){
        ARPProtocolLayer arpLayer = new ARPProtocolLayer();
        this.registerPacketReceiver(arpLayer);

        byte[] ip;
        try {
            ip = Inet4Address.getByName("10.4.0.1").getAddress();
            arpLayer.getMacByIP(ip,this);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    private void testICMPProtocol(){
        ICMPProtocolLayer icmpProtocolLayer = new ICMPProtocolLayer();
        this.registerPacketReceiver(icmpProtocolLayer);
    }

    @Override
    public void receiveMacAddress(byte[] ip, byte[] mac) {
        System.out.println("receive arp reply msg with sender ip: ");
        for(byte b:ip){
            System.out.print(Integer.toUnsignedString(b&0xff)+".");
        }
        System.out.println("with sender mac: ");
        for(byte b:mac){
            System.out.print(Integer.toHexString(b&0xff)+":");
        }


    }
}
