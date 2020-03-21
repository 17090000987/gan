package gan.media.rtsp;

import gan.log.DebugLog;
import gan.core.system.server.ServerPlugin;
import gan.media.BufferInfo;
import gan.media.MediaOutputStreamRunnable;
import gan.media.h26x.HUtils;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

public class RtspFrame2RtpPlugin extends ServerPlugin<RtspMediaServer> implements RtspMediaServer.OnFrameCallBackPlugin {

    final static String Tag = RtspFrame2RtpPlugin.class.getName();
    private static final AtomicInteger sAtomSsrc = new AtomicInteger(1000);
    public static int generateSsrc(){
        if(sAtomSsrc.get() >= Integer.MAX_VALUE){
            sAtomSsrc.set(1000);
        }
        return sAtomSsrc.incrementAndGet();
    }

    /**
     * h264分包大小 MTU1500,还要出去头
     */
    final static int PackSize = 1440;
    int SSRCA;short rtpSequenceA;
    int SSRCV;short rtpSequenceV;
    ByteBuffer mPacket = ByteBuffer.allocate(PackSize);

    @Override
    protected void onCreate(RtspMediaServer server) {
        super.onCreate(server);
        DebugLog.info("onCreate");
        SSRCA = generateSsrc();
        SSRCV = generateSsrc();
    }

    BufferInfo info = new BufferInfo();
    @Override
    public void onFrame(byte channel, ByteBuffer frame, BufferInfo bufferInfo) {
        info.reset();
        info.copy(bufferInfo);
        //rtp封包还有问题
        int extSize;
        int rtpPacketLen=0;
        byte playLoad = (byte) (channel==0? 96:97);
        int rtpHeadLen = 12;
        extSize = rtpExSize(channel);
        rtpHeadLen+=extSize;
        int temp = PackSize-rtpHeadLen;
        if(info.length<=temp){//单包
            mPacket.clear();
            rtpPacketLen+= putRtpHead(channel, mPacket,true, playLoad, info.time, extSize, rtpExData(channel));
            mPacket.position(rtpPacketLen);
            if(channel == 0){//h264
                int startCodeSize = HUtils.startCodeSize(frame.array(), info.offset, info.length);
                info.offsets(startCodeSize);
                info.length-=startCodeSize;
                mPacket.put(frame.array(), info.offset, info.length);
                rtpPacketLen+=info.length;
                onRtpPacket(channel, mPacket.array(),0, rtpPacketLen, info.time);
            }else{
                info.offsets(7);
                info.length-=7;
                rtpPacketLen+=putAACHead(mPacket, info.length);
                mPacket.put(frame.array(), info.offset, info.length);
                rtpPacketLen+=info.length;
                onRtpPacket(channel, mPacket.array(),0, rtpPacketLen, info.time);
            }
        }else{
            if(channel==0){//h264 fu_a
                int startSize = HUtils.startCodeSize(frame.array(), info.offset, info.length);
                info.offsets(startSize);
                info.length-=startSize;
                frame.position(info.offset);
                byte NAL = frame.get();
                info.offsets(1);
                info.length-=1;
                byte FUindicator = (byte)(NAL&0xe0|0x1c);//这里是28，即FU-A
                boolean start=true,end;
                while(info.length>0){
                    mPacket.clear();
                    rtpPacketLen = 0;
                    rtpHeadLen = 12;
                    extSize = rtpExSize(channel);
                    rtpHeadLen+=extSize;
                    temp = PackSize - rtpHeadLen -2;
                    end = info.length<=temp;
                    rtpPacketLen+=putRtpHead(channel,mPacket, end, playLoad, info.time, extSize, rtpExData(channel));
                    mPacket.position(rtpPacketLen);
                    mPacket.put(FUindicator);
                    if(start){
                        mPacket.put((byte)(NAL&0x1f|0x80));
                    }else if(end) {
                        mPacket.put((byte)(NAL&0x5f));
                    }else{
                        mPacket.put((byte)(NAL&0x1f));
                    }
                    rtpPacketLen+=2;
                    temp = info.length<temp?info.length:temp;
                    mPacket.put(frame.array(), info.offset, temp);
                    rtpPacketLen+=temp;
                    info.offsets(temp);
                    info.length-=temp;
                    onRtpPacket(channel,mPacket.array(),0,rtpPacketLen, info.time);
                    start = false;
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    protected void onRtpPacket(byte channel,byte[] packet,int offset,int length,long time){
        mServer.outputPacketStream(MediaOutputStreamRunnable.PacketType_Rtp,channel, packet, offset, length, time);
    }

    public int putRtpHead(byte channel, ByteBuffer packet, boolean end, byte playLoad, long timeSample, int extSize, byte[] extData){
        packet.put((byte)0x80);
        if(end){
            packet.put((byte)(playLoad|0x80));
        }else{
            packet.put(playLoad);
        }
        packet.putShort(getRtpSequence(channel));
        packet.putInt((int) timeSample);
        packet.putInt(getSSRC(channel));
        return 12;
    }

    public int putAACHead(ByteBuffer packet,int aacLen){
        packet.put((byte)0x00);
        packet.put((byte)0x10);
        packet.putShort((short)(aacLen<<3));
        return 4;
    }

    public int rtpExSize(byte channel){
        return 0;
    }

    public byte[] rtpExData(byte channel){
        return null;
    }

    public int getSSRC(byte channel){
        if(channel == 0){
            return SSRCV;
        }
        return SSRCA;
    }

    public short getRtpSequence(byte channel){
        if(channel == 0){
            return rtpSequenceV++;
        }
        return  rtpSequenceA++;
    }

}
