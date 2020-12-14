package utils;

import jpcap.PacketReceiver;
import jpcap.packet.Packet;

import java.util.ArrayList;

public class PacketProvider  implements IPacketProvider{
    private ArrayList<PacketReceiver> receiverList = new ArrayList<PacketReceiver>();

    @Override
    public void registerPacketReceiver(PacketReceiver receiver) {
        if(this.receiverList.contains(receiver)!=true){
            this.receiverList.add(receiver);
        }
    }

    protected void pushPacketToReceivers(Packet packet){
        for(int i = 0; i < receiverList.size(); i++){
            PacketReceiver receiver = receiverList.get(i);
            receiver.receivePacket(packet);
        }
    }

}
