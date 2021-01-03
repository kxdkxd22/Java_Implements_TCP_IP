package Application;

import utils.ITCPHandler;
import java.net.InetAddress;

public class TCPRawDataSender implements ITCPHandler {
    TCPThreeHandShakes tcp_socket = null;
    private String[] buffer = new String[]{"h","e","l","l","o"};
    private int buffer_p = 0;
    private byte[] current_send_packet = null;

    private void send_content() throws Exception {
        if(buffer_p < buffer.length){
            System.out.println("send content: "+buffer[buffer_p]);
            byte[] send_content = buffer[buffer_p].getBytes();
            current_send_packet = send_content;
            tcp_socket.tcp_send(send_content);
        }
    }

    @Override
    public void connect_notify(boolean connect_res) {
        if(connect_res){
            System.out.println("connection established!");
            try {
                send_content();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }else{
            System.out.println("connection fail!");
        }
    }

    private void close_connection(){
        try {
            tcp_socket.tcp_close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void send_notify(boolean send_res, byte[] packet_send) {
        if(send_res == true){
            System.out.println("send data, buffer_p: "+buffer_p);
            if(packet_send == current_send_packet){
                buffer_p++;
                current_send_packet = null;
            }
        }

        if(buffer_p>=buffer.length||send_res==false){
            String info = "send all data ";
            if(send_res == false){
                info = "send fail with buffer_p: "+buffer_p;
            }
            System.out.println(info);
        }else{
            try {
                send_content();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    @Override
    public void connect_close_notify(boolean close_res) {
        if(close_res ==true){
            System.out.println("connection close complete!");
        }else{
            System.out.println("connection close fail!");
        }
    }

    public void run(){
        try {
            InetAddress ip = InetAddress.getByName("202.204.202.5");
            short port = 80;
            tcp_socket = new TCPThreeHandShakes(ip.getAddress(),port,this);
            tcp_socket.tcp_connect();
            System.out.println("finish handshake!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void recv_notify(byte[] packet_recv){
        System.out.println("receive data: "+new String(packet_recv));
        close_connection();
    }
}
