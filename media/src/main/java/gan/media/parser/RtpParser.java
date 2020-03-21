package gan.media.parser;

import gan.media.BufferInfo;

import java.nio.ByteBuffer;

public interface RtpParser{

    public void init();
    public void parse(byte channel, ByteBuffer packet, int offset, int length);
    public void stop();

    public static interface OnParserListener{
        public void onParsedPacket(byte channel,byte[] packet,int offset,int length,long time);
    }

    public static interface OnParserFrameListener{
        public void onParsedFrame(byte channel,ByteBuffer frame,BufferInfo frameInfo);
    }

}
