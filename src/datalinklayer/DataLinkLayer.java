package datalinklayer;

import jpcap.*;
import jpcap.packet.EthernetPacket;
import jpcap.packet.Packet;
import utils.PacketProvider;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Inet4Address;
import java.nio.ByteBuffer;

public class DataLinkLayer extends PacketProvider implements jpcap.PacketReceiver {
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
        ether.frametype = frameType;
        ether.src_mac = this.macAddress;
        ether.dst_mac = dstMacAddress;
        packet.datalink = ether;

        sender.sendPacket(packet);

        String path = "C:\\Users\\81258\\Desktop\\dump.txt";
        try {
            FileOutputStream fos = new FileOutputStream(path);
            fos.write(dstMacAddress);
            fos.write(ether.src_mac);
            byte[] buf = new byte[2];
            ByteBuffer buffer = ByteBuffer.wrap(buf);
            buffer.putShort(frameType);
            fos.write(buffer.array());
            fos.write(data);
            fos.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


}
