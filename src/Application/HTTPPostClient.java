package Application;

import utils.ITCPHandler;

import java.net.InetAddress;

public class HTTPPostClient implements ITCPHandler {
    private  TCPThreeHandShakes  tcp_socket = null;
    private HTTPEncoder httpEncoder = new HTTPEncoder();
    private String user_agent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 1-14_0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/81.0.4044.122 Safari/547.36";
    private String accept = "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3q=0.9";
    private String accept_encoding = "gzip, deflate";
    private String accept_language = "zh-CN,zh;q=0.9,en;q=0.8";
    private String cache_control = "max-age=0";
    private String connection = "close";
    private static int HTTP_OK = 200;
    private String file_name = "my_test.txt";
    private String content_type = "multipart/form-data; boundary=";
    private MIMETextPlainMediaEcnapusulation mime_encoded = new MIMETextPlainMediaEcnapusulation(file_name);

    private void send_content() throws Exception {//设置http请求数据的头部信息
        mime_encoded.add_content("This is test content for line1"); //文档里面的内容
        String send_content = mime_encoded.get_mime_part();
        String content_length = Integer.toString(send_content.length());

        httpEncoder.set_method(HTTPEncoder.HTTP_METHOD.HTTP_POST, "/");
        httpEncoder.set_header("Host","192.168.2.127:8888");
        httpEncoder.set_header("Connection", connection);
        httpEncoder.set_header("User-Agent", user_agent);
        httpEncoder.set_header("Content-Length", content_length);
        httpEncoder.set_header("Accept", accept);
        httpEncoder.set_header("Accept-Encoding", accept_encoding);
        httpEncoder.set_header("Accept-Language", accept_language);
        httpEncoder.set_header("Cache-Control", cache_control);
        httpEncoder.set_header("Content-type", content_type + mime_encoded.get_boundary_string());
        httpEncoder.set_header("Upgrade-Insecure-Requests", "1");
        httpEncoder.set_header("Origin",  "http://192.168.2.127:8888");
        httpEncoder.set_header("Referer", "http://192.168.2.127:8888/");

        String http_content = httpEncoder.get_http_content();
        http_content += send_content;
        System.out.println(http_content);
        byte[] send_content_bytes = http_content.getBytes();
        tcp_socket.tcp_send(send_content_bytes);
    }

    @Override
    public void connect_notify(boolean connect_res) {
        if (connect_res == true) {
            System.out.println("connect http server ok!");
            try {
                send_content();
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        else {
            System.out.println("connect http server fail!");
        }
    }

    @Override
    public void send_notify(boolean send_res, byte[] packet_send) {
        if (send_res) {
            System.out.println("send request to http server!");
        }
    }

    private void close_connection() {
        try {
            tcp_socket.tcp_close();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    public void recv_notify(byte[] packet_recv) {
        String content = new String(packet_recv);
        int code = httpEncoder.get_return_code(content);
        if  (code != HTTP_OK) {
            System.out.println("http return error: " + code);
        } else {
            System.out.println("Http return 200 OK! Post Success!");
        }

        close_connection();
    }

    @Override
    public void connect_close_notify(boolean close_res) {
        if (close_res) {
            System.out.println("Close connection with http server!");
        }
    }

    public void run() {
        try {
            InetAddress ip = InetAddress.getByName("192.168.2.127"); //连接ftp服务器
            short port = 8888;
            tcp_socket = new TCPThreeHandShakes(ip.getAddress(), port, this);
            tcp_socket.tcp_connect();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
