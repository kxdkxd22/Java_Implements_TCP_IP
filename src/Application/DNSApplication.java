package Application;

import protocol.IProtocol;
import protocol.ProtocolManager;
import protocol.UDPProtocolLayer;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Random;

public class DNSApplication extends Application {
    private byte[] resove_server_ip = null;
    private String domainName = "";
    private byte[] dnsHeader = null;
    private short transition_id = 0;
    private byte[] dnsQuestion = null;
    private static int QUESTION_TYPE_LENGTH = 2;
    private static int QUESTION_CLASS_LENGTH = 2;
    private static short QUESTION_TYPE_A = 1;
    private static short QUESTION_CLASS = 1;
    private static char DNS_SERVER_PORT = 53;

    private static short DNS_ANSWER_CANONICAL_NAME_FOR_ALIAS = 5;
    private static short DNS_ANSWER_HOST_ADDRESS = 1;

    public DNSApplication(byte[] destIP,String domainName){
        this.resove_server_ip = destIP;
        this.domainName = domainName;
        Random rand = new Random();
        transition_id = (short) rand.nextInt();
        this.port = (short)rand.nextInt();
        constructDNSPacketHeader();
        constructDNSPacketQuestion();
    }

    public void queryDomain(){
        byte[] dnsPacketBuffer = new byte[dnsHeader.length+dnsQuestion.length];
        ByteBuffer buffer = ByteBuffer.wrap(dnsPacketBuffer);
        buffer.put(dnsHeader);
        buffer.put(dnsQuestion);

        byte[] udpHeader = createUDPHeader(dnsPacketBuffer);
        byte[] ipHeader = createIP4Header(udpHeader.length);

        byte[] dnsPacket = new byte[udpHeader.length+ipHeader.length];
        buffer = ByteBuffer.wrap(dnsPacket);
        buffer.put(ipHeader);
        buffer.put(udpHeader);

        try {
            ProtocolManager.getInstance().sendData(dnsPacket,resove_server_ip);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void constructDNSPacketHeader(){
        byte[] header = new byte[12];
        ByteBuffer buffer = ByteBuffer.wrap(header);

        buffer.putShort(transition_id);
        short opCode = 0;
        opCode = (short)(opCode|(1<<8));
        buffer.putShort(opCode);

        short questionCount = 1;
        buffer.putShort(questionCount);
        short answerRRCount = 0;
        buffer.putShort(answerRRCount);
        short AuthorityRRCount = 0;
        buffer.putShort(AuthorityRRCount);
        short AdditionalRRCount = 0;
        buffer.putShort(AdditionalRRCount);
        this.dnsHeader = buffer.array();
    }

    private void constructDNSPacketQuestion(){
        dnsQuestion = new byte[1+1+domainName.length()+QUESTION_CLASS_LENGTH+QUESTION_TYPE_LENGTH];
        String[] doaminParts = domainName.split("\\.");
        ByteBuffer buffer = ByteBuffer.wrap(dnsQuestion);
        for(int i = 0; i < doaminParts.length; i++){
            buffer.put((byte) doaminParts[i].length());

            for(int j = 0; j < doaminParts[i].length(); j++){
                buffer.put((byte) doaminParts[i].charAt(j));
            }
        }

        byte end = 0;
        buffer.put(end);
        buffer.putShort(QUESTION_TYPE_A);
        buffer.putShort(QUESTION_CLASS);

    }

    private byte[] createUDPHeader(byte[] data){
        IProtocol udpProto = ProtocolManager.getInstance().getProtocol("udp");
        if(udpProto == null){
            return null;
        }

        HashMap<String,Object> headerInfo = new HashMap<String,Object>();
        char udpPort = (char) this.port;
        headerInfo.put("source_port",udpPort);
        headerInfo.put("dest_port",DNS_SERVER_PORT);
        headerInfo.put("data",data);
        return udpProto.createHeader(headerInfo);

    }

    private byte[] createIP4Header(int dataLength){
        IProtocol ip4Proto = ProtocolManager.getInstance().getProtocol("ip");
        if(ip4Proto == null || dataLength <= 0){
            return null;
        }
        HashMap<String,Object> headerInfo = new HashMap<String,Object>();
        headerInfo.put("data_length",dataLength);
        ByteBuffer destIP = ByteBuffer.wrap(resove_server_ip);
        headerInfo.put("destination_ip",destIP.getInt());
        byte protocol = UDPProtocolLayer.PROTOCOL_UDP;
        headerInfo.put("protocol",protocol);
        headerInfo.put("identification",transition_id);
        byte[] ipHeader = ip4Proto.createHeader(headerInfo);

        return ipHeader;
    }

    public void handleData(HashMap<String,Object>headerInfo){
        byte[] data = (byte[]) headerInfo.get("data");
        if(data == null){
            System.out.println("empty data");
            return;
        }

        ByteBuffer buffer = ByteBuffer.wrap(data);
        short transionID = buffer.getShort();
        if(transionID!=this.transition_id){
            System.out.println("transition id different!");
            return;
        }

        short flag = buffer.getShort();
        readFlags(flag);

        short questionCount = buffer.getShort();
        System.out.println("client send "+questionCount+" requests");

        short answerCount = buffer.getShort();
        System.out.println("server return "+answerCount+" answers");

        short authorityCount = buffer.getShort();
        System.out.println("server return "+authorityCount+" authority resources");

        short additionalInfoCount = buffer.getShort();
        System.out.println("server return "+additionalInfoCount+" additional infos");

        readQuestions(questionCount,buffer);

        readAnswers(answerCount,buffer);
    }

    private void readQuestions(int count,ByteBuffer buffer){
        for(int i = 0; i < count; i++){
            readStringContent(buffer);
            short questionType = buffer.getShort();
            if(questionType==QUESTION_TYPE_A){
                System.out.println("request ip for given domain name");
            }

            short questionClass = buffer.getShort();
            System.out.println("the class of the request is "+questionClass);
        }
    }
    private void readAnswers(int count,ByteBuffer buffer){
        for(int i = 0; i <count; i++){
            System.out.println("Name content in answer field is: ");
            if(isNameCompression(buffer.get())){
                int offset = buffer.get();
                byte[] array = buffer.array();
                ByteBuffer dup_buffer = ByteBuffer.wrap(array);
                dup_buffer.position(offset);
                readStringContent(dup_buffer);
            }else{
                readStringContent(buffer);
            }

            short type = buffer.getShort();
            System.out.println("answer type is : "+type);

            if(type==DNS_ANSWER_CANONICAL_NAME_FOR_ALIAS){
                System.out.println("this answer contain server string name");
            }

            short cls = buffer.getShort();
            System.out.println("answer class: "+cls);

            int ttl = buffer.getInt();
            System.out.println("this information can cache "+ttl+" seconds");

            short rdLength = buffer.getShort();
            System.out.println("content length is "+rdLength);

            if(type == DNS_ANSWER_CANONICAL_NAME_FOR_ALIAS){
                readStringContent(buffer);
            }

            if(type==DNS_ANSWER_HOST_ADDRESS){
                byte[] ip = new byte[4];
                for(int k = 0; k < 4; k++){
                    ip[k] = buffer.get();
                }
                try {
                    InetAddress ipAddr = InetAddress.getByAddress(ip);
                    System.out.println("ip address for domain name is: "+ipAddr.getHostAddress());
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }

            }

        }


    }

    private boolean isNameCompression(byte b){
        if((b&(1<<7))!=0&&(b&(1<<6))!=0){
            return true;
        }
        return false;
    }

    private void readStringContent(ByteBuffer buffer){
        byte charCount = buffer.get();
        while(charCount>0||isNameCompression(charCount)==true){
            if(isNameCompression(charCount)){
                int offset = buffer.get();
                byte[] array = buffer.array();
                ByteBuffer dup_buffer = ByteBuffer.wrap(array);
                dup_buffer.position(offset);
                readStringContent(dup_buffer);
                break;
            }
            for(int i = 0; i < charCount; i++){
                System.out.print((char)buffer.get());
            }
            charCount = buffer.get();
            if(charCount!=0){
                System.out.print(".");
            }

        }

        System.out.println("\n");
    }

    private void readFlags(short flag){
        if((flag&(1<<15))!=0){
            System.out.println("this is packet return from server");
        }

        if((flag&(1<<8))!=0){
            System.out.println("client requests recursive query!");
        }

        if((flag&(1<<7))!=0){
            System.out.println("server accept recursive query request!");
        }

        if((flag&(1<<5))!=0){
            System.out.println("server own the domain info");
        }else{
            System.out.println("server query domain info from other servers");
        }

    }

}
