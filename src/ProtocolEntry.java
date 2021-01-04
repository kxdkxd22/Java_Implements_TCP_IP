import Application.*;
import datalinklayer.DataLinkLayer;
import jpcap.JpcapCaptor;
import jpcap.NetworkInterface;
import jpcap.NetworkInterfaceAddress;

import java.io.IOException;
import java.net.InetAddress;

public class ProtocolEntry {

    public static void main(String[] args) throws IOException {
        NetworkInterface[] devices = JpcapCaptor.getDeviceList();
        JpcapCaptor captor = null;
        NetworkInterface device = null;

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

           // captor = JpcapCaptor.openDevice(devices[i],65536,false,20);

        }

        device = devices[1];
        System.out.println("open device: "+device.name);
        JpcapCaptor jpcap = JpcapCaptor.openDevice(device,2000,true,20);

        DataLinkLayer dataLinkLayer = DataLinkLayer.getInstance();
        dataLinkLayer.initWithOpenDevice(device);

        //String ip = "";
        //InetAddress address = InetAddress.getByName(ip);
        //HPingApp pingApp = new HPingApp(1,address.getAddress());
        //pingApp.startPing();
        //TraceRoute traceRoute = new TraceRoute(address.getAddress());
        //traceRoute.startTraceRoute();

        //DHCPApplication dhcpApp = new DHCPApplication();
        //dhcpApp.dhcpDiscovery();

        //InetAddress ip = InetAddress.getByName("");
        //DNSApplication dnsApp = new DNSApplication(ip.getAddress(),"pan.baidu.com");
        //dnsApp.queryDomain();



        /*try {
            InetAddress ip = InetAddress.getByName("");
            short port = 80;
            TCPThreeHandShakes tcpHandShake = new TCPThreeHandShakes(ip.getAddress(),port);
            tcpHandShake.beginThreeHandShakes();
        } catch (Exception e) {
            e.printStackTrace();
        }*/
       // TCPRawDataSender tcp_raw = new TCPRawDataSender();
        //tcp_raw.run();

        //TFTPClient tftp = new TFTPClient(ip.getAddress());
        //tftp.putFile("1.pdf");

      //  FTPClient ftpClient = new FTPClient();
       // ftpClient.run();

        HTTPClient httpClient = new HTTPClient();
        httpClient.run();

        jpcap.loopPacket(-1, DataLinkLayer.getInstance());
    }
}
