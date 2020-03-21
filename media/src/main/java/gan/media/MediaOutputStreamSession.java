package gan.media;

import gan.core.system.SystemUtils;

import java.nio.ByteBuffer;

public class MediaOutputStreamSession implements MediaOutputStream{

    MediaSession mMediaSession;

    public MediaOutputStreamSession(MediaSession mediaSession){
        mMediaSession = mediaSession;
    }

    @Override
    public void init() {

    }

    @Override
    public void write(byte channel, ByteBuffer packet, BufferInfo bufferInfo) throws Exception {
        mMediaSession.sendMessage(packet.array(), bufferInfo.offset,bufferInfo.length);
    }

    @Override
    public void close() {
        SystemUtils.close(mMediaSession);
    }
}
