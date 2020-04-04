package gan.media.parser;

import java.nio.ByteBuffer;

public class PSRtpParser extends RtpParserImpl implements PsPacketParser.OnFrameListener {

    OnPsPacketListener mOnPsPacketListener;
    PsPacketParser.OnFrameListener mOnFrameListener;
    PsPacketParser mPsPacketParser;

    public PSRtpParser(){
        mPsPacketParser = new PsPacketParser();
        mPsPacketParser.setOnFrameListener(this);
    }

    public PSRtpParser setParserFrameListener(PsPacketParser.OnFrameListener frameListener) {
        this.mOnFrameListener = frameListener;
        return this;
    }

    public PSRtpParser setOnPsPacketListener(OnPsPacketListener listener) {
        this.mOnPsPacketListener = listener;
        return this;
    }

    @Override
    public void init() {
    }

    @Override
    public void onPlayload(byte channel, ByteBuffer packet, int offset, int length, long timestamp) {
        onPsPacket(packet,offset,length);
    }

    @Override
    public void stop() {
    }

    @Override
    public void onPsFrame(byte channel, byte[] data, int offset, int length, long pts) {
        if(mOnFrameListener!=null){
            mOnFrameListener.onPsFrame(channel, data, offset, length, pts);
        }
    }

    protected void onPsPacket(ByteBuffer packet, int offset, int length){
        if(mOnPsPacketListener!=null){
            if(mOnPsPacketListener.onPsPacket(packet, offset, length)){
                return;
            }
        }
        mPsPacketParser.parse(packet, offset, length);
    }

    public interface OnPsPacketListener{
        public boolean onPsPacket(ByteBuffer packet, int offset, int length);
    }
}
