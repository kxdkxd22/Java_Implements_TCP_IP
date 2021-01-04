package Application;

public class MIMETextPlainMediaEcnapusulation {
    private String boundary_string = "----WebFormBoundaryAAABBBCCCDDD"; //用于传递数据的分割标志字符串
    private String post_file_name = "";
    private String content_part_header = "Content-Disposition: form-data; name=\"button\"; filename=\"";
    private String content_type = "Content-Type: text/plain\r\n\r\n";
    private String content_part = "";
    private String last_boundary = "\r\n--" + boundary_string + "--\r\n";

    public MIMETextPlainMediaEcnapusulation(String file_name) {
        this.post_file_name = file_name; //要上传的文件名


    }

    private void add_content_header() { //添加用于分割不同数据部分的boundary
        this.content_part += "--";
        this.content_part += boundary_string;
        this.content_part += "\r\n";
        this.content_part += content_part_header;
        this.content_part += post_file_name;
        this.content_part += "\r\n";
        this.content_part += content_type;
    }

    public void add_content(String content) { //调用该接口设置要上传给服务器的内容
        this.add_content_header();

        this.content_part += content;
        this.content_part += "\r\n";
        this.content_part += "--";
        this.content_part += boundary_string;
        this.content_part += "\r\n";
    }

    private void add_last_content_part() { //模拟最后upload数据部分，这部分与数据传输无关，但与服务器对接收数据的解读需要
        String last_content_disposition = "Content-Disposition: form-data; name=\"button\"\r\n\r\n";
        content_part += last_content_disposition;
        String content = "Upload\r\n";
        content_part += content;
        content_part += last_boundary;
    }

    public String get_mime_part() {
        this.add_last_content_part();
        return  content_part;
    }

    public String get_boundary_string() {
        return boundary_string;
    }
}
