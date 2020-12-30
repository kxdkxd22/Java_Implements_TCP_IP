package protocol;

import jpcap.packet.Packet;
import utils.Utility;

import java.nio.ByteBuffer;
import java.util.HashMap;

public class TCPProtocolLayer  implements IProtocol{
    private static int HEADER_LENGTH = 20;
    private int sequence_number = 2;
    private int acknowledgement_number = 0;
    private static int PSEUDO_HEADER_LENGTH = 12;
    public static byte TCP_PROTOCOL_NUMBER = 6;
    private static int POSITION_FOR_DATA_OFFSET = 12;
    private static int POSITION_FOR_CHECKSUM = 16;
    private static byte MAXIMUM_SEGMENT_SIZE_OPTION_LENGTH = 4;
    private static byte MAXIMUM_SEGMENT_OPTION_KIND = 2;
    private static byte WINDOW_SCALE_OPTION_KIND = 3;
    private static byte WINDOW_SCALE_OPTION_LENGTH = 3;
    private static byte WINDOW_SCALE_SHIFT_BYTES = 6;
    private static byte TCP_URG_BIT = (1<<5);
    private static byte TCP_ACK_BIT = (1<<4);
    private static byte TCP_PSH_BIT = (1<<3);
    private static byte TCP_RST_BIT = (1<<2);
    private static byte TCP_SYN_BIT = (1<<1);
    private static byte TCP_FIN_BIT = (1);


    @Override
    public byte[] createHeader(HashMap<String, Object> headerInfo) {
        short data_length = 0;
        byte[] data = null;
        if(headerInfo.get("data")!=null){
            data = (byte[]) headerInfo.get("data");
        }

        byte[] header_buf = new byte[HEADER_LENGTH];
        ByteBuffer byteBuffer = ByteBuffer.wrap(header_buf);

        if(headerInfo.get("src_port")==null){
            return null;
        }
        short srcPort = (short) headerInfo.get("src_port");
        byteBuffer.putShort(srcPort);

        if(headerInfo.get("dest_port")==null){
            return null;
        }
        short destPort = (short) headerInfo.get("dest_port");
        byteBuffer.putShort(destPort);

        if(headerInfo.get("seq_num")!=null){
            sequence_number = (int) headerInfo.get("seq_num");
        }

        if(headerInfo.get("ack_num")!=null){
            acknowledgement_number = (int) headerInfo.get("ack_num");
        }

        byteBuffer.putInt(sequence_number);
        byteBuffer.putInt(acknowledgement_number);
        short control_bits = 0;
        if(headerInfo.get("URG")!=null){
            control_bits|=(1<<5);
        }
        if(headerInfo.get("ACK")!=null){
            control_bits|=(1<<4);
        }
        if(headerInfo.get("PSH")!=null){
            control_bits|=(1<<3);
        }
        if (headerInfo.get("RST")!=null){
            control_bits|=(1<<2);
        }
        if(headerInfo.get("SYN")!=null){
            control_bits|=(1<<1);
        }
        if(headerInfo.get("FIN")!=null){
            control_bits|=(1);
        }
        byteBuffer.putShort(control_bits);
        System.out.println(Integer.toBinaryString(control_bits));

        char window = 65535;
        byteBuffer.putChar(window);
        short check_sum = 0;
        byteBuffer.putShort(check_sum);
        short urgent_pointer = 0;
        byteBuffer.putShort(urgent_pointer);

        byte[] maximum_segment_option = new byte[MAXIMUM_SEGMENT_SIZE_OPTION_LENGTH];
        ByteBuffer maximum_segment_buffer = ByteBuffer.wrap(maximum_segment_option);
        maximum_segment_buffer.put(MAXIMUM_SEGMENT_OPTION_KIND);
        maximum_segment_buffer.put(MAXIMUM_SEGMENT_SIZE_OPTION_LENGTH);
        short segment_size = 1460;
        maximum_segment_buffer.putShort(segment_size);

        byte[] window_scale_option = new byte[WINDOW_SCALE_OPTION_LENGTH];
        ByteBuffer window_scale_buffer = ByteBuffer.wrap(window_scale_option);
        window_scale_buffer.put(WINDOW_SCALE_OPTION_KIND);
        window_scale_buffer.put(WINDOW_SCALE_OPTION_LENGTH);
        window_scale_buffer.put(WINDOW_SCALE_SHIFT_BYTES);

        byte[] option_end = new byte[1];
        option_end[0] = 0;

        int total_length = data_length+header_buf.length+maximum_segment_option.length+window_scale_option.length+option_end.length;

        if(total_length%4!=0){
            total_length = (total_length/4+1)*4;
        }

        byte[] tcp_buffer = new byte[total_length];
        ByteBuffer buffer = ByteBuffer.wrap(tcp_buffer);
        buffer.put(header_buf);
        buffer.put(maximum_segment_option);
        buffer.put(window_scale_option);
        buffer.put(option_end);

        short data_offset = buffer.getShort(POSITION_FOR_DATA_OFFSET);
        data_offset |= (((total_length/4)&0x0F)<<12);
        System.out.println(Integer.toBinaryString(data_offset));
        buffer.putShort(POSITION_FOR_DATA_OFFSET,data_offset);
        check_sum = (short) comute_checksum(headerInfo,buffer);
        buffer.putShort(POSITION_FOR_CHECKSUM,check_sum);

        return buffer.array();
    }

