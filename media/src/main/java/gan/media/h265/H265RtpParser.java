package gan.media.h265;

import gan.media.BufferInfo;
import gan.media.MediaApplication;
import gan.media.parser.RtpParserImpl;
import gan.web.config.MediaConfig;

import java.nio.ByteBuffer;

public class H265RtpParser extends RtpParserImpl {

    ByteBuffer mFrameBuffer;
    BufferInfo mFrameBufferInfo;
    ByteBuffer mIFrame;//缓存I帧
    BufferInfo mIFrameBufferInfo;
    OnParserListener mParserListenner;
    OnParserFrameListener mParserFrameListener;
    byte[] b2 = new byte[2];

    public H265RtpParser setParserListenner(OnParserListener parserListenner) {
        this.mParserListenner = parserListenner;
        return this;
    }

    public H265RtpParser setParserFrameListener(OnParserFrameListener parserFrameListener) {
        this.mParserFrameListener = parserFrameListener;
        return this;
    }

    @Override
    public void init() {
        MediaConfig config = MediaApplication.getMediaConfig();
        mFrameBuffer = ByteBuffer.allocate(config.rtspFrameBufferSize);
        mFrameBufferInfo = new BufferInfo();
        mIFrame = ByteBuffer.allocate(config.rtspFrameBufferSize);
    }

    @Override
    public void onPlayload(byte channel, ByteBuffer packet, int offset, int length, long timestamp) {
        if (parseH265Frame(channel,packet, offset, length,timestamp)) {
            if (H265Utils.isIFrame(mFrameBuffer, mFrameBufferInfo)) {
                synchronized (mIFrame) {
                    mIFrame.clear();
                    System.arraycopy(mFrameBuffer.array(), 0, mIFrame.array(), 0, mFrameBufferInfo.length);
                    mIFrame.position(mFrameBufferInfo.length);
                    mIFrameBufferInfo = mFrameBufferInfo.clone();
                    mIFrameBufferInfo.setOffset(0);
                }
            }
            onParsedFrame(channel, mFrameBuffer, mFrameBufferInfo);
            mFrameBuffer.clear();
            mFrameBufferInfo.reset();
        }
    }

    @Override
    public void stop() {
        if(mIFrame!=null){
            mIFrame.clear();
        }
        if(mFrameBuffer!=null){
            mFrameBuffer.clear();
            mFrameBufferInfo.reset();
        }
    }

    public boolean parseH265Frame(byte channel, ByteBuffer packet, int start, int len, long timestamp){
        packet.position(start);
        packet.get(b2);
        byte FUindicator = (byte) (b2[0]>>1);
        byte type = (byte)(FUindicator&0x3f);
        byte FuHeader = packet.get();
        byte FuType = (byte)((FuHeader)&0x3f);
        byte nal = (byte)((b2[0]&0x01)|((FuType<<1))); // FU_A nal
        if(type==49){
            start+=3;
            len-=3;
            if((FuHeader&0x80)==0x80){//开始
                packet.position(start);
                mFrameBuffer.clear();
                mFrameBufferInfo.reset();
                mFrameBuffer.putInt(1);
                mFrameBuffer.put(nal);
                mFrameBuffer.put(b2[1]);
                System.arraycopy(packet.array(), start, mFrameBuffer.array(), 6, len);
                int packetLen = len+6;
                mFrameBufferInfo.setChannel(channel).setTime(timestamp).setOffset(0).setLength(packetLen);
                onParsedPacket(channel,mFrameBuffer.array(),0, packetLen,timestamp);
                mFrameBuffer.position(packetLen);
            } else {
                int packetLen = len;
                int temp = mFrameBuffer.position();
                System.arraycopy(packet.array(), start, mFrameBuffer.array(), temp, packetLen);
                mFrameBufferInfo.setTime(timestamp).lengths(packetLen);
                onParsedPacket(channel, mFrameBuffer.array(), temp, packetLen,timestamp);
                mFrameBuffer.position(temp+packetLen);
            }
            return (FuHeader&0x40)==0x40;
        }else {// 单包数据
            mFrameBuffer.clear();
            mFrameBufferInfo.reset();
            mFrameBuffer.putInt(1);//开始码
            System.arraycopy(packet.array(), start, mFrameBuffer.array(), 4, len);
            int packetLen= len+4;
            mFrameBufferInfo.setChannel(channel).setTime(timestamp).setOffset(0).setLength(packetLen);
            onParsedPacket(channel,mFrameBuffer.array(),0, packetLen, timestamp);
            mFrameBuffer.position(packetLen);
            return true;
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
