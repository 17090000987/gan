package gan.media.parser;

import gan.media.BufferInfo;

import java.nio.ByteBuffer;

public class AACRtpParser extends RtpParserImpl{

    BufferInfo mAACBufferInfo;
    OnParserListener mParserListenner;
    OnParserFrameListener mParserFrameListener;

    public AACRtpParser(){

    }

    public AACRtpParser setParserListenner(OnParserListener parserListenner) {
        this.mParserListenner = parserListenner;
        return this;
    }

    public AACRtpParser setParserFrameListener(OnParserFrameListener parserFrameListener) {
        this.mParserFrameListener = parserFrameListener;
        return this;
    }

    @Override
    public void init() {
        mAACBufferInfo = new BufferInfo();
    }

    @Override
    public void onPlayload(byte channel, ByteBuffer packet, int offset, int length, long timestamp) {
        offset += 4;//去掉rtp头和aac头
        int packetLen = length-4;
        mAACBufferInfo.setChannel(channel).setTime(timestamp).setOffset(offset).setLength(packetLen);
        onParsedPacket(channel, packet.array(), mAACBufferInfo.offset, mAACBufferInfo.length, mAACBufferInfo.time);
        onParsedFrame(channel, packet, mAACBufferInfo);
        mAACBufferInfo.reset();
    }

    @Override
    public void stop() {
        if(mAACBufferInfo!=null){
            mAACBufferInfo.reset();
        }
    }

    protected void onParsedPacket(byte channel, byte[] packet, int offset, int length, long time){
        if(mParserListenner!=null){
            mParserListenner.onParsedPacket(channel,packet,offset,length,time);
        }
    }

    protected void onParsedFrame(byte channel,ByteBuffer frame,BufferInfo frameInfo){
        if(mParserFrameListener!=null){
            mParserFrameListener.onParsedFrame(channel,frame,frameInfo);
        }
    }

}
