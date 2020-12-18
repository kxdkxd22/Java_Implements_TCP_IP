package protocol;

import jpcap.packet.Packet;

import java.util.HashMap;

public interface IProtocol {
    public byte[] createHeader(HashMap<String,Object>headerInfo);
    public HashMap<String, Object>handlePacket(Packet packet);
}
