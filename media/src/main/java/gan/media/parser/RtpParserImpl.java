package gan.media.parser;

import gan.media.utils.MediaUtils;
import gan.core.system.SystemUtils;

import java.nio.ByteBuffer;

public abstract class RtpParserImpl implements RtpParser{

    byte[] b2 = new byte[2];

    @Override
    public void init() {

    }

    @Override
    public void parse(byte channel, ByteBuffer packet, int offset, int length) {
        int rtpHeadLen = 12;
        packet.position(offset);
        byte first = packet.get();
        if((first&0x20)==0x20){//padding
            int paddingCount = SystemUtils.byteToUnsignInt8(packet.array(), offset+length-1);
            length-=paddingCount;
        }
        if((first&0x10)==0x10){//ex
            packet.position(rtpHeadLen);
            packet.getShort();
            packet.get(b2);
            int exCount = SystemUtils.byteToUnsignInt16(b2,0);
            rtpHeadLen+=(1+exCount*32);
        }

        packet.position(offset+4);
        long timestamp = MediaUtils.getUnsignedIntt(packet.getInt());
        int start = offset+rtpHeadLen;

        onPlayload(channel, packet, start, length -start, timestamp);
    }

    @Override
    public void stop() {

    }

    public abstract void onPlayload(byte channel, ByteBuffer packet, int offset, int length, long timestamp);
}
