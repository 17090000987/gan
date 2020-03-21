package gan.media.mp4;
import gan.media.BufferInfo;

import java.io.IOException;
import java.nio.ByteBuffer;

public interface MP4StreamMuxer extends Mp4Muxer{
    public void write(byte channel, ByteBuffer packet, BufferInfo bufferInfo) throws IOException;
}