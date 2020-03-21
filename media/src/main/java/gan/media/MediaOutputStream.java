package gan.media;

import java.nio.ByteBuffer;

public interface MediaOutputStream {
    public void init();
    public void write(byte channel, ByteBuffer packet, BufferInfo bufferInfo)throws Exception;
    public void close();
}
