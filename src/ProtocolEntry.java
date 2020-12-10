import jpcap.JpcapCaptor;
import jpcap.NetworkInterface;
import jpcap.NetworkInterfaceAddress;
import jpcap.PacketReceiver;
import jpcap.packet.Packet;

public class ProtocolEntry implements PacketReceiver {

    @Override
    public void receivePacket(Packet packet) {
        System.out.println(packet);
        System.out.println("Receive a packet");
    }

    public static void main(String[] args) {
        NetworkInterface[] devices = JpcapCaptor.getDeviceList();
        JpcapCaptor captor = null;

        for(int i = 0; i < devices.length; i++){
            System.out.println(i+": "+devices[i].name+"("+devices[i].description+")");

            System.out.println("datalink: "+devices[i].datalink_name+"("+devices[i].datalink_description+")");

            System.out.println("Mac Address: ");
            for(byte b:devices[i].mac_address){
                System.out.print(Integer.toHexString(b&0xff)+":");
            }

            System.out.println();

            for(NetworkInterfaceAddress a: devices[i].addresses){
                System.out.println(" address:"+a.address+" "+a.subnet+" "+a.broadcast);
            }



        }


    }
}
