package Application;

import utils.IFTPDataReceiver;
import utils.ITCPHandler;

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class FTPClient implements ITCPHandler, IFTPDataReceiver {
    private TCPThreeHandShakes tcp_socket = null;
    private int data_port = 0;
    private FTPDataReceiver data_receiver = null;
    private String server_ip;

    @Override
    public void receive_ftp_data(byte[] data) {
        System.out.println("Successfully get ftp data");
        String ftp_data = new String(data);
        System.out.println("content of ftp_data: "+ftp_data);
    }

    @Override
    public void connect_notify(boolean connect_res) {
        if(connect_res == true){
            System.out.println("connect ftp server ok!");
        }
    }

    @Override
    public void send_notify(boolean send_res, byte[] packet_send) {

    }

    @Override
    public void connect_close_notify(boolean close_res) {

    }

    @Override
    public void recv_notify(byte[] packet_recv) {
        try {
            String server_return = new String(packet_recv,"ASCII");
            System.out.println("receive info from ftp server: "+server_return);
            String return_code = server_return.substring(0,3);
            String return_str = server_return.substring(3);
            if(return_code.equals("220")){
                System.out.println("receive code 220: "+return_str);
                send_command("USER anonymous\r\n");
            }

            if(return_code.equals("331")){
                System.out.println("receive code 331: "+return_str);
                send_command("PASS 1111\r\n");
            }

            if(return_code.equals("230")){
                System.out.println("receive code 230: "+return_str);
                send_command("PWD\r\n");
            }

            if(return_code.equals("257")){
                System.out.println("receive code 257: "+return_str);
                send_command("PASV\r\n");
            }

            if(return_code.equals("227")){
                System.out.println("receive code 227: "+return_str);
                int ip_port_index = return_str.indexOf("(");
                String port_str = return_str.substring(ip_port_index);
                int ip_count = 4;
                while(ip_count>0){
                    int idx = port_str.indexOf(',');
                    ip_count--;
                    port_str = port_str.substring(idx+1);
                }
                int idx = port_str.indexOf(',');
                String p1 = port_str.substring(0,idx);
                port_str = port_str.substring(idx+1);
                idx = port_str.indexOf(')');
                String p2 = port_str.substring(0,idx);
                int port = Integer.parseInt(p1)*256+Integer.parseInt(p2);
                System.out.println("get data port: "+port);
                data_port = port;
                send_command("TYPE A\r\n");
            }

            if(return_code.equals("200")){
                System.out.println("receive code 200: "+return_str);
                send_command("LIST\r\n");
                data_receiver = new FTPDataReceiver(server_ip,data_port,this);
                data_receiver.get_data();
            }

            if(return_code.equals("150")){
                System.out.println("receive code 150: "+return_str);
                tcp_socket.tcp_close();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void send_command(String command){
        try {
            tcp_socket.tcp_send(command.getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void run(){
        try {
            InetAddress ip = InetAddress.getByName("10.4.7.85");
            server_ip = "10.4.7.85";
            short port = 21;
            tcp_socket = new TCPThreeHandShakes(ip.getAddress(),port,this);
            tcp_socket.tcp_connect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



}