    private long comute_checksum(HashMap<String,Object>headerInfo,ByteBuffer buffer){
        byte[] pseudo_header = new byte[PSEUDO_HEADER_LENGTH];
        ByteBuffer pseudo_header_buf = ByteBuffer.wrap(pseudo_header);
        byte[] src_addr = (byte[]) headerInfo.get("src_ip");
        byte[] dst_addr = (byte[]) headerInfo.get("dest_ip");
        pseudo_header_buf.put(src_addr);
        pseudo_header_buf.put(dst_addr);

        byte reserved = 0;
        pseudo_header_buf.put(reserved);
        pseudo_header_buf.put(TCP_PROTOCOL_NUMBER);

        short tcp_length = (short) buffer.array().length;
        pseudo_header_buf.putShort(tcp_length);

        byte[] total_buf = new byte[PSEUDO_HEADER_LENGTH+tcp_length];
        ByteBuffer total_buffer = ByteBuffer.wrap(total_buf);
        total_buffer.put(pseudo_header);
        total_buffer.put(buffer.array());

        return Utility.checksum(total_buf,total_buf.length);
    }

    @Override
    public HashMap<String, Object> handlePacket(Packet packet) {
        ByteBuffer buffer = ByteBuffer.wrap(packet.header);
        HashMap<String,Object>headerInfo = new HashMap<String, Object>();
        short src_port = buffer.getShort();
        headerInfo.put("src_port",src_port);
        short dest_port = buffer.getShort();
        headerInfo.put("dest_port",dest_port);
        int seq_num = buffer.getInt();
        headerInfo.put("seq_num",seq_num);
        int ack_num = buffer.getInt();
        headerInfo.put("ack_num",ack_num);

        short control_bits = buffer.getShort();
        if((control_bits&TCP_ACK_BIT)!=0){
            headerInfo.put("ACK",1);
        }

        if((control_bits&TCP_SYN_BIT)!=0){
            headerInfo.put("SYN",1);
        }

        if((control_bits&TCP_FIN_BIT)!=0){
            headerInfo.put("FIN",1);
        }

        short win_size = buffer.getShort();
        headerInfo.put("window",win_size);

        buffer.getShort();
        short urg_pointer = buffer.getShort();
        headerInfo.put("urg_ptr",urg_pointer);
        return headerInfo;
    }
}
