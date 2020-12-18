package Application;

import protocol.ICMPProtocolLayer;
import protocol.IProtocol;
import protocol.ProtocolManager;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Random;

public class PingApp extends Application{
    private int echo_times = 0;
    private short identifier = 0;
    private short sequence = 0;
    private byte[] destIP = null;

    public PingApp(int times,byte[] destIP){
        if(times>0){
            echo_times = times;
        }else {
            throw new IllegalArgumentException("echo tmes must > 0");
        }

        Random rand = new Random();
        identifier = (short)(rand.nextInt()&0x0000ffff);
        this.destIP = destIP;
        this.port = identifier;

    }

    public void startPing(){
        for(int i = 0; i < this.echo_times; i++){
            try {
                byte[] packet = createPackage(null);
                ProtocolManager.getInstance().sendData(packet,destIP);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private byte[] createPackage(byte[] data) throws Exception {
        byte[] icmpEchoHeader = this.createICMPEchoHeader();
        if(icmpEchoHeader == null){
            throw  new Exception("ICMP Header create fail");
        }
        byte[] ipHeader = this.createIP4Header(icmpEchoHeader.length);
        byte[] packet = new byte[icmpEchoHeader.length+ipHeader.length];
        ByteBuffer packetBuffer = ByteBuffer.wrap(packet);
        packetBuffer.put(ipHeader);
        packetBuffer.put(icmpEchoHeader);

        return packetBuffer.array();

    }

    private byte[] createICMPEchoHeader(){
        IProtocol icmpProto = ProtocolManager.getInstance().getProtocol("icmp");
        if(icmpProto == null){
            return null;
        }

        HashMap<String,Object> headerInfo = new HashMap<String,Object>();
        headerInfo.put("header","echo");
        headerInfo.put("identifier",identifier);
        headerInfo.put("sequence_number",sequence);
        sequence++;

        long time = System.currentTimeMillis();
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.putLong(time);
        byte[] timeBuffer = buffer.array();
        headerInfo.put("data",timeBuffer);
        byte[] icmpEchoHeader = icmpProto.createHeader(headerInfo);

        return icmpEchoHeader;
    }

    private byte[] createIP4Header(int dataLength){
        IProtocol ip4Proto = ProtocolManager.getInstance().getProtocol("ip");
        if(ip4Proto == null||dataLength<=0){
            return null;
        }
        HashMap<String,Object>headerInfo = new HashMap<String, Object>();
        headerInfo.put("data_length",dataLength);
        ByteBuffer destIP =ByteBuffer.wrap(this.destIP);
        headerInfo.put("destination_ip",destIP.getInt());
        byte protocol = ICMPProtocolLayer.PROTOCOL_ICMP;
        headerInfo.put("protocol",protocol);
        headerInfo.put("identification",(short)this.port);
        byte[] ipheader = ip4Proto.createHeader(headerInfo);
        return  ipheader;

    }

    public void handleData(HashMap<String,Object> data){
        long time = System.currentTimeMillis();
        short sequence = (short) data.get("sequence");
        byte[] time_buf = (byte[]) data.get("data");
        ByteBuffer buf  = ByteBuffer.wrap(time_buf);
        long send_time = buf.getLong();
        System.out.println("receive reply for ping request "+sequence+"for "+(time-send_time)/1000+"secs");
    }

}
