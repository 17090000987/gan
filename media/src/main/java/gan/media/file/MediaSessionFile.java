package gan.media.file;

import gan.core.file.FileHelper;
import gan.core.system.SystemUtils;
import gan.media.MediaSession;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;

public class MediaSessionFile implements MediaSession<String> {

    String mFilePath;
    FileOutputStream fos;

    public MediaSessionFile(String filePath){
        mFilePath = filePath;
    }

    @Override
    public String getSessionId() {
        return "file/"+mFilePath;
    }

    @Override
    public String getSession() {
        return mFilePath;
    }

    @Override
    public void sendMessage(String message) throws IOException {
        checkFileOutputStream();
        if(fos!=null){
            fos.write(message.getBytes(Charset.forName("utf-8")));
        }
    }

    @Override
    public void sendMessage(int b) throws IOException {
        checkFileOutputStream();
        if(fos!=null){
            fos.write(b);
        }
    }

    @Override
    public void sendMessage(byte[] b) throws IOException {
        checkFileOutputStream();
        if(fos!=null){
            fos.write(b);
        }
    }

    @Override
    public void sendMessage(byte[] b, int off, int len) throws IOException {
        checkFileOutputStream();
        if(fos!=null){
            fos.write(b,off,len);
        }
    }

    @Override
    public void close() throws IOException {
        SystemUtils.close(fos);
    }

    public void checkFileOutputStream(){
        if(fos==null){
            try {
                fos = FileHelper.createFileOutputStream(mFilePath);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
