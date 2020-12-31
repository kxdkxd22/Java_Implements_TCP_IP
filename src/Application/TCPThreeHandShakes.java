package Application;

import datalinklayer.DataLinkLayer;
import protocol.IProtocol;
import protocol.ProtocolManager;
import protocol.TCPProtocolLayer;

import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.HashMap;

public class TCPThreeHandShakes extends Application {
    private byte[] dest_ip;
    private short dest_port;
    private int ack_num = 0;
    private int seq_num = 0;

    private static int CONNECTION_IDLE = 0;
    private static int CONNECTION_INIT = 1;
    private static int CONNECTION_SUCCESS = 2;
    private static int CONNECTION_FIN_INIT = 3;
    private static int CONNECTION_FIN_SUCCESS = 4;
    private int tcp_state = CONNECTION_IDLE;

    public TCPThreeHandShakes(byte[] server_ip,short server_port){
        this.dest_ip = server_ip;
        this.dest_port = server_port;
        this.port = (short)11940;
    }

    public void beginThreeHandShakes() throws Exception {
        createAndSendPacket(null,"SYN");
        this.tcp_state = CONNECTION_INIT;
    }

    public void beginClose() throws Exception {
        createAndSendPacket(null,"FIN,ACK");
        this.tcp_state = CONNECTION_FIN_INIT;
    }

    private void createAndSendPacket(byte[] data,String flags) throws Exception {
        byte[] tcpHeader = createTCPHeader(null,flags);
        if(tcpHeader == null){
            throw new Exception("tcp Header create fail");
        }
        byte[] ipHeader = createIP4Header(tcpHeader.length);
        byte[] packet = new byte[tcpHeader.length+ipHeader.length];
        ByteBuffer packetBuffer = ByteBuffer.wrap(packet);
        packetBuffer.put(ipHeader);
        packetBuffer.put(tcpHeader);
        sendPacket(packet);
    }

    private void sendPacket(byte[] packet){
        try {
            InetAddress ip = InetAddress.getByName("10.4.0.1");
            ProtocolManager.getInstance().sendData(packet,ip.getAddress());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private byte[] createTCPHeader(byte[] data,String flags){
        IProtocol tcpProto = ProtocolManager.getInstance().getProtocol("tcp");
        if(tcpProto == null){
            return null;
        }

        HashMap<String,Object> headerInfo = new HashMap<String, Object>();
        byte[] src_ip = DataLinkLayer.getInstance().deviceIPAddress();
        headerInfo.put("src_ip",src_ip);
        headerInfo.put("dest_ip",this.dest_ip);
        headerInfo.put("src_port",(short)this.port);
        headerInfo.put("dest_port",this.dest_port);
        headerInfo.put("seq_num",seq_num);
        headerInfo.put("ack_num",ack_num);
        String[] flag_units = flags.split(",");
        for(int i = 0; i < flag_units.length; i++){
            headerInfo.put(flag_units[i],1);
        }

        byte[] tcpHeader = tcpProto.createHeader(headerInfo);
        return tcpHeader;
    }

    private byte[] createIP4Header(int dataLength){
        IProtocol ip4Proto = ProtocolManager.getInstance().getProtocol("ip");
        if(ip4Proto == null || dataLength <= 0){
            return null;
        }

        HashMap<String,Object> headerInfo = new HashMap<String, Object>();
        headerInfo.put("data_length",dataLength);
        ByteBuffer destIP = ByteBuffer.wrap(this.dest_ip);
        headerInfo.put("destination_ip",destIP.getInt());
        byte protocol = TCPProtocolLayer.TCP_PROTOCOL_NUMBER;
        headerInfo.put("protocol",protocol);
        headerInfo.put("identification",(short)this.port);
        byte[] ipHeader = ip4Proto.createHeader(headerInfo);

        return ipHeader;

    }

    public void handleData(HashMap<String,Object>headerInfo){
        short src_port = (short) headerInfo.get("src_port");
        System.out.println("receive TCP packet with port: "+src_port);
        boolean ack = false,syn = false,fin = false;
        if(headerInfo.get("ACK")!=null){
            System.out.println("it is a ACK packet");
            ack = true;
        }

        if(headerInfo.get("SYN")!=null){
            System.out.println("it is a SYN packet");
            syn = true;
        }

        if(headerInfo.get("FIN")!=null){
            System.out.println("it is a FIN packet");
            fin = true;
        }

        if(ack&&syn){
            int seq_num = (int) headerInfo.get("seq_num");
            int ack_num = (int)headerInfo.get("ack_num");
            System.out.println("tcp handshake from othersize with seq_num "+seq_num+" and ack_num: "+ack_num);
            this.seq_num = this.seq_num+1;
            this.ack_num = seq_num+1;
            try {
                if(this.tcp_state == CONNECTION_INIT){
                    this.tcp_state = CONNECTION_SUCCESS;
                    System.out.println("three handshake complete");
                }
                createAndSendPacket(null,"ACK");
                beginClose();
                System.out.println("hello world");

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if(ack&&fin){
            System.out.println("receive fin packet and close connection");
            if(this.tcp_state == CONNECTION_FIN_INIT){
                this.tcp_state = CONNECTION_FIN_SUCCESS;
                System.out.println("three handshake shutdown");

                int seq_num = (int) headerInfo.get("seq_num");
                int ack_num = (int) headerInfo.get("ack_num");
                System.out.println("tcp handshake closing from othersize with seq_num "+seq_num+" and ack_num "+ack_num);
                this.seq_num+=1;
                this.ack_num= seq_num+1;
                try {
                    createAndSendPacket(null,"ACK");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        }

    }

}