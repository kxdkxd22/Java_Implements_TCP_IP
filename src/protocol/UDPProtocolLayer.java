package protocol;

import jpcap.packet.Packet;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;

public class UDPProtocolLayer implements IProtocol {

    private static short UDP_LENGTH_WITHOUT_DATA = 8;
    public static byte PROTOCOL_UDP = 17;

    @Override
    public byte[] createHeader(HashMap<String, Object> headerInfo) {
        short total_length = UDP_LENGTH_WITHOUT_DATA;
        byte[] data = null;
        if(headerInfo.get("data")!=null){
            data = (byte[]) headerInfo.get("data");
            total_length+=data.length;
        }

        byte[] buf = new byte[total_length];
        ByteBuffer byteBuffer = ByteBuffer.wrap(buf);

        if(headerInfo.get("source_port")==null){
            return null;
        }
        char srcPort = (char) headerInfo.get("source_port");
        byteBuffer.order(ByteOrder.BIG_ENDIAN);
        byteBuffer.putChar(srcPort);

        if(headerInfo.get("dest_port")==null){
            return null;
        }
        char destPort = (char) headerInfo.get("dest_port");
        byteBuffer.order(ByteOrder.BIG_ENDIAN);
        byteBuffer.putChar(destPort);

        byteBuffer.order(ByteOrder.BIG_ENDIAN);
        byteBuffer.putShort(total_length);
        char checksum = 65535;
        byteBuffer.putChar(checksum);

        if(data!=null){
            byteBuffer.put(data);
        }

        return byteBuffer.array();
    }

    @Override
    public HashMap<String, Object> handlePacket(Packet packet) {
        return null;
    }
}
