package gan.media.mp4;

import gan.core.file.FileHelper;
import gan.core.system.SystemUtils;
import gan.media.MediaConfig;

import java.io.FileOutputStream;
import java.io.IOException;

public class Mp4MeidaOutputStreamFile extends Mp4MeidaOutputStream{

    FileOutputStream mFileOutputStream;
    String  mFilePath;

    public Mp4MeidaOutputStreamFile(String url,String filePath) {
        super(url, null);
        mFilePath = filePath;
    }

    public Mp4MeidaOutputStreamFile(String url,String filePath,MediaConfig config) {
        super(url, null, config);
        mFilePath = filePath;
    }

    @Override
    public void init() {
        super.init();
        try {
            mFileOutputStream = FileHelper.createFileOutputStream(mFilePath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onMp4(byte[] data, int length) {
        super.onMp4(data, length);
        if(mFileOutputStream!=null){
            try {
                mFileOutputStream.write(data,0, length);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void close() {
        super.close();
        SystemUtils.close(mFileOutputStream);
    }

}
