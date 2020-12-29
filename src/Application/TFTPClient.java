package Application;

import protocol.IProtocol;
import protocol.ProtocolManager;
import protocol.UDPProtocolLayer;
import sun.awt.windows.WBufferStrategy;

import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.HashMap;

public class TFTPClient extends Application {
    private byte[] server_ip = null;
    private static short OPTION_CODE_READ = 1;
    private static short OPTION_CODE_WRITE = 2;
    private static final short OPTION_CODE_ACK = 4;
    private static final short OPTION_CODE_DATA = 3;
    private static final short OPTION_CODE_ERR = 5;
    private static short TFTP_ERROR_FILE_NOT_FOUND = 1;
    private static short OPTION_CODE_LENGTH = 2;
    private short data_block = 1;
    private short put_block = 0;
    private static char TFTP_SERVER_PORT = 69;
    private char server_port = 0;
    private File download_file;
    private File upload_file;
    int upload_file_size = 0;
    private String file_name;
    FileOutputStream file_stream;
    FileInputStream file_input;

    public TFTPClient(byte[] server_ip){
        this.server_ip = server_ip;
        this.port = (short)10086;
        server_port = TFTP_SERVER_PORT;
    }

    public void getFile(String file_name){
        download_file = new File(file_name);
        this.file_name = file_name;
        try {
            file_stream = new FileOutputStream(download_file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        sendRequestPacket(OPTION_CODE_READ);
    }

    public void putFile(String file_name){
        upload_file = new File(file_name);
        this.file_name = file_name;

        try {
            file_input = new FileInputStream(upload_file);
            upload_file_size = file_input.available();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        sendRequestPacket(OPTION_CODE_WRITE);
    }

    private void sendRequestPacket(short option){
        String mode = "netascii";
        byte[] read_request = new byte[OPTION_CODE_LENGTH+this.file_name.length()+1+mode.length()+1];
        ByteBuffer buffer = ByteBuffer.wrap(read_request);
        buffer.putShort(option);
        buffer.put(file_name.getBytes());
        buffer.put((byte)0);
        buffer.put(mode.getBytes());
        buffer.put((byte)0);

        byte[] udpHeader = createUDPHeader(read_request);
        byte[] ipHeader = createIP4Header(udpHeader.length);

        byte[] readRequestPacket = new byte[udpHeader.length+ipHeader.length];
        buffer = ByteBuffer.wrap(readRequestPacket);
        buffer.put(ipHeader);
        buffer.put(udpHeader);

        try {
            ProtocolManager.getInstance().sendData(readRequestPacket,server_ip);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private byte[] createUDPHeader(byte[] data){
        IProtocol udpProto = ProtocolManager.getInstance().getProtocol("udp");
        if(udpProto == null){
            return null;
        }

        HashMap<String,Object> headerInfo = new HashMap<String, Object>();
        char udpPort = (char) this.port;
        headerInfo.put("source_port",udpPort);
        headerInfo.put("dest_port",server_port);
        headerInfo.put("data",data);
        return udpProto.createHeader(headerInfo);
    }

    private byte[] createIP4Header(int dataLength){
        IProtocol ip4Proto = ProtocolManager.getInstance().getProtocol("ip");
        if(ip4Proto == null||dataLength <= 0){
            return null;
        }

        HashMap<String,Object> headerInfo = new HashMap<String,Object>();
        headerInfo.put("data_length",dataLength);

        ByteBuffer destIP = ByteBuffer.wrap(server_ip);
        headerInfo.put("destination_ip",destIP.getInt());

        try {
            InetAddress fake_ip = InetAddress.getByName("10.4.0.22");
            ByteBuffer buf = ByteBuffer.wrap(fake_ip.getAddress());
            headerInfo.put("source_ip",buf.getInt());
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        byte protocol = UDPProtocolLayer.PROTOCOL_UDP;
        headerInfo.put("protocol",protocol);
        byte[] ipHeader = ip4Proto.createHeader(headerInfo);

        return ipHeader;
    }

    public void handleData(HashMap<String,Object> headerInfo){
        byte[] data = (byte[]) headerInfo.get("data");
        if(data == null){
            System.out.println("empty data");
            return;
        }

        short port = (short) headerInfo.get("src_port");
        server_port = (char) port;
        ByteBuffer buff = ByteBuffer.wrap(data);
        short opCode = buff.getShort();
        switch(opCode){
            case OPTION_CODE_ERR:
                handleErrorPacket(buff);
                break;
            case OPTION_CODE_DATA:
                handleDataPacket(buff);
                break;
            case OPTION_CODE_ACK:
                handleACKPacket(buff);
                break;
        }

    }

    private void handleACKPacket(ByteBuffer buff){
        put_block = buff.getShort();

        System.out.println("receive server ack with block num : "+put_block);
        put_block++;
        sendDataBlockPacket();
    }

    private void sendDataBlockPacket(){
        System.out.println("send data block: "+put_block);
        byte[] file_content = new byte[512];

        try {
            int bytes_read = file_input.read(file_content);
            if(bytes_read<=0){
                return;
            }
            byte[] content = new byte[2+2+bytes_read];
            ByteBuffer buf = ByteBuffer.wrap(content);
            buf.putShort(OPTION_CODE_DATA);
            buf.putShort(put_block);
            buf.put(file_content,0,bytes_read);

            byte[] udpHeader = createUDPHeader(content);
            byte[] ipHeader = createIP4Header(udpHeader.length);
            byte[] dataPacket = new byte[udpHeader.length+ipHeader.length];
            buf = ByteBuffer.wrap(dataPacket);
            buf.put(ipHeader);
            buf.put(udpHeader);
            ProtocolManager.getInstance().sendData(dataPacket,server_ip);
            System.out.println("send content with bytes: "+bytes_read);
            upload_file_size-=bytes_read;
            if(upload_file_size<=0||bytes_read<512){
                System.out.println("put file complete");
                put_block = 0;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleDataPacket(ByteBuffer buffer){
        data_block = buffer.getShort();
        System.out.println("receive data block "+data_block);
        byte[] data = buffer.array();
        int content_len = data.length - buffer.position();
        byte[] file_content = new byte[content_len];
        buffer.get(file_content);

        try {
            file_stream.write(file_content);
            System.out.println("write data block "+data_block+" to file");
        } catch (IOException e) {
            e.printStackTrace();
        }

        if(content_len == 512){
            sendACKPacket();
            data_block++;
        }

        if(content_len<512){
            sendACKPacket();

            try {
                file_stream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            data_block = 1;
        }
    }

    private void sendACKPacket(){
        byte[] ack_content = new byte[4];
        ByteBuffer buffer = ByteBuffer.wrap(ack_content);
        buffer.putShort(OPTION_CODE_ACK);
        buffer.putShort(data_block);

        byte[] udpHeader = createUDPHeader(ack_content);
        byte[] ipHeader = createIP4Header(udpHeader.length);

        byte[] ackPacket = new byte[udpHeader.length+ipHeader.length];
        buffer = ByteBuffer.wrap(ackPacket);
        buffer.put(ipHeader);
        buffer.put(udpHeader);

        try {
            ProtocolManager.getInstance().sendData(ackPacket,server_ip);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void handleErrorPacket(ByteBuffer buffer){
        short err_info = buffer.getShort();
        if(err_info == TFTP_ERROR_FILE_NOT_FOUND){
            System.out.println("TFTP server return file not found packet");
        }
        byte[] data = buffer.array();
        int pos = buffer.position();
        int left_len = data.length-pos;
        byte[] err_msg = new byte[left_len];
        buffer.get(err_msg);
        String err_str = new String(err_msg);
        System.out.println("error message from server: "+err_str);
    }

}
