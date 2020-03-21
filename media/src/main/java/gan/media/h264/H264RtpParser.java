package gan.media.h264;

import gan.media.BufferInfo;
import gan.media.MediaApplication;
import gan.media.parser.RtpParserImpl;
import gan.web.config.MediaConfig;

import java.nio.ByteBuffer;

public class H264RtpParser extends RtpParserImpl {

    ByteBuffer mFrameBuffer;
    BufferInfo mFrameBufferInfo;
    ByteBuffer mIFrame;//缓存I帧
    BufferInfo mIFrameBufferInfo;
    OnParserListener mParserListenner;
    OnParserFrameListener mParserFrameListener;

    public H264RtpParser setParserListenner(OnParserListener parserListenner) {
        this.mParserListenner = parserListenner;
        return this;
    }

    public H264RtpParser setParserFrameListener(OnParserFrameListener parserFrameListener) {
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
        if (parseH264Frame(channel,packet, offset, length,timestamp)) {
            if (H264Utils.isIFrame(mFrameBuffer, mFrameBufferInfo)) {
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

    public boolean parseH264Frame(byte channel,ByteBuffer packet, int start, int len, long timestamp){
        packet.position(start);
        byte FUindicator = packet.get();
        byte type = (byte)(FUindicator&0x1f);
        byte FuHeader = packet.get();
        byte flag=(byte)(FuHeader&0xe0);
        byte nal_fua = (byte)((FUindicator&0xe0)|(FuHeader&0x1f)); // FU_A nal
        if(type==28){
            if  ((flag&0x80)==0x80){//开始
                packet.position(start);
                mFrameBuffer.clear();
                mFrameBufferInfo.reset();
                mFrameBuffer.putInt(1);
                mFrameBuffer.put(nal_fua);
                System.arraycopy(packet.array(),start+2, mFrameBuffer.array(), 4+1, len-2);
                int packetLen = len-2+4+1;
                mFrameBufferInfo.setChannel(channel).setTime(timestamp).setOffset(0).setLength(packetLen);
                onParsedPacket(channel,mFrameBuffer.array(),0, packetLen,timestamp);
                mFrameBuffer.position(packetLen);
            } else {
                int packetLen = len-2;
                int temp = mFrameBuffer.position();
                System.arraycopy(packet.array(),start+2, mFrameBuffer.array(), temp, packetLen);
                mFrameBufferInfo.setTime(timestamp).lengths(packetLen);
                onParsedPacket(channel, mFrameBuffer.array(), temp, packetLen,timestamp);
                mFrameBuffer.position(temp+packetLen);
            }
        }else {// 单包数据
            mFrameBuffer.clear();
            mFrameBufferInfo.reset();
            mFrameBuffer.putInt(1);//开始码
            System.arraycopy(packet.array(), start, mFrameBuffer.array(), 4, len);
            int packetLen= len+4;
            mFrameBufferInfo.setChannel(channel).setTime(timestamp).setOffset(0).setLength(packetLen);
            onParsedPacket(channel,mFrameBuffer.array(),0, packetLen, timestamp);
            mFrameBuffer.position(packetLen);
        }
        return (flag&0x40)==0x40;
    }

    protected void onParsedPacket(byte channel,byte[] packet,int offset,int length,long time){
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
