package ICMPProtocolLayer;

import jpcap.PacketReceiver;
import jpcap.packet.EthernetPacket;
import jpcap.packet.Packet;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;

public class ICMPProtocolLayer implements PacketReceiver {
    private static byte PROTOCOL_ICMP = 1;
    public static int ICMP_DATA_OFFSET = 20+14;
    private static int PROTOCOL_FIELD_IN_IP_HEADER = 14+9;

    private ArrayList<IICMPErrorMsgHandler> error_handler_list = new ArrayList<IICMPErrorMsgHandler>();

    private enum ICMP_MSG_TYPE{
        ICMP_UNKNOW_MSG,
        ICMP_ERROR_MSG,
        ICMP_INFO_MSG
    };

    private int icmp_type = 0;
    private int icmp_code = 0;
    private byte[] packet_header = null;
    private byte[] packet_data = null;

    public ICMPProtocolLayer(){
        error_handler_list.add(new ICMPUnReachableMsgHandler());
    }

    @Override
    public void receivePacket(Packet packet) {
        if(packet == null){
            return;
        }

        EthernetPacket ethernetPacket = (EthernetPacket) packet.datalink;
        if(ethernetPacket.frametype!=EthernetPacket.ETHERTYPE_IP){
            return;
        }

        if(packet.header[PROTOCOL_FIELD_IN_IP_HEADER]!=PROTOCOL_ICMP){
            return;
        }

        packet_header = Arrays.copyOfRange(packet.header,ICMP_DATA_OFFSET,packet.header.length);
        packet_data = packet.data;

        analyzeICMPMessage(packet_header);
    }

    private ICMP_MSG_TYPE checkType(int type){
        if(type>=0&&type<=127){
            return ICMP_MSG_TYPE.ICMP_ERROR_MSG;
        }
        if(type>=128&&type<=255){
            return ICMP_MSG_TYPE.ICMP_INFO_MSG;
        }
        return ICMP_MSG_TYPE.ICMP_UNKNOW_MSG;
    }

    private void analyzeICMPMessage(byte[] data){
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        icmp_type = byteBuffer.get(0);
        icmp_code = byteBuffer.get(1);

        if(checkType(icmp_type)==ICMP_MSG_TYPE.ICMP_ERROR_MSG){
            handleICMPErrorMsg(packet_data);
        }
    }

    private void handleICMPErrorMsg(byte[] data){
        for(int i = 0; i < error_handler_list.size(); i++){
            if(error_handler_list.get(i).handleICMPErrorMsg(icmp_type,icmp_code,data)==true){
                break;
            }
        }
    }
}
