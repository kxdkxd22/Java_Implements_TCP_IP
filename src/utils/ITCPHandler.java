package utils;

public interface ITCPHandler {
    public void connect_notify(boolean connect_res);
    public void send_notify(boolean send_res,byte[] packet_send);
    public void connect_close_notify(boolean close_res);
    public void recv_notify(byte[] data);
}
