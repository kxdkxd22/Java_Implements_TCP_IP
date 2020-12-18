package protocol;


import jpcap.packet.Packet;

import java.util.ArrayList;
import java.util.HashMap;

public class ICMPProtocolLayer implements IProtocol {
    public static byte PROTOCOL_ICMP = 1;
    private ArrayList<IProtocol> protocol_header_list = new ArrayList<IProtocol>();
    private Packet packet;

    @Override
    public byte[] createHeader(HashMap<String, Object> headerInfo) {
        for(int i = 0; i < protocol_header_list.size(); i++){
            byte[] buff = protocol_header_list.get(i).createHeader(headerInfo);
            if(buff!=null){
                return buff;
            }
        }
        return null;
    }

    @Override
    public HashMap<String, Object> handlePacket(Packet packet) {
        this.packet = packet;
        return analyzeICMPMessage();
    }


    public ICMPProtocolLayer(){
        protocol_header_list.add(new ICMPEchoHeader());
    }


    private HashMap<String,Object> analyzeICMPMessage(){
       HashMap<String,Object> info = null;
       info = handleICMPInfoMsg(this.packet);
       return info;
    }

    private HashMap<String,Object> handleICMPInfoMsg(Packet packet){
        for(int i = 0; i < protocol_header_list.size(); i++){
            IProtocol handler = protocol_header_list.get(i);
            HashMap<String,Object> info = handler.handlePacket(packet);
            if(info!=null){
                return info;
            }
        }
        return null;
    }

}
