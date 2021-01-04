package Application;

import utils.ITCPHandler;

import java.net.InetAddress;

public class HTTPClient implements ITCPHandler {
    private TCPThreeHandShakes tcp_socket = null;
    private  HTTPEncoder httpEncoder = new HTTPEncoder();
    private String user_agent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:84.0) Gecko/20100101 Firefox/84.0";
    private String accept = "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8";
    private int total_length = 0;
    private String http_return_content = "";
    private boolean receive_first_time = true;
    private int receive_content_length = 0;
    private static int HTTP_OK = 200;

    private void send_content() throws Exception {
        httpEncoder.set_method(HTTPEncoder.HTTP_METHOD.HTTP_GET,"/reader/login.php");
        httpEncoder.set_header("host","");
        httpEncoder.set_header("Connection","keep-alive");
        httpEncoder.set_header("Upgrade-Insecure-Requests","1");
        httpEncoder.set_header("User-Agent",user_agent);
        httpEncoder.set_header("Accept",accept);
        httpEncoder.set_header("Accept-Encoding","gzip,deflate");
        httpEncoder.set_header("Accept-Language","zh-CN,zh;q=0.8,zh-TW;q=0.7,zh-HK;q=0.5,en-US;q=0.3,en;q=0.2");
        String http_content = httpEncoder.get_http_content();
        System.out.println(http_content);
        byte[] send_content = http_content.getBytes();
        tcp_socket.tcp_send(send_content);

    }


    @Override
    public void connect_notify(boolean connect_res) {
        if(connect_res == true){
            System.out.println("connect http server ok!");
            try {
                send_content();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }else{
            System.out.println("connect http server fail!");
        }
    }

    @Override
    public void send_notify(boolean send_res, byte[] packet_send) {
        if(send_res){
            System.out.println("send request to http server!");
        }
    }

    @Override
    public void connect_close_notify(boolean close_res) {
        if(close_res){
            System.out.println("Close connection with http server!");
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
    public void recv_notify(byte[] data) {
        String content = new String(data);
        if(receive_first_time){
            receive_first_time = false;
            int code = httpEncoder.get_return_code(content);
            if(code != HTTP_OK){
                System.out.println("http return error: "+code);
                close_connection();
                return;
            }else {
                System.out.println("Http return 200 ok!");
            }
            this.total_length = httpEncoder.get_content_length(content);
            if(this.total_length<=0){
                close_connection();
                return;
            }
            http_return_content+=content;
            receive_content_length+=data.length;
            System.out.println("Content Length: "+total_length);
        }else {
            http_return_content+=content;
            receive_content_length+=data.length;
            System.out.println("00000000000000000000000000000000: "+data.length);
            if(receive_content_length >= total_length+435){
                System.out.println("receive content is: "+http_return_content);
                close_connection();
            }
        }
    }

    public void run(){
        try {
            InetAddress ip = InetAddress.getByName("");
            short port = 80;
            tcp_socket = new TCPThreeHandShakes(ip.getAddress(),port,this);
            tcp_socket.tcp_connect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
