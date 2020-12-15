package ICMPProtocolLayer;

import jpcap.packet.IPPacket;

import java.nio.ByteBuffer;

public class ICMPUnReachableMsgHandler implements IICMPErrorMsgHandler {
    private static int ICMP_UNREACHABLE = 3;
    private static int IP_HEADER_LENGTH = 20;

    enum ICMP_ERROR_MSG_CODE{
        ICMP_NETWORK_UNREACHABLE,
        ICMP_HOST_UNREACHABLE,
        ICMP_PROTOCOL_UNREACHABLE,
        ICMP_PORT_UNREACHABLE,
        ICMP_FRAGMETATION_NEEDED_AND_DF_SET
    };


    @Override
    public boolean handleICMPErrorMsg(int type, int code, byte[] data) {
        if(type != ICMPUnReachableMsgHandler.ICMP_UNREACHABLE){
            return false;
        }

        ByteBuffer buffer = ByteBuffer.wrap(data);

        switch (ICMP_ERROR_MSG_CODE.values()[code]){
            case ICMP_PORT_UNREACHABLE:
                byte protocol = buffer.get(9);
                if(protocol == IPPacket.IPPROTO_ICMP){
                    handleICMPError(buffer);
                }
                break;
            default:
                return false;
        }

        return true;
    }

    private void handleICMPError(ByteBuffer buffer){
        System.out.println("Source IP Address is :");
        int source_ip_offset = 12;
        for(int i = 0; i < 4; i++){
            int v = buffer.get(source_ip_offset+i)&0xff;
            System.out.print(v+".");
        }

        System.out.println("\nDest IP Address is: ");
        int dest_ip_offset = 16;
        for(int i = 0; i < 4; i++){
            int v = buffer.get(dest_ip_offset+i)&0xff;
            System.out.print(v+".");
        }

    }

}
