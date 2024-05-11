package org.jianghu.app.common;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.*;

/**
 * 由于流只能读取一次，所以使用此包装类对HttpServletRequest对象进行包装，读取完之后再将
 * 内容塞回去，不影响后续springmvc的参数处理。
 */
public class RequestWrapper extends HttpServletRequestWrapper {
    private String body;
    public RequestWrapper(HttpServletRequest request) {
        super(request);
        if (request.getHeader("Content-Type") != null
                && request.getHeader("Content-Type").contains("multipart/form-data")){
            try{
                request.getParts();
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        StringBuilder stringBuilder = new StringBuilder();
        try (InputStream inputStream = request.getInputStream();
             BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream))){
            char[] charBuffer = new char[128];
            int bytesRead = -1;
            while ((bytesRead = bufferedReader.read(charBuffer)) > 0) {
                stringBuilder.append(charBuffer, 0, bytesRead);
            }

        }catch (NullPointerException ex){
            stringBuilder.append("");
        } catch (Exception ex) {

        }
        body = stringBuilder.toString();
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(body.getBytes());
        ServletInputStream servletInputStream = new ServletInputStream() {
            @Override
            public boolean isFinished() {
                return false;
            }
            @Override
            public boolean isReady() {
                return false;
            }
            @Override
            public void setReadListener(ReadListener readListener) {
            }
            @Override
            public int read() throws IOException {
                return byteArrayInputStream.read();
            }
        };
        return servletInputStream;

    }

    @Override
    public BufferedReader getReader() throws IOException {
        return new BufferedReader(new InputStreamReader(this.getInputStream()));
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }
}
