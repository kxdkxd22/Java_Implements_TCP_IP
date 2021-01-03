package Application;

import utils.IFTPDataReceiver;
import utils.ITCPHandler;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

public class FTPDataReceiver implements ITCPHandler {
    private int data_port = 0;
    private IFTPDataReceiver dataReceiver = null;
    private TCPThreeHandShakes tcp_socket = null;
    private String server_ip = "";
    private byte[] data_buffer = new byte[4096];
    private ByteBuffer byteBuffer = null;
    public FTPDataReceiver(String ip,int port,IFTPDataReceiver receiver){
        this.data_port = port;
        this.server_ip = ip;
        this.dataReceiver = receiver;
        byteBuffer = ByteBuffer.wrap(data_buffer);
    }

    public void get_data(){
        try {
            InetAddress ip = InetAddress.getByName(server_ip);
            tcp_socket = new TCPThreeHandShakes(ip.getAddress(),(short)data_port,this);
            tcp_socket.tcp_connect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void connect_notify(boolean connect_res) {
        if(connect_res){
            System.out.println("ftp data connection ok");
        }else{
            System.out.println("ftp data connection fail");
        }
    }

    @Override
    public void send_notify(boolean send_res, byte[] packet_send) {

    }

    @Override
    public void connect_close_notify(boolean close_res) {
        try {
            tcp_socket.tcp_close();
            dataReceiver.receive_ftp_data(byteBuffer.array());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void recv_notify(byte[] data) {
        System.out.println("ftp receiving data");
        byteBuffer.put(data);
    }
}
