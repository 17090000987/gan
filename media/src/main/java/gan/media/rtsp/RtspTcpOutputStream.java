package gan.media.rtsp;

import gan.core.system.SystemUtils;
import gan.media.BufferInfo;
import gan.media.MediaOutputStream;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

public class RtspTcpOutputStream implements MediaOutputStream {

    OutputStream mOutputStream;
    ByteBuffer mByteBuffer;

    public RtspTcpOutputStream(OutputStream outputStream){
        mOutputStream = outputStream;
    }

    @Override
    public void init() {
        mByteBuffer = ByteBuffer.allocate(1500);
    }

    public void write(byte channel, ByteBuffer packet, BufferInfo info) throws Exception {
        mByteBuffer.clear();
        mByteBuffer.put((byte)0x24);
        mByteBuffer.put(channel);
        mByteBuffer.putShort((short) info.length);
        mByteBuffer.put(packet.array(),0, info.length);
        mOutputStream.write(mByteBuffer.array(),0, mByteBuffer.position());
    }

    public void write(byte b[], int off, int len)throws IOException{
        mOutputStream.write(b,off,len);
    }

    @Override
    public void close() {
        SystemUtils.close(mOutputStream);
    }

}
